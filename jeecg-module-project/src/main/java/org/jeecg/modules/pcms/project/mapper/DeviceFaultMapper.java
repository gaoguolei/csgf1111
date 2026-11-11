package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DeviceFault;

public interface DeviceFaultMapper extends BaseMapper<DeviceFault> {

    @Delete("delete  from device_fault where source = 1")
    void deleteAllDeviceFault();
}
