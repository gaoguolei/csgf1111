package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.pcms.project.entity.SysInverter;

/**
 * @Author gongzhou
 * @Date 2023/7/7 上午10:13
 * @Version 1.5
 */
public interface SysInverterMapper extends BaseMapper<SysInverter> {



    @Delete("delete sys_inverter,sys_inverter_wire,sys_inverter_wire_mppt from sys_inverter LEFT JOIN" +
            "  sys_inverter_wire on sys_inverter.id = sys_inverter_wire.inverter_id" +
            " left join sys_inverter_wire_mppt on sys_inverter_wire.id = sys_inverter_wire_mppt.inverter_wire_id where sys_inverter.project_id=#{projectId}")
    boolean deleteByProjectId(@Param("projectId")Integer projectId);

}
