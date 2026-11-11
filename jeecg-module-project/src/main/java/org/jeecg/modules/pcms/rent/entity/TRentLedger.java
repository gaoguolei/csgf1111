package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 租金台账，一个电站 × 一个年度 × 一期 = 一行
 *
 * 与 t_rent 的区别：t_rent 存的是"单价档位"这种相对静态的合同约定，
 * 这张表存的是"某年某期实际该付多少、实付了多少"的流水。
 *
 * 同一个电站同一期可能存在多行：安装信息改过之后重新生成会追加新行，
 * 旧行由人工作废（void_flag=1），所以唯一键不能建在 station+year+period 上。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_ledger")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_ledger对象", description = "租金台账")
public class TRentLedger implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 审核状态：待提交，可改可删 */
    public static final int AUDIT_DRAFT = 0;
    /** 审核状态：已提交，等待项目经理/管理员做项目审核（第一级） */
    public static final int AUDIT_PENDING_PROJECT = 1;
    /** 审核状态：项目审核驳回，可改可再次提交 */
    public static final int AUDIT_PROJECT_REJECTED = 2;
    /** 审核状态：项目审核通过，等待业务专员审核（第二级） */
    public static final int AUDIT_PENDING_BUSINESS = 3;
    /** 审核状态：业务审核驳回，可改可再次提交 */
    public static final int AUDIT_BUSINESS_REJECTED = 4;
    /** 审核状态：业务审核通过，等待财务专员审核（第三级） */
    public static final int AUDIT_PENDING_FINANCE = 5;
    /** 审核状态：财务审核驳回，可改可再次提交 */
    public static final int AUDIT_FINANCE_REJECTED = 6;
    /** 审核状态：财务审核通过，等待公司领导审核（第四级） */
    public static final int AUDIT_PENDING_LEADER = 7;
    /** 审核状态：领导审核驳回，可改可再次提交 */
    public static final int AUDIT_LEADER_REJECTED = 8;
    /** 审核状态：四级审核全部通过，即待支付 */
    public static final int AUDIT_APPROVED = 9;

    /** 所有"待审核"的审核状态值，聚合页签按它筛选 */
    public static final Integer[] AUDIT_PENDING_VALUES = {
            AUDIT_PENDING_PROJECT, AUDIT_PENDING_BUSINESS, AUDIT_PENDING_FINANCE, AUDIT_PENDING_LEADER};
    /** 所有"驳回"的审核状态值，聚合页签按它筛选 */
    public static final Integer[] AUDIT_REJECTED_VALUES = {
            AUDIT_PROJECT_REJECTED, AUDIT_BUSINESS_REJECTED, AUDIT_FINANCE_REJECTED, AUDIT_LEADER_REJECTED};

    /** 支付状态：未支付 */
    public static final int PAY_UNPAID = 0;
    /** 支付状态：已支付 */
    public static final int PAY_PAID = 1;

    /** 对账状态：未对账 */
    public static final int RECON_NONE = 0;
    /** 对账状态：已对账，回单与库里三项全对上 */
    public static final int RECON_OK = 1;
    /** 对账状态：对账异常，差异写在 recon_remark 里 */
    public static final int RECON_ERROR = 2;

    // ---------- 需求稿第 15 条定义的对外统一状态 ----------
    // 审核拆成项目、业务、财务、领导四级，对外状态在原来的主线里插入四级：
    // 待提交 → 待项目审核 → (项目驳回) → 待业务审核 → (业务驳回)
    //        → 待财务审核 → (财务驳回) → 待领导审核 → (领导驳回) → 待支付 → 已支付 → 已对账。
    // 库里存的仍是审核、支付、对账三个正交字段，这里合成一个展示状态，
    // 前端只认这一个，不用自己拼条件。
    public static final String STATUS_DRAFT = "DRAFT";
    /** 待项目审核（提交后的第一级） */
    public static final String STATUS_PENDING_PROJECT_AUDIT = "PENDING_PROJECT_AUDIT";
    /** 项目审核驳回 */
    public static final String STATUS_PROJECT_REJECTED = "PROJECT_REJECTED";
    /** 项目审核通过，等待业务专员审核 */
    public static final String STATUS_PENDING_BUSINESS_AUDIT = "PENDING_BUSINESS_AUDIT";
    /** 业务审核驳回 */
    public static final String STATUS_BUSINESS_REJECTED = "BUSINESS_REJECTED";
    /** 业务审核通过，等待财务专员审核 */
    public static final String STATUS_PENDING_FINANCE_AUDIT = "PENDING_FINANCE_AUDIT";
    /** 财务审核驳回 */
    public static final String STATUS_FINANCE_REJECTED = "FINANCE_REJECTED";
    /** 财务审核通过，等待公司领导审核 */
    public static final String STATUS_PENDING_LEADER_AUDIT = "PENDING_LEADER_AUDIT";
    /** 领导审核驳回 */
    public static final String STATUS_LEADER_REJECTED = "LEADER_REJECTED";
    /** 聚合状态：任意一级的待审核（项目/业务/财务/领导） */
    public static final String STATUS_PENDING_AUDIT = "PENDING_AUDIT";
    /** 聚合状态：任意一级的审核驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PENDING_PAY = "PENDING_PAY";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_RECONCILED = "RECONCILED";
    public static final String STATUS_RECON_ERROR = "RECON_ERROR";
    public static final String STATUS_DISPUTE = "DISPUTE";
    public static final String STATUS_VOID = "VOID";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "农户电站信息表ID")
    private Long stationId;

    @ApiModelProperty(value = "农户姓名，冗余")
    @Excel(name = "农户名称", width = 15)
    private String farmerName;

    @ApiModelProperty(value = "发电户号，冗余")
    @Excel(name = "发电户号", width = 15)
    private String powerUserNo;

    @ApiModelProperty(value = "安装地址，冗余")
    @Excel(name = "安装地址", width = 25)
    private String installAddr;

    @ApiModelProperty(value = "身份证号，冗余")
    @Excel(name = "身份证号", width = 20)
    private String idCard;

    @ApiModelProperty(value = "银行卡号，冗余，对账用")
    @Excel(name = "银行卡号", width = 25)
    private String bankCardNo;

    @ApiModelProperty(value = "银行卡姓名，冗余，对账用")
    @Excel(name = "银行卡姓名", width = 15)
    private String bankCardName;

    @ApiModelProperty(value = "所属项目ID，冗余")
    private Long projectId;

    @ApiModelProperty(value = "租金年度")
    @Excel(name = "年份", width = 8)
    private Integer rentYear;

    @ApiModelProperty(value = "期数，从1开始。一年几期由所属项目决定：默认4期按季度，海南1期整年")
    @Excel(name = "期数", width = 6)
    private Integer periodNo;

    @ApiModelProperty(value = "期数名称，如 2025年1期")
    @Excel(name = "期数名称", width = 15)
    private String periodName;

    @ApiModelProperty(value = "安装总块数，生成时的快照")
    @Excel(name = "安装总块数", width = 12)
    private Integer totalBlocks;

    @ApiModelProperty(value = "安装总容量kW，生成时的快照")
    @Excel(name = "安装总容量(kW)", width = 15)
    private BigDecimal totalCapacity;

    @ApiModelProperty(value = "生成时的安装信息快照（JSON）：房型、型号、块数、容量、档位单价。"
            + "详情和 PDF 按它展示，农户后续的修改不影响已生成的期")
    private String installSnapshot;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期，冗余")
    @Excel(name = "并网日期", width = 14, format = "yyyy-MM-dd")
    private Date gridDate;

    @ApiModelProperty(value = "并网当月剩余天数，含并网当天")
    @Excel(name = "并网当月剩余天数", width = 16)
    private Integer gridMonthRemainDays;

    @ApiModelProperty(value = "并网月数，并网当月到本期末的整月数")
    @Excel(name = "并网月数", width = 10)
    private Integer gridMonths;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "本期租金起算时间，已与并网日、租金截止日取交集")
    @Excel(name = "本期租金起算时间", width = 16, format = "yyyy-MM-dd")
    private Date periodBegin;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "本期租金截止时间，已与并网日、租金截止日取交集")
    @Excel(name = "本期租金截止时间", width = 16, format = "yyyy-MM-dd")
    private Date periodEnd;

    @ApiModelProperty(value = "本期理论计租天数，按 periodBegin~periodEnd 算")
    @Excel(name = "计租天数", width = 10)
    private Integer periodDays;

    @ApiModelProperty(value = "本期累计实际发电天数，由月明细汇总")
    @Excel(name = "实际发电天数", width = 12)
    private Integer actualGenDays;

    @ApiModelProperty(value = "周期应付累计总金额，全租期理论值，系统算，不可改")
    @Excel(name = "累计应付金额1(全周期)", width = 20)
    private BigDecimal cycleTotalAmount;

    @ApiModelProperty(value = "本期应付金额。默认由月明细汇总，也支持手工调整")
    @Excel(name = "本期应付", width = 12)
    private BigDecimal periodPayable;

    @ApiModelProperty(value = "1=本期应付金额被人工调整过，锁住不再被月明细重算覆盖。"
            + "把值改回系统计算值会自动解除")
    private Integer payableAdjustFlag;

    @ApiModelProperty(value = "累计应付金额2 = 截止上期累计实付 + 本期应付，不可改。"
            + "需求第6条：基数取上期的累计实付，这样超/欠付只看本期")
    @Excel(name = "累计应付金额2", width = 15)
    private BigDecimal accumPayable;

    @ApiModelProperty(value = "截止上期累计实付金额，不含本期，不可改")
    @Excel(name = "上期累计实付", width = 15)
    private BigDecimal prevAccumPaid;

    @ApiModelProperty(value = "本期实付金额，可手工调整，未调整时等于本期应付")
    @Excel(name = "本期实付", width = 12)
    private BigDecimal periodPaid;

    @ApiModelProperty(value = "截止本期累计实付金额 = 上期累计实付 + 本期实付，不可改")
    @Excel(name = "累计实付金额", width = 15)
    private BigDecimal accumPaid;

    @ApiModelProperty(value = "超/欠付金额 = 累计实付金额 - 累计应付金额2，正数超付负数欠付。"
            + "按需求第6条的口径，等值于 本期实付 - 本期应付")
    @Excel(name = "超/欠付", width = 12)
    private BigDecimal diffAmount;

    // ---------- 以下两个是导出用的派生字段，不落库 ----------
    // 直接叫 currentPayAmount / statusName 会和下面的同名方法打架，
    // 所以加 export 前缀，导入导出时明确它们只在导出场景有值

    @ApiModelProperty(value = "本次支付金额 = min(本期应付, 本期实付)。需求第6条，导出用，非数据库字段")
    @TableField(exist = false)
    @Excel(name = "本次支付金额", width = 14)
    private BigDecimal exportCurrentPayAmount;

    @ApiModelProperty(value = "统一状态名称，如 待提交/待支付/已对账。导出用，非数据库字段")
    @TableField(exist = false)
    @Excel(name = "状态", width = 12)
    private String exportStatusName;

    @ApiModelProperty(value = "审核状态 0待提交 1待项目审核 2项目审核驳回 3待业务审核 4业务审核驳回 "
            + "5待财务审核 6财务审核驳回 7待领导审核 8领导审核驳回 9领导审核通过(待支付)")
    private Integer auditStatus;

    @ApiModelProperty(value = "支付状态 0未支付 1已支付")
    private Integer payStatus;

    @ApiModelProperty(value = "对账状态 0未对账 1已对账 2对账异常")
    private Integer reconStatus;

    @ApiModelProperty(value = "银行回单上OCR识别出的金额")
    private BigDecimal receiptAmount;

    @ApiModelProperty(value = "对账异常说明，逐项写明哪里对不上")
    private String reconRemark;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "对账时间")
    private Date reconTime;

    @ApiModelProperty(value = "1=已标记纠纷暂停支付，此状态下不可提交审核")
    private Integer disputeFlag;

    @ApiModelProperty(value = "最近一次标记纠纷的原因。历次纠纷的来龙去脉看 t_rent_dispute_record")
    @Excel(name = "纠纷暂停原因", width = 30)
    private String disputeRemark;

    @ApiModelProperty(value = "第几次纠纷，从1开始，每标记一次纠纷 +1。"
            + "历次纠纷的时间和原因按这个值分组存在 t_rent_dispute_record")
    private Integer disputeRound;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "标记纠纷暂停支付的时间，反复标记时记最近一次")
    private Date disputeTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "取消纠纷暂停支付的时间，反复标记时记最近一次")
    private Date disputeCancelTime;

    @ApiModelProperty(value = "最近一次的驳回原因。历次驳回原因看 t_rent_audit_record")
    private String rejectRemark;

    @ApiModelProperty(value = "审核轮次，从1开始，每提交一次审核 +1。"
            + "审核附件按这个值分组，才能分清哪几个附件属于哪一次驳回")
    private Integer auditRound;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "提交审核时间")
    private Date submitTime;

    @ApiModelProperty(value = "提交人")
    private String submitBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "审核时间")
    private Date auditTime;

    @ApiModelProperty(value = "审核人（最近一次审核的人，兼容旧字段）")
    private String auditBy;

    @ApiModelProperty(value = "项目审核人。四级审核中的第一级")
    private String projectAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "项目审核时间")
    private Date projectAuditTime;

    @ApiModelProperty(value = "业务审核人。四级审核中的第二级")
    private String businessAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "业务审核时间")
    private Date businessAuditTime;

    @ApiModelProperty(value = "财务审核人。四级审核中的第三级")
    private String financeAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "财务审核时间")
    private Date financeAuditTime;

    @ApiModelProperty(value = "领导审核人。四级审核中的第四级")
    private String leaderAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "领导审核时间")
    private Date leaderAuditTime;

    @ApiModelProperty(value = "1=已作废。重新生成后旧行由人工作废，只剩一行时不允许作废")
    private Integer voidFlag;

    @ApiModelProperty(value = "作废原因")
    private String voidRemark;

    @ApiModelProperty(value = "1=系统重新生成的行，0=首次生成")
    private Integer regenFlag;

    private String remark;

    @ApiModelProperty(value = "0正常 1删除")
    private Integer delFlag;

    @ApiModelProperty(value = "租户ID，用于数据隔离。需求第14条")
    private Integer tenantId;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /**
     * 待提交和四种驳回状态下才允许改数据、改标记。
     * 任何一级驳回都回到可编辑，改完重新提交从项目审核重新走。
     */
    public boolean editable() {
        return !voided()
                && (auditStatus == null
                || auditStatus == AUDIT_DRAFT
                || auditStatus == AUDIT_PROJECT_REJECTED
                || auditStatus == AUDIT_BUSINESS_REJECTED
                || auditStatus == AUDIT_FINANCE_REJECTED
                || auditStatus == AUDIT_LEADER_REJECTED);
    }

    public boolean voided() {
        return voidFlag != null && voidFlag == 1;
    }

    public boolean disputed() {
        return disputeFlag != null && disputeFlag == 1;
    }

    public boolean paid() {
        return payStatus != null && payStatus == PAY_PAID;
    }

    /**
     * 本次支付金额（需求第6条）。按平衡规则取值：
     *   本期应付 = 本期实付 → 取本期实付
     *   本期应付 &gt; 本期实付 → 取本期实付
     *   本期应付 &lt; 本期实付 → 取本期应付
     * 三条合起来就是 min(本期应付, 本期实付)。
     *
     * 需求第18条的对账就是拿银行回单上的金额跟这个值核对。
     * 本期实付为空时与累计链保持一致，按本期应付看待。
     */
    public BigDecimal currentPayAmount() {
        BigDecimal payable = periodPayable == null ? BigDecimal.ZERO : periodPayable;
        BigDecimal paid = periodPaid == null ? payable : periodPaid;
        return payable.min(paid);
    }

    /** 当前审核轮次，没提交过是 0 */
    public int auditRound() {
        return auditRound == null ? 0 : auditRound;
    }

    /** 已经发生过几次纠纷，没标记过是 0 */
    public int disputeRound() {
        return disputeRound == null ? 0 : disputeRound;
    }

    /** 本期应付金额是否被人工调整过 */
    public boolean payableAdjusted() {
        return payableAdjustFlag != null && payableAdjustFlag == 1;
    }

    /**
     * 四级审核都在流程中，或者四级已全部通过，重新生成时要跳过这些期。
     * 任何一级驳回都会解锁，可以重新生成。
     */
    public boolean locked() {
        return auditStatus != null
                && (auditStatus == AUDIT_PENDING_PROJECT
                || auditStatus == AUDIT_PENDING_BUSINESS
                || auditStatus == AUDIT_PENDING_FINANCE
                || auditStatus == AUDIT_PENDING_LEADER
                || auditStatus == AUDIT_APPROVED);
    }

    /** 当前审核级别，不在审核中返回 null。提交/审核接口据此判断该由谁审 */
    public static Integer pendingLevel(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case AUDIT_PENDING_PROJECT:
            case AUDIT_PENDING_BUSINESS:
            case AUDIT_PENDING_FINANCE:
            case AUDIT_PENDING_LEADER:
                return status;
            default:
                return null;
        }
    }

    /** 审核状态名称，列表展示和报错文案共用一套说法 */
    public static String auditName(Integer status) {
        if (status == null) {
            return "待提交";
        }
        switch (status) {
            case AUDIT_PENDING_PROJECT:
                return "待项目审核";
            case AUDIT_PROJECT_REJECTED:
                return "项目审核驳回";
            case AUDIT_PENDING_BUSINESS:
                return "待业务审核";
            case AUDIT_BUSINESS_REJECTED:
                return "业务审核驳回";
            case AUDIT_PENDING_FINANCE:
                return "待财务审核";
            case AUDIT_FINANCE_REJECTED:
                return "财务审核驳回";
            case AUDIT_PENDING_LEADER:
                return "待领导审核";
            case AUDIT_LEADER_REJECTED:
                return "领导审核驳回";
            case AUDIT_APPROVED:
                return "领导审核通过";
            default:
                return "待提交";
        }
    }

    /**
     * 需求稿第 15 条的统一状态编码。
     *
     * 优先级从高到低：作废 > 纠纷暂停 > 对账 > 支付 > 审核。
     * 纠纷排在审核前面，是因为需求稿第 14 条要求"状态一直显示纠纷暂停，直至取消纠纷"；
     * 对账排在支付前面，是因为对账异常比"已支付"更需要人去处理。
     */
    public String statusCode() {
        if (voided()) {
            return STATUS_VOID;
        }
        if (disputed()) {
            return STATUS_DISPUTE;
        }
        if (reconStatus != null && reconStatus == RECON_ERROR) {
            return STATUS_RECON_ERROR;
        }
        if (reconStatus != null && reconStatus == RECON_OK) {
            return STATUS_RECONCILED;
        }
        if (paid()) {
            return STATUS_PAID;
        }
        if (auditStatus != null && auditStatus == AUDIT_APPROVED) {
            return STATUS_PENDING_PAY;
        }
        if (auditStatus != null && auditStatus == AUDIT_LEADER_REJECTED) {
            return STATUS_LEADER_REJECTED;
        }
        if (auditStatus != null && auditStatus == AUDIT_PENDING_LEADER) {
            return STATUS_PENDING_LEADER_AUDIT;
        }
        if (auditStatus != null && auditStatus == AUDIT_FINANCE_REJECTED) {
            return STATUS_FINANCE_REJECTED;
        }
        if (auditStatus != null && auditStatus == AUDIT_PENDING_FINANCE) {
            return STATUS_PENDING_FINANCE_AUDIT;
        }
        if (auditStatus != null && auditStatus == AUDIT_BUSINESS_REJECTED) {
            return STATUS_BUSINESS_REJECTED;
        }
        if (auditStatus != null && auditStatus == AUDIT_PENDING_BUSINESS) {
            return STATUS_PENDING_BUSINESS_AUDIT;
        }
        if (auditStatus != null && auditStatus == AUDIT_PROJECT_REJECTED) {
            return STATUS_PROJECT_REJECTED;
        }
        if (auditStatus != null && auditStatus == AUDIT_PENDING_PROJECT) {
            return STATUS_PENDING_PROJECT_AUDIT;
        }
        return STATUS_DRAFT;
    }

    /** 统一状态的中文名，列表和详情直接显示这个 */
    public String statusName() {
        return statusName(statusCode());
    }

    public static String statusName(String code) {
        if (code == null) {
            return "待提交";
        }
        switch (code) {
            case STATUS_VOID:
                return "已作废";
            case STATUS_DISPUTE:
                return "纠纷暂停";
            case STATUS_RECON_ERROR:
                return "对账异常";
            case STATUS_RECONCILED:
                return "已对账";
            case STATUS_PAID:
                return "已支付";
            case STATUS_PENDING_PAY:
                return "待支付";
            case STATUS_PENDING_LEADER_AUDIT:
                return "待领导审核";
            case STATUS_LEADER_REJECTED:
                return "领导审核驳回";
            case STATUS_PENDING_FINANCE_AUDIT:
                return "待财务审核";
            case STATUS_FINANCE_REJECTED:
                return "财务审核驳回";
            case STATUS_PENDING_BUSINESS_AUDIT:
                return "待业务审核";
            case STATUS_BUSINESS_REJECTED:
                return "业务审核驳回";
            case STATUS_PENDING_PROJECT_AUDIT:
                return "待项目审核";
            case STATUS_PROJECT_REJECTED:
                return "项目审核驳回";
            default:
                return "待提交";
        }
    }

    /** 对账状态名称 */
    public static String reconName(Integer status) {
        if (status == null) {
            return "未对账";
        }
        switch (status) {
            case RECON_OK:
                return "已对账";
            case RECON_ERROR:
                return "对账异常";
            default:
                return "未对账";
        }
    }
}
