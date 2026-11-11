package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailSolar;
import org.jeecg.modules.pcms.energy.vo.DeviceDataVo;
import org.jeecg.modules.pcms.energy.vo.DevicePointVo;

import java.util.List;

public interface DeviceDetailSolarMapper  extends BaseMapper<DeviceDetailSolar> {




    @Select("SELECT ps_key, install_date,DATE_FORMAT(install_date, '%Y-%m-%d') AS formatted_install_date FROM device_detail_solar")
    List<DevicePointVo> getPsKeyVoList();


    @Delete("delete from device_detail_solar")
    void deleteAll();

    @Select("SELECT ps_key FROM device_detail_solar")
    List<String> getDevicePsKey();

    @Select("select ps_key from device_detail_solar where ps_id =#{deviceId}")
    String plantPsKey(Integer deviceId);



}
