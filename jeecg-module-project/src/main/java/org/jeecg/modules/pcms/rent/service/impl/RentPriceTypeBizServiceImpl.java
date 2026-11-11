package org.jeecg.modules.pcms.rent.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeFormDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeSaveDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeVO;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TRent;
import org.jeecg.modules.pcms.rent.entity.TRentPriceType;
import org.jeecg.modules.pcms.rent.service.IRentPriceTypeBizService;
import org.jeecg.modules.pcms.rent.service.ITRentPriceTypeService;
import org.jeecg.modules.pcms.rent.service.ITRentService;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租金档位字典维护。需求：按项目管理租金类型。
 *
 * 【为什么改删要先查引用】
 * t_rent 里存的是"某农户某档位单价多少"。档位被引用后如果直接把年份区间改掉，
 * 或者把档位删掉，那些农户的租金就失去了计价依据——已有的台账金额不会自动跟着变，
 * 但重新生成时会算不出来或算错。所以被引用就一律拦住，让运营先去处理农户。
 *
 * 【为什么新增要成组】
 * 只加一条（比如只加 1-3 年）会让 4-25 年这段没有单价，租金直接算不出来。
 * 所以强制一次把 1-25 年覆盖齐。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentPriceTypeBizServiceImpl implements IRentPriceTypeBizService {

    /** 租期固定 25 年，档位必须覆盖满 */
    private static final int TOTAL_YEARS = 25;

    @Autowired
    private ITRentPriceTypeService priceTypeService;
    @Autowired
    private ITRentService rentService;
    @Autowired
    private OperLogHelper operLog;
    @Autowired
    private RentRoleGuard roleGuard;

    @Override
    public List<RentPriceTypeVO> listByRegion(String regionCode) {
        if (!notBlank(regionCode)) {
            throw new JeecgBootException("请指定大区");
        }
        List<RentPriceTypeVO> list = new ArrayList<>();
        for (TRentPriceType t : priceTypeService.list(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode.trim())
                .orderByAsc(TRentPriceType::getSortNo)
                .orderByAsc(TRentPriceType::getId))) {
            int used = usedCount(t.getId());
            list.add(new RentPriceTypeVO()
                    .setId(t.getId())
                    .setRegionCode(t.getRegionCode())
                    .setTypeCode(t.getTypeCode())
                    .setTypeName(t.getTypeName())
                    .setYearRangeDesc(yearRangeDesc(t))
                    .setStartYear(t.getStartYear())
                    .setEndYear(t.getEndYear())
                    .setSplitMode(t.getSplitMode())
                    .setSortNo(t.getSortNo())
                    .setUsedCount(used)
                    .setEditable(used == 0 ? 1 : 0)
                    .setBlockReason(used == 0 ? null
                            : "已有 " + used + " 条租金单价在用这个档位，不能修改或删除"));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTypes(RentPriceTypeSaveDTO form) {
        roleGuard.requireLedgerOps("新增租金档位");
        if (form == null || !notBlank(form.getRegionCode())) {
            throw new JeecgBootException("请指定大区");
        }
        String regionCode = form.getRegionCode().trim();
        List<RentPriceTypeFormDTO> types = form.getTypes();
        checkCoverage(types);

        // 按起始年排好，sortNo 没传就按顺序补，保证先生效的排前面
        List<RentPriceTypeFormDTO> sorted = new ArrayList<>(types);
        sorted.sort(Comparator.comparingInt(t -> t.getStartYear()));
        int seq = nextSortNo(regionCode);

        for (RentPriceTypeFormDTO t : sorted) {
            if (!notBlank(t.getTypeName())) {
                throw new JeecgBootException("档位名称不能为空");
            }
            String code = buildTypeCode(regionCode, t.getStartYear(), t.getEndYear());
            if (existsCode(regionCode, code)) {
                throw new JeecgBootException(String.format(
                        "%s 已经存在第%d-%d年的档位了，请先删除或改名后再加",
                        regionCode, t.getStartYear(), t.getEndYear()));
            }
            priceTypeService.save(new TRentPriceType()
                    .setRegionCode(regionCode)
                    .setTypeCode(code)
                    .setTypeName(t.getTypeName().trim())
                    // 按年切分：相对每户的并网日期
                    .setSplitMode(TRentPriceType.SPLIT_BY_YEAR)
                    .setStartYear(t.getStartYear())
                    .setEndYear(t.getEndYear())
                    .setSortNo(t.getSortNo() == null ? seq++ : t.getSortNo()));
        }

        operLog.recordText(TOperLog.ACTION_INSERT, TOperLog.BIZ_PRICE_TYPE, null,
                String.format("[%s] 新增租金档位：%s", regionCode, describe(sorted)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editType(RentPriceTypeFormDTO form) {
        roleGuard.requireLedgerOps("修改租金档位");
        if (form == null || form.getId() == null) {
            throw new JeecgBootException("缺少档位ID");
        }
        TRentPriceType before = getExisting(form.getId());
        assertNotUsed(before, "修改");

        if (!notBlank(form.getTypeName())) {
            throw new JeecgBootException("档位名称不能为空");
        }
        if (form.getStartYear() == null || form.getEndYear() == null) {
            throw new JeecgBootException("起始年和结束年都不能为空");
        }
        if (form.getStartYear() > form.getEndYear()) {
            throw new JeecgBootException(String.format("起始年(%d)不能大于结束年(%d)",
                    form.getStartYear(), form.getEndYear()));
        }
        if (form.getStartYear() < 1 || form.getEndYear() > TOTAL_YEARS) {
            throw new JeecgBootException(String.format("年份区间必须落在第1-%d年之内", TOTAL_YEARS));
        }

        TRentPriceType update = new TRentPriceType()
                .setId(before.getId())
                .setTypeName(form.getTypeName().trim())
                .setStartYear(form.getStartYear())
                .setEndYear(form.getEndYear());
        if (form.getSortNo() != null) {
            update.setSortNo(form.getSortNo());
        }
        // 年份变了，编码跟着改，否则编码和区间对不上
        if (!eq(before.getStartYear(), form.getStartYear())
                || !eq(before.getEndYear(), form.getEndYear())) {
            update.setTypeCode(buildTypeCode(before.getRegionCode(),
                    form.getStartYear(), form.getEndYear()));
        }
        priceTypeService.updateById(update);

        operLog.recordText(TOperLog.ACTION_UPDATE, TOperLog.BIZ_PRICE_TYPE, before.getId(),
                String.format("[%s] 修改租金档位：%s → %s",
                        before.getRegionCode(), describe(before),
                        describe(form.getTypeName(), form.getStartYear(), form.getEndYear())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        roleGuard.requireLedgerOps("删除租金档位");
        TRentPriceType before = getExisting(id);
        assertNotUsed(before, "删除");
        priceTypeService.removeById(id);
        operLog.recordText(TOperLog.ACTION_DELETE, TOperLog.BIZ_PRICE_TYPE, id,
                String.format("[%s] 删除租金档位：%s", before.getRegionCode(), describe(before)));
    }

    // ------------------------------------------------------------------

    /**
     * 校验这一组档位从第1年连续覆盖到第25年：不重叠、不留空。
     *
     * 按起始年排序后逐个推进游标 expect，每个档位必须正好从 expect 开始，
     * 全部走完 expect 应正好是 26（即覆盖到第25年）。
     * 这样既能挡住"只加一段"，也能挡住"两段之间有缝"和"两段重叠"。
     */
    private void checkCoverage(List<RentPriceTypeFormDTO> types) {
        if (types == null || types.isEmpty()) {
            throw new JeecgBootException("请提交要新增的租金档位");
        }
        for (RentPriceTypeFormDTO t : types) {
            if (t.getStartYear() == null || t.getEndYear() == null) {
                throw new JeecgBootException("档位的起始年和结束年都不能为空");
            }
            if (t.getStartYear() > t.getEndYear()) {
                throw new JeecgBootException(String.format("起始年(%d)不能大于结束年(%d)",
                        t.getStartYear(), t.getEndYear()));
            }
            if (t.getStartYear() < 1 || t.getEndYear() > TOTAL_YEARS) {
                throw new JeecgBootException(String.format("年份区间必须落在第1-%d年之内", TOTAL_YEARS));
            }
        }
        List<RentPriceTypeFormDTO> sorted = new ArrayList<>(types);
        sorted.sort(Comparator.comparingInt(RentPriceTypeFormDTO::getStartYear));

        int expect = 1;
        for (RentPriceTypeFormDTO t : sorted) {
            if (t.getStartYear() != expect) {
                throw new JeecgBootException(String.format(
                        "档位要从第1年连续覆盖到第%d年：第%d年这段%s",
                        TOTAL_YEARS, expect,
                        t.getStartYear() > expect ? "没被覆盖" : "重复覆盖了"));
            }
            expect = t.getEndYear() + 1;
        }
        if (expect != TOTAL_YEARS + 1) {
            throw new JeecgBootException(String.format(
                    "档位要覆盖到第%d年，当前只到第%d年", TOTAL_YEARS, expect - 1));
        }
    }

    /** 档位被多少条（未删除的）租金单价引用 */
    private int usedCount(Long priceTypeId) {
        return (int) rentService.count(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getPriceTypeId, priceTypeId)
                .eq(TRent::getDelFlag, 0));
    }

    /** 已被引用就不让改删，把"被几户在用"说清楚，运营好去处理 */
    private void assertNotUsed(TRentPriceType t, String action) {
        int used = usedCount(t.getId());
        if (used > 0) {
            throw new JeecgBootException(String.format(
                    "档位[%s]已被 %d 条租金单价引用，不能%s。"
                            + "请先到相关农户的安装信息里把这个档位去掉，或者改用其他档位",
                    t.getTypeName(), used, action));
        }
    }

    private TRentPriceType getExisting(Long id) {
        TRentPriceType t = priceTypeService.getById(id);
        if (t == null) {
            throw new JeecgBootException("租金档位不存在");
        }
        return t;
    }

    /** 同大区里下一个可用的排序号 */
    private int nextSortNo(String regionCode) {
        int max = 0;
        for (TRentPriceType t : priceTypeService.list(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode))) {
            if (t.getSortNo() != null && t.getSortNo() > max) {
                max = t.getSortNo();
            }
        }
        return max + 1;
    }

    private boolean existsCode(String regionCode, String typeCode) {
        return priceTypeService.count(new LambdaQueryWrapper<TRentPriceType>()
                .eq(TRentPriceType::getRegionCode, regionCode)
                .eq(TRentPriceType::getTypeCode, typeCode)) > 0;
    }

    /** 档位编码，沿用库里已有的写法：TJ_Y1_5 这种 */
    private String buildTypeCode(String regionCode, int startYear, int endYear) {
        return codePrefix(regionCode) + "_Y" + startYear + "_" + endYear;
    }

    private String codePrefix(String regionCode) {
        switch (regionCode == null ? "" : regionCode.toUpperCase()) {
            case "BJ":
                return "BJ";
            case "TJLF":
                return "TJ";
            case "LF":
                return "LF";
            case "XZ":
                return "XZ";
            case "HAIN":
                return "HN";
            case "HENAN":
                return "HEN";
            default:
                return regionCode;
        }
    }

    private String yearRangeDesc(TRentPriceType t) {
        if (t.getSplitMode() != null && t.getSplitMode() == TRentPriceType.SPLIT_BY_DATE) {
            return "按自然日期";
        }
        return "第" + t.getStartYear() + "-" + t.getEndYear() + "年";
    }

    private String describe(List<RentPriceTypeFormDTO> types) {
        List<String> parts = new ArrayList<>();
        for (RentPriceTypeFormDTO t : types) {
            parts.add(describe(t.getTypeName(), t.getStartYear(), t.getEndYear()));
        }
        return String.join("、", parts);
    }

    private String describe(String name, Integer startYear, Integer endYear) {
        return String.format("%s（第%d-%d年）", name, startYear, endYear);
    }

    private String describe(TRentPriceType t) {
        return describe(t.getTypeName(), t.getStartYear(), t.getEndYear());
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}