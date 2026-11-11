package org.jeecg.modules.pcms.rent.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.RentLedgerListVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerQueryDTO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerSummaryVO;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.service.IFarmerAuditService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerQueryService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.util.RentQueryParamUtil;
import org.jeecg.modules.pcms.rent.util.RentRegionResolver;
import org.jeecg.modules.pcms.rent.util.TenantScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 租金台账查询实现。
 *
 * 列表要展示的字段在生成台账时就全部冗余进 t_rent_ledger 了，所以不联表：
 * 台账是"某年某期该付多少"的历史快照，农户后来改了名字或地址，
 * 已生成的台账不该跟着变，联表反而会把当时的账改掉。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentLedgerQueryServiceImpl implements IRentLedgerQueryService {

    @Autowired
    private ITRentLedgerService ledgerService;
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private TenantScopeHelper tenantScope;
    @Autowired
    private RentRegionResolver regionResolver;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private IFarmerAuditService farmerAuditService;

    @Override
    public IPage<RentLedgerListVO> pageList(RentLedgerQueryDTO query, int pageNo, int pageSize) {
        LambdaQueryWrapper<TRentLedger> w = buildWrapper(
                query == null ? new RentLedgerQueryDTO() : query, false);
        if (w == null) {
            return new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        }

        Page<TRentLedger> page = new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        IPage<TRentLedger> data = ledgerService.page(page, w);

        Map<String, Integer> siblings = siblingCounts(data.getRecords());
        Map<Long, String> projectNames = projectNameMap();
        return data.convert(e -> {
            RentLedgerListVO vo = toListVO(e, projectNames);
            vo.setSiblingCount(siblings.getOrDefault(periodKey(e), 1));
            return vo;
        });
    }

    @Override
    public List<TRentLedger> listForExport(RentLedgerQueryDTO query) {
        // 没勾选具体台账时导全量：年度和状态页签都不参与，只有用户显式填的筛选条件生效。
        // 列表页默认只显示本年度，但"导出全部"要的是这个项目/租户下的所有台账，
        // 否则跨年补导会被静默截成当年。
        LambdaQueryWrapper<TRentLedger> w = buildWrapper(
                query == null ? new RentLedgerQueryDTO() : query, true);
        if (w == null) {
            return new ArrayList<>();
        }
        return fillExportFields(ledgerService.list(w));
    }

    @Override
    public List<TRentLedger> listByIdsForExport(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<TRentLedger> w = new LambdaQueryWrapper<>();
        // 只排除逻辑删除的行；作废的行是业务可见状态，勾选了就照导
        w.eq(TRentLedger::getDelFlag, 0);
        // 勾选导出同样要过租户隔离
        tenantScope.apply(w, TRentLedger::getTenantId);
        w.in(TRentLedger::getId, ids);
        w.orderByAsc(TRentLedger::getStationId)
                .orderByAsc(TRentLedger::getRentYear)
                .orderByAsc(TRentLedger::getPeriodNo)
                .orderByAsc(TRentLedger::getId);
        return fillExportFields(ledgerService.list(w));
    }

    /**
     * 填充导出专用的派生字段（本次支付金额、统一状态名）。
     *
     * 这两个值是由其他字段算出来的（min(应付,实付)、三个正交状态合成），
     * 数据库里没有对应列，所以只能导出前在内存里补一遍。
     */
    private List<TRentLedger> fillExportFields(List<TRentLedger> list) {
        for (TRentLedger l : list) {
            l.setExportCurrentPayAmount(l.currentPayAmount());
            l.setExportStatusName(l.statusName());
        }
        return list;
    }

    @Override
    public List<RentLedgerSummaryVO> summarize(List<TRentLedger> list) {
        List<RentLedgerSummaryVO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        Map<Long, String> projectNames = projectNameMap();
        // 按「项目 + 期数」分组，用 LinkedHashMap 保持导出时的顺序
        Map<String, RentLedgerSummaryVO> grouped = new LinkedHashMap<>();
        for (TRentLedger l : list) {
            String key = l.getProjectId() + "|" + l.getPeriodName();
            RentLedgerSummaryVO vo = grouped.computeIfAbsent(key, k -> new RentLedgerSummaryVO()
                    .setProjectName(projectNames.getOrDefault(l.getProjectId(), ""))
                    .setPeriodName(l.getPeriodName())
                    .setCount(0)
                    .setPayableSum(BigDecimal.ZERO)
                    .setPaidSum(BigDecimal.ZERO));
            vo.setCount(vo.getCount() + 1);
            vo.setPayableSum(vo.getPayableSum().add(
                    l.getPeriodPayable() == null ? BigDecimal.ZERO : l.getPeriodPayable()));
            vo.setPaidSum(vo.getPaidSum().add(
                    l.getPeriodPaid() == null ? BigDecimal.ZERO : l.getPeriodPaid()));
        }
        return new ArrayList<>(grouped.values());
    }

    /** 状态页签，顺序与前端页签一致（"全部"在 statusCount 里单独算） */
    private static final String[] STATUS_TABS = {
            TRentLedger.STATUS_DRAFT,
            // 审核只按"待审核 / 驳回"两个口径出页签，不再细分到项目/业务/财务/领导四级。
            // 明细状态值仍然可以传，见 applyStatusCondition
            TRentLedger.STATUS_PENDING_AUDIT,
            TRentLedger.STATUS_REJECTED,
            TRentLedger.STATUS_PENDING_PAY,
            TRentLedger.STATUS_PAID,
            TRentLedger.STATUS_RECONCILED,
            TRentLedger.STATUS_RECON_ERROR,
            TRentLedger.STATUS_DISPUTE,
            TRentLedger.STATUS_VOID
    };

    @Override
    public Map<String, Integer> statusCount(RentLedgerQueryDTO query) {
        RentLedgerQueryDTO q = query == null ? new RentLedgerQueryDTO() : query;
        // LinkedHashMap 保证返回顺序跟页签一致，前端直接遍历渲染就行
        Map<String, Integer> counts = new LinkedHashMap<>();

        // "全部"：与列表默认口径一致（作废默认隐藏，除非显式 includeVoid=1）
        counts.put("ALL", countWithStatus(q, null));
        for (String status : STATUS_TABS) {
            counts.put(status, countWithStatus(q, status));
        }
        return counts;
    }

    /**
     * 按公共筛选条件 + 指定状态计数。
     *
     * 注意用的是 buildBaseWrapper（不含状态条件）：
     * 如果沿用列表那套 buildWrapper，它会把 query 上的 status 也拼进去，
     * 统计别的页签时就成了"状态A AND 状态B"，结果恒为 0。
     *
     * @param status null 表示"全部"，不加状态条件
     */
    private int countWithStatus(RentLedgerQueryDTO query, String status) {
        LambdaQueryWrapper<TRentLedger> w = buildBaseWrapper(query, false);
        if (w == null) {
            // 大区下没有项目这类"条件本身必然为空"的情况
            return 0;
        }
        if (status == null) {
            w.eq(query.getIncludeVoid() == null || query.getIncludeVoid() != 1,
                    TRentLedger::getVoidFlag, 0);
        } else {
            applyStatusCondition(w, status, query.getIncludeVoid());
        }
        return (int) ledgerService.count(w);
    }

    /**
     * 按查询条件拼 where。列表和导出共用，两边筛出来的结果永远一致。
     *
     * @param exportAll true=导出全量。不带"状态页签"，年度也不按本年度/历史页签收敛，
     *                  否则没勾选行时导出来的只是当前页签下那部分，不是"所有台账"
     *
     * 返回 null 表示条件本身不可能命中任何行（比如指定的大区下一个项目都没有），
     * 调用方直接返回空结果即可，不要拿着 null 去查库。
     */
    private LambdaQueryWrapper<TRentLedger> buildWrapper(RentLedgerQueryDTO query, boolean exportAll) {
        LambdaQueryWrapper<TRentLedger> w = buildBaseWrapper(query, exportAll);
        if (w == null) {
            return null;
        }
        if (!exportAll) {
            applyStatus(w, query);
        }
        applySort(w, query);
        return w;
    }

    /**
     * 公共筛选条件：租户隔离、年度、姓名/户号/地址/身份证、项目、期数、并网日期区间等。
     * **不含状态条件和排序** —— 状态统计要复用这部分，但不能把"当前选中的状态"带进去，
     * 否则统计某一个状态时其余状态的条数会全变成 0。
     *
     * 返回 null 表示条件本身不可能命中任何行。
     */
    private LambdaQueryWrapper<TRentLedger> buildBaseWrapper(RentLedgerQueryDTO query, boolean exportAll) {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        LambdaQueryWrapper<TRentLedger> w = new LambdaQueryWrapper<>();
        w.eq(TRentLedger::getDelFlag, 0);

        // 需求第14条：数据隔离。管理员（rel_tenant_ids 为空）看全部，
        // 普通用户只看自己租户的台账
        tenantScope.apply(w, TRentLedger::getTenantId);

        // 需求稿第 9 条：默认只显示本年度，历史租金页签显示往年。
        // 导出全量时这条不生效 —— 用户显式传了 rentYear 才按年度导，不传就是所有年度
        if (exportAll) {
            w.eq(query.getRentYear() != null, TRentLedger::getRentYear, query.getRentYear());
        } else if (query.getRentYear() != null) {
            w.eq(TRentLedger::getRentYear, query.getRentYear());
        } else if (query.getHistory() != null && query.getHistory() == 1) {
            w.lt(TRentLedger::getRentYear, thisYear);
        } else {
            w.eq(TRentLedger::getRentYear, thisYear);
        }

        w.like(notBlank(query.getFarmerName()), TRentLedger::getFarmerName, trim(query.getFarmerName()));
        w.like(notBlank(query.getPowerUserNo()), TRentLedger::getPowerUserNo, trim(query.getPowerUserNo()));
        w.like(notBlank(query.getInstallAddr()), TRentLedger::getInstallAddr, trim(query.getInstallAddr()));
        w.like(notBlank(query.getIdCard()), TRentLedger::getIdCard, trim(query.getIdCard()));

        w.eq(query.getStationId() != null, TRentLedger::getStationId, query.getStationId());
        w.eq(query.getProjectId() != null, TRentLedger::getProjectId, query.getProjectId());
        if (query.getProjectId() == null && notBlank(query.getRegionCode())) {
            List<Long> projectIds = projectIdsOfRegion(trim(query.getRegionCode()));
            if (projectIds.isEmpty()) {
                // 该大区下没有项目，直接返回空，不要退化成全表查询
                return null;
            }
            w.in(TRentLedger::getProjectId, projectIds);
        }

        w.eq(query.getPeriodNo() != null, TRentLedger::getPeriodNo, query.getPeriodNo());
        // auditStatus 支持逗号分隔的多个值，前端按「驳回」「待审核」一次传一组时不用拆成多次请求
        List<Integer> auditStatuses = RentQueryParamUtil.parseIntList(query.getAuditStatus());
        w.in(!auditStatuses.isEmpty(), TRentLedger::getAuditStatus, auditStatuses);
        w.eq(query.getPayStatus() != null, TRentLedger::getPayStatus, query.getPayStatus());
        w.eq(query.getReconStatus() != null, TRentLedger::getReconStatus, query.getReconStatus());
        w.eq(query.getDisputeFlag() != null, TRentLedger::getDisputeFlag, query.getDisputeFlag());

        w.ge(query.getGridDateBegin() != null, TRentLedger::getGridDate, query.getGridDateBegin());
        w.le(query.getGridDateEnd() != null, TRentLedger::getGridDate, query.getGridDateEnd());
        return w;
    }

    @Override
    public List<RentLedgerListVO> listByStation(Long stationId, Integer year) {
        int rentYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        LambdaQueryWrapper<TRentLedger> w = new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, stationId)
                .eq(TRentLedger::getRentYear, rentYear)
                .eq(TRentLedger::getDelFlag, 0);
        // 换一个 stationId 不能越权看到别的租户的台账
        tenantScope.apply(w, TRentLedger::getTenantId);
        w.orderByAsc(TRentLedger::getPeriodNo).orderByAsc(TRentLedger::getId);
        List<TRentLedger> list = ledgerService.list(w);

        Map<String, Integer> siblings = siblingCounts(list);
        Map<Long, String> projectNames = projectNameMap();
        List<RentLedgerListVO> result = new ArrayList<>();
        for (TRentLedger e : list) {
            RentLedgerListVO vo = toListVO(e, projectNames);
            vo.setSiblingCount(siblings.getOrDefault(periodKey(e), 1));
            result.add(vo);
        }
        return result;
    }

    @Override
    public RentLedgerListVO toListVO(TRentLedger e) {
        return toListVO(e, projectNameMap());
    }

    private RentLedgerListVO toListVO(TRentLedger e, Map<Long, String> projectNames) {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        boolean history = e.getRentYear() != null && e.getRentYear() < thisYear;

        RentLedgerListVO vo = new RentLedgerListVO()
                .setId(e.getId())
                .setStationId(e.getStationId())
                .setFarmerName(e.getFarmerName())
                .setPowerUserNo(e.getPowerUserNo())
                .setInstallAddr(e.getInstallAddr())
                .setIdCard(e.getIdCard())
                .setBankCardNo(e.getBankCardNo())
                .setBankCardName(e.getBankCardName())
                .setTotalBlocks(e.getTotalBlocks())
                .setTotalCapacity(e.getTotalCapacity())
                .setGridDate(e.getGridDate())
                .setGridMonthRemainDays(e.getGridMonthRemainDays())
                .setGridMonths(e.getGridMonths())
                .setRentYear(e.getRentYear())
                .setPeriodNo(e.getPeriodNo())
                .setPeriodName(e.getPeriodName())
                .setActualGenDays(e.getActualGenDays())
                .setPeriodBegin(e.getPeriodBegin())
                .setPeriodEnd(e.getPeriodEnd())
                .setCycleTotalAmount(e.getCycleTotalAmount())
                .setPeriodPayable(e.getPeriodPayable())
                .setPayableAdjustFlag(e.payableAdjusted() ? 1 : 0)
                .setAccumPayable(e.getAccumPayable())
                .setPrevAccumPaid(e.getPrevAccumPaid())
                .setPeriodPaid(e.getPeriodPaid())
                .setAccumPaid(e.getAccumPaid())
                .setCurrentPayAmount(e.currentPayAmount())
                .setDiffAmount(e.getDiffAmount())
                .setStatusCode(e.statusCode())
                .setStatusName(e.statusName())
                .setAuditStatus(e.getAuditStatus())
                .setAuditStatusName(TRentLedger.auditName(e.getAuditStatus()))
                .setAuditRound(e.auditRound())
                .setSubmitBy(e.getSubmitBy())
                .setSubmitTime(e.getSubmitTime())
                .setAuditBy(e.getAuditBy())
                .setAuditTime(e.getAuditTime())
                .setProjectAuditBy(e.getProjectAuditBy())
                .setProjectAuditTime(e.getProjectAuditTime())
                .setBusinessAuditBy(e.getBusinessAuditBy())
                .setBusinessAuditTime(e.getBusinessAuditTime())
                .setFinanceAuditBy(e.getFinanceAuditBy())
                .setFinanceAuditTime(e.getFinanceAuditTime())
                .setLeaderAuditBy(e.getLeaderAuditBy())
                .setLeaderAuditTime(e.getLeaderAuditTime())
                .setPayStatus(e.getPayStatus())
                .setReconStatus(e.getReconStatus())
                .setReconStatusName(TRentLedger.reconName(e.getReconStatus()))
                .setReconRemark(e.getReconRemark())
                .setDisputeFlag(e.getDisputeFlag())
                .setDisputeRemark(e.getDisputeRemark())
                .setDisputeRound(e.disputeRound())
                .setDisputeTime(e.getDisputeTime())
                .setDisputeCancelTime(e.getDisputeCancelTime())
                .setRejectRemark(e.getRejectRemark())
                .setVoidFlag(e.getVoidFlag())
                .setHistoryFlag(history ? 1 : 0)
                // 历史年度一律只读，其余看审核状态
                .setEditable(!history && e.editable() && !e.paid() ? 1 : 0)
                .setSiblingCount(1);

        // 需求31：带上农户审核状态，前端据此在左上角显示状态、隐藏操作按钮
        TFarmerStation station = farmerStationService.getById(e.getStationId());
        int auditStatus = station == null || station.getAuditStatus() == null
                ? TFarmerStation.AUDIT_NORMAL : station.getAuditStatus();
        vo.setFarmerAuditStatus(auditStatus)
                .setFarmerAuditStatusName(TFarmerStation.auditStatusName(auditStatus))
                // 该期是否被锁：待审核按提交时间判断，新增驳回整户锁死
                .setFarmerLocked(farmerAuditService.periodLocked(
                        e.getStationId(), e.getPeriodEnd()) ? 1 : 0);

        vo.setProjectName(projectNames.get(e.getProjectId()));
        return vo;
    }

    /** t_project 只有12行，一次读全比逐行查库划算 */
    private Map<Long, String> projectNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (TProject p : projectService.list()) {
            map.put(p.getId(), p.getProjectName());
        }
        return map;
    }

    // ------------------------------------------------------------------

    /**
     * 统计每个"电站+年度+期数"下未作废的台账条数。
     * 需求稿第 12 条：只有多于一条时前端才显示作废按钮。
     */
    private Map<String, Integer> siblingCounts(List<TRentLedger> records) {
        Map<String, Integer> counts = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return counts;
        }
        Set<Long> stationIds = new HashSet<>();
        Set<Integer> years = new HashSet<>();
        for (TRentLedger e : records) {
            stationIds.add(e.getStationId());
            years.add(e.getRentYear());
        }
        // 一次把这批电站涉及年度的台账取回来在内存里数，避免每行一次 count
        List<TRentLedger> all = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .select(TRentLedger::getStationId, TRentLedger::getRentYear, TRentLedger::getPeriodNo)
                .in(TRentLedger::getStationId, stationIds)
                .in(TRentLedger::getRentYear, years)
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0));
        for (TRentLedger e : all) {
            String key = periodKey(e);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts;
    }

    private String periodKey(TRentLedger e) {
        return e.getStationId() + "_" + e.getRentYear() + "_" + e.getPeriodNo();
    }

    /**
     * 大区 -> 项目ID列表。
     * 拆分规则（天津/廊坊同属 TJLF，按项目编码前缀区分）统一放在 RentRegionResolver，
     * 农户列表、台账列表、下拉框三处共用，避免口径不一致。
     */
    private List<Long> projectIdsOfRegion(String regionCode) {
        return regionResolver.projectIds(regionCode);
    }

    /**
     * 统一状态筛选。
     *
     * 状态是三个正交字段合出来的（见 TRentLedger.statusCode），筛选条件必须跟合成时的
     * 优先级一一对应，否则会出现"页签上有这条、点进去状态却是另一个"的错位。
     * 比如筛"已支付"要排掉已经对过账的，筛"待提交"要排掉纠纷暂停的。
     */
    private void applyStatus(LambdaQueryWrapper<TRentLedger> w, RentLedgerQueryDTO query) {
        String status = trim(query.getStatus());
        if (notBlank(status)) {
            applyStatusCondition(w, status, query.getIncludeVoid());
            return;
        }
        // 没指定状态（"全部"页签）：作废的默认藏起来，只有显式要求时才带出来
        w.eq(query.getIncludeVoid() == null || query.getIncludeVoid() != 1, TRentLedger::getVoidFlag, 0);
    }

    /**
     * 某个统一状态对应的 where 条件。
     *
     * 状态是三个正交字段（审核/支付/对账）加上纠纷、作废合成出来的，
     * 所以每个页签都要拼一组条件。列表筛选和状态统计共用这一份，
     * 保证"页签上显示的数量"和"点进去看到的列表条数"永远对得上。
     */
    private void applyStatusCondition(LambdaQueryWrapper<TRentLedger> w, String status, Integer includeVoid) {
        if (TRentLedger.STATUS_VOID.equals(status)) {
            w.eq(TRentLedger::getVoidFlag, 1);
            return;
        }
        // 作废的台账默认藏起来，只有显式要求时才带出来
        w.eq(includeVoid == null || includeVoid != 1, TRentLedger::getVoidFlag, 0);
        if (TRentLedger.STATUS_DISPUTE.equals(status)) {
            w.eq(TRentLedger::getDisputeFlag, 1);
            return;
        }
        // 除"纠纷暂停"外，其余状态都不含被纠纷标记盖住的行
        w.eq(TRentLedger::getDisputeFlag, 0);
        switch (status) {
            // 聚合页签：把四个审核级别合并成一个口径
            case TRentLedger.STATUS_PENDING_AUDIT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .in(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PENDING_VALUES);
                break;
            case TRentLedger.STATUS_REJECTED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .in(TRentLedger::getAuditStatus, TRentLedger.AUDIT_REJECTED_VALUES);
                break;
            case TRentLedger.STATUS_RECON_ERROR:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_ERROR);
                break;
            case TRentLedger.STATUS_RECONCILED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_OK);
                break;
            case TRentLedger.STATUS_PAID:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_PAID);
                break;
            case TRentLedger.STATUS_PENDING_PAY:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_APPROVED);
                break;
            case TRentLedger.STATUS_LEADER_REJECTED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_LEADER_REJECTED);
                break;
            case TRentLedger.STATUS_PENDING_LEADER_AUDIT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PENDING_LEADER);
                break;
            case TRentLedger.STATUS_FINANCE_REJECTED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_FINANCE_REJECTED);
                break;
            case TRentLedger.STATUS_PENDING_FINANCE_AUDIT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PENDING_FINANCE);
                break;
            case TRentLedger.STATUS_BUSINESS_REJECTED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_BUSINESS_REJECTED);
                break;
            case TRentLedger.STATUS_PENDING_BUSINESS_AUDIT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PENDING_BUSINESS);
                break;
            case TRentLedger.STATUS_PROJECT_REJECTED:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PROJECT_REJECTED);
                break;
            case TRentLedger.STATUS_PENDING_PROJECT_AUDIT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_PENDING_PROJECT);
                break;
            case TRentLedger.STATUS_DRAFT:
                w.eq(TRentLedger::getReconStatus, TRentLedger.RECON_NONE)
                        .eq(TRentLedger::getPayStatus, TRentLedger.PAY_UNPAID)
                        .eq(TRentLedger::getAuditStatus, TRentLedger.AUDIT_DRAFT);
                break;
            default:
                log.warn("未知的状态筛选条件[{}]，本次忽略", status);
                break;
        }
    }

    /** 排序全部走 Lambda 列引用，前端传什么都注入不进来 */
    private void applySort(LambdaQueryWrapper<TRentLedger> w, RentLedgerQueryDTO query) {
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        String field = query.getSortField() == null ? "" : query.getSortField();
        switch (field) {
            case "farmerName":
                w.orderBy(true, asc, TRentLedger::getFarmerName);
                break;
            case "gridDate":
                w.orderBy(true, asc, TRentLedger::getGridDate);
                break;
            case "periodPayable":
                w.orderBy(true, asc, TRentLedger::getPeriodPayable);
                break;
            case "periodPaid":
                w.orderBy(true, asc, TRentLedger::getPeriodPaid);
                break;
            case "diffAmount":
                w.orderBy(true, asc, TRentLedger::getDiffAmount);
                break;
            case "createTime":
                w.orderBy(true, asc, TRentLedger::getCreateTime);
                break;
            default:
                // 默认按年度、期数正序，同一农户的四期挨在一起看着顺
                w.orderByDesc(TRentLedger::getRentYear).orderByAsc(TRentLedger::getPeriodNo);
                break;
        }
        w.orderByDesc(TRentLedger::getId);
    }

    private int normalizeSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 500);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}