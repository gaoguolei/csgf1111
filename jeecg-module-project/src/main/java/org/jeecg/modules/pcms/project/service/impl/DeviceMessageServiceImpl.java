package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.energy.entity.DeviceMessage;
import org.jeecg.modules.pcms.project.mapper.DeviceMessageMapper;
import org.jeecg.modules.pcms.project.service.IDeviceMessageService;
import org.springframework.stereotype.Service;

@Service
public class DeviceMessageServiceImpl extends ServiceImpl<DeviceMessageMapper, DeviceMessage> implements IDeviceMessageService {
}
