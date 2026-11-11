package org.jeecg.modules.pcms.aspect;

import org.jeecg.modules.pcms.common.DictOperateTypeEnum;
import org.jeecg.modules.pcms.project.service.IPvOperateLogService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jun
 */
@Component
public class PvOperateLogProjectStrategy implements PvOperateLogStrategy, InitializingBean {

    @Autowired
    IPvOperateLogService pvOperateLogService;

    @Override
    public <T> int addOperateLog(T object, DictOperateTypeEnum operateTypeEnum) {
        pvOperateLogService.addOperateLog(object,operateTypeEnum);
        return 0;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        PvOperateLogStrategyFactory.register("PROJECT",this);

    }
}
