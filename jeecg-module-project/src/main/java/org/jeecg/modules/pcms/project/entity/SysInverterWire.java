package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author gongzhou
 * @Date 2023/7/7 上午9:59
 * @Version 1.5
 */

@Data
@TableName("sys_inverter_wire")
public class SysInverterWire implements Serializable {


    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;

    @ApiModelProperty(value = "逆变器id")
    private java.lang.Integer inverterId;

    @ApiModelProperty(value = "接线数量")
    private java.lang.Integer wireNum;

    @ApiModelProperty(value = "设备名称")
    private java.lang.String wireName;

}
