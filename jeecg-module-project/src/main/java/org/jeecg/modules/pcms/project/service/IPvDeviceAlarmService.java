package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvDeviceAlarm;
import org.jeecg.modules.pcms.project.vo.PageVo;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:26
 * @Version 1.5
 */
public interface IPvDeviceAlarmService extends IService<PvDeviceAlarm> {
    public void insert();

    public void delete(Integer id);

    public void update(Integer id);


    public  IPage<PvDeviceAlarm> stationAlarm(PageVo pageVo);
}
