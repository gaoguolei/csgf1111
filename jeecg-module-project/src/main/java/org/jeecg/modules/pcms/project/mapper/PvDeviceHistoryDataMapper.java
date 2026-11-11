package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceHistoryData;
import org.jeecg.modules.pcms.project.stationvo.ResultPvDeviceVO;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/11 下午4:30
 * @Version 1.5
 */
public interface PvDeviceHistoryDataMapper extends BaseMapper<PvDeviceHistoryData> {




    @Select("SELECT * FROM pv_device_history_data WHERE sn=#{sn} and year = #{year} and month=#{month} and day=#{day} ORDER BY time DESC LIMIT 1")
    public ResultPvDeviceVO getList(@Param("sn")String sn,@Param("year")Integer year,@Param("month")Integer month,@Param("day")Integer day);


    @Select("SELECT SUM(total_data) AS month_total_data FROM (SELECT MAX(today_energy) AS total_data FROM pv_device_history_data WHERE   sn=#{sn} and year=#{year} and month = #{month}  GROUP BY DATE(time)) AS temp_table")
    public String getTotalEnergy(@Param("sn")String sn,@Param("year")Integer year,@Param("month")Integer month);

}
