package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.dto.solar.DeviceResultDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.vo.DeviceReturnVo;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface DeviceMapper extends BaseMapper<Device> {


    @Delete("delete  from device where source = 1")
    void deleteSolar();

    @Delete("delete  from device where source = 2")
    void deleteGrw();


    @Select("select device_id,device_name,source from device ")
    public List<DeviceResultDto> getDeviceName();

    @Select("select device_id,device_name,source from device where project_type=#{renterId}")
    public List<DeviceResultDto> getDeviceNameByProjectType(String renterId);

    @Select("select device_id,device_name,source from device where source=2  and project_type=#{renterId}")
    public List<DeviceResultDto> getDeviceNameByProjectTypeGrw(String renterId);


    @Select("select monthly_energy,yearly_energy,today_energy from device_detail_growatt where plant_id = #{plantId}")
    public DeviceStatsVo getStatsData(@Param("plantId")Integer plantId);



    @Select("select de.device_name AS psName, DATE_FORMAT(ds.install_date, '%Y-%m-%d') AS createTime , de.grid_connection_time as gridTime ,de.ps_location as address,de.total_capcity as designCapacity,total_energy as totalEnergy,total_energy_unit as totalEnergyUnit from device de LEFT JOIN device_detail_solar ds on de.device_id= ds.ps_id where ds.ps_key=#{psKey}")
    public PsInfoVo getPsInfoByPsKey(@Param("psKey")String psKey);

    @Select("select device_name AS psName, install_date AS createTime, total_capcity as designCapacity , device_name as address from device where device_id =#{psId}")
    public PsInfoVo getPsInfoByPsKeyForGrw(@Param("psId")String psId);


    @Select("select d.device_name,d.ps_location,ds.ps_key from device d LEFT JOIN device_detail_solar ds on d.device_id=ds.ps_id  where d.device_id=#{plantId}")
    public DeviceReturnVo getPsNameByPsId(@Param("plantId")Integer plantId);


    @Select("select device_name from device where device_id=#{psId}")
    public String getPsName(Long psId);


    @Select("select device_id from device where project_type=#{renterId}")
    public List<Long>getPsIdsByProjectType(String renterId);



    @Select("select dds.ps_key from device de left join device_detail_solar dds on de.device_id=dds.ps_id where de.project_type=#{projectType}")
    public List<String> getPsKeyList(String projectType);


    @Select("select device_id from device where project_type=#{projectType} and source=2")
    public List<String> getPsKeyListGrw(String projectType);



    @Select("<script>SELECT device_id, device_name FROM device WHERE device_id IN " +
            "<foreach item='id' collection='psIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach></script>")
    List<Map<String, String>> getPsNameBatch(@Param("psIds") Set<Long> psIds);


    @Select("<script>" +
            "SELECT device_id AS psId, device_name AS psName " +
            "FROM device " +
            "WHERE device_id IN " +
            "<foreach item='id' collection='psIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<Long, String>> selectPsNames(@Param("psIds") Set<Long> psIds);
}
