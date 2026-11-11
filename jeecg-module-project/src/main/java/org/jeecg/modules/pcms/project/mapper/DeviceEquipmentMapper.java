package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.vo.DeviceEquipmentPointVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import java.util.Set;

import java.util.List;

public interface DeviceEquipmentMapper extends BaseMapper<DeviceEquipment> {



    @Select("SELECT ps_key,grid_connection_date, DATE_FORMAT(grid_connection_date, '%Y-%m-%d') AS connection_date FROM device_equipment WHERE source=1 AND device_type=1 and dev_fault_status in (1,2) ORDER BY connection_date, grid_connection_date")
    List<DeviceEquipmentPointVo> getPsKeyVoListOfFault();


    @Delete("delete from device_equipment where source=2")
    void deleteDeviceEquipmentGrw();


    @Delete("delete from device_equipment where source=1")
    void deleteDeviceEquipmentSolar();


    @Select("select de.device_id,de.device_name AS psName,de.total_capcity as designCapacity, dee.device_sn as deviceSn, dee.communication_dev_sn as communicationDevSn, dee.device_model_code as deviceModelCode,dee.dev_status as deviceStatus from device de LEFT JOIN device_equipment dee  on  dee.ps_id= de.device_id where dee.ps_key= #{psKey} and de.source = 1")
    PsInfoVo getDeviceInfo(String psKey);

    @Select("select de.device_id,de.device_name AS psName,de.total_capcity as designCapacity, dee.device_sn as deviceSn, dee.communication_dev_sn as communicationDevSn, dee.device_model_code as deviceModelCode,dee.grw_status as deviceStatus from device de LEFT JOIN device_equipment dee  on  dee.ps_id= de.device_id where dee.device_sn=#{psKey} and de.source = 2")
    PsInfoVo getDeviceInfoGrw(String deviceSn);



    List<PsInfoVo>getDeviceInfos(@Param("psKeys") Set<String> psKeys);

    @Select("select deq.ps_key from device de LEFT JOIN device_equipment deq on de.device_id=deq.ps_id where de.project_type =#{renterId}  and de.source ='1' and deq.device_type= '1'")
    List<String> getPsKeyByProjectType(String renterId);

    @Select("select deq.device_sn from device de LEFT JOIN device_equipment deq on de.device_id=deq.ps_id where de.project_type =#{renterId}  and de.source ='2' and deq.device_type= '1'")
    List<String> getPsKeyByProjectTypeGrw(String renterId);

}
