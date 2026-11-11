package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author gongzhou
 * @Date 2023/7/7 上午10:04
 * @Version 1.5
 */

@Data
@TableName("sys_inverter_wire_mppt")
public class SysInverterWireMppt implements Serializable {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;

    @ApiModelProperty(value = "逆变器id")
    private java.lang.Integer inverterWireId;

    @ApiModelProperty(value = "串")
    private java.lang.String strand;

    @ApiModelProperty(value = "块")
    private java.lang.Integer piece;

    @ApiModelProperty(value = "安装容量")
    private java.lang.String capacity;

    @ApiModelProperty(value = "安装角度")
    private java.lang.String angle;

    @ApiModelProperty(value = "组串电压")
    private java.lang.String voltage;

}
