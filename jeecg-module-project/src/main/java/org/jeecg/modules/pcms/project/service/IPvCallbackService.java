package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvCallback;

/**
 * @Author gongzhou
 * @Date 2023/11/20 下午6:03
 * @Version 1.5
 */
public interface IPvCallbackService extends IService<PvCallback> {


    String getSignFlowId(Integer projectId);
}
