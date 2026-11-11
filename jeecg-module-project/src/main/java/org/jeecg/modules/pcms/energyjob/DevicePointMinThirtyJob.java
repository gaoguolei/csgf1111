package org.jeecg.modules.pcms.energyjob;

import org.jeecg.modules.pcms.project.service.IDevicePointMinThirtyService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DevicePointMinThirtyJob implements Job {

    @Autowired
    private IDevicePointMinThirtyService iDevicePointMinThirtyService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iDevicePointMinThirtyService.insert();
    }
}
