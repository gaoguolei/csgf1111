package org.jeecg.modules.pcms.rent.job;

import java.util.Calendar;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.RentGenerateResultVO;
import org.jeecg.modules.pcms.rent.service.IRentLedgerBizService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 年度租金台账生成任务。需求稿第 7 条：每年 3 月 1 日生成当年所有电站全年的租金台账。
 *
 * cron: 0 0 1 1 3 ? —— 每年 3 月 1 日凌晨 1 点。
 * 实际以 sys_quartz_job 里的配置为准（增量脚本 20261110_rent_generate_job_cron.sql 统一过）。
 * 全年四期一次生成完，已经存在的期会跳过，任务重跑不会产生重复台账。
 *
 * 参数 year 可选，填了就补生成指定年度，用于漏跑后手工补：
 *   在定时任务的"参数"里填 {"year":2025}
 *
 * @Version: V1.0
 */
@Slf4j
public class RentLedgerGenerateJob implements Job {

    /** 定时任务参数：年度。jeecg 会把同名 setter 注进来 */
    private Integer year;

    @Autowired
    private IRentLedgerBizService rentLedgerBizService;

    public void setYear(Integer year) {
        this.year = year;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int target = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        log.info("开始生成 {} 年度租金台账", target);
        try {
            RentGenerateResultVO result = rentLedgerBizService.generateYear(target);
            log.info("{} 年度租金台账生成完成：电站{}个，新生成{}条，跳过{}条，失败{}个，耗时{}ms",
                    target, result.getStationCount(), result.getCreatedCount(),
                    result.getSkippedCount(), result.getFailCount(), result.getCostMs());
        } catch (Exception e) {
            // 抛出去让 quartz 记录失败，但异常信息先落到自己的日志里，方便定位
            log.error("{} 年度租金台账生成失败", target, e);
            throw new JobExecutionException(e);
        }
    }
}
