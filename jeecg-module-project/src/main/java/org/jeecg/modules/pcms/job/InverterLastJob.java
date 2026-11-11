package org.jeecg.modules.pcms.job;

import org.jeecg.modules.pcms.project.service.IPvDeviceLastService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author gongzhou
 * @Date 2023/11/3 下午12:32
 * @Version 1.5
 */

/**
 * 更新电站最新数据
 */
public class InverterLastJob implements Job {
    @Autowired
    private IPvDeviceLastService iPvDeviceLastService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        iPvDeviceLastService.insert();
    }
}
