package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvDataloggerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author gongzhou
 * @Date 2023/11/16 上午8:11
 * @Version 1.5
 */
public class DataloggerJob implements Job {

    @Autowired
    private IPvDataloggerService iPvDataloggerService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iPvDataloggerService.insert();
    }
}
