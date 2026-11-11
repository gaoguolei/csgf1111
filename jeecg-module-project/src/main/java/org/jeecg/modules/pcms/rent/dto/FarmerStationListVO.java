package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 农户信息列表行。
 * 需求稿第 3 条要求展示：编号、农户姓名、安装地址、安装总容量、发电户号、
 * 并网日期、租金截止日期、安装块数。
 *
 * 这些字段全部在 t_farmer_station 单表里，不需要联表；
 * 项目名称和大区名称由 Service 从 12 行的项目档案内存映射补上。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "农户列表行", description = "农户列表行")
public class FarmerStationListVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "编号：北京为GJ开头的编号，天津为4位编码，徐州海南河南无")
    private String stationCode;

    @ApiModelProperty(value = "农户姓名，可能是村委会、小学等单位")
    private String farmerName;

    @ApiModelProperty(value = "安装地址")
    private String installAddr;

    @ApiModelProperty(value = "安装总容量 kW")
    private BigDecimal totalCapacity;

    @ApiModelProperty(value = "发电户号")
    private String powerUserNo;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期")
    private Date gridDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "租金截止日期 = 并网日期 + 租期年限")
    private Date rentDeadline;

    @ApiModelProperty(value = "安装总块数")
    private Integer totalBlocks;

    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @ApiModelProperty(value = "所属项目名称，如 北京市户用分布式光伏项目一期")
    private String projectName;

    @ApiModelProperty(value = "大区编码")
    private String regionCode;

    @ApiModelProperty(value = "大区名称")
    private String regionName;

    @ApiModelProperty(value = "1=该农户存在待人工处理的数据，如占位户号或占位姓名")
    private Integer needCheck;

    @ApiModelProperty(value = "审核状态 0正常 "
            + "1新增待项目审核 2新增项目驳回 3新增待业务审核 4新增业务驳回 5新增待领导审核 6新增领导驳回 "
            + "7修改待项目审核 8修改项目驳回 9修改待业务审核 10修改业务驳回 11修改待领导审核 12修改领导驳回")
    private Integer auditStatus;

    @ApiModelProperty(value = "审核状态名称，前端直接显示")
    private String auditStatusName;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最近一次提交审核的时间")
    private Date auditSubmitTime;
}
