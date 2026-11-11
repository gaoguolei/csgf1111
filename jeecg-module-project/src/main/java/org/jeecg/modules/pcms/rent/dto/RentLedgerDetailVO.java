package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 租金详情。列表行的全部字段 + 月明细 + 附件。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金详情", description = "租金详情")
public class RentLedgerDetailVO {

    @ApiModelProperty(value = "台账主信息，字段同列表行")
    private RentLedgerListVO base;

    @ApiModelProperty(value = "本期月明细，实际发电天数在这里录")
    private List<MonthVO> months = new ArrayList<>();

    @ApiModelProperty(value = "该农户的安装信息（房型、型号、块数、各档位单价）。"
            + "结构与农户详情里的 installs 完全一致，前端可直接复用同一套渲染。"
            + "本期应付金额就是按这里的单价算出来的，核对金额时可以对照着看")
    private List<FarmerStationDetailVO.InstallVO> installs = new ArrayList<>();
    @ApiModelProperty(value = "系统按月明细算出来的本期应付金额。"
            + "base.payableAdjustFlag=1 时它与 base.periodPayable 不一致，"
            + "页面可提示「系统计算 X，已手工调整为 Y」；把这个值原样提交回来即可恢复系统计算")
    private BigDecimal sysPeriodPayable;

    @ApiModelProperty(value = "本期计租时间段，如 2026-01-01 ~ 2026-03-31。"
            + "需求34：补算租金等重算操作的弹窗上直接展示这一段")
    private String periodRange;

    @ApiModelProperty(value = "业务附件和银行回单。审核附件不在这里，在 auditRounds 里按轮次分组")
    private List<RentAttachVO> attaches = new ArrayList<>();

    @ApiModelProperty(value = "按审核轮次分组的审核过程，轮次倒序（最近一次在最前）。"
            + "每一轮带自己的驳回原因和自己那几个审核附件，"
            + "页面上的「驳回理由」区块直接按这个列表渲染，不要再用 base.rejectRemark 配全部附件")
    private List<AuditRoundVO> auditRounds = new ArrayList<>();

    @ApiModelProperty(value = "历次纠纷暂停支付，次数倒序（最近一次在最前）。"
            + "每次带自己的标记时间/原因和取消时间/原因，cancelTime 为空表示这次还在暂停中。"
            + "页面上的纠纷区块按这个列表渲染，不要用 base.disputeRemark 配全部时间")
    private List<DisputeRoundVO> disputeRounds = new ArrayList<>();

    @ApiModelProperty(value = "1=当前状态允许编辑月明细、实付金额和附件")
    private Integer editable;

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "一次纠纷暂停", description = "一次纠纷暂停")
    public static class DisputeRoundVO {

        @ApiModelProperty(value = "第几次纠纷，从1开始")
        private Integer round;

        @ApiModelProperty(value = "标题，如 第2次纠纷暂停（已取消）")
        private String title;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "标记纠纷的时间")
        private Date disputeTime;

        @ApiModelProperty(value = "标记纠纷的原因")
        private String disputeRemark;

        @ApiModelProperty(value = "标记人")
        private String disputeBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "取消纠纷的时间，为空表示还在暂停中")
        private Date cancelTime;

        @ApiModelProperty(value = "取消纠纷的原因")
        private String cancelRemark;

        @ApiModelProperty(value = "取消人")
        private String cancelBy;

        @ApiModelProperty(value = "1=这次纠纷还在暂停中，尚未取消")
        private Integer ongoing;
    }

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "一轮审核", description = "一轮审核")
    public static class AuditRoundVO {

        @ApiModelProperty(value = "第几轮审核，从1开始。0=升级前的历史数据，轮次不详")
        private Integer round;

        @ApiModelProperty(value = "轮次标题，如 第2次业务审核驳回 / 第1次提交项目审核")
        private String title;

        @ApiModelProperty(value = "本轮的最终动作 SUBMIT提交 APPROVE通过 REJECT驳回；"
                + "为空表示这一轮还在审核中，没出结果")
        private String action;

        @ApiModelProperty(value = "最终动作所在的审核阶段 PROJECT项目审核 BUSINESS业务审核 "
                + "FINANCE财务审核 LEADER领导审核")
        private String stage;

        @ApiModelProperty(value = "动作名称")
        private String actionName;

        @ApiModelProperty(value = "驳回原因。需求第15条规定非必填，可能为空")
        private String remark;

        @ApiModelProperty(value = "提交人")
        private String submitBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "提交时间")
        private Date submitTime;

        @ApiModelProperty(value = "审核人（最近一次审核的人，兼容旧字段）")
        private String auditBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "审核时间（最近一次审核时间，兼容旧字段）")
        private Date auditTime;

        @ApiModelProperty(value = "项目审核人（这一轮项目审核通过/驳回的人）")
        private String projectAuditBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "项目审核时间")
        private Date projectAuditTime;

        @ApiModelProperty(value = "业务审核人（这一轮业务审核通过/驳回的人）")
        private String businessAuditBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "业务审核时间")
        private Date businessAuditTime;

        @ApiModelProperty(value = "财务审核人（这一轮财务审核通过/驳回的人）")
        private String financeAuditBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "财务审核时间")
        private Date financeAuditTime;

        @ApiModelProperty(value = "领导审核人（这一轮领导审核通过/驳回的人）")
        private String leaderAuditBy;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @ApiModelProperty(value = "领导审核时间")
        private Date leaderAuditTime;

        @ApiModelProperty(value = "这一轮上传的审核附件")
        private List<RentAttachVO> attaches = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "租金月明细", description = "租金月明细")
    public static class MonthVO {

        @ApiModelProperty(value = "月明细ID，回填时按这个ID匹配")
        private Long id;

        @ApiModelProperty(value = "年月 yyyy-MM")
        private String ym;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
        @ApiModelProperty(value = "该月计租起日")
        private Date segBegin;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
        @ApiModelProperty(value = "该月计租止日")
        private Date segEnd;

        @ApiModelProperty(value = "当月自然天数")
        private Integer monthDays;

        @ApiModelProperty(value = "该月实际计租天数。前端详情页「实际发电天数」这一列一直用 rentDays 取值，"
                + "所以这里返回的是【实际生效天数】：录入过发电天数就是录入值，没录入就回落到理论计租天数")
        private Integer rentDays;

        @ApiModelProperty(value = "该月理论计租天数（系统按本期起止日期算的那个值）。"
                + "rentDays 被上面那个含义占用后，需要看理论值就用这个字段")
        private Integer theoryRentDays;

        @ApiModelProperty(value = "该月实际发电天数，为空表示未录入")
        private Integer genDays;

        @ApiModelProperty(value = "满月租金 = 年租金 ÷ 12")
        private BigDecimal monthRent;

        @ApiModelProperty(value = "该月理论金额，按计租天数算")
        private BigDecimal theoryAmount;

        @ApiModelProperty(value = "该月金额，录入发电天数后按天折算")
        private BigDecimal amount;
    }
}