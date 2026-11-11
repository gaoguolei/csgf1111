package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 农户安装信息表，一个农户 × 一个安装类型 = 一行
 * @Version: V1.0
 */
@Data
@TableName("t_install_info")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_install_info对象", description = "农户安装信息表")
public class TInstallInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "农户电站信息表ID")
    private Long stationId;

    @ApiModelProperty(value = "农户姓名，冗余")
    private String farmerName;

    @ApiModelProperty(value = "所属项目ID，冗余")
    private Long projectId;

    @ApiModelProperty(value = "发电户号，冗余")
    private String powerUserNo;

    @ApiModelProperty(value = "安装类型ID")
    private Long installTypeId;

    @ApiModelProperty(value = "组件型号，字符串原样存")
    private String moduleModel;

    @ApiModelProperty(value = "1=多型号混写，待人工拆分")
    private Integer isMultiModel;

    @ApiModelProperty(value = "安装块数")
    private Integer blockCount;

    @ApiModelProperty(value = "该类型容量kW，多型号行留空")
    private BigDecimal capacityKw;

    private Integer delFlag;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
