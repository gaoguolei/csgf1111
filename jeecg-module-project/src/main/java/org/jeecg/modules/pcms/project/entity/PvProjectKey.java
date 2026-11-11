package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: pv_project_key
 * @Author: ld
 * @Date: 2024-12-05
 * @Version: V1.0
 */
@ApiModel(value = "pv_project_key对象", description = "pv_project_key")
@Data
@TableName("pv_project_key")
public class PvProjectKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "项目ID")
    private Integer projectId;

    @ApiModelProperty(value = "所属租户")
    private Integer tenantId;

    @ApiModelProperty(value = "密钥")
    private String passwordKey;

    @ApiModelProperty(value = "秘钥类型 1:git秘钥")
    private String keyType;

    @ApiModelProperty(value = "创建时间戳")
    private Date createTime;

    @ApiModelProperty(value = "修改时间戳")
    private Date updateTime;

    @ApiModelProperty(value = "行状态：{正常：1，Normal；逻辑删除：8，Delete}", allowableValues = "1, 8")
    @TableLogic
    private String rowStatus;


}
