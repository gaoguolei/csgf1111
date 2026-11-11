package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.pcms.project.entity.SysAssembly;

/**
 * @Author gongzhou
 * @Date 2023/7/8 上午8:09
 * @Version 1.5
 */
public interface SysAssemblyMapper extends BaseMapper<SysAssembly> {


    @Delete(" delete sys_assembly from sys_assembly where project_id=#{projectId}")
    boolean deleteByProjectId(@Param("projectId")Integer projectId);
}
