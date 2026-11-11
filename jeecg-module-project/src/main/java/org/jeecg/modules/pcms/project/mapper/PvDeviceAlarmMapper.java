package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvDeviceAlarm;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:26
 * @Version 1.5
 */
public interface PvDeviceAlarmMapper extends BaseMapper<PvDeviceAlarm> {


    @Delete("delete from pv_device_alarm ")
    public void deleteAll();

    @Select("SELECT * FROM pv_device_alarm WHERE sn=#{sn} ORDER BY start_time LIMIT 1;")
    public PvDeviceAlarm getMaxTime(@Param("sn")String sn);

    @Update("update pv_device_alarm set status=0 where id=#{id}")
    public void updateAlarm(@Param("id")Integer id);


    @Delete("delete from pv_device_alarm where id=#{id}")
    public void deleteAlarmById(@Param("id")Integer id);
}
