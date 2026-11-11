package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvProjectInfoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jun
 */
public class DeleteExpiredImagesJob implements Job {
    @Autowired
    private IPvProjectInfoService pvProjectInfoService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        pvProjectInfoService.deleteExpiredImages();
    }
}
