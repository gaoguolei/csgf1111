package org.jeecg.modules.pcms.rent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TInstallInfo;
import org.jeecg.modules.pcms.rent.entity.TInstallType;
import org.jeecg.modules.pcms.rent.entity.TRent;
import org.jeecg.modules.pcms.rent.entity.TRentLedgerMonth;
import org.jeecg.modules.pcms.rent.entity.TRentPriceType;
import org.jeecg.modules.pcms.rent.util.CellUtil;
import org.jeecg.modules.pcms.rent.util.RentPeriodUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 租金金额计算引擎。台账里所有"系统计算不可修改"的金额都出自这里。
 *
 * 计算口径（需求稿第 7 条）：
 *   月租金 = 该月适用档位的年租金 ÷ 12
 *   月金额 = 月租金 ÷ 当月自然天数 × 当月计租天数
 * 整月时 计租天数 = 当月天数，月金额正好等于月租金，不会因为大小月产生误差。
 *
 * 一个电站可能同时有多条安装信息（双坡 + 阳光房），每条又有多个档位
 * （前10年/后15年），所以先把它们摊平成一组"某段时间内年租金是多少"的
 * 计划（Plan），之后所有区间金额都在这组计划上算，档位在月中切换也不会算错。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class RentCalcEngine {

    /** 金额对外统一保留两位 */
    private static final int MONEY_SCALE = 2;
    /** 中间过程多留几位，避免逐月累加时被四舍五入啃掉 */
    private static final int CALC_SCALE = 8;
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    @Autowired
    private ITInstallInfoService installInfoService;
    @Autowired
    private ITInstallTypeService installTypeService;
    @Autowired
    private ITRentService rentService;
    @Autowired
    private ITRentPriceTypeService rentPriceTypeService;

    /**
     * 把电站的安装信息 + 租金档位摊平成按时间排布的计划表。
     * 每条计划表示"从 begin 到 end 这段时间，年租金是 annualAmount"。
     */
    public List<Plan> buildPlans(TFarmerStation station) {
        List<Plan> plans = new ArrayList<>();
        if (station == null || station.getGridDate() == null) {
            return plans;
        }
        Date gridDate = RentPeriodUtil.truncate(station.getGridDate());
        Date deadline = station.getRentDeadline() == null
                ? null : RentPeriodUtil.truncate(station.getRentDeadline());
        if (deadline == null) {
            // 租金截止日期理论上由并网日期加租期算出，缺了就按 25 年兜底，
            // 否则整条电站算不出任何金额
            deadline = RentPeriodUtil.truncate(CellUtil.rentDeadline(gridDate, 25));
        }

        List<TInstallInfo> installs = installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, station.getId())
                .eq(TInstallInfo::getDelFlag, 0));
        if (installs.isEmpty()) {
            return plans;
        }
        Map<Long, Integer> blockMap = new HashMap<>();
        List<Long> installIds = new ArrayList<>();
        for (TInstallInfo i : installs) {
            installIds.add(i.getId());
            blockMap.put(i.getId(), i.getBlockCount() == null ? 0 : i.getBlockCount());
        }

        List<TRent> rents = rentService.list(new LambdaQueryWrapper<TRent>()
                .in(TRent::getInstallId, installIds)
                .eq(TRent::getDelFlag, 0));
        if (rents.isEmpty()) {
            return plans;
        }
        Map<Long, TRentPriceType> priceMap = priceTypeMap();

        for (TRent r : rents) {
            TRentPriceType pt = priceMap.get(r.getPriceTypeId());
            if (pt == null) {
                log.warn("租金记录 id={} 的档位 {} 已不存在，跳过", r.getId(), r.getPriceTypeId());
                continue;
            }
            BigDecimal annual = annualAmount(r, blockMap.get(r.getInstallId()));
            if (annual == null || annual.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Date[] range = tierRange(pt, gridDate, deadline);
            if (range == null) {
                continue;
            }
            // 需求第21条：改价时旧单价只在次月1号前生效、新单价从次月1号起生效。
            // 单价的生效区间还要跟档位本身的生效区间取交集，两者都满足才计这个价。
            Date effBegin = r.getEffectDateFrom() == null ? range[0] : r.getEffectDateFrom();
            Date effEnd = r.getEffectDateTo() == null ? range[1] : r.getEffectDateTo();
            range = RentPeriodUtil.intersect(effBegin, effEnd, range[0], range[1]);
            if (range == null) {
                continue;
            }
            plans.add(new Plan(range[0], range[1], annual, r.getInstallId(), pt.getTypeName(),
                    pt.getSortNo() == null ? Integer.MAX_VALUE : pt.getSortNo()));
        }
        return resolveOverlaps(plans, installs, station);
    }

    /**
     * 同一条安装信息下，任意时刻只能有一个租金档位生效，重叠时保留优先档位。
     *
     * 海南一种房型挂着"前10年/后15年"和"1-8年/9-25年"两套档位，这两套各自都覆盖整个租期，
     * 是两版合同口径，只会用其中一套。要是两套单价都填了，逐条累加就会把租金算成两倍。
     * 项目经理定的规则是"两套都填时一律以前10/后15那套为准，另一套忽略"，
     * 这里不把编码写死，而是按档位字典的 sort_no 定优先级——排在前面的赢，
     * 需求里各项目的档位清单只是举例、以后还会加档位，写死编码迟早要回来改。
     * 海南四档的 sort_no 已在建表脚本里钉成 前10=1 后15=2 1-8=3 9-25=4，
     * 效果就是项目经理要的那套优先。
     *
     * 河南的"按年档位"和"按自然日期档位"虽然也并存，但两批农户零重叠、
     * 每户只落其中一套，正常数据走不到取舍这一步。
     */
    List<Plan> resolveOverlaps(List<Plan> plans, List<TInstallInfo> installs, TFarmerStation station) {
        if (plans == null || plans.size() < 2) {
            return plans;
        }
        // 按 安装信息 -> 排序号 -> 档位名 排好，排在前面的就是优先档位
        List<Plan> sorted = new ArrayList<>(plans);
        sorted.sort((x, y) -> {
            int c = Long.compare(nvlId(x.getInstallId()), nvlId(y.getInstallId()));
            if (c != 0) {
                return c;
            }
            c = Integer.compare(x.getSortNo(), y.getSortNo());
            if (c != 0) {
                return c;
            }
            return nvlName(x.getPriceTypeName()).compareTo(nvlName(y.getPriceTypeName()));
        });

        List<Plan> kept = new ArrayList<>();
        for (Plan p : sorted) {
            Plan winner = null;
            Date[] cross = null;
            for (Plan k : kept) {
                if (nvlId(k.getInstallId()) != nvlId(p.getInstallId())) {
                    // 不同安装信息（双坡 + 阳光房）本来就该同时计租，金额相加是对的
                    continue;
                }
                Date[] c = RentPeriodUtil.intersect(k.getBegin(), k.getEnd(), p.getBegin(), p.getEnd());
                if (c != null) {
                    winner = k;
                    cross = c;
                    break;
                }
            }
            if (winner == null) {
                kept.add(p);
                continue;
            }
            if (winner.getSortNo() == p.getSortNo()) {
                // 两档排序号一样，谁该让路是猜不出来的，宁可不生成也不能算错钱
                throw new JeecgBootException(String.format(
                        "%s 的租金档位有冲突：[%s]和[%s]在 %s ~ %s 这段时间同时生效，"
                                + "而且两档在字典里的排序号相同，无法判断该按哪一档计租，"
                                + "请先到农户详情里删掉多余的那一档，或调整档位字典的排序号",
                        installName(p.getInstallId(), installs),
                        winner.getPriceTypeName(), p.getPriceTypeName(),
                        RentPeriodUtil.ymd(cross[0]), RentPeriodUtil.ymd(cross[1])));
            }
            // 数据本身就有问题，租金还是照算，但要留痕，方便事后核对是不是丢错了档
            log.warn("电站[{}/{}]的{}同时挂了[{}]和[{}]两档，{} ~ {} 重叠，"
                            + "按档位排序号取[{}]，忽略[{}]",
                    station == null ? null : station.getFarmerName(),
                    station == null ? null : station.getPowerUserNo(),
                    installName(p.getInstallId(), installs),
                    winner.getPriceTypeName(), p.getPriceTypeName(),
                    RentPeriodUtil.ymd(cross[0]), RentPeriodUtil.ymd(cross[1]),
                    winner.getPriceTypeName(), p.getPriceTypeName());
        }
        return kept;
    }

    private long nvlId(Long id) {
        return id == null ? -1L : id;
    }

    private String nvlName(String s) {
        return s == null ? "" : s;
    }

    /** 报错文案里用房型加型号定位，比甩一个 installId 好找 */
    private String installName(Long installId, List<TInstallInfo> installs) {
        for (TInstallInfo i : installs) {
            if (i.getId().equals(installId)) {
                TInstallType type = installTypeService.getById(i.getInstallTypeId());
                String typeName = type == null ? "安装信息" : type.getTypeName();
                return i.getModuleModel() == null ? typeName : typeName + "(" + i.getModuleModel() + ")";
            }
        }
        return "安装信息";
    }

    /**
     * 一段时间内的应付金额，按自然月逐月累加。
     * 档位在月中切换时，该月会被切成两段分别算，不会整月按一个档位记。
     */
    public BigDecimal amountForRange(List<Plan> plans, Date from, Date to) {
        if (plans == null || plans.isEmpty() || from == null || to == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Plan p : plans) {
            Date[] cross = RentPeriodUtil.intersect(p.getBegin(), p.getEnd(), from, to);
            if (cross == null) {
                continue;
            }
            for (RentPeriodUtil.Seg seg : RentPeriodUtil.monthSegments(cross[0], cross[1])) {
                total = total.add(segAmount(p.getAnnualAmount(), seg.getDays(), seg.getMonthDays()));
            }
        }
        return money(total);
    }

    /**
     * 全租期理论总额：从并网日算到租金截止日。
     * 这就是台账上的"周期应付累计总金额"，一条电站只跟安装信息和单价有关，
     * 与期数无关，所以生成一年四期时算一次就够。
     */
    public BigDecimal cycleTotal(TFarmerStation station, List<Plan> plans) {
        if (station == null || station.getGridDate() == null) {
            return BigDecimal.ZERO;
        }
        Date deadline = station.getRentDeadline() == null
                ? CellUtil.rentDeadline(station.getGridDate(), 25) : station.getRentDeadline();
        return amountForRange(plans, station.getGridDate(), deadline);
    }

    /**
     * 生成本期的月明细。每个自然月一行，天数按 [from, to] 与该月的交集算。
     * 这里只填理论值，人工录入的实际发电天数由调用方回填后再走 applyGenDays。
     */
    public List<TRentLedgerMonth> buildMonths(List<Plan> plans, Date from, Date to) {
        List<TRentLedgerMonth> months = new ArrayList<>();
        for (RentPeriodUtil.Seg seg : RentPeriodUtil.monthSegments(from, to)) {
            BigDecimal amount = amountForRange(plans, seg.getBegin(), seg.getEnd());
            months.add(new TRentLedgerMonth()
                    .setYm(seg.getYm())
                    .setSegBegin(seg.getBegin())
                    .setSegEnd(seg.getEnd())
                    .setMonthDays(seg.getMonthDays())
                    .setRentDays(seg.getDays())
                    .setMonthRent(monthlyRentAt(plans, seg.getBegin()))
                    .setTheoryAmount(amount)
                    .setAmount(amount));
        }
        return months;
    }

    /**
     * 按实际发电天数折算某月金额。
     *
     * 理论金额是按 rentDays 算出来的，人工把发电天数改成 genDays 之后，
     * 金额按同一个日单价等比缩放：月金额 ÷ 计租天数 × 发电天数。
     * 这样即使该月跨了两个档位，缩放后各档位的占比也保持不变。
     */
    public BigDecimal applyGenDays(BigDecimal theoreticalAmount, Integer rentDays, Integer genDays) {
        if (theoreticalAmount == null) {
            return BigDecimal.ZERO;
        }
        if (genDays == null || rentDays == null || rentDays <= 0) {
            return money(theoreticalAmount);
        }
        if (genDays.equals(rentDays)) {
            return money(theoreticalAmount);
        }
        return money(theoreticalAmount
                .divide(new BigDecimal(rentDays), CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(genDays)));
    }

    /** 某一天所处档位的满月租金，供月明细展示 */
    public BigDecimal monthlyRentAt(List<Plan> plans, Date day) {
        BigDecimal total = BigDecimal.ZERO;
        if (plans == null || day == null) {
            return total;
        }
        Date d = RentPeriodUtil.truncate(day);
        for (Plan p : plans) {
            if (!d.before(p.getBegin()) && !d.after(p.getEnd())) {
                total = total.add(p.getAnnualAmount()
                        .divide(MONTHS_PER_YEAR, CALC_SCALE, RoundingMode.HALF_UP));
            }
        }
        return money(total);
    }

    /** 金额统一两位小数，四舍五入 */
    public static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------

    /** 月租金 ÷ 当月天数 × 片段天数 */
    private BigDecimal segAmount(BigDecimal annual, int days, int monthDays) {
        if (days <= 0 || monthDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal monthly = annual.divide(MONTHS_PER_YEAR, CALC_SCALE, RoundingMode.HALF_UP);
        if (days == monthDays) {
            return monthly;
        }
        return monthly.divide(new BigDecimal(monthDays), CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(days));
    }

    /**
     * 档位的生效区间。
     * 按年切分的档位相对每户并网日推算，按自然日期切分的档位对全体农户是同一时点；
     * 两种都不能超出租金截止日。
     */
    private Date[] tierRange(TRentPriceType pt, Date gridDate, Date deadline) {
        Date begin;
        Date end;
        if (pt.getSplitMode() != null && pt.getSplitMode() == TRentPriceType.SPLIT_BY_DATE) {
            begin = pt.getEffectDateFrom() == null ? gridDate : pt.getEffectDateFrom();
            end = pt.getEffectDateTo() == null ? deadline : pt.getEffectDateTo();
        } else {
            int startYear = pt.getStartYear() == null ? 1 : pt.getStartYear();
            int endYear = pt.getEndYear() == null ? 25 : pt.getEndYear();
            begin = CellUtil.yearRangeBegin(gridDate, startYear);
            end = CellUtil.yearRangeEnd(gridDate, endYear);
        }
        return RentPeriodUtil.intersect(begin, end, gridDate, deadline);
    }

    /**
     * 年租金。t_rent.amount 在保存安装信息时已经按"单价 × 块数"算好了，
     * 这里优先取它；历史数据 amount 为空时用单价现算一次。
     */
    private BigDecimal annualAmount(TRent r, Integer blockCount) {
        if (r.getAmount() != null && r.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            return r.getAmount();
        }
        if (r.getUnitPrice() == null || blockCount == null || blockCount <= 0) {
            return null;
        }
        return r.getUnitPrice().multiply(new BigDecimal(blockCount));
    }

    private Map<Long, TRentPriceType> priceTypeMap() {
        Map<Long, TRentPriceType> map = new HashMap<>();
        for (TRentPriceType t : rentPriceTypeService.list()) {
            map.put(t.getId(), t);
        }
        return map;
    }

    /** 某段时间内年租金是多少 */
    public static class Plan {
        private final Date begin;
        private final Date end;
        private final BigDecimal annualAmount;
        private final Long installId;
        private final String priceTypeName;
        /** 档位字典里的排序号，同一安装信息下档位重叠时数字小的优先 */
        private final int sortNo;

        public Plan(Date begin, Date end, BigDecimal annualAmount, Long installId,
                    String priceTypeName, int sortNo) {
            this.begin = begin;
            this.end = end;
            this.annualAmount = annualAmount;
            this.installId = installId;
            this.priceTypeName = priceTypeName;
            this.sortNo = sortNo;
        }

        public int getSortNo() {
            return sortNo;
        }

        public Date getBegin() {
            return begin;
        }

        public Date getEnd() {
            return end;
        }

        public BigDecimal getAnnualAmount() {
            return annualAmount;
        }

        public Long getInstallId() {
            return installId;
        }

        public String getPriceTypeName() {
            return priceTypeName;
        }
    }
}
