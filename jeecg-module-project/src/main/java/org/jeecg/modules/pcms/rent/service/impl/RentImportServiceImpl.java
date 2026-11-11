package org.jeecg.modules.pcms.rent.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.pcms.rent.dto.ImportResultVO;
import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TInstallInfo;
import org.jeecg.modules.pcms.rent.entity.TInstallType;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.entity.TRent;
import org.jeecg.modules.pcms.rent.entity.TRentPriceType;
import org.jeecg.modules.pcms.rent.parser.RentExcelParser;
import org.jeecg.modules.pcms.rent.service.IRentImportService;
import org.jeecg.modules.pcms.rent.service.RentTenantResolver;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITInstallInfoService;
import org.jeecg.modules.pcms.rent.service.ITInstallTypeService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.ITRentPriceTypeService;
import org.jeecg.modules.pcms.rent.service.ITRentService;
import org.jeecg.modules.pcms.rent.util.CellUtil;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.jeecg.modules.pcms.rent.util.ExcelRowReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金台账导入实现。
 *
 * 五个项目的差异全部收敛在 Parser 里，本类只做五件事：
 * 1. 按 regionCode 选 Parser，读 Excel，逐行解析成 ImportRowDTO
 * 2. 匹配项目 / 安装类型 / 租金档位三张字典
 * 3. 处理文件内重复户号：出现多次的户号依次加 -1 / -2 / -3 后缀
 * 4. 按 (项目, 户号) 判断新增还是更新，更新时先清掉旧的安装和租金再重建，
 *    保证同一个文件反复导入结果一致，不会产生重复数据
 * 5. 收集失败和告警明细同步返回，不落库
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentImportServiceImpl implements IRentImportService {

    /** 缺发电户号时的占位前缀，后面跟 Excel 行号 */
    private static final String NO_USER_NO_PREFIX = "待补户号-";
    /** 缺农户姓名时的占位值 */
    private static final String NO_NAME = "待补姓名";

    @Autowired
    private List<RentExcelParser> parsers;
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private ITInstallTypeService installTypeService;
    @Autowired
    private ITRentPriceTypeService rentPriceTypeService;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITInstallInfoService installInfoService;
    @Autowired
    private ITRentService rentService;
    @Autowired
    private RentTenantResolver tenantResolver;
    @Autowired
    private RentRoleGuard roleGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcel(MultipartFile file, String regionCode) {
        roleGuard.requireFarmerOps("导入农户信息");
        long start = System.currentTimeMillis();
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择要导入的文件");
        }
        RentExcelParser parser = findParser(regionCode);

        ImportResultVO result = new ImportResultVO()
                .setFileName(file.getOriginalFilename())
                .setRegionCode(regionCode);

        // ---------- 1. 读 Excel ----------
        ExcelRowReader.Result read;
        try (InputStream is = file.getInputStream()) {
            read = ExcelRowReader.read(is, parser.sheetName());
        } catch (Exception e) {
            log.error("读取Excel失败 region={} file={}", regionCode, file.getOriginalFilename(), e);
            throw new JeecgBootException("读取 Excel 失败：" + e.getMessage());
        }
        result.setSheetName(read.getSheetName());
        List<ExcelRow> rows = read.getRows();
        result.setTotal(rows.size());

        // ---------- 2. 装载三张字典 ----------
        Map<String, TProject> projectByName = new HashMap<>();
        List<TProject> projects = projectService.list(new LambdaQueryWrapper<TProject>()
                .eq(TProject::getRegionCode, regionCode));
        if (projects.isEmpty()) {
            throw new JeecgBootException("t_project 中没有大区 " + regionCode + " 的项目，请先执行建表脚本的初始化数据");
        }
        for (TProject p : projects) {
            projectByName.put(normalize(p.getProjectName()), p);
        }

        Map<String, TInstallType> installTypeByCode = new HashMap<>();
        for (TInstallType t : installTypeService.list(new LambdaQueryWrapper<TInstallType>()
                .eq(TInstallType::getRegionCode, regionCode))) {
            installTypeByCode.put(t.getTypeCode(), t);
        }

        Map<String, TRentPriceType> priceTypeByCode = new HashMap<>();
        for (TRentPriceType t : rentPriceTypeService.list(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode))) {
            priceTypeByCode.put(t.getTypeCode(), t);
        }

        // 租户字典（需求第14条）。sys_tenant 只有十几行，一次读全按名称建索引，
        // 避免在几万行的循环里逐行查库
        Map<String, Integer> tenantIdByName = tenantResolver.loadTenantIds();

        // ---------- 3. 逐行解析 ----------
        List<ImportRowDTO> parsed = new ArrayList<>();
        Map<String, Integer> noCount = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            ExcelRow row = rows.get(i);
            int rowNo = i + 2;  // Excel 里的真实行号：表头占第1行
            if (row.isBlank()) {
                result.setSkipCount(result.getSkipCount() + 1);
                continue;
            }
            ImportRowDTO dto;
            try {
                dto = parser.parse(row, rowNo);
            } catch (Exception e) {
                result.addError(rowNo, null, "解析失败：" + e.getMessage());
                continue;
            }
            if (dto == null) {
                result.setSkipCount(result.getSkipCount() + 1);
                continue;
            }
            if (dto.getPowerUserNo() == null) {
                // 没有户号又没有任何安装数据，是尾部垃圾行，直接跳过
                if (dto.getInstalls().isEmpty()) {
                    result.setSkipCount(result.getSkipCount() + 1);
                    continue;
                }
                // 有块数有租金却确实漏了户号，生成占位户号先落库，标告警等人工补
                dto.setPowerUserNo(NO_USER_NO_PREFIX + rowNo);
                result.addWarning(rowNo, dto.getPowerUserNo(),
                        "该行没有发电户号，已按行号生成占位户号，请人工补录后重新导入");
            }

            // 户号是从上方合并单元格继承来的，说明本行和上一行是同一个电站，
            // 只是把不同房型分成了多行写。合并成一条记录，容量和块数累加。
            // 河南行71-72（洼张小学）、行73-74（康洼小学）、行77-79（老窝镇政府）就是这种
            ImportRowDTO last = parsed.isEmpty() ? null : parsed.get(parsed.size() - 1);
            if (row.isUserNoFromMerge() && last != null
                    && dto.getPowerUserNo().equals(last.getPowerUserNo())) {
                last.getInstalls().addAll(dto.getInstalls());
                last.setTotalCapacity(add(last.getTotalCapacity(), dto.getTotalCapacity()));
                last.setTotalBlocks(add(last.getTotalBlocks(), dto.getTotalBlocks()));
                result.addWarning(rowNo, dto.getPowerUserNo(),
                        "该行与上一行在 Excel 中共用同一个发电户号（合并单元格），已合并为同一条电站记录");
                result.setMergedRowCount(result.getMergedRowCount() + 1);
                continue;
            }

            parsed.add(dto);
            noCount.merge(dto.getPowerUserNo(), 1, Integer::sum);
        }

        // ---------- 4. 文件内重复户号加后缀 ----------
        // 项目经理决策：同一户号出现多次时写成 1230000-1 / 1230000-2 / 1230000-3
        Map<String, Integer> seq = new HashMap<>();
        for (ImportRowDTO dto : parsed) {
            String no = dto.getPowerUserNo();
            if (noCount.getOrDefault(no, 0) > 1) {
                int n = seq.merge(no, 1, Integer::sum);
                dto.setPowerUserNo(no + "-" + n);
                result.addWarning(dto.getRowNo(), dto.getPowerUserNo(),
                        "发电户号 " + no + " 在文件中重复出现，已加后缀区分");
            }
        }

        // ---------- 5. 落库 ----------
        String operator = currentUser();
        Date now = new Date();
        for (ImportRowDTO dto : parsed) {
            try {
                saveRow(dto, regionCode, projectByName, installTypeByCode, priceTypeByCode,
                        tenantIdByName, result, operator, now);
            } catch (Exception e) {
                log.error("导入落库失败 row={} no={}", dto.getRowNo(), dto.getPowerUserNo(), e);
                result.addError(dto.getRowNo(), dto.getPowerUserNo(), e.getMessage());
            }
        }

        result.setCostMs(System.currentTimeMillis() - start);
        log.info("导入完成 region={} sheet={} 总{} 新增{} 更新{} 跳过{} 失败{} 耗时{}ms",
                regionCode, result.getSheetName(), result.getTotal(), result.getInsertCount(),
                result.getUpdateCount(), result.getSkipCount(), result.getFailCount(), result.getCostMs());
        return result;
    }

    /**
     * 单行落库。已存在则更新农户主表并重建其安装和租金，保证重复导入幂等
     */
    private void saveRow(ImportRowDTO dto, String regionCode,
                         Map<String, TProject> projectByName,
                         Map<String, TInstallType> installTypeByCode,
                         Map<String, TRentPriceType> priceTypeByCode,
                         Map<String, Integer> tenantIdByName,
                         ImportResultVO result, String operator, Date now) {

        // 匹配项目。所属项目为空时，若该大区只有一个项目就自动归属
        TProject project = projectByName.get(normalize(dto.getProjectName()));
        if (project == null && projectByName.size() == 1) {
            project = projectByName.values().iterator().next();
        }
        if (project == null) {
            throw new IllegalArgumentException("所属项目[" + dto.getProjectName()
                    + "]在 t_project 中不存在，请先补充项目档案");
        }

        // 农户主表
        TFarmerStation exists = farmerStationService.getOne(new LambdaQueryWrapper<TFarmerStation>()
                .eq(TFarmerStation::getProjectId, project.getId())
                .eq(TFarmerStation::getPowerUserNo, dto.getPowerUserNo())
                .eq(TFarmerStation::getDelFlag, 0)
                .last("limit 1"), false);

        // 农户姓名是非空字段，但河南有 9 行客户姓名是空的。
        // 依次回退到银行卡姓名、占位值，不能让整行因为缺个姓名就全丢了
        String farmerName = dto.getFarmerName();
        if (farmerName == null) {
            farmerName = dto.getBankCardName();
        }
        if (farmerName == null) {
            farmerName = NO_NAME;
            result.addWarning(dto.getRowNo(), dto.getPowerUserNo(),
                    "客户姓名和银行卡姓名都为空，已填占位值[" + NO_NAME + "]，请人工补录");
        }

        TFarmerStation station = exists == null ? new TFarmerStation() : exists;

        // 需求第14条：农户必须归属到租户，否则普通用户查不到自己导入的数据
        Integer tenantId = tenantResolver.resolve(regionCode, project, tenantIdByName);

        station.setFarmerName(farmerName)
                .setIdCard(dto.getIdCard())
                .setBankCardNo(dto.getBankCardNo())
                .setBankCardName(dto.getBankCardName())
                .setInstallAddr(dto.getInstallAddr())
                .setPowerUserNo(dto.getPowerUserNo())
                .setProjectId(project.getId())
                .setRegionCode(regionCode)
                .setTenantId(tenantId)
                .setStationCode(dto.getStationCode())
                .setSalesman(dto.getSalesman())
                .setGridDate(dto.getGridDate())
                .setActualGenDate(dto.getActualGenDate())
                // 租金截止日期 = 并网日期 + 租期年限 - 1天
                // 一年算"并网日到次年同月同日的前一天"，2025-03-20并网则第1年到2026-03-19
                .setRentDeadline(CellUtil.rentDeadline(dto.getGridDate(),
                        project.getLeaseYears() == null ? 25 : project.getLeaseYears()))
                .setTotalCapacity(dto.getTotalCapacity())
                .setTotalBlocks(dto.getTotalBlocks())
                .setDelFlag(0)
                .setUpdateBy(operator)
                .setUpdateTime(now);

        if (exists == null) {
            station.setCreateBy(operator).setCreateTime(now);
            farmerStationService.save(station);
            result.setInsertCount(result.getInsertCount() + 1);
        } else {
            farmerStationService.updateById(station);
            result.setUpdateCount(result.getUpdateCount() + 1);
            // 重建子表，避免重复导入累积出多份安装和租金
            List<TInstallInfo> old = installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                    .eq(TInstallInfo::getStationId, station.getId()));
            if (!old.isEmpty()) {
                List<Long> oldIds = new ArrayList<>();
                for (TInstallInfo o : old) {
                    oldIds.add(o.getId());
                }
                rentService.remove(new LambdaQueryWrapper<TRent>().in(TRent::getInstallId, oldIds));
                installInfoService.removeByIds(oldIds);
            }
        }

        if (dto.getGridDate() == null) {
            result.addWarning(dto.getRowNo(), dto.getPowerUserNo(), "并网日期为空，租金截止日期无法计算");
        }

        // 安装信息 + 租金
        Set<String> missingType = new HashSet<>();
        for (ImportRowDTO.InstallDTO in : dto.getInstalls()) {
            TInstallType type = installTypeByCode.get(in.getInstallTypeCode());
            if (type == null) {
                missingType.add(in.getInstallTypeCode());
                continue;
            }
            boolean multi = CellUtil.isMultiModel(in.getModuleModel());
            TInstallInfo install = new TInstallInfo()
                    .setStationId(station.getId())
                    .setFarmerName(station.getFarmerName())
                    .setProjectId(project.getId())
                    .setPowerUserNo(station.getPowerUserNo())
                    .setInstallTypeId(type.getId())
                    .setModuleModel(in.getModuleModel())
                    .setIsMultiModel(multi ? 1 : 0)
                    .setBlockCount(in.getBlockCount())
                    .setCapacityKw(CellUtil.capacityKw(in.getModuleModel(), in.getBlockCount()))
                    .setDelFlag(0)
                    .setCreateBy(operator).setCreateTime(now)
                    .setUpdateBy(operator).setUpdateTime(now);
            installInfoService.save(install);

            if (multi) {
                result.addWarning(dto.getRowNo(), dto.getPowerUserNo(),
                        type.getTypeName() + " 组件型号为多型号混写[" + in.getModuleModel() + "]，需人工拆分");
            }

            if (in.getRents().isEmpty()) {
                result.addWarning(dto.getRowNo(), dto.getPowerUserNo(),
                        type.getTypeName() + " 有 " + in.getBlockCount() + " 块但没有任何租金单价");
                continue;
            }
            List<TRent> rents = new ArrayList<>();
            for (ImportRowDTO.RentDTO r : in.getRents()) {
                TRentPriceType priceType = priceTypeByCode.get(r.getPriceTypeCode());
                if (priceType == null) {
                    throw new IllegalArgumentException("租金档位[" + r.getPriceTypeCode()
                            + "]在 t_rent_price_type 中不存在");
                }
                rents.add(new TRent()
                        .setStationId(station.getId())
                        .setInstallId(install.getId())
                        .setFarmerName(station.getFarmerName())
                        .setPowerUserNo(station.getPowerUserNo())
                        .setProjectId(project.getId())
                        .setPriceTypeId(priceType.getId())
                        .setUnitPrice(r.getUnitPrice())
                        .setAmount(r.getUnitPrice().multiply(new BigDecimal(in.getBlockCount())))
                        .setDelFlag(0)
                        .setCreateBy(operator).setCreateTime(now)
                        .setUpdateBy(operator).setUpdateTime(now));
            }
            rentService.saveBatch(rents);
        }
        if (!missingType.isEmpty()) {
            throw new IllegalArgumentException("安装类型 " + missingType
                    + " 在 t_install_type 中不存在，请先补充字典");
        }
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        if (a == null) {
            return b;
        }
        return b == null ? a : a.add(b);
    }

    private static Integer add(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        return b == null ? a : a + b;
    }

    private RentExcelParser findParser(String regionCode) {
        for (RentExcelParser p : parsers) {
            if (p.regionCode().equalsIgnoreCase(regionCode)) {
                return p;
            }
        }
        throw new JeecgBootException("未知的项目大区：" + regionCode);
    }

    /** 项目名比对时忽略空格和全角括号差异 */
    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s", "").replace("(", "（").replace(")", "）");
    }

    private String currentUser() {
        try {
            return JwtUtil.getUserNameByToken(
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder
                                    .currentRequestAttributes()).getRequest());
        } catch (Exception e) {
            return "system";
        }
    }
}