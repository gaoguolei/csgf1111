package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

@ApiModel(value = "pv_project_type对象", description = "pv_project_type")
@Data
@TableName("pv_project_type")
public class PvProjectType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;

    @ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;

    @ApiModelProperty(value = "项目类型value")
    private java.lang.String typeValue;

    @ApiModelProperty(value = "天津查询参数")
    private java.lang.String tjType;
    @ApiModelProperty(value = "海南查询参数")
    private java.lang.String hainType;
    @ApiModelProperty(value = "海南2期查询参数")
    private java.lang.String hain2Type;
    @ApiModelProperty(value = "徐州查询参数")
    private java.lang.String xzhType;
    @ApiModelProperty(value = "北京查询参数")
    private java.lang.String bjType;
    @ApiModelProperty(value = "河南查询参数")
    private java.lang.String henType;
    @ApiModelProperty(value = "廊坊查询参数")
    private java.lang.String lfType;

//    @ApiModelProperty(value = "创建人")
//    private java.lang.String createBy;
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
//    @ApiModelProperty(value = "更新人")
//    private java.lang.String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;


    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
//    @TableLogic
    private java.lang.Integer delFlag;

}
