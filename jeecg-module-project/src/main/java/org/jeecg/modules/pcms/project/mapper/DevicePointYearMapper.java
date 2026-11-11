package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DevicePointYear;

public interface DevicePointYearMapper extends BaseMapper<DevicePointYear> {

    @Delete("delete from device_point_year where time_stamp=#{timeStamp}")
    void deleteYear(String timeStamp);
}
