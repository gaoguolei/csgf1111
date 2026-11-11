package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.mapper.PvDeviceMapper;
import org.jeecg.modules.pcms.project.service.IPvDeviceService;
import org.jeecg.modules.pcms.project.stationentity.PvDevice;
import org.springframework.stereotype.Service;

@Service
public class PvDeviceServiceImpl extends ServiceImpl<PvDeviceMapper, PvDevice> implements IPvDeviceService {
}
