package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.springframework.stereotype.Service;

import java.util.List;

public interface PvStationsMapper extends BaseMapper<PvStations> {



    @Delete("delete from pv_stations")
    public void deleteAll();



    IPage<PvStations> pageList(Page page, @Param("pageVo") PageVo pageVo);


    List<PvStations> selectStationCount(@Param("status") String status);


    @Select("SELECT CASE WHEN  MAX(CONVERT(pd.status, SIGNED)) = 1 THEN 1 ELSE 0 END AS ss FROM pv_stations pv LEFT JOIN pv_datalogger pd ON pd.plant_id = pv.plant_id  WHERE pv.plant_id=#{plantId}  GROUP BY pv.id")
    public String getStatus(@Param("plantId") Integer plantId);


    @Select("select username from sys_user where create_by=#{username}")
    public List<String> getUserIds(@Param("username") String username);

}
