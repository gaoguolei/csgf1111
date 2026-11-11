package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.entity.PvProjectKey;
import org.jeecg.modules.pcms.project.mapper.PvProjectKeyMapper;
import org.jeecg.modules.pcms.project.service.PvProjectKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PvProjectKeyServiceImpl extends ServiceImpl<PvProjectKeyMapper, PvProjectKey> implements PvProjectKeyService {

    @Autowired
    private PvProjectKeyMapper pvProjectKeyMapper;

    @Override
    public PvProjectKey getByProjectId(Integer projectInfoId, Integer tenantId) {
//        LambdaQueryWrapper<PvProjectKey> pvProjectKeyLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        pvProjectKeyLambdaQueryWrapper.eq(PvProjectKey::getProjectId,projectInfoId);
//        pvProjectKeyLambdaQueryWrapper.eq(PvProjectKey::getTenantId,tenantId);
//        pvProjectKeyLambdaQueryWrapper.eq(PvProjectKey::getRowStatus,"1");
//        PvProjectKey pvProjectKey = pvProjectKeyMapper.selectOne(pvProjectKeyLambdaQueryWrapper);
//        return pvProjectKey;
        return pvProjectKeyMapper.getByProjectId(projectInfoId,tenantId);
    }

    @Override
    public PvProjectKey getByProjectIdNew(Integer tenantId) {
        return pvProjectKeyMapper.getByProjectIdNew(tenantId);
    }

    @Override
    public PvProjectKey getByProjectIdNewNew() {
        return pvProjectKeyMapper.getByProjectIdNewNew();
    }

    @Override
    public void updatePasswordKey(String keyStr, Integer id) {
        pvProjectKeyMapper.updatePasswordKey(keyStr,id);
    }

}
