package org.jeecg.modules.pcms.rent.service.impl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.RentGenerateResultVO;
import org.jeecg.modules.pcms.rent.entity.TRentGenerateTask;
import org.jeecg.modules.pcms.rent.service.IRentLedgerBizService;
import org.jeecg.modules.pcms.rent.service.ITRentGenerateTaskService;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 年度台账生成的异步执行体。
 *
 * 单独拆成 Bean 是为了让 @Async 生效 —— 同类内部调用走不到代理，
 * 在 RentLedgerBizServiceImpl 里自调的话根本不会异步，还是在请求线程里跑，
 * 几万个电站跑几分钟，网关照样 504。
 *
 * 跑的是单线程池 rentGenerateExecutor：同一个时间只跑一个年度生成任务，
 * 两个一起跑只会互相拖慢、抢数据库连接。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class RentGenerateTaskRunner {

    /** 每处理这么多电站写一次进度，几万个电站逐条 update 太浪费 */
    private static final int PROGRESS_STEP = 20;

    @Autowired
    private ITRentGenerateTaskService taskService;

    @Autowired
    private OperLogHelper operLog;

    /** 业务类又要反过来调本类提交任务，加 @Lazy 打断循环依赖 */
    @Lazy
    @Autowired
    private IRentLedgerBizService ledgerBizService;

    /**
     * 后台跑一个年度生成任务。调用方拿到 taskId 就返回，不要等这个方法。
     *
     * @param taskId    任务ID
     * @param rentYear  生成年度
     * @param operUser  发起人，异步线程里没有请求上下文，只能靠传进来
     * @param projectId 只生成这个项目的电站，null=不限
     * @param tenantIds 只生成这些租户的电站，null=不限（空列表由调用方挡住）
     */
    @Async("rentGenerateExecutor")
    public void run(String taskId, int rentYear, String operUser,
                    Long projectId, java.util.List<Integer> tenantIds) {
        long start = System.currentTimeMillis();
        // 任务挂了也要知道跑到第几个电站了，所以用 AtomicInteger 在回调和 catch 之间共享
        AtomicInteger processed = new AtomicInteger();
        try {
            taskService.updateById(new TRentGenerateTask()
                    .setId(taskId)
                    .setStatus(TRentGenerateTask.STATUS_RUNNING));

            // 台账的 create_by/update_by 是用 operLog.currentUser() 取的，
            // 后台线程里没有请求上下文，得先把这个线程的操作人身份切过去
            RentGenerateResultVO result = operLog.callAs(operUser,
                    () -> ledgerBizService.generateYear(rentYear, operUser, done -> {
                        processed.set(done);
                        if (done % PROGRESS_STEP == 0) {
                            taskService.updateById(new TRentGenerateTask()
                                    .setId(taskId).setProgress(done));
                        }
                    }, projectId, tenantIds));

            JSONObject detail = new JSONObject();
            detail.put("skip", result.getSkipDetails());
            detail.put("fail", result.getFailDetails());
            long cost = System.currentTimeMillis() - start;

            taskService.updateById(new TRentGenerateTask()
                    .setId(taskId)
                    .setStatus(TRentGenerateTask.STATUS_DONE)
                    .setProgress(result.getStationCount())
                    .setCreatedCount(result.getCreatedCount())
                    .setSkippedCount(result.getSkippedCount())
                    .setFailCount(result.getFailCount())
                    .setPeriodRange(result.getPeriodRange())
                    .setDetailJson(detail.toJSONString())
                    .setCostMs(cost)
                    .setFinishTime(new Date()));

            log.info("{}年度台账生成任务完成 taskId={} 电站{}个 新生成{}条 跳过{}条 失败{}个 耗时{}ms",
                    rentYear, taskId, result.getStationCount(), result.getCreatedCount(),
                    result.getSkippedCount(), result.getFailCount(), cost);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("{}年度台账生成任务失败 taskId={}", rentYear, taskId, e);
            taskService.updateById(new TRentGenerateTask()
                    .setId(taskId)
                    .setStatus(TRentGenerateTask.STATUS_FAILED)
                    .setProgress(processed.get())
                    .setErrorMsg(brief(e.getMessage()))
                    .setCostMs(cost)
                    .setFinishTime(new Date()));
        }
    }

    private String brief(String msg) {
        if (msg == null) {
            return null;
        }
        String s = msg.replace("\n", " ").trim();
        return s.length() > 480 ? s.substring(0, 480) + "…" : s;
    }
}
