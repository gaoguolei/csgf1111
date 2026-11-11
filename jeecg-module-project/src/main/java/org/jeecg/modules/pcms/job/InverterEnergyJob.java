package org.jeecg.modules.pcms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.pcms.project.entity.PvEnergy;
import org.jeecg.modules.pcms.project.service.IPvEnergyService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/30 上午10:07
 * @Version 1.5
 *
 * 历史电量
 */
public class InverterEnergyJob implements Job {

    @Autowired
    private IPvEnergyService iPvEnergyService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //如果存过就新增新的数据。没有存过就全量更新数据
        //判断是否是有2023-01-01的数据，如果有说明走更新逻辑，没有就走新增逻辑.
        QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
        pvEnergyQueryWrapper.eq("year",2023);
        List<PvEnergy> list = iPvEnergyService.list(pvEnergyQueryWrapper);
        if(list != null && list.size()>0){
            iPvEnergyService.updatePvEnergy();
        }else {
            iPvEnergyService.insert();
        }
    }
}
