package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DevicePointYearGrw;

public interface DevicePointYearGrwMapper extends BaseMapper<DevicePointYearGrw> {

    @Delete("delete from device_point_year_grw where time_stamp = #{timeStamp}" )
    void  deleteByTimeStamp(String timeStamp);
}
