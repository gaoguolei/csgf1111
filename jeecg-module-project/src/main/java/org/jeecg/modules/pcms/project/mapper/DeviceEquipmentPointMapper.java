package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPoint;

public interface DeviceEquipmentPointMapper extends BaseMapper<DeviceEquipmentPoint> {


    @Delete("delete from device_equipment_point where source = #{source}")
    void deleteBySource(String source);
}
