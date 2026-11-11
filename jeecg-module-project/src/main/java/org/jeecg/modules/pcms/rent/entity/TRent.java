package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 租金表，一个安装类型 × 一个租金档位 = 一行
 * @Version: V1.0
 */
@Data
@TableName("t_rent")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent对象", description = "租金表")
public class TRent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "农户电站信息表ID")
    private Long stationId;

    @ApiModelProperty(value = "安装信息表ID")
    private Long installId;

    @ApiModelProperty(value = "农户姓名，冗余")
    private String farmerName;

    @ApiModelProperty(value = "发电户号，冗余")
    private String powerUserNo;

    @ApiModelProperty(value = "所属项目ID，冗余")
    private Long projectId;

    @ApiModelProperty(value = "租金单价类型ID")
    private Long priceTypeId;

    @ApiModelProperty(value = "租金单价，元/块/年")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "金额 = 单价 × 块数，元/年")
    private BigDecimal amount;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "单价生效起日，NULL=从并网日或档位起始日起。"
            + "需求第21条：改价时新单价从次月1号起生效")
    private Date effectDateFrom;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "单价生效止日，NULL=到租期结束。"
            + "需求第21条：改价时旧单价只生效到次月1号前")
    private Date effectDateTo;

    private Integer delFlag;
    private Date delTime;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
