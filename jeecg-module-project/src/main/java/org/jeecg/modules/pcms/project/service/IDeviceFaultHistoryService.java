package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceFaultDto;
import org.jeecg.modules.pcms.energy.entity.DeviceFaultHistory;

import java.util.List;

public interface IDeviceFaultHistoryService extends IService<DeviceFaultHistory> {



    void saveDeviceFaultAll(List<SolarDeviceFaultDto.PageListItem> pageItems);


    void insert();

}
