package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvStationsViewService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午4:39
 * @Version 1.5
 */

/**
 * 电站概览
 */
public class StationViewJob implements Job {

    @Autowired
    private IPvStationsViewService iPvStationsViewService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iPvStationsViewService.insert();
    }
}
