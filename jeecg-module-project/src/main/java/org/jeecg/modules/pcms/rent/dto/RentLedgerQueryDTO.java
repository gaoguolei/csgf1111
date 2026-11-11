package org.jeecg.modules.pcms.rent.dto;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 租金台账查询条件。
 *
 * 需求稿第 9 条：列表默认只显示本年度，历史租金是单独一个页签。
 * 两个页签走同一个接口，用 history 区分：
 *   history=0 只查本年度，history=1 只查往年
 * 传了 rentYear 就以 rentYear 为准，history 不再起作用。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金台账查询条件", description = "租金台账查询条件")
public class RentLedgerQueryDTO {

    @ApiModelProperty(value = "农户姓名，模糊")
    private String farmerName;

    @ApiModelProperty(value = "发电户号，模糊")
    private String powerUserNo;

    @ApiModelProperty(value = "安装地址，模糊")
    private String installAddr;

    @ApiModelProperty(value = "身份证号，模糊")
    private String idCard;

    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @ApiModelProperty(value = "大区编码，只在没传 projectId 时生效")
    private String regionCode;

    @ApiModelProperty(value = "电站ID，农户详情页的租金页签用")
    private Long stationId;

    @ApiModelProperty(value = "租金年度，不传按 history 取本年度或往年")
    private Integer rentYear;

    @ApiModelProperty(value = "期数 1-4")
    private Integer periodNo;

    @ApiModelProperty(value = "0=本年度台账 1=历史租金页签，默认0")
    private Integer history;
    @ApiModelProperty(value = "统一状态编码，列表页的状态页签用这个筛。"
            + "DRAFT待提交 PENDING_AUDIT待审核(四级合并) REJECTED驳回(四级合并) "
            + "PENDING_PAY待支付 PAID已支付 RECONCILED已对账 RECON_ERROR对账异常 "
            + "DISPUTE纠纷暂停 VOID已作废。"
            + "明细状态 PENDING_PROJECT_AUDIT/PROJECT_REJECTED/PENDING_BUSINESS_AUDIT/BUSINESS_REJECTED/"
            + "PENDING_FINANCE_AUDIT/FINANCE_REJECTED/PENDING_LEADER_AUDIT/LEADER_REJECTED 仍可传")
    private String status;
    @ApiModelProperty(value = "审核状态，可传单个值或逗号分隔的多个值，如 2 或 2,4,6,8。"
            + "0待提交 1待项目审核 2项目审核驳回 3待业务审核 4业务审核驳回 "
            + "5待财务审核 6财务审核驳回 7待领导审核 8领导审核驳回 9领导审核通过(待支付)")
    private String auditStatus;

    @ApiModelProperty(value = "支付状态 0未支付 1已支付")
    private Integer payStatus;

    @ApiModelProperty(value = "对账状态 0未对账 1已对账 2对账异常")
    private Integer reconStatus;

    @ApiModelProperty(value = "1=只看纠纷暂停支付的")
    private Integer disputeFlag;

    @ApiModelProperty(value = "1=含已作废，默认不含")
    private Integer includeVoid;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期起")
    private Date gridDateBegin;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期止")
    private Date gridDateEnd;

    @ApiModelProperty(value = "排序字段")
    private String sortField;

    @ApiModelProperty(value = "asc / desc")
    private String sortOrder;
}
