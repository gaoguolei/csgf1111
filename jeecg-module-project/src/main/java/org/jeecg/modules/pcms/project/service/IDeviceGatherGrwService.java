package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceGatherGrw;

import java.util.List;

public interface IDeviceGatherGrwService  extends IService<DeviceGatherGrw> {


    void insert();

    List<String> listMessageSn(String psId);

}
