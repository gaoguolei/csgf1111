package org.jeecg.modules.pcms.energyjob;

import org.jeecg.modules.pcms.project.service.IDevicePointMinFifteenService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DevicePointMinFifteenJob implements Job {
    @Autowired
    private IDevicePointMinFifteenService iDevicePointMinFifteenService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iDevicePointMinFifteenService.insert();
    }
}
