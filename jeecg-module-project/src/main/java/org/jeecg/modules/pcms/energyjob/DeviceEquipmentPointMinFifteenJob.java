package org.jeecg.modules.pcms.energyjob;

import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinFifteenService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DeviceEquipmentPointMinFifteenJob  implements Job {
    @Autowired
    private IDeviceEquipmentPointMinFifteenService iDeviceEquipmentPointMinFiveService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iDeviceEquipmentPointMinFiveService.insert();
    }
}
