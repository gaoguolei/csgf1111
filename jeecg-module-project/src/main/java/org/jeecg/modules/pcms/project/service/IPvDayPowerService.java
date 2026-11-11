package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvDayPower;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午7:23
 * @Version 1.5
 */
public interface IPvDayPowerService extends IService<PvDayPower> {

    public void updatePower();
    public void insert();
}
