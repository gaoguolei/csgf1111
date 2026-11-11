package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Author gongzhou
 * @Date 2023/7/8 上午8:06
 * @Version 1.5
 */
@Data
@TableName("sys_assembly")
public class SysAssembly implements Serializable {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;


    @ApiModelProperty(value = "组件名称")
    private java.lang.String assemblyName;

    @ApiModelProperty(value = "组件规格")
    private java.lang.String assemblySpec;

    @ApiModelProperty(value = "组件型号")
    private java.lang.String assemblyModel;

    @ApiModelProperty(value = "项目id")
    private java.lang.Integer projectId;


    @ApiModelProperty(value = "租户id")
    private java.lang.Integer tenantId;

    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;


    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    @TableLogic
    private java.lang.Integer delFlag;
}
