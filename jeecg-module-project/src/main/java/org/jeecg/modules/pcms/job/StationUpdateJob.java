package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvStationsService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author gongzhou
 * @Date 2023/10/25 上午7:56
 * @Version 1.5
 */
public class StationUpdateJob implements Job {

    @Autowired
    private IPvStationsService iPvStationsService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        iPvStationsService.insert();
    }



}
