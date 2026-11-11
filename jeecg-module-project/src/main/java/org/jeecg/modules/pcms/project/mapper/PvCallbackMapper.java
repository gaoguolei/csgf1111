package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.entity.PvCallback;

/**
 * @Author gongzhou
 * @Date 2023/11/20 下午6:02
 * @Version 1.5
 */
public interface PvCallbackMapper  extends BaseMapper<PvCallback> {



    @Select("SELECT sign_flow_id from pv_callback where project_id = #{projectId}")
    public String getSignFlowId(@Param("projectId")Integer projectId);
}
