package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;

public interface IDeviceDetailGrowattService  extends IService<DeviceDetailGrowatt> {

    void deleteAll();

    void insert();


    DeviceDetailGrowatt getDeviceDetail(String psId);

}
