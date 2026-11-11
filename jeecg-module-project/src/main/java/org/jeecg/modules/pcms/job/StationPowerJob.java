package org.jeecg.modules.pcms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.pcms.project.entity.PvDayPower;
import org.jeecg.modules.pcms.project.service.IPvDayPowerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午7:26
 * @Version 1.5
 */
public class StationPowerJob implements Job {

    @Autowired
    private IPvDayPowerService iPvDayPowerService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        QueryWrapper<PvDayPower> pvDayPowerQueryWrapper = new QueryWrapper<>();
        pvDayPowerQueryWrapper.eq("year",2023);
        List<PvDayPower> list = iPvDayPowerService.list(pvDayPowerQueryWrapper);
        if(list != null && list.size()>0){
            iPvDayPowerService.updatePower();
        }
        else {
            iPvDayPowerService.insert();
        }
    }
}
