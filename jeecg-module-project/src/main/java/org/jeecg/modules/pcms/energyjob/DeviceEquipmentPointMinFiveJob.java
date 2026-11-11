package org.jeecg.modules.pcms.energyjob;

import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinFiveService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DeviceEquipmentPointMinFiveJob implements Job {


    @Autowired
    private IDeviceEquipmentPointMinFiveService iDeviceEquipmentPointMinFiveService;



    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iDeviceEquipmentPointMinFiveService.insert();
    }

}
