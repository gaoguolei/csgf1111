package org.jeecg.modules.pcms.aspect;

import org.jeecg.modules.pcms.common.DictOperateTypeEnum;

/**
 * @author jun
 */
public interface PvOperateLogStrategy {
    /**
     * 根据参数类型增加日志
     * @param object
     * @param operateTypeEnum
     * @return
     * @param <T>
     */
    public <T> int  addOperateLog(T object, DictOperateTypeEnum operateTypeEnum);
}
