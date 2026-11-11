package org.jeecg.modules.pcms.rent.dto;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 农户信息列表查询条件。
 * 对应需求稿第 4 条：姓名、发电户号、安装地址、所属项目（下拉框）、并网日期（区间），
 * 其中前三项为模糊查询。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "农户列表查询条件", description = "农户列表查询条件")
public class FarmerStationQueryDTO {

    @ApiModelProperty(value = "农户姓名，模糊查询")
    private String farmerName;

    @ApiModelProperty(value = "发电户号，模糊查询。一格多号的农户靠中间匹配才能命中，"
            + "重复户号的 1230000-1/-2 也靠模糊才能一次查全")
    private String powerUserNo;

    @ApiModelProperty(value = "安装地址，模糊查询")
    private String installAddr;

    @ApiModelProperty(value = "项目编号，下拉框选具体项目")
    private Long projectId;

    @ApiModelProperty(value = "大区编码 BJ/TJLF/XZ/HAIN/HENAN，只选大区不选具体项目时用")
    private String regionCode;

    @ApiModelProperty(value = "项目编号/编码，模糊查询")
    private String stationCode;

    @ApiModelProperty(value = "审核状态，可传单个值或逗号分隔的多个值，如 2 或 2,4,6,8。"
            + "0正常 "
            + "1新增待项目审核 2新增项目驳回 3新增待业务审核 4新增业务驳回 5新增待领导审核 6新增领导驳回 "
            + "7修改待项目审核 8修改项目驳回 9修改待业务审核 10修改业务驳回 11修改待领导审核 12修改领导驳回。"
            + "农户列表的状态栏按它筛选")
    private String auditStatus;

    @ApiModelProperty(value = "审核状态聚合口径 PENDING=所有待审核 REJECTED=所有驳回。"
            + "传了 auditStatus 时本字段不生效；只做筛选，返回的状态名仍是明细")
    private String auditStatusGroup;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期起，含当天，格式 yyyy-MM-dd")
    private Date gridDateBegin;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期止，含当天，格式 yyyy-MM-dd")
    private Date gridDateEnd;

    @ApiModelProperty(value = "排序字段，仅支持 id/gridDate/rentDeadline/totalCapacity/totalBlocks/createTime")
    private String sortField;

    @ApiModelProperty(value = "排序方向 asc/desc，默认 desc")
    private String sortOrder;
}
