package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointYear;

public interface DeviceEquipmentPointYearMapper extends BaseMapper<DeviceEquipmentPointYear> {



    @Delete("delete from device_equipment_point_year where time_stamp=#{year}")
    void deleteYear(String year);

}
