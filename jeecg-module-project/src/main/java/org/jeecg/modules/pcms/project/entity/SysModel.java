package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/7/3 上午8:42
 * @Version 1.5
 */
@TableName("sys_model")
@Data
public class SysModel {
    private Integer id;

    //模板名称
    private String modelName;

    //模板类型 1：模板·村镇资料。2：模板·申报资料。3：模板·竣工资料。4：模板·接入方案 5：模板·通用
    private Integer modelType;

    //备用字段
    private String standby;


    //状态0：正常。8：删除
    private Integer status;


}
