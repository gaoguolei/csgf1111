package org.jeecg.modules.pcms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.pcms.project.service.IPvDeviceHistoryDataService;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceHistoryData;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/11 下午4:39
 * @Version 1.5
 * 逆变器历史数据 次定时任务一天执行一次
 */
public class DeviceHistoryDataJob implements Job {

    @Autowired
    private IPvDeviceHistoryDataService iPvDeviceHistoryDataService;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        QueryWrapper<PvDeviceHistoryData> pvDeviceHistoryDataQueryWrapper = new QueryWrapper<>();
        pvDeviceHistoryDataQueryWrapper.eq("year",2023);
        pvDeviceHistoryDataQueryWrapper.eq("month",11);
        pvDeviceHistoryDataQueryWrapper.eq("day",1);
        List<PvDeviceHistoryData> list = iPvDeviceHistoryDataService.list(pvDeviceHistoryDataQueryWrapper);
        try {
            if(list != null && list.size()>0){
                iPvDeviceHistoryDataService.updateData();
            }else {
                iPvDeviceHistoryDataService.insert();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
