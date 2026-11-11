package org.jeecg.modules.pcms.energyjob;

import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinThirtyService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DeviceEquipmentPointMinThirtyJob implements Job {

    @Autowired
    private IDeviceEquipmentPointMinThirtyService iDeviceEquipmentPointMinThirtyService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iDeviceEquipmentPointMinThirtyService.insert();
    }
}
