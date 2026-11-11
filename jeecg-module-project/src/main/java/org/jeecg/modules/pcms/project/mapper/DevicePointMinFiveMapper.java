package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DevicePointMinFive;

import java.util.List;

public interface DevicePointMinFiveMapper extends BaseMapper<DevicePointMinFive> {



    @Select("SELECT ps_key FROM device_point_min_five GROUP BY ps_key;")
    List<String> getHaveDataPsKeyList();

    @Select("SELECT ps_key FROM device_point_day GROUP BY ps_key;")
    List<String> getHavePsKeyList();
}
