package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMonth;

public interface DeviceEquipmentPointMonthMapper  extends BaseMapper<DeviceEquipmentPointMonth> {


    @Delete("delete from device_equipment_point_month where time_stamp=#{month}")
    void deleteMonty(String month);
}
