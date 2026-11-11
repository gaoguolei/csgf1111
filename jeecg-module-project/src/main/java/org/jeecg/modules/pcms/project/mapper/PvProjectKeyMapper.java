package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;
import org.jeecg.modules.pcms.project.entity.PvProjectKey;

public interface PvProjectKeyMapper extends BaseMapper<PvProjectKey> {

    PvProjectKey getByProjectId(@Param("projectInfoId") Integer projectInfoId, @Param("tenantId") Integer tenantId);

    void updatePasswordKey(@Param("keyStr") String keyStr, @Param("id") Integer id);

    PvProjectKey getByProjectIdNew(Integer tenantId);

    PvProjectKey getByProjectIdNewNew();

}
