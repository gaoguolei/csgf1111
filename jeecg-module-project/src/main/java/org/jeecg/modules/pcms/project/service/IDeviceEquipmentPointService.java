package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPoint;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;

import java.util.List;
import java.util.Set;

public interface IDeviceEquipmentPointService extends IService<DeviceEquipmentPoint> {


    DeviceStatsVo getPointDataInPsKey(List<String> psKeyList);

    void deleteBySource(String source);



    public DeviceEquipmentPoint getDeviceEquipmentByPsKey(String psKey);

    void insert();

    void insertGrw();


    List<DeviceEquipmentPoint> getDeviceEquipmentByPsKeys(Set<String> psKeys);
}
