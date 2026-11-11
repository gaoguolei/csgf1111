package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceLast;

public interface PvDeviceLastMapper extends BaseMapper<PvDeviceLast> {

    @Delete("delete from pv_device_last")
    public void deleteAll();
}
