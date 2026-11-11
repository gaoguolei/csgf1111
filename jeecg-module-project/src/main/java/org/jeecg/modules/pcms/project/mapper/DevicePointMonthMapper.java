package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DevicePointMonth;

import java.math.BigDecimal;
import java.util.List;

public interface DevicePointMonthMapper extends BaseMapper<DevicePointMonth> {


    @Select("SELECT ps_key FROM device_point_month GROUP BY ps_key")
    List<String> getHavePsKeyList();


    @Delete("delete from device_point_month where time_stamp=#{timeStamp}")
    void deleteMonth(String timeStamp);


    BigDecimal getMonth(@Param("timeStamp") String timeStamp,@Param("psKeyList")List<String> psKeyList,@Param("deviceSns")List<String> deviceSns);




}
