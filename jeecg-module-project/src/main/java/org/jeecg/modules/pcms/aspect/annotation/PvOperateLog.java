package org.jeecg.modules.pcms.aspect.annotation;

import org.jeecg.modules.pcms.common.DictOperateTypeEnum;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @Author jun
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PvOperateLog {

    /**
     * 操作日志类型
     *
     * @return （1查询，2添加，3修改，4删除）
     */
    DictOperateTypeEnum operateType() ;

}
