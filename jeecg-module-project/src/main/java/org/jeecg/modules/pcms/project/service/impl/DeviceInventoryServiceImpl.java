package org.jeecg.modules.pcms.project.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.energy.entity.DeviceInventory;
import org.jeecg.modules.pcms.project.mapper.DeviceInventoryMapper;
import org.jeecg.modules.pcms.project.service.IDeviceInventoryService;
import org.springframework.stereotype.Service;

@Service
public class DeviceInventoryServiceImpl extends ServiceImpl<DeviceInventoryMapper, DeviceInventory> implements IDeviceInventoryService {
}
