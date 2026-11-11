package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvDeviceAlarmService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.annotation.Resource;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:06
 * @Version 1.5
 * 报警定时任务
 */
public class StationAlarmJob implements Job {

    @Resource
    private IPvDeviceAlarmService iPvDeviceAlarmService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iPvDeviceAlarmService.insert();
    }
}
