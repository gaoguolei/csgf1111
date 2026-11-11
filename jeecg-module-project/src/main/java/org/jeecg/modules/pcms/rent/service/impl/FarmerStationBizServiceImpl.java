package org.jeecg.modules.pcms.rent.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.DictOptionVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationDetailVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationFormDTO;
import org.jeecg.modules.pcms.rent.dto.InstallFormDTO;
import org.jeecg.modules.pcms.rent.entity.TFarmerChangeRequest;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TInstallInfo;
import org.jeecg.modules.pcms.rent.entity.TInstallType;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.entity.TRent;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.entity.TRentPriceType;
import org.jeecg.modules.pcms.rent.service.IFarmerAuditService;
import org.jeecg.modules.pcms.rent.service.IFarmerStationBizService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITInstallInfoService;
import org.jeecg.modules.pcms.rent.service.ITInstallTypeService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.service.ITRentPriceTypeService;
import org.jeecg.modules.pcms.rent.service.ITRentService;
import org.jeecg.modules.pcms.rent.service.RentTenantResolver;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.CellUtil;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.jeecg.modules.pcms.rent.util.RentPeriodUtil;
import org.jeecg.modules.pcms.rent.util.RentRegionResolver;
import org.jeecg.modules.pcms.rent.util.TenantScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 农户信息增删改与详情实现。
 *
 * 几条贯穿始终的规则：
 * 1. 租金截止日期永远由"并网日期 + 项目租期年限"算出，不接受手填
 * 2. 安装房型和租金档位的可选范围由农户所属大区决定，提交时会校验，
 *    防止给北京的农户挂上河南的"斜屋面"
 * 3. 金额 = 单价 × 块数，块数一改，该安装信息下所有档位的金额同步重算
 * 4. 删除一律逻辑删除，并同步写 del_time 供唯一索引使用
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class FarmerStationBizServiceImpl implements IFarmerStationBizService {

    /**
     * 未删除时 del_time 的固定占位值。
     *
     * 取 TFarmerStation.NOT_DELETED（字符串）显式解析，不要写 new Date(0L)：
     * 后者是 UTC 的 1970-01-01 00:00:00，在东八区会序列化成 08:00:00，
     * 和 Excel 导入写进去的（库默认值）00:00:00 对不上。
     *
     * 农户表的唯一键是 (project_id, power_user_no, del_time)，靠这个占位值区分
     * "没删过"的行；两处写入的占位值不一致，唯一键就会失效——同一个项目下
     * 同一个户号能建出两条有效记录。
     */
    private static final Date NOT_DELETED = Timestamp.valueOf(TFarmerStation.NOT_DELETED);

    /** 运营自建租金档位的编码前缀，用来跟内置档位（BJ_Y1_5 这种）区分开 */
    private static final String CUSTOM_PRICE_CODE_PREFIX = "CUSTOM_";

    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITInstallInfoService installInfoService;
    @Autowired
    private ITRentService rentService;
    @Autowired
    private ITRentLedgerService rentLedgerService;
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private ITInstallTypeService installTypeService;
    @Autowired
    private ITRentPriceTypeService rentPriceTypeService;
    @Autowired
    private OperLogHelper operLog;
    @Autowired
    private RentTenantResolver tenantResolver;
    @Autowired
    private TenantScopeHelper tenantScope;
    @Autowired
    private IFarmerAuditService auditService;
    @Autowired
    private RentRoleGuard roleGuard;
    @Autowired
    private RentRegionResolver regionResolver;

    // ==================== 详情 ====================

    @Override
    public FarmerStationDetailVO detail(Long id) {
        TFarmerStation s = getExistingStation(id);
        TProject project = projectService.getById(s.getProjectId());

        FarmerStationDetailVO vo = new FarmerStationDetailVO()
                .setId(s.getId())
                .setFarmerName(s.getFarmerName())
                .setBankCardName(s.getBankCardName())
                .setBankCardNo(s.getBankCardNo())
                .setIdCard(s.getIdCard())
                .setStationCode(s.getStationCode())
                .setPowerUserNo(s.getPowerUserNo())
                .setInstallAddr(s.getInstallAddr())
                .setSalesman(s.getSalesman())
                .setProjectId(s.getProjectId())
                .setRegionCode(s.getRegionCode())
                .setGridDate(s.getGridDate())
                .setActualGenDate(s.getActualGenDate())
                .setRentDeadline(s.getRentDeadline())
                .setTotalCapacity(s.getTotalCapacity())
                .setTotalBlocks(s.getTotalBlocks())
                .setRemark(s.getRemark());
        if (project != null) {
            // 天津和廊坊共用库里的 region_code=TJLF、region_name=天津廊坊，详情页同样要拆开
            vo.setProjectName(project.getProjectName())
                    .setRegionCode(regionResolver.regionCodeOf(project))
                    .setRegionName(regionResolver.regionNameOf(project));
        }

        List<FarmerStationDetailVO.InstallVO> installs = installsOf(id);
        vo.getInstalls().addAll(installs);

        // 首年租金：每家装下排序号最小的那个档位，金额相加，仅供参考
        BigDecimal firstYear = BigDecimal.ZERO;
        for (FarmerStationDetailVO.InstallVO iv : installs) {
            FarmerStationDetailVO.RentVO first = null;
            for (FarmerStationDetailVO.RentVO r : iv.getRents()) {
                if (first == null || nvlSort(r.getSortNo()) < nvlSort(first.getSortNo())) {
                    first = r;
                }
            }
            if (first != null && first.getAmount() != null) {
                firstYear = firstYear.add(first.getAmount());
            }
        }
        vo.setFirstYearRent(firstYear);
        return vo;
    }

    @Override
    public List<FarmerStationDetailVO.InstallVO> installsOf(Long stationId) {
        List<FarmerStationDetailVO.InstallVO> result = new ArrayList<>();
        if (stationId == null) {
            return result;
        }
        Map<Long, TInstallType> typeMap = installTypeMap();
        Map<Long, TRentPriceType> priceMap = priceTypeMap();
        // 档位的实际日期区间要按这户的并网日期推，所以先把农户捞出来
        TFarmerStation station = farmerStationService.getById(stationId);

        List<TInstallInfo> installs = installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, stationId)
                .eq(TInstallInfo::getDelFlag, 0)
                .orderByAsc(TInstallInfo::getId));
        if (installs.isEmpty()) {
            return result;
        }

        List<Long> installIds = new ArrayList<>();
        for (TInstallInfo i : installs) {
            installIds.add(i.getId());
        }
        // 只取"当前生效"的档位单价。
        // 改过价的档位在 t_rent 里会多留一条旧价切片（effect_date_to=本月底），
        // 那是给计租引擎算历史区间用的，页面上不该重复展示同一个档位。
        List<TRent> rents = rentService.list(new LambdaQueryWrapper<TRent>()
                .in(TRent::getInstallId, installIds)
                .eq(TRent::getDelFlag, 0)
                .isNull(TRent::getEffectDateTo)
                .orderByAsc(TRent::getId));
        Map<Long, List<TRent>> rentByInstall = new HashMap<>();
        for (TRent r : rents) {
            rentByInstall.computeIfAbsent(r.getInstallId(), k -> new ArrayList<>()).add(r);
        }

        for (TInstallInfo i : installs) {
            TInstallType type = typeMap.get(i.getInstallTypeId());
            FarmerStationDetailVO.InstallVO iv = new FarmerStationDetailVO.InstallVO()
                    .setId(i.getId())
                    .setInstallTypeId(i.getInstallTypeId())
                    .setInstallTypeName(type == null ? null : type.getTypeName())
                    .setModuleModel(i.getModuleModel())
                    .setIsMultiModel(i.getIsMultiModel())
                    .setBlockCount(i.getBlockCount())
                    .setCapacityKw(i.getCapacityKw());
            for (TRent r : rentByInstall.getOrDefault(i.getId(), Collections.emptyList())) {
                TRentPriceType pt = priceMap.get(r.getPriceTypeId());
                iv.getRents().add(new FarmerStationDetailVO.RentVO()
                        .setId(r.getId())
                        .setPriceTypeId(r.getPriceTypeId())
                        .setPriceTypeName(pt == null ? null : pt.getTypeName())
                        .setDateRangeDesc(dateRangeDesc(pt, station))
                        .setSplitMode(pt == null ? null : pt.getSplitMode())
                        .setSortNo(pt == null ? null : pt.getSortNo())
                        .setEffectDateFrom(r.getEffectDateFrom())
                        .setEffectDateTo(r.getEffectDateTo())
                        .setUnitPrice(r.getUnitPrice())
                        .setAmount(r.getAmount()));
            }
            result.add(iv);
        }
        return result;
    }

    /** 排序号缺省当最大，排到最后 */
    private int nvlSort(Integer sortNo) {
        return sortNo == null ? Integer.MAX_VALUE : sortNo;
    }

    // ==================== 农户增删改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFarmer(FarmerStationFormDTO form) {
        roleGuard.requireFarmerOps("新增农户");
        require(form.getFarmerName(), "农户姓名不能为空");
        require(form.getPowerUserNo(), "发电户号不能为空");
        TProject project = resolveProject(form.getProjectId());
        checkUserNoUnique(project.getId(), form.getPowerUserNo().trim(), null);

        TFarmerStation s = new TFarmerStation();
        fill(s, form, project);
        s.setDelFlag(0).setDelTime(NOT_DELETED).setCreateTime(new Date()).setCreateBy(operLog.currentUser());
        farmerStationService.save(s);

        operLog.record(TOperLog.ACTION_INSERT, TOperLog.BIZ_FARMER, s.getId(), null, s);

        // 选填的安装信息：前端想在新建农户时一块儿录进来。
        // 走的是同一个 saveInstall，所以房型字典自动建、块数容量校验、
        // 农户总块数/总容量重算这些行为跟单独调安装信息接口完全一致。
        // 有一条失败整个新增回滚 —— 农户建好了安装信息没建上，比整体失败更难处理
        saveInstallsOfNewFarmer(s, form.getInstalls());

        // 新增即进入"新增待审核"：安装信息保存完再统一提交，快照才包含刚录的安装
        auditService.submitChange(s.getId(), TFarmerChangeRequest.TYPE_INSERT,
                null, auditService.snapshot(s.getId()));

        return s.getId();
    }

    /** 新增农户时顺带保存的安装信息。列表为空就什么都不做 */
    private void saveInstallsOfNewFarmer(TFarmerStation station, List<InstallFormDTO> installs) {
        if (installs == null || installs.isEmpty()) {
            return;
        }
        for (InstallFormDTO install : installs) {
            if (install == null) {
                continue;
            }
            // 农户ID以刚建出来的为准，前端传什么都覆盖掉
            install.setStationId(station.getId());
            // 新增农户时安装信息必然是新的，带上ID会被当成"编辑别人的安装信息"
            install.setId(null);
            // 不单独提交审核：这批安装信息会并进外层的新增申请
            saveInstallInternal(install, false);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editFarmer(FarmerStationFormDTO form) {
        roleGuard.requireFarmerOps("修改农户");
        if (form.getId() == null) {
            throw new JeecgBootException("缺少农户ID");
        }
        TFarmerStation before = getExistingStation(form.getId());
        // 上一次变更还没审完，不允许叠加
        if (auditService.isPending(before.getId())) {
            throw new JeecgBootException("该农户还有一次变更在审核中，请等审核结果或取消后再修改");
        }
        require(form.getFarmerName(), "农户姓名不能为空");
        require(form.getPowerUserNo(), "发电户号不能为空");

        // 没传就沿用原项目；传了就按新值解析（同样兼容前端传租户ID）
        TProject project = form.getProjectId() == null
                ? projectService.getById(before.getProjectId())
                : resolveProject(form.getProjectId());
        if (project == null) {
            throw new JeecgBootException("所属项目不存在或已停用");
        }
        checkUserNoUnique(project.getId(), form.getPowerUserNo().trim(), form.getId());

        TFarmerStation s = new TFarmerStation();
        s.setId(before.getId());
        fill(s, form, project);
        s.setUpdateTime(new Date()).setUpdateBy(operLog.currentUser());

        // 需求28：什么都没改就点提交，直接提示，不落一条空申请
        if (!farmerChanged(before, s)) {
            throw new JeecgBootException("没有修改任何信息");
        }
        // 改前快照必须在写库之前取，否则 before 已经被新值覆盖
        String beforeJson = auditService.snapshot(before.getId());

        farmerStationService.updateById(s);

        // 姓名或户号变了，同步刷新子表里的冗余字段，否则安装信息和租金里还是旧值
        if (!eq(before.getFarmerName(), s.getFarmerName())
                || !eq(before.getPowerUserNo(), s.getPowerUserNo())
                || !eq(before.getProjectId(), s.getProjectId())) {
            syncRedundantFields(before.getId(), s.getFarmerName(), s.getPowerUserNo(), project.getId());
        }

        operLog.record(TOperLog.ACTION_UPDATE, TOperLog.BIZ_FARMER, s.getId(), before, s);

        // 提交修改审核：提交时间就是租金锁定的分界线
        auditService.submitChange(before.getId(), TFarmerChangeRequest.TYPE_UPDATE,
                beforeJson, auditService.snapshot(before.getId()));
    }

    /**
     * 农户主表有没有实质变化。
     * 需求28：什么都没改就点提交，要提示"没有修改任何信息"，不能落一条空申请。
     */
    private boolean farmerChanged(TFarmerStation before, TFarmerStation after) {
        return !eq(before.getFarmerName(), after.getFarmerName())
                || !eq(before.getPowerUserNo(), after.getPowerUserNo())
                || !eq(before.getIdCard(), after.getIdCard())
                || !eq(before.getBankCardNo(), after.getBankCardNo())
                || !eq(before.getBankCardName(), after.getBankCardName())
                || !eq(before.getInstallAddr(), after.getInstallAddr())
                || !eq(before.getProjectId(), after.getProjectId())
                || !eq(before.getStationCode(), after.getStationCode())
                || !eq(before.getSalesman(), after.getSalesman())
                || !eq(before.getGridDate(), after.getGridDate())
                || !eq(before.getActualGenDate(), after.getActualGenDate())
                || !eq(before.getRemark(), after.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmer(Long id) {
        roleGuard.requireFarmerOps("删除农户");
        TFarmerStation before = getExistingStation(id);
        Date now = new Date();

        // 逻辑删除同时写 del_time：唯一键用的是 del_time 而非 del_flag，
        // 这样同一个户号删了再建再删也不会撞唯一索引
        farmerStationService.updateById(new TFarmerStation()
                .setId(id).setDelFlag(1).setDelTime(now)
                .setUpdateTime(now).setUpdateBy(operLog.currentUser()));

        List<TInstallInfo> installs = installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, id).eq(TInstallInfo::getDelFlag, 0));
        for (TInstallInfo i : installs) {
            softDeleteInstall(i.getId(), now);
        }
        softDeleteDraftLedgers(id, now);

        operLog.record(TOperLog.ACTION_DELETE, TOperLog.BIZ_FARMER, id, before, null);
    }

    /**
     * 农户删除后处理它的租金台账。
     *
     * 只删还没提交审核的那些——留着会在租金台账列表里变成查无此人的孤儿行。
     * 已提交审核、已通过、已支付的一概不动：钱已经走完流程甚至已经付出去了，
     * 那是账，不能因为农户档案被删就跟着消失。
     */
    private void softDeleteDraftLedgers(Long stationId, Date now) {
        List<TRentLedger> ledgers = rentLedgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, stationId)
                .eq(TRentLedger::getDelFlag, 0));
        int kept = 0;
        for (TRentLedger l : ledgers) {
            if (l.locked() || l.paid()) {
                kept++;
                continue;
            }
            rentLedgerService.updateById(new TRentLedger().setId(l.getId()).setDelFlag(1)
                    .setUpdateTime(now).setUpdateBy(operLog.currentUser()));
        }
        if (kept > 0) {
            log.info("农户{}被删除，其中{}条已进入审核或已支付的租金台账保留未删", stationId, kept);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmerBatch(List<Long> ids) {
        roleGuard.requireFarmerOps("批量删除农户");
        if (ids == null || ids.isEmpty()) {
            throw new JeecgBootException("请选择要删除的记录");
        }
        for (Long id : ids) {
            deleteFarmer(id);
        }
    }

    // ==================== 安装信息与租金 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveInstall(InstallFormDTO form) {
        roleGuard.requireFarmerOps("保存安装信息");
        return saveInstallInternal(form, true);
    }

    /**
     * 保存安装信息。
     *
     * @param submitAudit 是否创建"修改农户信息"审核申请。
     *                    新增农户时传 false —— 那批安装信息要并进"新增审核"里，
     *                    不能一条安装单开一个申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveInstallInternal(InstallFormDTO form, boolean submitAudit) {
        if (form.getStationId() == null) {
            throw new JeecgBootException("缺少农户ID");
        }
        TFarmerStation station = getExistingStation(form.getStationId());
        if (form.getBlockCount() == null || form.getBlockCount() <= 0) {
            throw new JeecgBootException("安装块数必须大于0");
        }
        // 改动前的整户快照，审核驳回时要按它回滚
        String beforeJson = submitAudit ? auditService.snapshot(station.getId()) : null;
        // 选了下拉里的已有房型就用它，手输了新名字就自动建一条字典
        TInstallType type = resolveInstallType(station, form);

        boolean multi = CellUtil.isMultiModel(form.getModuleModel());
        TInstallInfo install = new TInstallInfo()
                .setStationId(station.getId())
                .setFarmerName(station.getFarmerName())
                .setProjectId(station.getProjectId())
                .setPowerUserNo(station.getPowerUserNo())
                .setInstallTypeId(type.getId())
                .setModuleModel(form.getModuleModel())
                .setIsMultiModel(multi ? 1 : 0)
                .setBlockCount(form.getBlockCount())
                .setCapacityKw(CellUtil.capacityKw(form.getModuleModel(), form.getBlockCount()))
                .setDelFlag(0)
                .setUpdateTime(new Date())
                .setUpdateBy(operLog.currentUser());

        TInstallInfo before = null;
        // 编辑时租金是整体替换，先把旧单价快照下来，替换后逐档位比对写日志；
        // 只改租金不改安装字段时，安装信息那条日志会显示"内容无变化"，租金的变化靠这条补上
        Map<Long, BigDecimal> oldPrices = form.getId() == null
                ? Collections.emptyMap() : currentUnitPrices(form.getId());
        // 需求第21条：改价时保留旧价的时间区间，快照当前生效中的租金记录
        Map<Long, TRent> oldRentMap = form.getId() == null
                ? Collections.emptyMap() : currentRentMap(form.getId());
        if (form.getId() == null) {
            install.setCreateTime(new Date()).setCreateBy(operLog.currentUser());
            installInfoService.save(install);
        } else {
            before = installInfoService.getById(form.getId());
            if (before == null || before.getDelFlag() != null && before.getDelFlag() == 1) {
                throw new JeecgBootException("安装信息不存在或已删除");
            }
            install.setId(form.getId());
            installInfoService.updateById(install);
            // 需求第21条：不再物理删除旧租金，改价、增删档位都交给 saveRents 做 diff，
            // 旧价通过 effect_date_to 保留到次月1号，历史台账重算才拿得到旧价
        }

        saveRents(station, install, form.getRents(), oldRentMap);
        refreshStationTotal(station.getId());

        // 安装字段没动就不写安装信息这条日志——只改租金时它只会显示"内容无变化"，
        // 变更内容由租金那条日志说明；安装和租金都没动则一条都不写
        if (before == null || installChanged(before, install)) {
            operLog.record(form.getId() == null ? TOperLog.ACTION_INSERT : TOperLog.ACTION_UPDATE,
                    TOperLog.BIZ_INSTALL, install.getId(), before, install);
        }
        logRentChanges(install.getId(), oldPrices, form.getRents());

        // 安装信息是农户数据的一部分，改了也要走审核
        if (submitAudit) {
            submitUpdateAudit(station.getId(), beforeJson);
        }
        return install.getId();
    }

    /**
     * 安装信息改动后提交"修改农户信息"审核。
     * 农户已经有一条待审核的（比如刚新增还没审）就并进那一条：
     * 只把最新快照刷到 afterJson 上，beforeJson 和提交时间保持不变。
     */
    private void submitUpdateAudit(Long stationId, String beforeJson) {
        String afterJson = auditService.snapshot(stationId);
        if (auditService.mergePendingSnapshot(stationId, afterJson)) {
            return;
        }
        auditService.submitChange(stationId, TFarmerChangeRequest.TYPE_UPDATE, beforeJson, afterJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstall(Long installId) {
        roleGuard.requireFarmerOps("删除安装信息");
        TInstallInfo before = installInfoService.getById(installId);
        if (before == null) {
            throw new JeecgBootException("安装信息不存在");
        }
        // 删除安装信息同样是农户数据的变更，要走审核
        String beforeJson = auditService.snapshot(before.getStationId());
        softDeleteInstall(installId, new Date());
        refreshStationTotal(before.getStationId());
        operLog.record(TOperLog.ACTION_DELETE, TOperLog.BIZ_INSTALL, installId, before, null);
        submitUpdateAudit(before.getStationId(), beforeJson);
    }

    // ==================== 下拉框 ====================

    @Override
    public List<DictOptionVO> installTypeOptions(Long stationId, Long projectId) {
        String regionCode = resolveRegionCode(stationId, projectId);
        List<DictOptionVO> list = new ArrayList<>();
        for (TInstallType t : installTypeService.list(new LambdaQueryWrapper<TInstallType>()
                .eq(TInstallType::getRegionCode, regionCode)
                .eq(TInstallType::getStatus, 1)
                .orderByAsc(TInstallType::getSortNo))) {
            list.add(new DictOptionVO()
                    .setId(t.getId()).setCode(t.getTypeCode()).setName(t.getTypeName())
                    .setBuiltin(t.builtin() ? 1 : 0));
        }
        return list;
    }

    /**
     * 下拉选项取决于农户所属大区，所以农户ID和项目ID传一个就能定位到大区。
     *
     * 新建农户时农户还没建出来、拿不到 stationId，只能靠 projectId 反查大区 ——
     * 这是加这个分支的唯一原因。
     *
     * 注意这里的 projectId 是【租金模块自己的 t_project.id】（来自 /rent/farmerStation/projectOptions），
     * 不是项目模块的 pv_project_info.id，也不是租户 sys_tenant.id ——
     * 那两张表跟租金这边没有任何关系，传错了这里会直接报"项目不存在"。
     * 同时做了租户校验：不让普通用户拿别的租户的项目ID来查选项。
     */
    private String resolveRegionCode(Long stationId, Long projectId) {
        if (stationId != null) {
            return getExistingStation(stationId).getRegionCode();
        }
        if (projectId == null) {
            throw new JeecgBootException("请传农户ID或所属项目ID");
        }

        // ① 先当租金模块的项目ID找（t_project.id，取自 /rent/farmerStation/projectOptions）
        TProject p = projectService.getById(projectId);
        if (p != null) {
            if (p.getStatus() != null && p.getStatus() == 0) {
                throw new JeecgBootException("所属项目" + p.getProjectName() + "已停用");
            }
            assertProjectInTenantScope(p);
            return p.getRegionCode();
        }

        // ② 找不到项目时，再看是不是租户ID。
        //    房型/档位下拉只跟大区有关，租户能唯一确定大区，所以传租户ID是够的 ——
        //    前端顶部租户栏给的就是 sys_tenant.id，直接透传过来也能用
        String region = tenantResolver.regionOfTenant(projectId.intValue());
        if (region != null) {
            return region;
        }

        throw new JeecgBootException("ID " + projectId + " 既不是租金模块的项目，也不是租户。"
                + "项目ID取自 /rent/farmerStation/projectOptions 的 children[].id");
    }

    /**
     * 把前端传的"所属项目"解析成真实的 t_project。
     *
     * 前端顶部租户栏给的是 sys_tenant.id，而农户必须挂在具体项目上 ——
     * 一个租户下可能有多个项目（北京3个、天津5个），所以传租户ID时
     * 按「该租户下编码最小的启用项目」兜底。
     *
     * 同一租户下的项目，大区、租期年限、一年几期都一样，租金算出来是相同的，
     * 区别只在项目名这个标签，所以兜底不会算错钱。
     *
     * ⚠️ 不能只按大区找：天津和廊坊共用 region_code=TJLF，
     * 按大区取第一个会选中 LF_01 廊坊，把天津的农户挂到廊坊去。
     * 所以这里逐个项目反推它属于哪个租户，再跟目标租户比对。
     */
    private TProject resolveProject(Long rawId) {
        if (rawId == null) {
            throw new JeecgBootException("所属项目不能为空");
        }
        // ① 先当项目ID找（t_project.id，取自 /rent/farmerStation/projectOptions）
        TProject p = projectService.getById(rawId);
        if (p != null) {
            if (p.getStatus() != null && p.getStatus() == 0) {
                throw new JeecgBootException("所属项目" + p.getProjectName() + "已停用");
            }
            assertProjectInTenantScope(p);
            return p;
        }
        // ② 再当租户ID找，取该租户下编码最小的启用项目
        TProject fallback = defaultProjectOfTenant(rawId.intValue());
        if (fallback != null) {
            log.info("所属项目传的是租户ID {}，已按其默认项目[{}]处理",
                    rawId, fallback.getProjectName());
            return fallback;
        }
        throw new JeecgBootException("ID " + rawId + " 既不是租金模块的项目，也不是租户。"
                + "项目ID取自 /rent/farmerStation/projectOptions 的 children[].id");
    }

    /** 租户下编码最小的启用项目。找不到返回 null */
    private TProject defaultProjectOfTenant(Integer tenantId) {
        Map<String, Integer> ids = tenantResolver.loadTenantIds();
        for (TProject p : projectService.list(new LambdaQueryWrapper<TProject>()
                .eq(TProject::getStatus, 1)
                .orderByAsc(TProject::getProjectCode))) {
            String tenantName = tenantResolver.tenantNameOf(p.getRegionCode(), p);
            if (tenantName != null && tenantId.equals(ids.get(tenantName))) {
                return p;
            }
        }
        return null;
    }

    /** 项目是否落在当前用户的租户范围内。管理员（rel_tenant_ids 为空）不过滤 */
    private void assertProjectInTenantScope(TProject project) {
        List<Integer> allowed = tenantScope.allowedTenantIds();
        if (allowed == null) {
            return;
        }
        // t_project 上没有 tenant_id，项目归属要靠"大区+项目编码"推，
        // 用的是和农户落库、下拉过滤同一个 RentTenantResolver，判断口径保持一致
        String tenantName = tenantResolver.tenantNameOf(project.getRegionCode(), project);
        Integer tenantId = tenantName == null
                ? null : tenantResolver.loadTenantIds().get(tenantName);
        if (tenantId == null || !allowed.contains(tenantId)) {
            throw new JeecgBootException("无权查看该项目下的选项");
        }
    }

    @Override
    public List<DictOptionVO> priceTypeOptions(Long stationId, Long projectId) {
        String regionCode = resolveRegionCode(stationId, projectId);
        // 有 stationId 就顺带把农户捞出来，用它推档位的实际日期区间；
        // 新建农户时只传 projectId，没有并网日期，dateRangeDesc 会为空
        TFarmerStation station = stationId == null ? null : farmerStationService.getById(stationId);
        List<DictOptionVO> list = new ArrayList<>();
        for (TRentPriceType t : rentPriceTypeService.list(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode)
                .orderByAsc(TRentPriceType::getSortNo))) {
            list.add(new DictOptionVO()
                    .setId(t.getId()).setCode(t.getTypeCode()).setName(t.getTypeName())
                    .setSplitMode(t.getSplitMode())
                    .setRangeDesc(rangeDesc(t))
                    .setDateRangeDesc(dateRangeDesc(t, station)));
        }
        return list;
    }

    // ==================== 安装房型的解析与自动建档 ====================

    /**
     * 确定这条安装信息用哪个房型。
     *
     * 优先用下拉框选中的 installTypeId；没选而是手输了 installTypeName，
     * 就在该农户所属大区下找同名的，找不到就自动建一条。
     */
    private TInstallType resolveInstallType(TFarmerStation station, InstallFormDTO form) {
        if (form.getInstallTypeId() != null) {
            TInstallType type = installTypeService.getById(form.getInstallTypeId());
            // 房型必须属于该农户所在大区，挡住给北京农户挂河南斜屋面这种错配
            if (type == null || !type.getRegionCode().equals(station.getRegionCode())) {
                throw new JeecgBootException("安装房型不属于该农户所在项目，请重新选择");
            }
            return type;
        }
        if (form.getInstallTypeName() == null || form.getInstallTypeName().trim().isEmpty()) {
            throw new JeecgBootException("请选择或输入安装房型");
        }
        return findOrCreateInstallType(station.getRegionCode(), form.getInstallTypeName().trim());
    }

    /**
     * 按大区 + 名称找房型，没有就建一条。
     *
     * 只要运营输入 type_name，其余字段全由系统生成：
     *   region_code 取当前农户所属大区
     *   type_code   自动生成 CUSTOM_大区_序号，与内置房型的编码彻底隔开
     *   is_builtin  固定为 0，区别于导入 Parser 依赖的内置房型
     */
    private TInstallType findOrCreateInstallType(String regionCode, String typeName) {
        TInstallType exists = installTypeService.getOne(new LambdaQueryWrapper<TInstallType>()
                .eq(TInstallType::getRegionCode, regionCode)
                .eq(TInstallType::getTypeName, typeName)
                .last("limit 1"), false);
        if (exists != null) {
            // 之前被停用过又被重新输入，直接恢复启用，免得运营输了却存不进去
            if (exists.getStatus() != null && exists.getStatus() == 0) {
                installTypeService.updateById(new TInstallType()
                        .setId(exists.getId()).setStatus(1)
                        .setUpdateBy(operLog.currentUser()).setUpdateTime(new Date()));
                exists.setStatus(1);
            }
            return exists;
        }

        TInstallType created = new TInstallType()
                .setRegionCode(regionCode)
                .setTypeName(typeName)
                .setTypeCode(nextCustomCode(regionCode))
                .setIsBuiltin(0)
                .setStatus(1)
                .setSortNo(nextSortNo(regionCode))
                .setCreateBy(operLog.currentUser())
                .setCreateTime(new Date());
        installTypeService.save(created);

        operLog.record(TOperLog.ACTION_INSERT, TOperLog.BIZ_INSTALL, created.getId(), null, created);
        log.info("大区[{}]下自动新建安装房型[{}] code={}", regionCode, typeName, created.getTypeCode());
        return created;
    }

    /**
     * 生成 CUSTOM_大区_序号 形式的编码。
     * 序号按该大区已有的自建房型数量递推，撞了就往后顺延，
     * 顺延 100 次还撞不上就说明有别的问题，直接退回时间戳保证不阻塞业务。
     */
    private String nextCustomCode(String regionCode) {
        long custom = installTypeService.count(new LambdaQueryWrapper<TInstallType>()
                .eq(TInstallType::getRegionCode, regionCode)
                .likeRight(TInstallType::getTypeCode, TInstallType.CUSTOM_CODE_PREFIX));
        for (int i = 1; i <= 100; i++) {
            String code = String.format("%s%s_%02d", TInstallType.CUSTOM_CODE_PREFIX, regionCode, custom + i);
            boolean taken = installTypeService.count(new LambdaQueryWrapper<TInstallType>()
                    .eq(TInstallType::getRegionCode, regionCode)
                    .eq(TInstallType::getTypeCode, code)) > 0;
            if (!taken) {
                return code;
            }
        }
        return TInstallType.CUSTOM_CODE_PREFIX + regionCode + "_" + System.currentTimeMillis();
    }

    /** 自建房型排在该大区内置房型后面 */
    private int nextSortNo(String regionCode) {
        List<TInstallType> all = installTypeService.list(new LambdaQueryWrapper<TInstallType>()
                .eq(TInstallType::getRegionCode, regionCode));
        int max = 0;
        for (TInstallType t : all) {
            if (t.getSortNo() != null && t.getSortNo() > max) {
                max = t.getSortNo();
            }
        }
        return max + 1;
    }

    // ==================== 内部方法 ====================

    /**
     * 定位一个租金档位：传了 ID 就查出来校验，没传 ID 但填了名字就在该大区下现建一条。
     *
     * 和安装房型（resolveInstallType）一样的思路 —— 运营在新增安装信息时
     * 经常碰到下拉框里没有的档位（新项目、新合同口径），
     * 不该为了这个再跑到字典维护页面去加。
     */
    private TRentPriceType resolvePriceType(TFarmerStation station, InstallFormDTO.RentItem it,
                                            Map<Long, TRentPriceType> priceMap) {
        if (it.getPriceTypeId() != null) {
            TRentPriceType pt = priceMap.get(it.getPriceTypeId());
            // 档位必须属于该农户所在大区，挡住给北京农户挂河南档位这种错配
            if (pt == null || !pt.getRegionCode().equals(station.getRegionCode())) {
                throw new JeecgBootException("租金档位不属于该农户所在项目，请重新选择");
            }
            return pt;
        }
        if (it.getPriceTypeName() == null || it.getPriceTypeName().trim().isEmpty()) {
            throw new JeecgBootException("请选择或输入租金档位");
        }
        TRentPriceType created = findOrCreatePriceType(station.getRegionCode(), it);
        // 新建的塞进本次的缓存，同一个档位名在同一批里出现两次不会重复建
        priceMap.put(created.getId(), created);
        return created;
    }

    /** 按"档位名 + 大区"找，找不到就建。建的时候把切分方式、区间、排序号一起落下来 */
    private TRentPriceType findOrCreatePriceType(String regionCode, InstallFormDTO.RentItem it) {
        String name = it.getPriceTypeName().trim();
        TRentPriceType exists = rentPriceTypeService.getOne(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode)
                .eq(TRentPriceType::getTypeName, name)
                .last("limit 1"), false);
        if (exists != null) {
            return exists;
        }

        int splitMode = it.getSplitMode() == null
                ? TRentPriceType.SPLIT_BY_YEAR : it.getSplitMode();
        if (splitMode != TRentPriceType.SPLIT_BY_YEAR
                && splitMode != TRentPriceType.SPLIT_BY_DATE) {
            throw new JeecgBootException("租金档位的切分方式只能是 1按年 或 2按自然日期");
        }

        Integer startYear = null;
        Integer endYear = null;
        if (splitMode == TRentPriceType.SPLIT_BY_YEAR) {
            startYear = it.getStartYear() == null ? 1 : it.getStartYear();
            endYear = it.getEndYear() == null ? 25 : it.getEndYear();
            if (startYear < 1 || endYear < startYear) {
                throw new JeecgBootException("租金档位的年份区间不合法：第"
                        + startYear + "-" + endYear + "年");
            }
        }

        TRentPriceType created = new TRentPriceType()
                .setRegionCode(regionCode)
                .setTypeName(name)
                .setTypeCode(nextPriceTypeCode(regionCode))
                .setSplitMode(splitMode)
                .setStartYear(startYear)
                .setEndYear(endYear)
                .setEffectDateFrom(splitMode == TRentPriceType.SPLIT_BY_DATE
                        ? it.getEffectDateFrom() : null)
                .setEffectDateTo(splitMode == TRentPriceType.SPLIT_BY_DATE
                        ? it.getEffectDateTo() : null)
                .setSortNo(it.getSortNo() != null ? it.getSortNo() : nextPriceTypeSortNo(regionCode));
        rentPriceTypeService.save(created);

        operLog.record(TOperLog.ACTION_INSERT, TOperLog.BIZ_RENT, created.getId(), null, created);
        log.info("大区[{}]下自动新建租金档位[{}] code={} splitMode={} 区间={}~{}",
                regionCode, name, created.getTypeCode(), splitMode,
                splitMode == TRentPriceType.SPLIT_BY_YEAR ? startYear : created.getEffectDateFrom(),
                splitMode == TRentPriceType.SPLIT_BY_YEAR ? endYear : created.getEffectDateTo());
        return created;
    }

    /** 生成 CUSTOM_大区_序号 形式的档位编码，撞了就往后顺延 */
    private String nextPriceTypeCode(String regionCode) {
        long custom = rentPriceTypeService.count(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode)
                .likeRight(TRentPriceType::getTypeCode, CUSTOM_PRICE_CODE_PREFIX));
        for (int i = 1; i <= 100; i++) {
            String code = String.format("%s%s_%02d", CUSTOM_PRICE_CODE_PREFIX, regionCode, custom + i);
            boolean taken = rentPriceTypeService.count(new LambdaQueryWrapper<TRentPriceType>()
                    .eq(TRentPriceType::getRegionCode, regionCode)
                    .eq(TRentPriceType::getTypeCode, code)) > 0;
            if (!taken) {
                return code;
            }
        }
        return CUSTOM_PRICE_CODE_PREFIX + regionCode + "_" + System.currentTimeMillis();
    }

    /** 自建档位排在最后。档位重叠时数字小的优先，自建的放后面免得抢了内置档位的位置 */
    private int nextPriceTypeSortNo(String regionCode) {
        int max = 0;
        for (TRentPriceType t : rentPriceTypeService.list(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode))) {
            if (t.getSortNo() != null && t.getSortNo() > max) {
                max = t.getSortNo();
            }
        }
        return max + 1;
    }

    private void saveRents(TFarmerStation station, TInstallInfo install,
                           List<InstallFormDTO.RentItem> items, Map<Long, TRent> oldRentMap) {
        if (items == null) {
            items = Collections.emptyList();
        }
        Map<Long, TRentPriceType> priceMap = priceTypeMap();
        Date now = new Date();
        String user = operLog.currentUser();
        // 需求第21条：改价时旧价只生效到本月底，新价从次月1号起，两段时间不相交
        Date monthEnd = RentPeriodUtil.endOfMonth(now);
        Date nextMonthFirst = RentPeriodUtil.nextMonthFirst(now);
        List<Long> used = new ArrayList<>();
        List<TRentPriceType> picked = new ArrayList<>();
        for (InstallFormDTO.RentItem it : items) {
            if (it.getUnitPrice() == null) {
                throw new JeecgBootException("租金单价不能为空");
            }
            // 档位：下拉框里选了的就查出来校验；没选但填了名字的，在该大区下现建一条
            TRentPriceType pt = resolvePriceType(station, it, priceMap);
            if (used.contains(pt.getId())) {
                throw new JeecgBootException("租金档位[" + pt.getTypeName() + "]重复了");
            }
            used.add(pt.getId());
            // 区间不能和已经选好的档位重叠
            checkOverlapWith(picked, pt);
            picked.add(pt);
            BigDecimal amount = it.getUnitPrice().multiply(new BigDecimal(install.getBlockCount()));

            TRent old = oldRentMap == null ? null : oldRentMap.get(pt.getId());
            if (old == null) {
                // 新增档位。
                // 动手之前先把该档位残留的切片清掉：这个档位以前可能被删过，
                // 或者改过价留下过 efrom=null 的老切片。不清的话新插的这条
                // (efrom=null, del_time=1970-01-01) 会跟它撞唯一键
                // uk_install_price(install_id, price_type_id, del_time, effect_date_from)
                softDeleteSlices(install.getId(), pt.getId(), now, user);
                rentService.save(new TRent()
                        .setStationId(station.getId()).setInstallId(install.getId())
                        .setFarmerName(station.getFarmerName()).setPowerUserNo(station.getPowerUserNo())
                        .setProjectId(station.getProjectId()).setPriceTypeId(pt.getId())
                        .setUnitPrice(it.getUnitPrice()).setAmount(amount)
                        .setDelFlag(0).setDelTime(NOT_DELETED)
                        .setCreateTime(now).setCreateBy(user)
                        .setUpdateTime(now).setUpdateBy(user));
            } else if (old.getUnitPrice() != null
                    && old.getUnitPrice().compareTo(it.getUnitPrice()) != 0) {
                // 需求第21条：改价。旧价只生效到本月底，新价从次月1号起。
                if (sameDay(old.getEffectDateFrom(), nextMonthFirst)) {
                    /*
                     * 同一个月里第二次改价。
                     *
                     * 上一次改价已经切出一条"次月1号起生效"的切片了（efrom=次月1号），
                     * 这次再切一条同样是 次月1号 开头的，就会撞唯一键
                     * uk_install_price(install_id, price_type_id, del_time, effect_date_from)
                     * —— 这个唯一键里没有 effect_date_to，两条同一天开头的记录共存不了。
                     *
                     * 而且业务上也没必要留两条：11月1日往后就是按最新价走，
                     * 上次那个"20元"的中间价根本没生效过。直接改掉它。
                     */
                    rentService.updateById(new TRent()
                            .setId(old.getId())
                            .setUnitPrice(it.getUnitPrice())
                            .setAmount(amount)
                            .setUpdateTime(now).setUpdateBy(user));
                } else {
                    // 常规改价：当前生效的切片截到本月底，另起一条次月1号生效的新价
                    rentService.updateById(new TRent()
                            .setId(old.getId())
                            .setEffectDateTo(monthEnd)
                            .setUpdateTime(now).setUpdateBy(user));
                    rentService.save(new TRent()
                            .setStationId(station.getId()).setInstallId(install.getId())
                            .setFarmerName(station.getFarmerName()).setPowerUserNo(station.getPowerUserNo())
                            .setProjectId(station.getProjectId()).setPriceTypeId(pt.getId())
                            .setUnitPrice(it.getUnitPrice()).setAmount(amount)
                            .setEffectDateFrom(nextMonthFirst)
                            .setDelFlag(0).setDelTime(NOT_DELETED)
                            .setCreateTime(now).setCreateBy(user)
                            .setUpdateTime(now).setUpdateBy(user));
                }
            } else {
                // 单价没变、块数可能变了。
                // 该档位下所有切片（含改价留下的历史区间）的金额都要按新块数重算，
                // 只更当前生效那条的话，历史区间会一直挂着旧块数的金额
                for (TRent slice : slicesOf(install.getId(), pt.getId())) {
                    BigDecimal sliceAmount = slice.getUnitPrice() == null
                            ? amount
                            : slice.getUnitPrice().multiply(new BigDecimal(install.getBlockCount()));
                    rentService.updateById(new TRent()
                            .setId(slice.getId())
                            .setAmount(sliceAmount)
                            .setUpdateTime(now).setUpdateBy(user));
                }
            }
        }

        // 提交里没有的档位，连同它的历史切片一起软删
        // （保留 del_time 供唯一键和历史追溯）
        if (oldRentMap != null) {
            for (Map.Entry<Long, TRent> e : oldRentMap.entrySet()) {
                if (!used.contains(e.getKey())) {
                    softDeleteSlices(install.getId(), e.getKey(), now, user);
                }
            }
        }
    }

    /**
     * 校验新选的档位跟已经选好的档位区间不重叠。
     *
     * 【为什么要拦】
     * 重叠意味着同一段时间挂了两个单价，租金算出来是含糊的。
     * RentCalcEngine 里虽然有"按 sortNo 取优先档"的兜底，但那是给历史脏数据擦屁股的，
     * 不该指望它来兜正常录入 —— 客户自己都不知道最后按哪个价算了。
     *
     * 海南项目在字典里同时提供"前10年/后15年"和"前8年/后17年"两套合同口径，
     * 本意是让人选一套。实际出过案例：选了前10年又加了前8年，两套全挂上，
     * 2025-06-21~2033-06-20 这段同时按 55 元和 100 元计租。
     *
     * 【为什么只跟同切分方式的比】
     * 按年切分的档位以各户并网日为原点（第1年=并网日起一年），
     * 按自然日期切分的是绝对日期。两套坐标系对不上，混在一起比会误伤
     * 河南那种"按年档位 + 按日期档位"并存的正常数据。
     */
    private void checkOverlapWith(List<TRentPriceType> picked, TRentPriceType now) {
        for (TRentPriceType exist : picked) {
            if (splitModeOf(exist) != splitModeOf(now)) {
                continue;
            }
            boolean hit;
            if (splitModeOf(now) == TRentPriceType.SPLIT_BY_DATE) {
                hit = overlapDate(exist.getEffectDateFrom(), exist.getEffectDateTo(),
                        now.getEffectDateFrom(), now.getEffectDateTo());
            } else {
                hit = overlap(exist.getStartYear(), exist.getEndYear(),
                        now.getStartYear(), now.getEndYear());
            }
            if (hit) {
                throw new JeecgBootException(String.format(
                        "租金档位「%s」与「%s」的时间区间存在重叠，同一时段不可挂载两个单价。",
                        exist.getTypeName(), now.getTypeName()));
            }
        }
    }

    /** 切分方式，没填按"按年"处理（与建表默认值一致） */
    private int splitModeOf(TRentPriceType t) {
        return t.getSplitMode() == null ? TRentPriceType.SPLIT_BY_YEAR : t.getSplitMode();
    }

    /** 两个年份闭区间是否重叠。null 起点当第1年、null 终点当第25年 */
    private boolean overlap(Integer aFrom, Integer aTo, Integer bFrom, Integer bTo) {
        int a1 = aFrom == null ? 1 : aFrom;
        int a2 = aTo == null ? 25 : aTo;
        int b1 = bFrom == null ? 1 : bFrom;
        int b2 = bTo == null ? 25 : bTo;
        return a1 <= b2 && b1 <= a2;
    }

    /** 两个日期闭区间是否重叠。null 起点当"从最初"、null 终点当"到最后" */
    private boolean overlapDate(Date aFrom, Date aTo, Date bFrom, Date bTo) {
        boolean overlapLeft = bTo == null || aFrom == null || !aFrom.after(bTo);
        boolean overlapRight = aTo == null || bFrom == null || !bFrom.after(aTo);
        return overlapLeft && overlapRight;
    }

    /**
     * 某个档位在一条安装信息下的全部有效记录，含改价留下的历史切片。
     *
     * 改价会在同一档位下留两条（旧价一段时间 + 新价一段时间），
     * 所以"这个档位的记录"往往不止一条，不能只取 efto 为空的那条。
     */
    private List<TRent> slicesOf(Long installId, Long priceTypeId) {
        return rentService.list(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getInstallId, installId)
                .eq(TRent::getPriceTypeId, priceTypeId)
                .eq(TRent::getDelFlag, 0));
    }

    /** 把一个档位下的所有切片软删，避免它们和新插入的记录撞唯一键 */
    private void softDeleteSlices(Long installId, Long priceTypeId, Date now, String user) {
        for (TRent slice : slicesOf(installId, priceTypeId)) {
            rentService.updateById(new TRent()
                    .setId(slice.getId())
                    .setDelFlag(1).setDelTime(now)
                    .setUpdateTime(now).setUpdateBy(user));
        }
    }

    /** 两个日期是不是同一天，用于判断"上次改价切出的次月切片"和这次是不是同一个起点 */
    private boolean sameDay(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        return RentPeriodUtil.ymd(a).equals(RentPeriodUtil.ymd(b));
    }

    /** 该安装信息下各档位当前的单价，编辑前快照用，key 是租金档位ID */
    private Map<Long, BigDecimal> currentUnitPrices(Long installId) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (TRent r : rentService.list(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getInstallId, installId).eq(TRent::getDelFlag, 0)
                .isNull(TRent::getEffectDateTo))) {
            map.put(r.getPriceTypeId(), r.getUnitPrice());
        }
        return map;
    }

    /** 该安装信息下各档位当前生效中的完整记录，key 是租金档位ID。改价时用它保留旧价的时间区间 */
    private Map<Long, TRent> currentRentMap(Long installId) {
        Map<Long, TRent> map = new LinkedHashMap<>();
        for (TRent r : rentService.list(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getInstallId, installId).eq(TRent::getDelFlag, 0)
                .isNull(TRent::getEffectDateTo))) {
            map.put(r.getPriceTypeId(), r);
        }
        return map;
    }

    /**
     * 租金单价的变更日志。整体替换后逐档位和替换前的快照比对：
     * 改价的记"租金单价 由 x 改为 y"，删掉的档位记"删除档位单价 x"，新加的记"新增档位单价 x"。
     * 没变化就不写，避免每次保存都堆一条空日志。
     */
    private void logRentChanges(Long installId, Map<Long, BigDecimal> oldPrices,
                                List<InstallFormDTO.RentItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, TRentPriceType> priceMap = priceTypeMap();
        List<String> changes = new ArrayList<>();
        Map<Long, BigDecimal> submitted = new LinkedHashMap<>();
        for (InstallFormDTO.RentItem it : items) {
            submitted.put(it.getPriceTypeId(), it.getUnitPrice());
            BigDecimal old = oldPrices.get(it.getPriceTypeId());
            if (old == null) {
                changes.add("新增档位[" + priceName(priceMap, it.getPriceTypeId())
                        + "]单价 " + it.getUnitPrice().toPlainString());
            } else if (old.compareTo(it.getUnitPrice()) != 0) {
                changes.add("档位[" + priceName(priceMap, it.getPriceTypeId()) + "]租金单价 由 "
                        + old.toPlainString() + " 改为 " + it.getUnitPrice().toPlainString());
            }
        }
        for (Map.Entry<Long, BigDecimal> e : oldPrices.entrySet()) {
            if (!submitted.containsKey(e.getKey())) {
                changes.add("删除档位[" + priceName(priceMap, e.getKey())
                        + "]单价 " + e.getValue().toPlainString());
            }
        }
        if (changes.isEmpty()) {
            return;
        }
        operLog.recordText(TOperLog.ACTION_UPDATE, TOperLog.BIZ_RENT, installId,
                String.join("；", changes));
    }

    private String priceName(Map<Long, TRentPriceType> priceMap, Long priceTypeId) {
        TRentPriceType t = priceMap.get(priceTypeId);
        return t == null ? String.valueOf(priceTypeId) : t.getTypeName();
    }

    /** 表单能改的安装信息字段是否真的变了，决定要不要写安装信息那条日志 */
    private boolean installChanged(TInstallInfo before, TInstallInfo after) {
        return !eq(before.getInstallTypeId(), after.getInstallTypeId())
                || !eq(before.getModuleModel(), after.getModuleModel())
                || !eq(before.getIsMultiModel(), after.getIsMultiModel())
                || !eq(before.getBlockCount(), after.getBlockCount())
                || decimalChanged(before.getCapacityKw(), after.getCapacityKw());
    }

    /** 容量是算出来的，BigDecimal 的 equals 连精度一起比，这里只比数值 */
    private boolean decimalChanged(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a != b;
        }
        return a.compareTo(b) != 0;
    }

    /** 安装信息增删改后，重算农户的总容量和总块数 */
    private void refreshStationTotal(Long stationId) {
        List<TInstallInfo> list = installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, stationId).eq(TInstallInfo::getDelFlag, 0));
        int blocks = 0;
        BigDecimal capacity = BigDecimal.ZERO;
        boolean capacityKnown = true;
        for (TInstallInfo i : list) {
            blocks += i.getBlockCount() == null ? 0 : i.getBlockCount();
            if (i.getCapacityKw() == null) {
                // 多型号混写的行算不出容量，这时总容量不动，保留导入时的原值
                capacityKnown = false;
            } else {
                capacity = capacity.add(i.getCapacityKw());
            }
        }
        TFarmerStation upd = new TFarmerStation().setId(stationId).setTotalBlocks(blocks);
        if (capacityKnown) {
            upd.setTotalCapacity(capacity);
        }
        farmerStationService.updateById(upd);
    }

    private void softDeleteInstall(Long installId, Date now) {
        installInfoService.updateById(new TInstallInfo()
                .setId(installId).setDelFlag(1).setUpdateTime(now).setUpdateBy(operLog.currentUser()));
        List<TRent> rents = rentService.list(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getInstallId, installId).eq(TRent::getDelFlag, 0));
        for (TRent r : rents) {
            rentService.updateById(new TRent().setId(r.getId()).setDelFlag(1).setDelTime(now)
                    .setUpdateTime(now).setUpdateBy(operLog.currentUser()));
        }
    }

    /** 农户姓名、户号、项目变更后，刷新子表的冗余字段 */
    private void syncRedundantFields(Long stationId, String name, String userNo, Long projectId) {
        for (TInstallInfo i : installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, stationId))) {
            installInfoService.updateById(new TInstallInfo().setId(i.getId())
                    .setFarmerName(name).setPowerUserNo(userNo).setProjectId(projectId));
        }
        for (TRent r : rentService.list(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getStationId, stationId))) {
            rentService.updateById(new TRent().setId(r.getId())
                    .setFarmerName(name).setPowerUserNo(userNo).setProjectId(projectId));
        }
    }

    private void fill(TFarmerStation s, FarmerStationFormDTO f, TProject project) {
        s.setFarmerName(f.getFarmerName().trim())
                .setPowerUserNo(f.getPowerUserNo().trim())
                .setProjectId(project.getId())
                .setRegionCode(project.getRegionCode())
                // 需求第14条：农户必须归属到租户，否则普通用户查不到自己建的数据
                .setTenantId(tenantResolver.resolve(project.getRegionCode(), project))
                .setStationCode(f.getStationCode())
                .setBankCardName(f.getBankCardName())
                .setBankCardNo(f.getBankCardNo())
                .setIdCard(f.getIdCard())
                .setInstallAddr(f.getInstallAddr())
                .setSalesman(f.getSalesman())
                .setGridDate(f.getGridDate())
                .setActualGenDate(f.getActualGenDate())
                .setTotalCapacity(f.getTotalCapacity())
                .setTotalBlocks(f.getTotalBlocks())
                .setRemark(f.getRemark())
                // 租金截止日期永远算出来，不接受前端传
                // = 并网日期 + 租期年限 - 1天，一年算到次年同月同日的前一天
                .setRentDeadline(CellUtil.rentDeadline(f.getGridDate(),
                        project.getLeaseYears() == null ? 25 : project.getLeaseYears()));
    }

    private void checkUserNoUnique(Long projectId, String powerUserNo, Long excludeId) {
        LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<TFarmerStation>()
                .eq(TFarmerStation::getProjectId, projectId)
                .eq(TFarmerStation::getPowerUserNo, powerUserNo)
                .eq(TFarmerStation::getDelFlag, 0);
        if (excludeId != null) {
            w.ne(TFarmerStation::getId, excludeId);
        }
        if (farmerStationService.count(w) > 0) {
            throw new JeecgBootException("该项目下已存在发电户号[" + powerUserNo
                    + "]，如果确实是不同农户共用同一户号，请按 " + powerUserNo + "-1 的形式区分");
        }
    }

    private TFarmerStation getExistingStation(Long id) {
        if (id == null) {
            throw new JeecgBootException("缺少农户ID");
        }
        TFarmerStation s = farmerStationService.getById(id);
        if (s == null || (s.getDelFlag() != null && s.getDelFlag() == 1)) {
            throw new JeecgBootException("农户不存在或已删除");
        }
        return s;
    }

    private Map<Long, TInstallType> installTypeMap() {
        Map<Long, TInstallType> map = new HashMap<>();
        for (TInstallType t : installTypeService.list()) {
            map.put(t.getId(), t);
        }
        return map;
    }

    private Map<Long, TRentPriceType> priceTypeMap() {
        Map<Long, TRentPriceType> map = new HashMap<>();
        for (TRentPriceType t : rentPriceTypeService.list()) {
            map.put(t.getId(), t);
        }
        return map;
    }

    private String rangeDesc(TRentPriceType t) {
        if (t.getSplitMode() != null && t.getSplitMode() == TRentPriceType.SPLIT_BY_DATE) {
            String from = t.getEffectDateFrom() == null ? "并网日起" : fmt(t.getEffectDateFrom());
            String to = t.getEffectDateTo() == null ? "租期结束" : fmt(t.getEffectDateTo());
            return from + " ~ " + to;
        }
        return "第" + t.getStartYear() + "-" + t.getEndYear() + "年";
    }

    /**
     * 档位的实际日期区间，按该农户的并网日期推算，如 2025-03-21 ~ 2030-03-20。
     *
     * 与 rangeDesc 是两回事、并存不互斥：
     *   rangeDesc    说"第几年到第几年"（相对概念，对谁都一样）
     *   dateRangeDesc 说"具体哪天到哪天"（跟着每户的并网日期走，户户不同）
     * 运营对着日历核对时看日期，对着合同看年份，所以两个都留着。
     *
     * 按自然日期切分的档位本身就有起止日，直接用；
     * 按年切分的档位用 CellUtil 从并网日推，口径与生成租金时的 tierRange 一致（都不超过租期截止日）。
     * 拿不到并网日期时（新建农户还没填）返回 null，前端不展示这一段。
     */
    private String dateRangeDesc(TRentPriceType t, TFarmerStation station) {
        if (t == null) {
            return null;
        }
        if (t.getSplitMode() != null && t.getSplitMode() == TRentPriceType.SPLIT_BY_DATE) {
            Date from = t.getEffectDateFrom();
            Date to = t.getEffectDateTo();
            if (from == null && to == null) {
                return null;
            }
            return (from == null ? "并网日起" : fmt(from))
                    + " ~ " + (to == null ? "租期结束" : fmt(to));
        }
        if (station == null || station.getGridDate() == null) {
            return null;
        }
        int startYear = t.getStartYear() == null ? 1 : t.getStartYear();
        int endYear = t.getEndYear() == null ? 25 : t.getEndYear();
        Date begin = CellUtil.yearRangeBegin(station.getGridDate(), startYear);
        Date finish = CellUtil.yearRangeEnd(station.getGridDate(), endYear);
        Date deadline = station.getRentDeadline();
        if (deadline != null && finish != null && finish.after(deadline)) {
            finish = deadline;
        }
        if (begin == null || finish == null) {
            return null;
        }
        return fmt(begin) + " ~ " + fmt(finish);
    }

    private String fmt(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private void require(String v, String msg) {
        if (v == null || v.trim().isEmpty()) {
            throw new JeecgBootException(msg);
        }
    }

    private boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
