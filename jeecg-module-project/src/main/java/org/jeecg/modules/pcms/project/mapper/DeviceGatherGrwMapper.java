package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.energy.entity.DeviceGatherGrw;

import java.util.List;

public interface DeviceGatherGrwMapper extends BaseMapper<DeviceGatherGrw> {


    @Delete("delete from device_gather_grw")
    void  deleteAll();

    @Select("select sn from device_gather_grw where ps_id=#{psId}")
    List<String> listMessageSn(String psId);
}
