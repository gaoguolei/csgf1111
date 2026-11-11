package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DevicePointDay;

import java.math.BigDecimal;
import java.util.List;

public interface DevicePointDayMapper extends BaseMapper<DevicePointDay> {




    BigDecimal getWeek(@Param("timeStamp") String timeStamp, @Param("time")String time, @Param("psKeyList")List<String> psKeyList,@Param("deviceSns")List<String> deviceSns);
}
