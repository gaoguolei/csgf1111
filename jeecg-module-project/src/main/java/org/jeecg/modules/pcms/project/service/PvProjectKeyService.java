package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvProjectKey;

public interface PvProjectKeyService extends IService<PvProjectKey> {

    PvProjectKey getByProjectId(Integer projectInfoId, Integer tenantId);

    void updatePasswordKey(String keyStr, Integer id);

    PvProjectKey getByProjectIdNew(Integer tenantId);

    PvProjectKey getByProjectIdNewNew();

}
