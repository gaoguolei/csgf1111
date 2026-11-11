package org.jeecg.modules.pcms.rent.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.IntConsumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.*;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.entity.TRentAuditRecord;
import org.jeecg.modules.pcms.rent.entity.TRentDisputeRecord;
import org.jeecg.modules.pcms.rent.entity.TRentGenerateTask;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.entity.TRentLedgerMonth;
import org.jeecg.modules.pcms.rent.service.IFarmerAuditService;
import org.jeecg.modules.pcms.rent.service.IFarmerStationBizService;
import org.jeecg.modules.pcms.rent.service.IRentAttachService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerBizService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerQueryService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.ITRentAuditRecordService;
import org.jeecg.modules.pcms.rent.service.ITRentDisputeRecordService;
import org.jeecg.modules.pcms.rent.service.ITRentGenerateTaskService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerMonthService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.service.RentCalcEngine;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.jeecg.modules.pcms.rent.util.RentPeriodUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租金台账的生成、编辑与审核流转。
 *
 * 几条贯穿始终的规则：
 * 1. 能改的只有"每月实际发电天数"和"本期实付金额"两处，其余金额一律系统算
 * 2. 累计类金额（截止本期累计应付/实付）永远由前一期顺推，任何一期改了实付金额，
 *    后面所有期都会被重算，不会出现前后对不上的账
 * 3. 审核中和审核通过的台账一律不许动，包括改数据、改标记、作废
 * 4. 标记纠纷、取消纠纷、驳回、作废都必须填备注，落在操作日志里
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentLedgerBizServiceImpl implements IRentLedgerBizService {

    /** 分批查电站的批大小，避免几万条一次性进内存 */
    private static final int STATION_BATCH = 500;

    /** 生成任务超过这个时长还没结束，按中断处理（正常几十秒到几分钟） */
    private static final long STALE_TASK_MS = 2 * 60 * 60 * 1000L;

    @Autowired
    private ITRentLedgerService ledgerService;
    @Autowired
    private ITRentLedgerMonthService ledgerMonthService;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private ITRentAuditRecordService auditRecordService;
    @Autowired
    private ITRentDisputeRecordService disputeRecordService;
    @Autowired
    private RentCalcEngine calcEngine;
    @Autowired
    private OperLogHelper operLog;
    @Autowired
    private IRentLedgerQueryService queryService;
    @Autowired
    private RentRoleGuard roleGuard;
    @Autowired
    private IRentAttachService attachService;
    @Autowired
    private IFarmerStationBizService farmerStationBizService;
    @Autowired
    private IFarmerAuditService farmerAuditService;
    @Autowired
    private ITRentGenerateTaskService generateTaskService;
    /** 异步执行体。这里只负责提交，跑的是它自己的线程池 */
    @Autowired
    private RentGenerateTaskRunner generateTaskRunner;
    /** 数据范围：普通用户按自己租户，管理员按顶部租户下拉 */
    @Autowired
    private org.jeecg.modules.pcms.rent.util.TenantScopeHelper tenantScope;

    // ==================== 生成 ====================

    @Override
    public RentGenerateResultVO generateYear(Integer year) {
        // 定时任务走同步版本，跑完才返回；HTTP 接口走 submitGenerate
        return generateYear(year, operLog.currentUser(), null, null, null);
    }

    @Override
    public RentGenerateResultVO generateYear(Integer year, String operUser, IntConsumer onProgress,
                                             Long projectId, List<Integer> tenantIds) {
        int rentYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        long start = System.currentTimeMillis();
        RentGenerateResultVO result = new RentGenerateResultVO().setRentYear(rentYear);
        // 项目档案只有十几行，一次读全，别在几万个电站的循环里逐个查
        Map<Long, Integer> periodCountByProject = periodCountByProject();

        // 按 id 分批往后翻，中途新增的电站不会导致漏读或重复读
        Long lastId = 0L;
        // 需求34：结果弹窗要写明本次生成/重算覆盖的时间段。
        // 批量生成按所有电站各期实际计租区间的包络取最小起 / 最大止
        Date rangeBegin = null;
        Date rangeEnd = null;
        while (true) {
            LambdaQueryWrapper<TFarmerStation> w = stationScope(projectId, tenantIds);
            List<TFarmerStation> batch = farmerStationService.list(w
                    .gt(TFarmerStation::getId, lastId)
                    .orderByAsc(TFarmerStation::getId)
                    .last("limit " + STATION_BATCH));
            if (batch.isEmpty()) {
                break;
            }
            for (TFarmerStation station : batch) {
                lastId = station.getId();
                result.setStationCount(result.getStationCount() + 1);
                try {
                    Date[] range = generateStation(station, rentYear, result, false,
                            periodCountByProject, null, null);
                    if (range != null) {
                        if (rangeBegin == null || range[0].before(rangeBegin)) {
                            rangeBegin = range[0];
                        }
                        if (rangeEnd == null || range[1].after(rangeEnd)) {
                            rangeEnd = range[1];
                        }
                    }
                } catch (Exception e) {
                    // 一户出错不能让整年生成停摆，记下来继续跑
                    log.error("生成台账失败 stationId={} year={}", station.getId(), rentYear, e);
                    result.addFail(desc(station) + "：" + briefMsg(e));
                }
                if (onProgress != null) {
                    // 异步任务靠这个刷进度，processed 就是已处理的电站数
                    onProgress.accept(result.getStationCount());
                }
            }
            if (batch.size() < STATION_BATCH) {
                break;
            }
        }
        if (rangeBegin != null) {
            result.setPeriodRange(RentPeriodUtil.ymd(rangeBegin) + " ~ " + RentPeriodUtil.ymd(rangeEnd));
        }

        // 缺并网日期是最常见的一类失败，几十上百户逐条列会把真正的问题淹掉，合并成一条
        String noGridDateMsg = result.noGridDateSummary();
        if (noGridDateMsg != null) {
            result.addFailSummary(noGridDateMsg);
        }

        result.setCostMs(System.currentTimeMillis() - start);
        operLog.recordText(operUser, TOperLog.ACTION_GENERATE, TOperLog.BIZ_LEDGER, null,
                String.format("生成%d年度租金台账：电站%d个，新生成%d条，跳过%d条，失败%d个，耗时%dms",
                        rentYear, result.getStationCount(), result.getCreatedCount(),
                        result.getSkippedCount(), result.getFailCount(), result.getCostMs()));
        log.info("{}年度租金台账生成完成 {}", rentYear, result);
        return result;
    }

    /**
     * 电站查询的公共范围：未删除 + 项目 + 租户。
     *
     * 导出/列表一直按"当前项目/租户"过滤，生成和重新生成以前是全场跑的，
     * 一个项目经理点一下就会把所有项目的台账都算一遍，必须收在同一个范围里。
     *
     * @param tenantIds null=不按租户过滤；空列表=没有可用租户，查不到任何数据
     */
    private LambdaQueryWrapper<TFarmerStation> stationScope(Long projectId, List<Integer> tenantIds) {
        LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<TFarmerStation>()
                .eq(TFarmerStation::getDelFlag, 0)
                .eq(projectId != null, TFarmerStation::getProjectId, projectId);
        if (tenantIds != null) {
            if (tenantIds.isEmpty()) {
                // 传了租户却一个都没解析出来：宁可查不到，也不能把别人的数据算进去
                w.eq(TFarmerStation::getId, -1L);
            } else {
                w.in(TFarmerStation::getTenantId, tenantIds);
            }
        }
        return w;
    }

    @Override
    public RentGenerateTaskVO submitGenerate(Integer year, Long projectId) {
        roleGuard.requireLedgerOps("生成租金");
        int rentYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;

        // 异步线程里没有请求上下文（也读不到 renterId），租户范围必须在请求线程里先定下来，
        // 否则后台线程里 allowedTenantIds() 会退化成"不过滤"，把别的租户的数据也算进去
        List<Integer> tenantIds = tenantScope.allowedTenantIds();
        if (tenantIds != null && tenantIds.isEmpty()) {
            throw new JeecgBootException("当前账号没有可操作的租户或项目，无法生成台账");
        }

        // 单线程池 + 任务表双重挡重复提交：两个年度生成一起跑只会互相拖慢、抢数据库连接
        TRentGenerateTask running = generateTaskService.getOne(new LambdaQueryWrapper<TRentGenerateTask>()
                .in(TRentGenerateTask::getStatus,
                        TRentGenerateTask.STATUS_PENDING, TRentGenerateTask.STATUS_RUNNING)
                .orderByDesc(TRentGenerateTask::getCreateTime)
                .last("limit 1"), false);
        if (running != null && running.getCreateTime() != null
                && running.getCreateTime().before(new Date(System.currentTimeMillis() - STALE_TASK_MS))) {
            // 服务重启会把后台线程带走，任务就永远停在 RUNNING，把后面所有提交都挡住。
            // 超过两小时（正常几十秒到几分钟）就按中断处理，让用户能重新提交
            generateTaskService.updateById(new TRentGenerateTask()
                    .setId(running.getId())
                    .setStatus(TRentGenerateTask.STATUS_FAILED)
                    .setErrorMsg("任务超过两小时未结束，按中断处理（服务重启会中断后台任务）")
                    .setFinishTime(new Date()));
            running = null;
        }
        if (running != null) {
            throw new JeecgBootException(String.format(
                    "%d年度台账正在后台生成（已处理 %d/%d 个电站），请等它跑完再提交",
                    running.getRentYear(),
                    running.getProgress() == null ? 0 : running.getProgress(),
                    running.getTotalStations() == null ? 0 : running.getTotalStations()));
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        String operUser = operLog.currentUser();
        long stationTotal = farmerStationService.count(stationScope(projectId, tenantIds));

        generateTaskService.save(new TRentGenerateTask()
                .setId(taskId)
                .setStatus(TRentGenerateTask.STATUS_PENDING)
                .setRentYear(rentYear)
                .setTotalStations((int) stationTotal)
                .setProgress(0)
                .setCreatedCount(0)
                .setSkippedCount(0)
                .setFailCount(0)
                .setCostMs(0L)
                .setCreateBy(operUser)
                .setCreateTime(new Date()));

        try {
            // 提交给后台线程就返回，几万个电站的遍历在 rentGenerateExecutor 里跑
            generateTaskRunner.run(taskId, rentYear, operUser, projectId, tenantIds);
        } catch (Exception e) {
            // 线程池拒绝（排队满了）之类，任务表里得留个失败记录，别让它一直挂在 PENDING
            generateTaskService.updateById(new TRentGenerateTask()
                    .setId(taskId)
                    .setStatus(TRentGenerateTask.STATUS_FAILED)
                    .setErrorMsg("任务提交失败：" + e.getMessage())
                    .setFinishTime(new Date()));
            throw new JeecgBootException("生成任务提交失败：" + e.getMessage());
        }
        return toTaskVO(generateTaskService.getById(taskId));
    }

    @Override
    public RentGenerateTaskVO generateTaskStatus(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new JeecgBootException("缺少任务ID");
        }
        TRentGenerateTask task = generateTaskService.getById(taskId.trim());
        if (task == null) {
            throw new JeecgBootException("任务不存在：" + taskId);
        }
        return toTaskVO(task);
    }

    private RentGenerateTaskVO toTaskVO(TRentGenerateTask task) {
        RentGenerateTaskVO vo = new RentGenerateTaskVO()
                .setTaskId(task.getId())
                .setStatus(task.getStatus())
                .setStatusName(taskStatusName(task.getStatus()))
                .setRentYear(task.getRentYear())
                .setTotalStations(task.getTotalStations() == null ? 0 : task.getTotalStations())
                .setProgress(task.getProgress() == null ? 0 : task.getProgress())
                .setCreatedCount(task.getCreatedCount() == null ? 0 : task.getCreatedCount())
                .setSkippedCount(task.getSkippedCount() == null ? 0 : task.getSkippedCount())
                .setFailCount(task.getFailCount() == null ? 0 : task.getFailCount())
                .setPeriodRange(task.getPeriodRange())
                .setErrorMsg(task.getErrorMsg())
                .setCostMs(task.getCostMs())
                .setCreateTime(task.getCreateTime())
                .setFinishTime(task.getFinishTime());
        if (task.getDetailJson() != null && !task.getDetailJson().isEmpty()) {
            try {
                JSONObject detail = JSON.parseObject(task.getDetailJson());
                vo.setSkipDetails(toStringList(detail.getJSONArray("skip")));
                vo.setFailDetails(toStringList(detail.getJSONArray("fail")));
            } catch (Exception e) {
                // 明细解析不了不影响看状态，只是页面上少两行原因
                log.warn("生成任务明细解析失败 taskId={}", task.getId(), e);
            }
        }
        return vo;
    }

    private List<String> toStringList(JSONArray arr) {
        return arr == null ? new ArrayList<>() : arr.toJavaList(String.class);
    }

    private String taskStatusName(String status) {
        if (TRentGenerateTask.STATUS_PENDING.equals(status)) {
            return "待执行";
        }
        if (TRentGenerateTask.STATUS_RUNNING.equals(status)) {
            return "执行中";
        }
        if (TRentGenerateTask.STATUS_DONE.equals(status)) {
            return "已完成";
        }
        if (TRentGenerateTask.STATUS_FAILED.equals(status)) {
            return "失败";
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentGenerateResultVO regenerate(RentRegenerateDTO form) {
        roleGuard.requireLedgerOps("重新生成租金");
        if (form == null || form.getStationId() == null) {
            throw new JeecgBootException("缺少电站ID");
        }
        Long stationId = form.getStationId();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        int rentYear = form.getYear() == null ? thisYear : form.getYear();
        // 需求稿第 12 条只允许重新生成本年度，第 9 条也规定历史租金不支持任何操作
        if (rentYear < thisYear) {
            throw new JeecgBootException(rentYear + "年的租金属于历史租金，只能查看，不支持重新生成");
        }
        TFarmerStation station = farmerStationService.getById(stationId);
        if (station == null || (station.getDelFlag() != null && station.getDelFlag() == 1)) {
            throw new JeecgBootException("农户不存在或已删除");
        }

        Map<Long, Integer> periodCountByProject = periodCountByProject();
        int periodCount = periodCountOf(station, periodCountByProject);

        // 起始期由三个来源依次决定：
        //   wholeYear=1  → 第1期（需求第25条：农户详情的「生成租金」出全年）
        //   periodNo     → 指定期（需求第20条：详情页按钮只重出当期）
        //   fromDate     → 该日所在期（需求第21条：安装信息改后从修改当日起算）
        int startPeriod;
        if (form.getWholeYear() != null && form.getWholeYear() == 1) {
            startPeriod = 1;
        } else if (form.getPeriodNo() != null) {
            if (form.getPeriodNo() < 1 || form.getPeriodNo() > periodCount) {
                throw new JeecgBootException(String.format("%s 所属项目一年结算 %d 期，期数 %d 超出范围",
                        station.getFarmerName(), periodCount, form.getPeriodNo()));
            }
            startPeriod = form.getPeriodNo();
        } else {
            Date fromDate = form.getFromDate();
            startPeriod = RentPeriodUtil.periodOf(fromDate == null ? new Date() : fromDate, periodCount);
        }

        long start = System.currentTimeMillis();
        RentGenerateResultVO result = new RentGenerateResultVO().setRentYear(rentYear).setStationCount(1);
        // 需求第21条：重算"当期和后面的租金"，所以从起始期一路跑到年末。
        // 已有的台账原地改（不新增行），已结算的期跳过。
        Date[] range = generateStation(station, rentYear, result, true,
                periodCountByProject, startPeriod, periodCount);
        if (range != null) {
            result.setPeriodRange(RentPeriodUtil.ymd(range[0]) + " ~ " + RentPeriodUtil.ymd(range[1]));
        }
        String noGridDateMsg = result.noGridDateSummary();
        if (noGridDateMsg != null) {
            result.addFailSummary(noGridDateMsg);
        }
        result.setCostMs(System.currentTimeMillis() - start);

        operLog.recordText(TOperLog.ACTION_GENERATE, TOperLog.BIZ_LEDGER, stationId,
                String.format("重新生成%s %d年度第%d期起的租金台账：更新%d条，新生成%d条，跳过%d条%s",
                        station.getFarmerName(), rentYear, startPeriod,
                        result.getUpdatedCount(), result.getCreatedCount(),
                        result.getSkippedCount(),
                        result.getSkipDetails().isEmpty() ? "" : "（" + String.join("；", result.getSkipDetails()) + "）"));
        return result;
    }

    @Override
    public RentGenerateCheckVO checkBeforeGenerate(Long stationId, Integer year) {
        if (stationId == null) {
            throw new JeecgBootException("缺少电站ID");
        }
        TFarmerStation station = farmerStationService.getById(stationId);
        if (station == null || (station.getDelFlag() != null && station.getDelFlag() == 1)) {
            throw new JeecgBootException("农户不存在或已删除");
        }
        int rentYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;

        // 需求34：弹窗要写明这次生成覆盖的是哪一段（年月日）。
        // 按各期与并网日、租金截止日取交集后的包络，和真正生成的区间一致
        Date[] range = yearRange(station, rentYear);
        String periodRange = range == null ? null
                : RentPeriodUtil.ymd(range[0]) + " ~ " + RentPeriodUtil.ymd(range[1]);

        // 1. 能不能算出租金。
        // 用 buildPlans 而不是"有没有安装信息记录"：有安装信息但没填租金单价，
        // 生成时同样是失败的（"没有可用的安装信息或租金单价"），
        // 提前在这里判掉，用户才不会点下去才发现生成不了。
        boolean hasInstall = !calcEngine.buildPlans(station).isEmpty();

        // 2. 本年度有没有已生成的租金（已作废/已删除的不算，它们不影响"是否已生成"的判断）
        int ledgerCount = (int) ledgerService.count(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, stationId)
                .eq(TRentLedger::getRentYear, rentYear)
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0));
        boolean hasLedger = ledgerCount > 0;

        RentGenerateCheckVO vo = new RentGenerateCheckVO()
                .setHasInstall(hasInstall ? 1 : 0)
                .setHasLedger(hasLedger ? 1 : 0)
                .setLedgerCount(ledgerCount)
                .setPeriodRange(periodRange);

        if (!hasInstall) {
            // 需求第25条：没有安装信息时不让生成
            return vo.setAction(RentGenerateCheckVO.ACTION_BLOCK)
                    .setMessage("请先添加组件安装信息？");
        }
        if (hasLedger) {
            // 需求第25条：已生成过要二次确认，生成会覆盖现有金额
            return vo.setAction(RentGenerateCheckVO.ACTION_CONFIRM)
                    .setMessage("租金信息已存在，生成租金将更新现有的租金金额信息"
                            + (periodRange == null ? "" : "，本次生成时间段：" + periodRange)
                            + "，请确认是否继续？");
        }
        return vo.setAction(RentGenerateCheckVO.ACTION_DIRECT)
                .setMessage("确认生成本年度租金"
                        + (periodRange == null ? "" : "（" + periodRange + "）") + "？");
    }

    /**
     * 需求34：某电站某年度实际会生成/重算的时间段。
     * 各期先与并网日、租金截止日取交集（完全落在租期外的期不算），再取最小起 / 最大止。
     */
    private Date[] yearRange(TFarmerStation station, int rentYear) {
        if (station.getGridDate() == null) {
            return null;
        }
        int periodCount = periodCountOf(station, periodCountByProject());
        Date begin = null;
        Date end = null;
        for (int periodNo = 1; periodNo <= periodCount; periodNo++) {
            Date[] eff = RentPeriodUtil.intersect(
                    RentPeriodUtil.periodBegin(rentYear, periodNo, periodCount),
                    RentPeriodUtil.periodEnd(rentYear, periodNo, periodCount),
                    station.getGridDate(),
                    station.getRentDeadline());
            if (eff == null) {
                continue;
            }
            if (begin == null || eff[0].before(begin)) {
                begin = eff[0];
            }
            if (end == null || eff[1].after(end)) {
                end = eff[1];
            }
        }
        return begin == null ? null : new Date[]{begin, end};
    }

    /**
     * 生成一个电站某年度的台账。
     *
     * 一年几期由该电站所属项目决定：多数项目按季度四期，海南一年只结算 1 期，
     * 整年 1.1-12.31 算一期。
     *
     * @param regen true=重新生成。已提交审核、审核通过、已支付的期跳过；
     *              其余期**原地更新已有台账，不新增行**（需求第21条）。
     *              false=年度生成。该期已有台账就跳过，重复跑安全。
     * @param fromPeriod 起始期，null 表示从第 1 期
     * @param toPeriod   结束期，null 表示到最后 1 期
     * @return 本次实际覆盖的时间段 {起, 止}；一期都没处理时返回 null
     */
    private Date[] generateStation(TFarmerStation station, int rentYear,
                                   RentGenerateResultVO result, boolean regen,
                                   Map<Long, Integer> periodCountByProject,
                                   Integer fromPeriod, Integer toPeriod) {
        if (station.getGridDate() == null) {
            result.addNoGridDate(desc(station));
            return null;
        }
        // 需求33：农户信息待审核期间不能生成/重新生成租金。
        // 批量生成时跳过这一户继续跑其余，单户重算时外层会直接报错
        if (farmerAuditService.isPending(station.getId())) {
            result.addSkip(desc(station) + "：农户信息审核中，暂不生成");
            return null;
        }
        List<RentCalcEngine.Plan> plans = calcEngine.buildPlans(station);
        if (plans.isEmpty()) {
            if (!regen) {
                // 年度首次生成：这个电站还没录安装信息或单价，算不出租金，跳过。
                // 保持原行为 —— 首次生成不该冒出一堆金额为 0 的台账
                result.addFail(desc(station) + "：没有可用的安装信息或租金单价");
                return null;
            }
            // 重新生成：安装信息可能被删光了（比如录错了先清掉）。
            // 这时不再跳过，而是按"块数为 0"继续往下算 —— 单价 × 0 = 0，
            // 所以本期应付、月明细、累计链全都会被重算成 0，
            // 相当于把这一户的租金归零。原来直接 return 会让运营点了"重新生成"没任何反应
            log.info("电站 {} 没有可用的安装信息或单价，按 0 金额重新生成",
                    station.getFarmerName());
        }
        // 安装信息快照：每期对账单只认生成时的这份数据，农户后续改动不影响已经提交的期。
        // 本电站各期共用同一份，循环外只取一次
        String installSnapshot = JSON.toJSONString(
                farmerStationBizService.installsOf(station.getId()));
        // 全租期总额与期数无关，本年度各期共用一次计算结果
        BigDecimal cycleTotal = calcEngine.cycleTotal(station, plans);
        int periodCount = periodCountOf(station, periodCountByProject);

        int from = fromPeriod == null ? 1 : Math.max(1, fromPeriod);
        int to = toPeriod == null ? periodCount : Math.min(periodCount, toPeriod);

        boolean changed = false;
        // 记录本次实际覆盖的日期范围：弹窗要告诉用户"这次重算的是 2026-04-01 ~ 2026-12-31"，
        // 而不是笼统说一句"重新生成完成"
        Date rangeBegin = null;
        Date rangeEnd = null;
        for (int periodNo = from; periodNo <= to; periodNo++) {
            Date[] eff = RentPeriodUtil.intersect(
                    RentPeriodUtil.periodBegin(rentYear, periodNo, periodCount),
                    RentPeriodUtil.periodEnd(rentYear, periodNo, periodCount),
                    station.getGridDate(),
                    station.getRentDeadline());
            if (eff == null) {
                // 该期整个落在并网之前或租期结束之后，本来就不该有租金
                continue;
            }
            if (rangeBegin == null || eff[0].before(rangeBegin)) {
                rangeBegin = eff[0];
            }
            if (rangeEnd == null || eff[1].after(rangeEnd)) {
                rangeEnd = eff[1];
            }

            List<TRentLedger> exists = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                    .eq(TRentLedger::getStationId, station.getId())
                    .eq(TRentLedger::getRentYear, rentYear)
                    .eq(TRentLedger::getPeriodNo, periodNo)
                    .eq(TRentLedger::getDelFlag, 0)
                    .eq(TRentLedger::getVoidFlag, 0));

            if (!regen && !exists.isEmpty()) {
                continue;
            }
            if (regen) {
                String blocked = blockedReason(exists);
                if (blocked != null) {
                    result.addSkip(RentPeriodUtil.periodName(rentYear, periodNo) + blocked);
                    continue;
                }
                if (!exists.isEmpty()) {
                    // 需求第21条：重新生成不新增数据，直接把已有的那条改掉。
                    // 台账 ID 保住，挂在它下面的附件、纠纷记录、审核记录都还在
                    updateLedger(newestOf(exists), station, eff[0], eff[1], plans, cycleTotal,
                            installSnapshot);
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                    changed = true;
                    continue;
                }
                // 该期一条台账都没有（首次生成），落到下面走新增
            }

            saveLedger(station, rentYear, periodNo, eff[0], eff[1], plans, cycleTotal, regen,
                    exists, installSnapshot);
            result.setCreatedCount(result.getCreatedCount() + 1);
            changed = true;
        }

        if (changed) {
            // 金额动过之后，本电站整条累计链要重排一遍
            recalcChain(station.getId());
        }
        return rangeBegin == null ? null : new Date[]{rangeBegin, rangeEnd};
    }

    /** 同期可能有多条有效台账（历史遗留的追加行），取 id 最大的那条作为原地更新的目标 */
    /**
     * 失败原因掐短。
     *
     * MyBatis 的异常 message 会把整条 INSERT 的字段列表带进来，几百个字符一条，
     * 几十户失败就能糊满页面。真正的根因在最后的 Cause 段，取它、再截到一行。
     */
    private String briefMsg(Throwable e) {
        String msg = e == null ? null : e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return e == null ? "未知错误" : e.getClass().getSimpleName();
        }
        int idx = msg.lastIndexOf("Cause:");
        if (idx >= 0) {
            msg = msg.substring(idx + "Cause:".length());
        }
        int semi = msg.indexOf(';');
        if (semi > 0) {
            msg = msg.substring(0, semi);
        }
        msg = msg.replaceAll("\\s+", " ").trim();
        return msg.length() > 200 ? msg.substring(0, 200) + "…" : msg;
    }

    private TRentLedger newestOf(List<TRentLedger> list) {
        TRentLedger newest = list.get(0);
        for (TRentLedger l : list) {
            if (l.getId() != null && (newest.getId() == null || l.getId() > newest.getId())) {
                newest = l;
            }
        }
        return newest;
    }

    /**
     * 重出已有台账：**原地更新，不新增行**（需求第21条）。
     *
     * 只改"按安装信息算出来"的那部分：块数、容量、计租区间、月明细、金额。
     * 人工录入的实际发电天数按年月保留；人工调过的本期实付也保留。
     * 状态、纠纷标记、附件、审核记录一概不动 —— 台账 ID 没变，它们还挂在原来那条上。
     */
    private void updateLedger(TRentLedger existing, TFarmerStation station,
                              Date periodBegin, Date periodEnd,
                              List<RentCalcEngine.Plan> plans, BigDecimal cycleTotal,
                              String installSnapshot) {
        List<TRentLedgerMonth> months = buildMonths(plans, periodBegin, periodEnd,
                Collections.singletonList(existing));

        BigDecimal payable = BigDecimal.ZERO;
        int genDays = 0;
        for (TRentLedgerMonth m : months) {
            payable = payable.add(m.getAmount() == null ? BigDecimal.ZERO : m.getAmount());
            genDays += m.effectiveDays();
        }
        payable = RentCalcEngine.money(payable);

        Date now = new Date();
        String user = operLog.currentUser();
        int periodDays = RentPeriodUtil.daysBetween(periodBegin, periodEnd);

        // 月明细整体重建：先清旧的再写新的，保证和新的计租区间一一对应
        ledgerMonthService.remove(new LambdaQueryWrapper<TRentLedgerMonth>()
                .eq(TRentLedgerMonth::getLedgerId, existing.getId()));
        for (TRentLedgerMonth m : months) {
            m.setLedgerId(existing.getId()).setCreateBy(user).setCreateTime(now);
        }
        if (!months.isEmpty()) {
            ledgerMonthService.saveBatch(months);
        }

        // 实付金额：
        //   安装信息被删光时（plans 为空）一律归零 —— 这户已经不租了，
        //   之前人工调过的实付也不该留着，否则会出现"应付 0、实付还有数"的怪账
        //   其余情况维持原口径：人工调过的保留（不等于旧应付），没调过的跟着新应付走
        BigDecimal oldPayable = nvlMoney(existing.getPeriodPayable());
        BigDecimal periodPaid;
        if (plans == null || plans.isEmpty()) {
            periodPaid = BigDecimal.ZERO;
        } else {
            periodPaid = existing.getPeriodPaid() == null
                    || existing.getPeriodPaid().compareTo(oldPayable) == 0
                    ? payable : existing.getPeriodPaid();
        }

        BigDecimal oldPayableShow = oldPayable;
        ledgerService.updateById(new TRentLedger()
                .setId(existing.getId())
                .setFarmerName(station.getFarmerName())
                .setPowerUserNo(station.getPowerUserNo())
                .setInstallAddr(station.getInstallAddr())
                .setIdCard(station.getIdCard())
                .setBankCardNo(station.getBankCardNo())
                .setBankCardName(station.getBankCardName())
                .setProjectId(station.getProjectId())
                .setTenantId(station.getTenantId())
                .setTotalBlocks(station.getTotalBlocks())
                .setTotalCapacity(station.getTotalCapacity())
                .setInstallSnapshot(installSnapshot)
                .setGridDate(station.getGridDate())
                .setGridMonthRemainDays(RentPeriodUtil.gridMonthRemainDays(station.getGridDate()))
                .setGridMonths(RentPeriodUtil.gridMonths(station.getGridDate(), periodEnd))
                .setPeriodBegin(periodBegin)
                .setPeriodEnd(periodEnd)
                .setPeriodDays(periodDays)
                .setActualGenDays(genDays)
                .setCycleTotalAmount(cycleTotal)
                .setPeriodPayable(payable)
                // 重算过了，之前的人工调整标记作废
                .setPayableAdjustFlag(0)
                .setPeriodPaid(periodPaid)
                .setRegenFlag(1)
                .setUpdateBy(user).setUpdateTime(now));

        operLog.recordText(TOperLog.ACTION_UPDATE, TOperLog.BIZ_LEDGER, existing.getId(),
                String.format("重新生成%s租金台账（原地更新，不新增行）：计租区间 %s ~ %s，"
                                + "计租天数 %d 天，本期应付 %s 元 → %s 元",
                        existing.getPeriodName(), RentPeriodUtil.ymd(periodBegin),
                        RentPeriodUtil.ymd(periodEnd), periodDays,
                        oldPayableShow.toPlainString(), payable.toPlainString()));
    }

    /**
     * 重新生成时，新台账继承被它顶掉的那些行的纠纷暂停标记。
     *
     * 需求稿第 14 条：纠纷暂停期间租金正常生成，但没取消纠纷就不能提交审核。
     * 如果新生成的行是干净的，"标记纠纷 → 重新生成 → 提交"就成了绕开纠纷暂停的口子，
     * 纠纷是挂在这个电站这一期上的，不是挂在某一行台账上，所以要跟着传下来。
     */
    private void inheritDispute(TRentLedger ledger, List<TRentLedger> superseded) {
        if (superseded == null || superseded.isEmpty()) {
            return;
        }
        for (TRentLedger old : superseded) {
            if (old.disputed()) {
                ledger.setDisputeFlag(1)
                        .setDisputeRemark(old.getDisputeRemark())
                        .setDisputeTime(old.getDisputeTime());
                log.info("重新生成的{}台账继承了原台账 id={} 的纠纷暂停支付标记", ledger.getPeriodName(), old.getId());
                return;
            }
        }
    }

    /** 重新生成时该期是否被锁住，返回原因；null 表示可以重出 */
    private String blockedReason(List<TRentLedger> exists) {
        for (TRentLedger l : exists) {
            if (l.paid()) {
                return "已支付，不重新生成";
            }
            if (l.locked()) {
                return TRentLedger.auditName(l.getAuditStatus()) + "，不重新生成";
            }
        }
        return null;
    }

    /**
     * 按计租区间生成月明细，并带上人工录入的实际发电天数。
     *
     * 发电天数跟安装信息、单价无关，是运营按月录的实际发电情况；
     * 重建月明细时不带过来的话会被清成空、金额跟着跌回满发——录过的数据就白录了。
     *
     * @param source 旧台账，用于按年月取回发电天数；首次生成时传空
     */
    private List<TRentLedgerMonth> buildMonths(List<RentCalcEngine.Plan> plans, Date begin, Date end,
                                               List<TRentLedger> source) {
        List<TRentLedgerMonth> months = calcEngine.buildMonths(plans, begin, end);
        Map<String, Integer> carried = carriedGenDays(source);
        for (TRentLedgerMonth m : months) {
            Integer keep = carried.get(m.getYm());
            if (keep != null) {
                m.setGenDays(keep)
                        .setAmount(calcEngine.applyGenDays(m.getTheoryAmount(), m.getRentDays(), keep));
            }
        }
        return months;
    }

    /**
     * 从被顶掉的旧台账里取出人工录入的实际发电天数，按年月（yyyy-MM）索引。
     *
     * 同一期可能有多条旧台账（反复重新生成累积出来的），以 id 最大的那条为准——
     * 它是最新一次生成的，月明细也最新。
     */
    private Map<String, Integer> carriedGenDays(List<TRentLedger> superseded) {
        Map<String, Integer> map = new HashMap<>();
        if (superseded == null || superseded.isEmpty()) {
            return map;
        }
        TRentLedger newest = superseded.get(0);
        for (TRentLedger l : superseded) {
            if (l.getId() != null && (newest.getId() == null || l.getId() > newest.getId())) {
                newest = l;
            }
        }
        for (TRentLedgerMonth m : months(newest.getId())) {
            if (m.getGenDays() != null) {
                map.put(m.getYm(), m.getGenDays());
            }
        }
        return map;
    }

    private void saveLedger(TFarmerStation station, int rentYear, int periodNo,
                            Date periodBegin, Date periodEnd,
                            List<RentCalcEngine.Plan> plans, BigDecimal cycleTotal, boolean regen,
                            List<TRentLedger> superseded, String installSnapshot) {

        List<TRentLedgerMonth> months = buildMonths(plans, periodBegin, periodEnd, superseded);

        BigDecimal payable = BigDecimal.ZERO;
        int genDays = 0;
        for (TRentLedgerMonth m : months) {
            payable = payable.add(m.getAmount() == null ? BigDecimal.ZERO : m.getAmount());
            genDays += m.effectiveDays();
        }
        payable = RentCalcEngine.money(payable);

        Date now = new Date();
        String user = operLog.currentUser();
        TRentLedger ledger = new TRentLedger()
                .setStationId(station.getId())
                .setFarmerName(station.getFarmerName())
                .setPowerUserNo(station.getPowerUserNo())
                .setInstallAddr(station.getInstallAddr())
                .setIdCard(station.getIdCard())
                .setBankCardNo(station.getBankCardNo())
                .setBankCardName(station.getBankCardName())
                .setProjectId(station.getProjectId())
                .setTenantId(station.getTenantId())
                .setRentYear(rentYear)
                .setPeriodNo(periodNo)
                .setPeriodName(RentPeriodUtil.periodName(rentYear, periodNo))
                .setTotalBlocks(station.getTotalBlocks())
                .setTotalCapacity(station.getTotalCapacity())
                .setInstallSnapshot(installSnapshot)
                .setGridDate(station.getGridDate())
                .setGridMonthRemainDays(RentPeriodUtil.gridMonthRemainDays(station.getGridDate()))
                .setGridMonths(RentPeriodUtil.gridMonths(station.getGridDate(), periodEnd))
                .setPeriodBegin(periodBegin)
                .setPeriodEnd(periodEnd)
                .setPeriodDays(RentPeriodUtil.daysBetween(periodBegin, periodEnd))
                .setActualGenDays(genDays)
                .setCycleTotalAmount(cycleTotal)
                .setPeriodPayable(payable)
                // 本期实付默认取本期应付，人工没调整就按这个走
                .setPeriodPaid(payable)
                // 累计三项先占位，saveLedger 之后由 recalcChain 统一顺推
                .setAccumPayable(payable)
                .setPrevAccumPaid(BigDecimal.ZERO)
                .setAccumPaid(payable)
                .setDiffAmount(BigDecimal.ZERO)
                .setAuditStatus(TRentLedger.AUDIT_DRAFT)
                .setPayStatus(TRentLedger.PAY_UNPAID)
                .setReconStatus(TRentLedger.RECON_NONE)
                .setDisputeFlag(0)
                .setVoidFlag(0)
                .setRegenFlag(regen ? 1 : 0)
                .setDelFlag(0)
                .setCreateBy(user).setCreateTime(now)
                .setUpdateBy(user).setUpdateTime(now);
        inheritDispute(ledger, superseded);
        ledgerService.save(ledger);

        for (TRentLedgerMonth m : months) {
            m.setLedgerId(ledger.getId()).setCreateBy(user).setCreateTime(now);
        }
        if (!months.isEmpty()) {
            ledgerMonthService.saveBatch(months);
        }

        operLog.recordText(TOperLog.ACTION_GENERATE, TOperLog.BIZ_LEDGER, ledger.getId(),
                String.format("%s生成%s租金台账：计租区间 %s ~ %s，计租天数 %d 天，本期应付 %s 元",
                        regen ? "重新" : "", ledger.getPeriodName(),
                        RentPeriodUtil.ymd(periodBegin), RentPeriodUtil.ymd(periodEnd),
                        ledger.getPeriodDays(), payable.toPlainString()));
    }

    // ==================== 详情与编辑 ====================

    @Override
    public RentLedgerDetailVO detail(Long id) {
        TRentLedger ledger = getExisting(id);
        RentLedgerDetailVO vo = new RentLedgerDetailVO();
        vo.setBase(queryService.toListVO(ledger));
        vo.setEditable(vo.getBase().getEditable());

        // 安装信息按生成时的快照展示，农户后续改动不影响这一期；
        // 存量台账没有快照（该列是后加的），回落到实时查询
        String installSnapshot = ledger.getInstallSnapshot();
        if (installSnapshot != null && !installSnapshot.trim().isEmpty()) {
            vo.setInstalls(JSON.parseArray(installSnapshot, FarmerStationDetailVO.InstallVO.class));
        } else {
            vo.setInstalls(farmerStationBizService.installsOf(ledger.getStationId()));
        }

        BigDecimal sysPayable = BigDecimal.ZERO;
        for (TRentLedgerMonth m : months(id)) {
            sysPayable = sysPayable.add(m.getAmount() == null ? BigDecimal.ZERO : m.getAmount());
            vo.getMonths().add(new RentLedgerDetailVO.MonthVO()
                    .setId(m.getId())
                    .setYm(m.getYm())
                    .setSegBegin(m.getSegBegin())
                    .setSegEnd(m.getSegEnd())
                    .setMonthDays(m.getMonthDays())
                    // 前端「实际发电天数」这一列绑的是 rentDays：
                    // 录入过就是录入值，没录入回落到理论计租天数，前端显示和回填才能对上
                    .setRentDays(m.effectiveDays())
                    .setTheoryRentDays(m.getRentDays())
                    .setGenDays(m.getGenDays())
                    .setMonthRent(m.getMonthRent())
                    .setTheoryAmount(m.getTheoryAmount())
                    .setAmount(m.getAmount()));
        }
        vo.setSysPeriodPayable(RentCalcEngine.money(sysPayable));
        // 需求34：补算/重算的弹窗要写明算的是哪一段
        vo.setPeriodRange(RentPeriodUtil.ymd(ledger.getPeriodBegin())
                + " ~ " + RentPeriodUtil.ymd(ledger.getPeriodEnd()));
        // 审核附件按轮次分组挂到 auditRounds 上，其余附件平铺在 attaches 里
        List<RentAttachVO> auditAttaches = new ArrayList<>();
        for (RentAttachVO a : attachService.listByLedger(id)) {
            if (TRentAttach.BIZ_AUDIT.equals(a.getBizType())) {
                auditAttaches.add(a);
            } else {
                vo.getAttaches().add(a);
            }
        }
        vo.setAuditRounds(buildAuditRounds(ledger, auditAttaches));
        vo.setDisputeRounds(buildDisputeRounds(ledger));
        return vo;
    }

    /**
     * 历次纠纷暂停，次数倒序返回，最近一次在最前面。
     * 升级前标记过纠纷的老数据没有记录行，用台账上那份"最近一次"的快照兜一条出来，
     * 免得页面上纠纷区块整个空掉。
     */
    private List<RentLedgerDetailVO.DisputeRoundVO> buildDisputeRounds(TRentLedger ledger) {
        List<TRentDisputeRecord> records = disputeRecordService.list(
                new LambdaQueryWrapper<TRentDisputeRecord>()
                        .eq(TRentDisputeRecord::getLedgerId, ledger.getId())
                        .orderByDesc(TRentDisputeRecord::getDisputeRound)
                        .orderByDesc(TRentDisputeRecord::getId));

        List<RentLedgerDetailVO.DisputeRoundVO> list = new ArrayList<>();
        if (records.isEmpty()) {
            if (ledger.getDisputeTime() == null && !ledger.disputed()) {
                return list;
            }
            list.add(disputeVO(new TRentDisputeRecord()
                    .setDisputeRound(Math.max(ledger.disputeRound(), 1))
                    .setDisputeTime(ledger.getDisputeTime())
                    .setDisputeRemark(ledger.getDisputeRemark())
                    .setCancelTime(ledger.disputed() ? null : ledger.getDisputeCancelTime())));
            return list;
        }
        for (TRentDisputeRecord r : records) {
            list.add(disputeVO(r));
        }
        return list;
    }

    private RentLedgerDetailVO.DisputeRoundVO disputeVO(TRentDisputeRecord r) {
        boolean ongoing = r.ongoing();
        int round = r.getDisputeRound() == null ? 1 : r.getDisputeRound();
        return new RentLedgerDetailVO.DisputeRoundVO()
                .setRound(round)
                .setTitle("第" + round + "次纠纷暂停" + (ongoing ? "（暂停中）" : "（已取消）"))
                .setDisputeTime(r.getDisputeTime())
                .setDisputeRemark(r.getDisputeRemark())
                .setDisputeBy(r.getDisputeBy())
                .setCancelTime(r.getCancelTime())
                .setCancelRemark(r.getCancelRemark())
                .setCancelBy(r.getCancelBy())
                .setOngoing(ongoing ? 1 : 0);
    }

    /**
     * 把审核记录和审核附件按轮次拼成一组一组。
     *
     * 之所以要分轮次：两次驳回的原因和附件原来全堆在一起，页面上分不清
     * 哪个附件是哪一次驳回带的。现在第 N 轮的附件和第 N 轮的驳回记录对得上。
     *
     * 轮次倒序返回，最近一次在最前面，页面顶部的「驳回理由」直接取第一条。
     * 升级前的老数据没有轮次（round=0），单独归成一组，标明轮次不详，不硬凑。
     */
    private List<RentLedgerDetailVO.AuditRoundVO> buildAuditRounds(TRentLedger ledger,
                                                                   List<RentAttachVO> auditAttaches) {
        List<TRentAuditRecord> records = auditRecordService.list(
                new LambdaQueryWrapper<TRentAuditRecord>()
                        .eq(TRentAuditRecord::getLedgerId, ledger.getId())
                        .orderByAsc(TRentAuditRecord::getAuditRound)
                        .orderByAsc(TRentAuditRecord::getId));

        // 轮次号可能出现在记录里，也可能只出现在附件上（老数据），两边都要收
        Map<Integer, RentLedgerDetailVO.AuditRoundVO> rounds = new TreeMap<>();
        for (TRentAuditRecord r : records) {
            round(rounds, r.getAuditRound());
        }
        for (RentAttachVO a : auditAttaches) {
            round(rounds, a.getAuditRound());
        }
        if (rounds.isEmpty()) {
            return new ArrayList<>();
        }

        // 同一轮里按时间顺序覆盖：提交 -> 通过/驳回，最后留下的就是这一轮的结果
        for (TRentAuditRecord r : records) {
            RentLedgerDetailVO.AuditRoundVO g = round(rounds, r.getAuditRound());
            if (TRentAuditRecord.ACTION_SUBMIT.equals(r.getAction())) {
                g.setSubmitBy(r.getOperBy()).setSubmitTime(r.getOperTime());
                if (g.getAction() == null) {
                    g.setAction(r.getAction())
                            .setStage(r.getAuditStage())
                            .setActionName(TRentAuditRecord.actionName(r.getAuditStage(), r.getAction()));
                }
            } else {
                String stage = r.getAuditStage();
                g.setAction(r.getAction())
                        .setStage(stage)
                        .setActionName(TRentAuditRecord.actionName(stage, r.getAction()))
                        .setRemark(r.getRemark())
                        .setAuditBy(r.getOperBy())
                        .setAuditTime(r.getOperTime());
                // 一轮里四级审核各留一份审核人/时间
                if (TRentAuditRecord.STAGE_PROJECT.equals(stage)) {
                    g.setProjectAuditBy(r.getOperBy()).setProjectAuditTime(r.getOperTime());
                } else if (TRentAuditRecord.STAGE_BUSINESS.equals(stage)) {
                    g.setBusinessAuditBy(r.getOperBy()).setBusinessAuditTime(r.getOperTime());
                } else if (TRentAuditRecord.STAGE_FINANCE.equals(stage)) {
                    g.setFinanceAuditBy(r.getOperBy()).setFinanceAuditTime(r.getOperTime());
                } else if (TRentAuditRecord.STAGE_LEADER.equals(stage)) {
                    g.setLeaderAuditBy(r.getOperBy()).setLeaderAuditTime(r.getOperTime());
                }
            }
        }
        for (RentAttachVO a : auditAttaches) {
            round(rounds, a.getAuditRound()).getAttaches().add(a);
        }

        List<RentLedgerDetailVO.AuditRoundVO> list = new ArrayList<>(rounds.values());
        for (RentLedgerDetailVO.AuditRoundVO g : list) {
            g.setTitle(roundTitle(g));
        }
        Collections.reverse(list);
        return list;
    }

    private RentLedgerDetailVO.AuditRoundVO round(Map<Integer, RentLedgerDetailVO.AuditRoundVO> rounds,
                                                  Integer round) {
        int no = round == null ? 0 : round;
        return rounds.computeIfAbsent(no, k -> new RentLedgerDetailVO.AuditRoundVO().setRound(k));
    }

    private String roundTitle(RentLedgerDetailVO.AuditRoundVO g) {
        if (g.getRound() == null || g.getRound() <= 0) {
            return "轮次不详（升级前的历史数据）";
        }
        // 四级审核后标题直接带阶段，如"第1次提交项目审核""第2次业务审核驳回"
        String what = TRentAuditRecord.actionName(g.getStage(), g.getAction());
        if (what == null || what.isEmpty()) {
            what = "提交审核";
        }
        return "第" + g.getRound() + "次" + what;
    }

    private void saveAuditRecord(Long ledgerId, int round, String stage, String action,
                                 String remark, Date now) {
        auditRecordService.save(new TRentAuditRecord()
                .setLedgerId(ledgerId)
                .setAuditRound(round)
                .setAuditStage(stage)
                .setAction(action)
                .setRemark(remark)
                .setOperBy(operLog.currentUser())
                .setOperTime(now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(RentLedgerEditDTO form) {
        roleGuard.requireLedgerOps("修改租金");
        if (form == null || form.getId() == null) {
            throw new JeecgBootException("缺少台账ID");
        }
        TRentLedger before = getExisting(form.getId());
        checkEditable(before);

        List<TRentLedgerMonth> months = months(before.getId());
        Map<Long, TRentLedgerMonth> byId = new HashMap<>();
        Map<String, TRentLedgerMonth> byYm = new HashMap<>();
        for (TRentLedgerMonth m : months) {
            byId.put(m.getId(), m);
            byYm.put(m.getYm(), m);
        }

        Date now = new Date();
        String user = operLog.currentUser();
        List<String> changes = new ArrayList<>();

        // 逐月回填实际发电天数，金额按天等比折算
        for (RentLedgerEditDTO.MonthItem item : nvl(form.getMonths())) {
            TRentLedgerMonth m = item.getId() != null ? byId.get(item.getId()) : byYm.get(item.getYm());
            if (m == null) {
                throw new JeecgBootException("月明细不存在：" + (item.getId() != null ? item.getId() : item.getYm()));
            }
            // 前端这一列一直用 rentDays 传值，这里统一收口成 genDays（两个都传以 genDays 为准）
            Integer genDays = item.getGenDays() != null ? item.getGenDays() : item.getRentDays();
            if (genDays != null) {
                if (genDays < 0) {
                    throw new JeecgBootException(m.getYm() + " 的实际发电天数不能为负数");
                }
                // 需求第16条：实际发电天数可能超过当月天数最大值，去掉上限校验
            }
            if (eq(m.getGenDays(), genDays)) {
                continue;
            }
            changes.add(String.format("%s 实际发电天数 由 %s 改为 %s",
                    m.getYm(), showDays(m.getGenDays(), m.getRentDays()), showDays(genDays, m.getRentDays())));

            BigDecimal amount = calcEngine.applyGenDays(m.getTheoryAmount(), m.getRentDays(), genDays);
            ledgerMonthService.updateById(new TRentLedgerMonth()
                    .setId(m.getId())
                    .setGenDays(genDays)
                    .setAmount(amount)
                    .setUpdateBy(user).setUpdateTime(now));
            m.setGenDays(genDays).setAmount(amount);
        }

        // 汇总本期发电天数与系统计算的应付金额
        int genDays = 0;
        BigDecimal sysPayable = BigDecimal.ZERO;
        for (TRentLedgerMonth m : months) {
            genDays += m.effectiveDays();
            sysPayable = sysPayable.add(m.getAmount() == null ? BigDecimal.ZERO : m.getAmount());
        }
        sysPayable = RentCalcEngine.money(sysPayable);

        /*
         * 本期应付：默认按月明细汇总，也允许人工调整。
         *
         * 调整过必须打标记锁住，否则下次谁改一下某个月的发电天数，
         * 这里一重算就把人工调的金额悄悄覆盖掉了——账面上看不出来，很难查。
         * 把值改回系统计算值时自动解锁，等于"恢复系统计算"。
         */
        BigDecimal payable;
        boolean payableAdjusted;
        if (form.getPeriodPayable() != null) {
            if (form.getPeriodPayable().compareTo(BigDecimal.ZERO) < 0) {
                throw new JeecgBootException("本期应付金额不能为负数");
            }
            payable = RentCalcEngine.money(form.getPeriodPayable());
            payableAdjusted = payable.compareTo(sysPayable) != 0;
        } else if (before.payableAdjusted()) {
            payable = before.getPeriodPayable() == null ? sysPayable : before.getPeriodPayable();
            payableAdjusted = true;
        } else {
            payable = sysPayable;
            payableAdjusted = false;
        }

        // 本期实付：传了就用传的，没传则跟着应付走
        BigDecimal periodPaid;
        if (form.getPeriodPaid() != null) {
            if (form.getPeriodPaid().compareTo(BigDecimal.ZERO) < 0) {
                throw new JeecgBootException("本期实付金额不能为负数");
            }
            periodPaid = RentCalcEngine.money(form.getPeriodPaid());
        } else if (before.getPeriodPaid() != null
                && before.getPeriodPaid().compareTo(before.getPeriodPayable()) != 0) {
            // 之前人工调过实付，这次没传就保持人工值，不被重算的应付覆盖
            periodPaid = before.getPeriodPaid();
        } else {
            periodPaid = payable;
        }

        if (before.getPeriodPayable() == null || before.getPeriodPayable().compareTo(payable) != 0) {
            changes.add(String.format("本期应付金额 由 %s 改为 %s%s",
                    plain(before.getPeriodPayable()), plain(payable),
                    payableAdjusted ? "（手工调整，系统计算值为 " + plain(sysPayable) + "）" : "（系统计算）"));
        } else if (before.payableAdjusted() && !payableAdjusted) {
            changes.add("本期应付金额 恢复为系统计算值 " + plain(payable));
        }
        if (before.getPeriodPaid() == null || before.getPeriodPaid().compareTo(periodPaid) != 0) {
            changes.add(String.format("本期实付金额 由 %s 改为 %s",
                    plain(before.getPeriodPaid()), plain(periodPaid)));
        }
        if (form.getRemark() != null && !form.getRemark().equals(before.getRemark())) {
            changes.add("备注 由 " + plainText(before.getRemark()) + " 改为 " + plainText(form.getRemark()));
        }

        ledgerService.updateById(new TRentLedger()
                .setId(before.getId())
                .setActualGenDays(genDays)
                .setPeriodPayable(payable)
                .setPayableAdjustFlag(payableAdjusted ? 1 : 0)
                .setPeriodPaid(periodPaid)
                .setRemark(form.getRemark() == null ? before.getRemark() : form.getRemark())
                .setUpdateBy(user).setUpdateTime(now));

        // 本期实付一动，后面所有期的累计实付都要跟着重排
        recalcChain(before.getStationId());

        if (!changes.isEmpty()) {
            operLog.recordText(TOperLog.ACTION_UPDATE, TOperLog.BIZ_LEDGER, before.getId(),
                    "修改" + before.getPeriodName() + "租金：" + String.join("；", changes));
        }
    }

    // ==================== 需求第16条：补算租金（只计算不保存） ====================

    @Override
    public RentLedgerDetailVO recalcByGenDays(RentLedgerEditDTO form) {
        if (form == null || form.getId() == null) {
            throw new JeecgBootException("缺少台账ID");
        }
        TRentLedger ledger = getExisting(form.getId());
        List<TRentLedgerMonth> months = months(ledger.getId());
        Map<Long, TRentLedgerMonth> byId = new HashMap<>();
        Map<String, TRentLedgerMonth> byYm = new HashMap<>();
        for (TRentLedgerMonth m : months) {
            byId.put(m.getId(), m);
            byYm.put(m.getYm(), m);
        }

        // 逐月计算 newAmount（不影响数据库）
        List<RentLedgerDetailVO.MonthVO> newMonths = new ArrayList<>();
        BigDecimal newPayable = BigDecimal.ZERO;
        int newGenDays = 0;
        for (RentLedgerDetailVO.MonthVO old : detail(ledger.getId()).getMonths()) {
            BigDecimal monthAmount = old.getAmount() == null ? BigDecimal.ZERO : old.getAmount();
            int monthGenDays = old.getGenDays() == null ? 0 : old.getGenDays();

            // 找 form 里是否传了这个月的新值
            for (RentLedgerEditDTO.MonthItem item : nvl(form.getMonths())) {
                TRentLedgerMonth m = item.getId() != null ? byId.get(item.getId()) : byYm.get(item.getYm());
                if (m != null && item.getId() != null && item.getId().equals(old.getId())) {
                    Integer newGen = item.getGenDays() != null ? item.getGenDays() : item.getRentDays();
                    if (newGen != null && newGen >= 0) {
                        monthGenDays = newGen;
                        monthAmount = calcEngine.applyGenDays(m.getTheoryAmount(), m.getRentDays(), newGen);
                    }
                    break;
                }
                if (m != null && item.getId() == null && item.getYm() != null && item.getYm().equals(old.getYm())) {
                    Integer newGen = item.getGenDays() != null ? item.getGenDays() : item.getRentDays();
                    if (newGen != null && newGen >= 0) {
                        monthGenDays = newGen;
                        monthAmount = calcEngine.applyGenDays(m.getTheoryAmount(), m.getRentDays(), newGen);
                    }
                    break;
                }
            }

            newMonths.add(new RentLedgerDetailVO.MonthVO()
                    .setId(old.getId()).setYm(old.getYm())
                    .setMonthDays(old.getMonthDays())
                    // 和 detail() 口径一致：rentDays 是前端输入框绑的列，
                    // 有录入值就显示录入值，没有就回落到理论计租天数
                    .setRentDays(monthGenDays > 0 ? monthGenDays : old.getTheoryRentDays())
                    .setTheoryRentDays(old.getTheoryRentDays())
                    .setGenDays(monthGenDays)
                    .setMonthRent(old.getMonthRent())
                    .setTheoryAmount(old.getTheoryAmount())
                    .setAmount(RentCalcEngine.money(monthAmount)));
            newPayable = newPayable.add(monthAmount);
            newGenDays += monthGenDays;
        }

        RentCalcEngine.money(newPayable);

        // 封装返回结果
        RentLedgerDetailVO vo = new RentLedgerDetailVO();
        vo.setMonths(newMonths);
        vo.setSysPeriodPayable(RentCalcEngine.money(newPayable));
        vo.setPeriodRange(RentPeriodUtil.ymd(ledger.getPeriodBegin())
                + " ~ " + RentPeriodUtil.ymd(ledger.getPeriodEnd()));

        // 附上基础信息，前端可以直接展示
        RentLedgerListVO base = queryService.toListVO(ledger);
        base.setActualGenDays(newGenDays);
        base.setPeriodPayable(RentCalcEngine.money(newPayable));
        vo.setBase(base);
        vo.setEditable(ledger.editable() ? 1 : 0);
        return vo;
    }

    // ==================== 审核流转 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(RentAuditDTO form) {
        roleGuard.requireLedgerOps("提交审核");
        for (Long id : idsOf(form)) {
            TRentLedger l = getExisting(id);
            checkNotHistory(l, "提交审核");
            checkFarmerLocked(l);
            if (l.voided()) {
                throw new JeecgBootException(l.getPeriodName() + "已作废，不能提交审核");
            }
            if (l.disputed()) {
                throw new JeecgBootException(l.getFarmerName() + " " + l.getPeriodName()
                        + "已标记纠纷暂停支付，取消标记后才能提交审核");
            }
            if (!l.editable()) {
                throw new JeecgBootException(l.getFarmerName() + " " + l.getPeriodName()
                        + "当前状态为" + auditName(l.getAuditStatus()) + "，不能重复提交");
            }
            // 同一电站同一期，只要已有一条在审或已通过或已支付，就不允许再提交第二条
            List<TRentLedger> siblings = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                    .eq(TRentLedger::getStationId, l.getStationId())
                    .eq(TRentLedger::getRentYear, l.getRentYear())
                    .eq(TRentLedger::getPeriodNo, l.getPeriodNo())
                    .eq(TRentLedger::getDelFlag, 0)
                    .eq(TRentLedger::getVoidFlag, 0)
                    .ne(TRentLedger::getId, l.getId()));
            for (TRentLedger s : siblings) {
                if (s.locked() || s.paid()) {
                    throw new JeecgBootException(l.getFarmerName() + " " + l.getPeriodName()
                            + "已有一条台账处于" + (s.paid() ? "已支付" : auditName(s.getAuditStatus()))
                            + "状态，同一期租金不能重复提交，请先作废多余的台账");
                }
            }

            Date now = new Date();
            // 每提交一次开一轮，这一轮里审核人传的附件、驳回原因都归到这个轮次下
            int round = l.auditRound() + 1;
            ledgerService.updateById(new TRentLedger()
                    .setId(l.getId())
                    .setAuditStatus(TRentLedger.AUDIT_PENDING_PROJECT)
                    .setAuditRound(round)
                    .setSubmitBy(operLog.currentUser()).setSubmitTime(now)
                    .setUpdateBy(operLog.currentUser()).setUpdateTime(now));
            saveAuditRecord(l.getId(), round, TRentAuditRecord.STAGE_PROJECT,
                    TRentAuditRecord.ACTION_SUBMIT, null, now);
            operLog.recordText(TOperLog.ACTION_SUBMIT, TOperLog.BIZ_LEDGER, l.getId(),
                    String.format("第%d次提交%s租金审核（提交项目审核）：本期应付 %s 元，本期实付 %s 元",
                            round, l.getPeriodName(), plain(l.getPeriodPayable()), plain(l.getPeriodPaid())));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(RentAuditDTO form) {
        for (Long id : idsOf(form)) {
            TRentLedger l = getExisting(id);
            checkNotHistory(l, "审核");
            checkFarmerLocked(l);
            String stage = auditStageOf(l.getAuditStatus());
            if (stage == null) {
                throw new JeecgBootException(l.getPeriodName() + "不在审核中，不能审核");
            }
            requireAuditRole(stage);
            Date now = new Date();
            String remark = blank(form.getRemark()) ? null : form.getRemark().trim();
            String operator = operLog.currentUser();
            TRentLedger update = new TRentLedger()
                    .setId(l.getId())
                    .setAuditBy(operator).setAuditTime(now)
                    .setUpdateBy(operator).setUpdateTime(now);
            // 四级：项目 → 业务 → 财务 → 领导，最后一级通过即进入待支付
            switch (stage) {
                case TRentAuditRecord.STAGE_PROJECT:
                    update.setAuditStatus(TRentLedger.AUDIT_PENDING_BUSINESS)
                            .setProjectAuditBy(operator).setProjectAuditTime(now);
                    break;
                case TRentAuditRecord.STAGE_BUSINESS:
                    update.setAuditStatus(TRentLedger.AUDIT_PENDING_FINANCE)
                            .setBusinessAuditBy(operator).setBusinessAuditTime(now);
                    break;
                case TRentAuditRecord.STAGE_FINANCE:
                    update.setAuditStatus(TRentLedger.AUDIT_PENDING_LEADER)
                            .setFinanceAuditBy(operator).setFinanceAuditTime(now);
                    break;
                default:
                    update.setAuditStatus(TRentLedger.AUDIT_APPROVED)
                            .setLeaderAuditBy(operator).setLeaderAuditTime(now);
                    break;
            }
            ledgerService.updateById(update);

            saveAuditRecord(l.getId(), l.auditRound(), stage, TRentAuditRecord.ACTION_APPROVE, remark, now);
            operLog.recordText(TOperLog.ACTION_APPROVE, TOperLog.BIZ_LEDGER, l.getId(),
                    "第" + l.auditRound() + "次" + TRentAuditRecord.actionName(stage, TRentAuditRecord.ACTION_APPROVE)
                            + l.getPeriodName() + "租金"
                            + (remark == null ? "" : "，备注：" + remark));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(RentAuditDTO form) {
        // 需求稿第 15 条：驳回原因和附件都是非必输。
        // 这一条推翻了第 8 条的"审核驳回时需要填写备注信息"，按后出的第 15 条执行。
        String remark = form == null || blank(form.getRemark()) ? null : form.getRemark().trim();
        for (Long id : idsOf(form)) {
            TRentLedger l = getExisting(id);
            checkNotHistory(l, "驳回");
            checkFarmerLocked(l);
            String stage = auditStageOf(l.getAuditStatus());
            if (stage == null) {
                throw new JeecgBootException(l.getPeriodName() + "不在审核中，不能驳回");
            }
            requireAuditRole(stage);
            Date now = new Date();
            String operator = operLog.currentUser();
            TRentLedger update = new TRentLedger()
                    .setId(l.getId())
                    // 台账上只留最近一次，历次驳回原因在 t_rent_audit_record 里按轮次存
                    .setRejectRemark(remark)
                    .setAuditBy(operator).setAuditTime(now)
                    .setUpdateBy(operator).setUpdateTime(now);
            // 任何一级驳回都回到可编辑，重新提交后从项目审核重新走
            switch (stage) {
                case TRentAuditRecord.STAGE_PROJECT:
                    update.setAuditStatus(TRentLedger.AUDIT_PROJECT_REJECTED)
                            .setProjectAuditBy(operator).setProjectAuditTime(now);
                    break;
                case TRentAuditRecord.STAGE_BUSINESS:
                    update.setAuditStatus(TRentLedger.AUDIT_BUSINESS_REJECTED)
                            .setBusinessAuditBy(operator).setBusinessAuditTime(now);
                    break;
                case TRentAuditRecord.STAGE_FINANCE:
                    update.setAuditStatus(TRentLedger.AUDIT_FINANCE_REJECTED)
                            .setFinanceAuditBy(operator).setFinanceAuditTime(now);
                    break;
                default:
                    update.setAuditStatus(TRentLedger.AUDIT_LEADER_REJECTED)
                            .setLeaderAuditBy(operator).setLeaderAuditTime(now);
                    break;
            }
            ledgerService.updateById(update);

            saveAuditRecord(l.getId(), l.auditRound(), stage, TRentAuditRecord.ACTION_REJECT, remark, now);
            operLog.recordText(TOperLog.ACTION_REJECT, TOperLog.BIZ_LEDGER, l.getId(),
                    "第" + l.auditRound() + "次" + TRentAuditRecord.actionName(stage, TRentAuditRecord.ACTION_REJECT)
                            + l.getPeriodName() + "租金"
                            + (remark == null ? "，未填写驳回原因" : "，驳回原因：" + remark));
        }
    }

    /** 当前审核状态对应哪一级；不在审核中返回 null */
    private String auditStageOf(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case TRentLedger.AUDIT_PENDING_PROJECT:
                return TRentAuditRecord.STAGE_PROJECT;
            case TRentLedger.AUDIT_PENDING_BUSINESS:
                return TRentAuditRecord.STAGE_BUSINESS;
            case TRentLedger.AUDIT_PENDING_FINANCE:
                return TRentAuditRecord.STAGE_FINANCE;
            case TRentLedger.AUDIT_PENDING_LEADER:
                return TRentAuditRecord.STAGE_LEADER;
            default:
                return null;
        }
    }

    /** 每一级审核只允许对应角色操作 */
    private void requireAuditRole(String stage) {
        switch (stage) {
            case TRentAuditRecord.STAGE_PROJECT:
                roleGuard.requireProjectAudit("项目审核");
                break;
            case TRentAuditRecord.STAGE_BUSINESS:
                roleGuard.requireBusinessAudit("业务审核");
                break;
            case TRentAuditRecord.STAGE_FINANCE:
                roleGuard.requireFinanceAudit("财务审核");
                break;
            default:
                roleGuard.requireLeaderAudit("领导审核");
                break;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDispute(RentAuditDTO form) {
        roleGuard.requireLedgerOps("标记纠纷");
        requireRemark(form, "纠纷说明");
        TRentLedger l = getExisting(form.getId());
        checkNotHistory(l, "标记纠纷");
        checkFarmerLocked(l);
        if (l.voided()) {
            throw new JeecgBootException("已作废的台账不能标记纠纷");
        }
        if (l.disputed()) {
            throw new JeecgBootException("该期租金已经是纠纷暂停支付状态");
        }
        if (!l.editable()) {
            throw new JeecgBootException(l.getPeriodName() + "当前为" + auditName(l.getAuditStatus())
                    + "，审核期间不能标记纠纷");
        }
        // 需求稿第15条（第二轮）：从标记之日起后续所有租金的状态都为纠纷暂停
        String remark = form.getRemark().trim();
        // 先标记本期的纠纷暂停
        doMarkDispute(l, remark);
        // 再级联标记同一电站同一年的后续所有未锁定、未过期的期
        cascadeDispute(l, remark);
    }

    /** 标记单条台账的纠纷暂停 */
    private void doMarkDispute(TRentLedger l, String remark) {
        Date now = new Date();
        int round = l.disputeRound() + 1;

        disputeRecordService.save(new TRentDisputeRecord()
                .setLedgerId(l.getId())
                .setDisputeRound(round)
                .setDisputeTime(now)
                .setDisputeRemark(remark)
                .setDisputeBy(operLog.currentUser()));

        ledgerService.update(new LambdaUpdateWrapper<TRentLedger>()
                .eq(TRentLedger::getId, l.getId())
                .set(TRentLedger::getDisputeFlag, 1)
                .set(TRentLedger::getDisputeRound, round)
                .set(TRentLedger::getDisputeRemark, remark)
                .set(TRentLedger::getDisputeTime, now)
                .set(TRentLedger::getDisputeCancelTime, null)
                .set(TRentLedger::getUpdateBy, operLog.currentUser())
                .set(TRentLedger::getUpdateTime, now));

        operLog.recordText(TOperLog.ACTION_DISPUTE, TOperLog.BIZ_LEDGER, l.getId(),
                "于 " + fmtTime(now) + " 第" + round + "次标记" + l.getPeriodName()
                        + "租金为纠纷暂停支付，原因：" + remark);
    }

    /** 级联标记同一电站同一年的后续未完成期（需求第15条） */
    private void cascadeDispute(TRentLedger current, String remark) {
        List<TRentLedger> nextPeriods = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, current.getStationId())
                .eq(TRentLedger::getRentYear, current.getRentYear())
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0)
                .eq(TRentLedger::getDisputeFlag, 0)
                .gt(TRentLedger::getPeriodNo, current.getPeriodNo())
                .orderByAsc(TRentLedger::getPeriodNo));
        for (TRentLedger next : nextPeriods) {
            // 跳过已经锁定的（审核中/已通过/已支付）
            if (next.locked() || next.paid()) {
                continue;
            }
            doMarkDispute(next, "级联-" + remark);
        }
        if (!nextPeriods.isEmpty()) {
            log.info("纠纷标记已级联到 {} 后续 {} 条租金", current.getPeriodName(), nextPeriods.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDispute(RentAuditDTO form) {
        roleGuard.requireLedgerOps("取消纠纷标记");
        requireRemark(form, "取消纠纷的说明");
        TRentLedger l = getExisting(form.getId());
        checkNotHistory(l, "取消纠纷标记");
        checkFarmerLocked(l);
        if (!l.disputed()) {
            throw new JeecgBootException("该期租金不是纠纷暂停支付状态");
        }
        String remark = form.getRemark().trim();
        // 先取消本期的纠纷
        doCancelDispute(l, remark);
        // 再级联取消同一电站同一年的后续所有被级联标记的纠纷暂停
        cascadeCancelDispute(l, remark);
    }

    /** 取消单条台账的纠纷暂停 */
    private void doCancelDispute(TRentLedger l, String remark) {
        Date now = new Date();

        List<TRentDisputeRecord> open = disputeRecordService.list(
                new LambdaQueryWrapper<TRentDisputeRecord>()
                        .eq(TRentDisputeRecord::getLedgerId, l.getId())
                        .isNull(TRentDisputeRecord::getCancelTime)
                        .orderByAsc(TRentDisputeRecord::getDisputeRound));
        for (TRentDisputeRecord r : open) {
            disputeRecordService.updateById(new TRentDisputeRecord()
                    .setId(r.getId())
                    .setCancelTime(now)
                    .setCancelRemark(remark)
                    .setCancelBy(operLog.currentUser()));
        }
        if (open.isEmpty()) {
            disputeRecordService.save(new TRentDisputeRecord()
                    .setLedgerId(l.getId())
                    .setDisputeRound(Math.max(l.disputeRound(), 1))
                    .setDisputeTime(l.getDisputeTime())
                    .setDisputeRemark(l.getDisputeRemark())
                    .setCancelTime(now)
                    .setCancelRemark(remark)
                    .setCancelBy(operLog.currentUser()));
        }

        ledgerService.updateById(new TRentLedger()
                .setId(l.getId())
                .setDisputeFlag(0)
                .setDisputeCancelTime(now)
                .setUpdateBy(operLog.currentUser()).setUpdateTime(now));
        operLog.recordText(TOperLog.ACTION_CANCEL_DISPUTE, TOperLog.BIZ_LEDGER, l.getId(),
                "于 " + fmtTime(now) + " 取消" + l.getPeriodName() + "租金第"
                        + Math.max(l.disputeRound(), 1) + "次纠纷暂停支付标记"
                        + (l.getDisputeTime() == null ? ""
                        : "（自 " + fmtTime(l.getDisputeTime()) + " 起暂停）")
                        + "，原因：" + remark);
    }

    /** 级联取消同一电站同一年的后续纠纷暂停期（需求第15条） */
    private void cascadeCancelDispute(TRentLedger current, String remark) {
        List<TRentLedger> nextPeriods = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, current.getStationId())
                .eq(TRentLedger::getRentYear, current.getRentYear())
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0)
                .eq(TRentLedger::getDisputeFlag, 1)
                .gt(TRentLedger::getPeriodNo, current.getPeriodNo())
                .orderByAsc(TRentLedger::getPeriodNo));
        for (TRentLedger next : nextPeriods) {
            doCancelDispute(next, "级联-" + remark);
        }
        if (!nextPeriods.isEmpty()) {
            log.info("纠纷取消已级联到 {} 后续 {} 条租金", current.getPeriodName(), nextPeriods.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidLedger(RentAuditDTO form) {
        roleGuard.requireLedgerOps("作废租金");
        requireRemark(form, "作废原因");
        TRentLedger l = getExisting(form.getId());
        checkNotHistory(l, "作废");
        checkFarmerLocked(l);
        if (l.voided()) {
            throw new JeecgBootException("该台账已作废");
        }
        // 需求：已支付之前都可以作废 —— 待提交、审核中、已驳回、待支付都放开。
        // 只有已支付的不许作废：钱已经付出去了，作废会让这笔账凭空消失。
        if (l.paid()) {
            throw new JeecgBootException(l.getPeriodName() + "已支付，不能作废");
        }

        ledgerService.updateById(new TRentLedger()
                .setId(l.getId())
                .setVoidFlag(1)
                .setVoidRemark(form.getRemark().trim())
                .setUpdateBy(operLog.currentUser()).setUpdateTime(new Date()));
        // 作废的行退出累计链，剩下的行重排
        recalcChain(l.getStationId());
        operLog.recordText(TOperLog.ACTION_VOID, TOperLog.BIZ_LEDGER, l.getId(),
                "作废" + l.getPeriodName() + "租金台账，原因：" + form.getRemark().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(RentAuditDTO form) {
        roleGuard.requirePaymentOps("标记支付");
        for (Long id : idsOf(form)) {
            TRentLedger l = getExisting(id);
            checkNotHistory(l, "标记支付");
            checkFarmerLocked(l);
            if (l.voided()) {
                throw new JeecgBootException("已作废的台账不能标记支付");
            }
            if (l.getAuditStatus() == null || l.getAuditStatus() != TRentLedger.AUDIT_APPROVED) {
                throw new JeecgBootException(l.getPeriodName() + "尚未审核通过，不能标记已支付");
            }
            if (l.paid()) {
                continue;
            }
            ledgerService.updateById(new TRentLedger()
                    .setId(l.getId())
                    .setPayStatus(TRentLedger.PAY_PAID)
                    .setUpdateBy(operLog.currentUser()).setUpdateTime(new Date()));
            operLog.recordText(TOperLog.ACTION_PAY, TOperLog.BIZ_LEDGER, l.getId(),
                    "标记" + l.getPeriodName() + "租金为已支付，本期实付 " + plain(l.getPeriodPaid()) + " 元"
                            + (blank(form.getRemark()) ? "" : "，备注：" + form.getRemark()));
        }
    }

    // ==================== 累计链重算 ====================

    /**
     * 把一个电站所有未作废的台账按年度、期数顺序重排累计金额。
     *
     * 累计类的四个字段全是顺推出来的：
     *   截止本期累计应付 = 上期累计应付 + 本期应付
     *   截止上期累计实付 = 上一期的"截止本期累计实付"
     *   截止本期累计实付 = 截止上期累计实付 + 本期实付
     *   超/欠付        = 截止本期累计实付 - 截止本期累计应付
     * 所以只要有一期的金额动了，后面每一期都得重算，否则账面会前后对不上。
     *
     * 同一期存在多条未作废台账时（重新生成后旧的还没作废），
     * 以 id 最大的那条作为链上成员往后传，其余几条用同样的上期基数各自算，
     * 这样谁被作废掉都不会让后面的期数跳变。
     */
    private void recalcChain(Long stationId) {
        List<TRentLedger> all = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, stationId)
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0)
                .orderByAsc(TRentLedger::getRentYear)
                .orderByAsc(TRentLedger::getPeriodNo)
                .orderByAsc(TRentLedger::getId));
        if (all.isEmpty()) {
            return;
        }

        // 累计应付金额2 的基数取"截止上期累计实付"，不是累计应付——
        // 这样 超/欠付 = 累计实付 - 累计应付2 就退化成"本期实付 - 本期应付"，
        // 每期只看本期的超欠，不会被前面几期的差额一直带着走（需求第6条新口径）
        //
        // prevPayable 是"上期累计应付"，只用来算跨期结转的默认实付，不落库：
        //   上期累计超欠付 = 上期累计实付 − 上期累计应付
        //   本期默认实付   = 本期应付 − 上期累计超欠付
        // 这样上期欠 100 本期就默认多付 100 补上，上期超 100 本期就少付 100 抵扣。
        // 累计应付金额2 的定义保持不变（还是"上期累计实付 + 本期应付"）
        BigDecimal prevPaid = BigDecimal.ZERO;
        BigDecimal prevPayable = BigDecimal.ZERO;
        int i = 0;
        while (i < all.size()) {
            // 取出同一期的所有台账
            int j = i;
            while (j + 1 < all.size() && samePeriod(all.get(j + 1), all.get(i))) {
                j++;
            }
            // 上期累计超欠付（正数超付、负数欠付），整期共用
            BigDecimal carriedDiff = RentCalcEngine.money(prevPaid.subtract(prevPayable));
            BigDecimal groupPaid = prevPaid;
            BigDecimal groupPayable = prevPayable;
            for (int k = i; k <= j; k++) {
                TRentLedger l = all.get(k);
                BigDecimal payable = nvlMoney(l.getPeriodPayable());
                // 默认实付 = 本期应付 − 上期累计超欠付
                BigDecimal defaultPaid = RentCalcEngine.money(payable.subtract(carriedDiff));
                /*
                 * 什么时候用默认值：
                 *   实付为空，或者实付恰好等于本期应付 —— 这两种都视为"没人工调过"。
                 * 人工调过的（实付 != 应付）一律保留，不能因为重排累计链就把人工值冲掉。
                 */
                BigDecimal paid;
                if (l.getPeriodPaid() == null || sameMoney(l.getPeriodPaid(), payable)) {
                    paid = defaultPaid;
                } else {
                    paid = l.getPeriodPaid();
                }
                // 累计应付金额2 = 截止上期累计实付 + 本期应付
                BigDecimal accumPayable = RentCalcEngine.money(prevPaid.add(payable));
                BigDecimal accumPaid = RentCalcEngine.money(prevPaid.add(paid));
                BigDecimal diff = RentCalcEngine.money(accumPaid.subtract(accumPayable));

                if (!sameMoney(l.getPeriodPaid(), paid)
                        || !sameMoney(l.getAccumPayable(), accumPayable)
                        || !sameMoney(l.getPrevAccumPaid(), prevPaid)
                        || !sameMoney(l.getAccumPaid(), accumPaid)
                        || !sameMoney(l.getDiffAmount(), diff)) {
                    ledgerService.updateById(new TRentLedger()
                            .setId(l.getId())
                            .setPeriodPaid(RentCalcEngine.money(paid))
                            .setAccumPayable(accumPayable)
                            .setPrevAccumPaid(RentCalcEngine.money(prevPaid))
                            .setAccumPaid(accumPaid)
                            .setDiffAmount(diff));
                }
                // 链上只认最后一条（id 最大的那条）
                if (k == j) {
                    groupPaid = accumPaid;
                    groupPayable = prevPayable.add(payable);
                }
            }
            prevPaid = groupPaid;
            prevPayable = groupPayable;
            i = j + 1;
        }
    }

    private boolean samePeriod(TRentLedger a, TRentLedger b) {
        return eq(a.getRentYear(), b.getRentYear()) && eq(a.getPeriodNo(), b.getPeriodNo());
    }

    // ==================== 内部方法 ====================

    private List<TRentLedgerMonth> months(Long ledgerId) {
        List<TRentLedgerMonth> list = ledgerMonthService.list(new LambdaQueryWrapper<TRentLedgerMonth>()
                .eq(TRentLedgerMonth::getLedgerId, ledgerId)
                .orderByAsc(TRentLedgerMonth::getYm));
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 需求31/33：农户有变更在审核中时，提交时间所在期及之后的租金一律不能操作。
     * 已结束的期（截止日早于提交日）不受影响。
     */
    private void checkFarmerLocked(TRentLedger l) {
        if (farmerAuditService.periodLocked(l.getStationId(), l.getPeriodEnd())) {
            throw new JeecgBootException(l.getFarmerName() + " " + l.getPeriodName()
                    + " 所在农户有变更正在审核中，审核通过前不能操作");
        }
    }

    private TRentLedger getExisting(Long id) {
        if (id == null) {
            throw new JeecgBootException("缺少台账ID");
        }
        TRentLedger l = ledgerService.getById(id);
        if (l == null || (l.getDelFlag() != null && l.getDelFlag() == 1)) {
            throw new JeecgBootException("租金台账不存在或已删除");
        }
        return l;
    }

    /**
     * 需求稿第 9 条：历史租金只支持查看详情，不支持任何操作。
     *
     * 提交、审核、驳回、纠纷标记、作废、标记支付全部要过这一关，
     * 不是只有"修改"才算操作——否则去年遗留的待提交台账今年还能被提交掉。
     */
    private void checkNotHistory(TRentLedger l, String action) {
        if (l.getRentYear() != null && l.getRentYear() < Calendar.getInstance().get(Calendar.YEAR)) {
            throw new JeecgBootException(l.getRentYear() + "年的租金属于历史租金，只能查看详情，不支持" + action);
        }
    }

    /**
     * 需求稿第 9 条：历史年度的台账只能看，不能改也不能删。
     * 第 8 条：审核中和审核通过的台账不能改。
     */
    private void checkEditable(TRentLedger l) {
        checkNotHistory(l, "修改");
        checkFarmerLocked(l);
        if (l.voided()) {
            throw new JeecgBootException("已作废的台账不能修改");
        }
        if (l.paid()) {
            throw new JeecgBootException("已支付的租金不能修改");
        }
        if (!l.editable()) {
            throw new JeecgBootException(l.getPeriodName() + "当前为" + auditName(l.getAuditStatus())
                    + "，审核期间不能修改");
        }
    }

    private List<Long> idsOf(RentAuditDTO form) {
        List<Long> ids = new ArrayList<>();
        if (form != null) {
            if (form.getId() != null) {
                ids.add(form.getId());
            }
            for (Long id : nvl(form.getIds())) {
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            throw new JeecgBootException("请选择要操作的租金台账");
        }
        return ids;
    }

    private void requireRemark(RentAuditDTO form, String what) {
        if (form == null || blank(form.getRemark())) {
            throw new JeecgBootException("请填写" + what);
        }
    }

    /** 项目ID -> 一年几期。项目档案只有十几行，一次读全 */
    private Map<Long, Integer> periodCountByProject() {
        Map<Long, Integer> map = new HashMap<>();
        for (TProject p : projectService.list()) {
            map.put(p.getId(), p.periodsPerYear());
        }
        return map;
    }

    /** 电站所属项目一年几期，查不到项目按默认四期 */
    private int periodCountOf(TFarmerStation station, Map<Long, Integer> map) {
        Integer count = map == null ? null : map.get(station.getProjectId());
        return count == null ? RentPeriodUtil.DEFAULT_PERIOD_COUNT : count;
    }

    private String auditName(Integer status) {
        return TRentLedger.auditName(status);
    }

    private String desc(TFarmerStation s) {
        return "[" + s.getFarmerName() + "/" + s.getPowerUserNo() + "]";
    }

    /** 发电天数为空时按理论计租天数展示，日志里才看得懂改了什么 */
    private String showDays(Integer genDays, Integer rentDays) {
        if (genDays != null) {
            return genDays + "天";
        }
        return (rentDays == null ? 0 : rentDays) + "天（未录入，按日期计算）";
    }

    private String plain(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private String fmtTime(Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private String plainText(String v) {
        return blank(v) ? "空" : v;
    }

    private BigDecimal nvlMoney(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean sameMoney(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private <T> List<T> nvl(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}