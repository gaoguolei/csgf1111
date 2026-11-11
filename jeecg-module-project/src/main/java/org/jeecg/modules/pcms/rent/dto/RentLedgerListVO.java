package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 租金台账列表行，字段顺序与需求稿第 6 条一致。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金台账列表行", description = "租金台账列表行")
public class RentLedgerListVO {

    @ApiModelProperty(value = "台账ID")
    private Long id;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "农户姓名")
    private String farmerName;

    @ApiModelProperty(value = "发电户号")
    private String powerUserNo;

    @ApiModelProperty(value = "安装地址")
    private String installAddr;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    @ApiModelProperty(value = "银行卡号，生成时快照，对账单按它展示")
    private String bankCardNo;

    @ApiModelProperty(value = "银行卡姓名，生成时快照")
    private String bankCardName;

    @ApiModelProperty(value = "所属项目")
    private String projectName;

    @ApiModelProperty(value = "安装总块数")
    private Integer totalBlocks;

    @ApiModelProperty(value = "安装总容量kW")
    private BigDecimal totalCapacity;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期")
    private Date gridDate;

    @ApiModelProperty(value = "并网当月剩余天数")
    private Integer gridMonthRemainDays;

    @ApiModelProperty(value = "并网月数")
    private Integer gridMonths;

    @ApiModelProperty(value = "租金年度")
    private Integer rentYear;

    @ApiModelProperty(value = "期数，从1开始。一年几期由所属项目决定：默认4期按季度，海南1期整年")
    private Integer periodNo;

    @ApiModelProperty(value = "租金所属期数，如 2025年1期")
    private String periodName;

    @ApiModelProperty(value = "本期累计实际发电天数")
    private Integer actualGenDays;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "本期租金起算时间")
    private Date periodBegin;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "本期租金截止时间")
    private Date periodEnd;

    @ApiModelProperty(value = "周期应付累计总金额，全租期理论值")
    private BigDecimal cycleTotalAmount;

    @ApiModelProperty(value = "本期应付金额")
    private BigDecimal periodPayable;

    @ApiModelProperty(value = "1=本期应付金额被人工调整过（不等于月明细汇总值），前端可加个标记提示")
    private Integer payableAdjustFlag;

    @ApiModelProperty(value = "累计应付金额2 = 截止上期累计实付 + 本期应付（需求第6条新口径）")
    private BigDecimal accumPayable;

    @ApiModelProperty(value = "截止上期累计实付金额")
    private BigDecimal prevAccumPaid;

    @ApiModelProperty(value = "本期实付金额")
    private BigDecimal periodPaid;

    @ApiModelProperty(value = "截止本期累计实付金额")
    private BigDecimal accumPaid;

    @ApiModelProperty(value = "本次支付金额 = min(本期应付, 本期实付)。需求第6条："
            + "应付=实付取实付，应付>实付取实付，应付<实付取应付。"
            + "需求第18条的对账就是拿银行回单金额跟这个值核对")
    private BigDecimal currentPayAmount;

    @ApiModelProperty(value = "超/欠付金额，正数超付负数欠付。需求第6条口径下等值于 本期实付-本期应付")
    private BigDecimal diffAmount;

    @ApiModelProperty(value = "统一状态编码，需求第15条的主状态线（四级审核：项目→业务→财务→领导）。"
            + "DRAFT待提交 PENDING_PROJECT_AUDIT待项目审核 PROJECT_REJECTED项目审核驳回 "
            + "PENDING_BUSINESS_AUDIT待业务审核 BUSINESS_REJECTED业务审核驳回 "
            + "PENDING_FINANCE_AUDIT待财务审核 FINANCE_REJECTED财务审核驳回 "
            + "PENDING_LEADER_AUDIT待领导审核 LEADER_REJECTED领导审核驳回 PENDING_PAY待支付 "
            + "PAID已支付 RECONCILED已对账 RECON_ERROR对账异常 DISPUTE纠纷暂停 VOID已作废。"
            + "前端列表和详情直接显示这个，不用自己拼下面那三个状态")
    private String statusCode;

    @ApiModelProperty(value = "统一状态名称")
    private String statusName;

    @ApiModelProperty(value = "审核状态 0待提交 1待项目审核 2项目审核驳回 3待业务审核 4业务审核驳回 "
            + "5待财务审核 6财务审核驳回 7待领导审核 8领导审核驳回 9领导审核通过(待支付)")
    private Integer auditStatus;

    @ApiModelProperty(value = "审核状态名称")
    private String auditStatusName;

    @ApiModelProperty(value = "审核轮次，每提交一次 +1")
    private Integer auditRound;

    @ApiModelProperty(value = "提交人")
    private String submitBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "提交审核时间")
    private Date submitTime;

    @ApiModelProperty(value = "最近一次审核人")
    private String auditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最近一次审核时间")
    private Date auditTime;

    @ApiModelProperty(value = "项目审核人")
    private String projectAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "项目审核时间")
    private Date projectAuditTime;

    @ApiModelProperty(value = "业务审核人")
    private String businessAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "业务审核时间")
    private Date businessAuditTime;

    @ApiModelProperty(value = "财务审核人")
    private String financeAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "财务审核时间")
    private Date financeAuditTime;

    @ApiModelProperty(value = "领导审核人")
    private String leaderAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "领导审核时间")
    private Date leaderAuditTime;

    @ApiModelProperty(value = "支付状态 0未支付 1已支付")
    private Integer payStatus;

    @ApiModelProperty(value = "对账状态 0未对账 1已对账 2对账异常")
    private Integer reconStatus;

    @ApiModelProperty(value = "对账状态名称")
    private String reconStatusName;

    @ApiModelProperty(value = "对账异常说明")
    private String reconRemark;

    @ApiModelProperty(value = "1=纠纷暂停支付")
    private Integer disputeFlag;

    @ApiModelProperty(value = "最近一次标记纠纷的原因")
    private String disputeRemark;

    @ApiModelProperty(value = "已经发生过几次纠纷，0=从没标记过。历次明细在详情的 disputeRounds 里")
    private Integer disputeRound;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "标记纠纷暂停支付的时间")
    private Date disputeTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "取消纠纷暂停支付的时间")
    private Date disputeCancelTime;

    @ApiModelProperty(value = "驳回备注")
    private String rejectRemark;

    @ApiModelProperty(value = "1=已作废")
    private Integer voidFlag;

    @ApiModelProperty(value = "同一电站同一期的台账条数，大于1时前端才显示作废按钮")
    private Integer siblingCount;

    @ApiModelProperty(value = "1=可编辑（待提交或已驳回，且未作废、非历史年度）")
    private Integer editable;

    @ApiModelProperty(value = "1=历史年度，只读")
    private Integer historyFlag;

    // ---------- 农户审核状态（需求31） ----------
    // 农户有待审核变更时，租金详情左上角显示农户状态，操作按钮除导出外全部隐藏

    @ApiModelProperty(value = "农户审核状态 0正常 1新增待审核 2修改待审核 3新增驳回 4修改驳回")
    private Integer farmerAuditStatus;

    @ApiModelProperty(value = "农户审核状态名称，如 修改农户信息待审核")
    private String farmerAuditStatusName;

    @ApiModelProperty(value = "1=该期租金被农户审核锁住，除导出外不能做任何操作")
    private Integer farmerLocked;
}