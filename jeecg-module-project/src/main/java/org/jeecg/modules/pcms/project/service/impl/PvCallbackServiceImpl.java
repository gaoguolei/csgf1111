package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.entity.PvCallback;
import org.jeecg.modules.pcms.project.mapper.PvCallbackMapper;
import org.jeecg.modules.pcms.project.service.IPvCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author gongzhou
 * @Date 2023/11/20 下午6:03
 * @Version 1.5
 */
@Service
public class PvCallbackServiceImpl extends ServiceImpl<PvCallbackMapper, PvCallback> implements IPvCallbackService {

    @Autowired
    private PvCallbackMapper pvCallbackMapper;
    @Override
    public String getSignFlowId(Integer projectId) {
        return pvCallbackMapper.getSignFlowId(projectId);
    }
}
