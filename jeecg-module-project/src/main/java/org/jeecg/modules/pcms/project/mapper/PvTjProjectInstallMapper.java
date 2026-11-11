package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.pcms.project.entity.PvTjProjectInstall;

/**
 * @Author gongzhou
 * @Date 2024/5/15 下午1:51
 * @Version 1.5
 */
public interface PvTjProjectInstallMapper extends BaseMapper<PvTjProjectInstall> {


    @Delete("delete from pv_tj_project_install where project_id = #{projectId}")
    void deleteByProjectId(@Param("projectId")Integer projectId);
}
