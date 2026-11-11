package org.jeecg.modules.pcms.project.service;

import org.jeecg.modules.pcms.common.DictOperateTypeEnum;
import org.jeecg.modules.pcms.project.entity.PvOperateLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: pv_operate_log
 * @Author: jun
 * @Date:   2023-03-11
 * @Version: V1.0
 */
public interface IPvOperateLogService extends IService<PvOperateLog> {

    /**
     * 新增日志
     * @param object
     * @param operateTypeEnum
     * @return
     * @param <T>
     */
    public <T> int  addOperateLog(T object, DictOperateTypeEnum operateTypeEnum);

}
