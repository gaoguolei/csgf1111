package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceFaultDto;
import org.jeecg.modules.pcms.energy.entity.DeviceFault;
import org.jeecg.modules.pcms.energy.entity.DeviceFaultHistory;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import java.util.List;

public interface IDeviceFaultService extends IService<DeviceFault> {


    void saveDeviceFaultAll(List<SolarDeviceFaultDto.PageListItem> pageItems);


    List<DeviceFault>  getFaultListByPlantId(DevicePageVo devicePageVo, String plantId, Integer source);

    List<DeviceFaultHistory> getFaultListByPlantIdHistory(DevicePageVo devicePageVo, String plantId, Integer source);

    void insert();
}
