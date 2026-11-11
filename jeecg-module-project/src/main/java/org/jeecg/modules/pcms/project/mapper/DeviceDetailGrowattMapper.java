package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;

public interface DeviceDetailGrowattMapper extends BaseMapper<DeviceDetailGrowatt> {


    @Delete("delete from device_detail_growatt")
    void deleteAll();
}
