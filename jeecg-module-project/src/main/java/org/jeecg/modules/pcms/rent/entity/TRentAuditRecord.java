package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
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
 * @Description: 租金审核记录，一次提交/通过/驳回一行
 *
 * 建这张表是因为 t_rent_ledger 上只存得下"最近一次"：
 * reject_remark 每驳回一次就被覆盖，第一次的驳回原因就找不回来了；
 * 审核附件也只挂在 ledger_id 上，两次驳回带的材料混在一起分不开。
 *
 * 有了轮次（audit_round）之后：
 *   第1次提交 -> round=1，审核人这轮传的附件 round 都是 1，驳回记 REJECT(round=1)
 *   改完再提交 -> round=2，这轮的附件和驳回记录都是 2
 * 前端按 round 分组，每次驳回的原因和它自己的附件就对上了。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_audit_record")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_audit_record对象", description = "租金审核记录")
public class TRentAuditRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 提交审核 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    /** 审核通过 */
    public static final String ACTION_APPROVE = "APPROVE";
    /** 审核驳回 */
    public static final String ACTION_REJECT = "REJECT";

    /** 审核阶段：项目审核。提交后先进这一级，由项目经理/管理员审 */
    public static final String STAGE_PROJECT = "PROJECT";
    /** 审核阶段：业务审核。项目审核通过后进这一级 */
    public static final String STAGE_BUSINESS = "BUSINESS";
    /** 审核阶段：财务审核。业务审核通过后进这一级 */
    public static final String STAGE_FINANCE = "FINANCE";
    /** 审核阶段：领导审核。财务审核通过后进这一级 */
    public static final String STAGE_LEADER = "LEADER";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "台账ID")
    private Long ledgerId;

    @ApiModelProperty(value = "审核轮次，从1开始，每次提交审核 +1")
    private Integer auditRound;

    @ApiModelProperty(value = "审核阶段 PROJECT项目审核 BUSINESS业务审核 FINANCE财务审核 LEADER领导审核")
    private String auditStage;

    @ApiModelProperty(value = "动作 SUBMIT提交 APPROVE通过 REJECT驳回")
    private String action;

    @ApiModelProperty(value = "备注。驳回时是驳回原因，需求第15条规定非必填")
    private String remark;

    @ApiModelProperty(value = "操作人")
    private String operBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    private Date operTime;

    /**
     * 审核操作名称。四级审核里同一个动作的说法不同，通过时带上"提交下一级"。
     */
    public static String actionName(String stage, String action) {
        if (action == null) {
            return "";
        }
        boolean project = STAGE_PROJECT.equals(stage);
        boolean business = STAGE_BUSINESS.equals(stage);
        boolean finance = STAGE_FINANCE.equals(stage);
        boolean leader = STAGE_LEADER.equals(stage);
        switch (action) {
            case ACTION_SUBMIT:
                return project ? "提交项目审核" : "提交审核";
            case ACTION_APPROVE:
                if (project) {
                    return "项目审核通过（提交业务审核）";
                }
                if (business) {
                    return "业务审核通过（提交财务审核）";
                }
                if (finance) {
                    return "财务审核通过（提交领导审核）";
                }
                return leader ? "领导审核通过（待支付）" : "审核通过";
            case ACTION_REJECT:
                if (project) {
                    return "项目审核驳回";
                }
                if (business) {
                    return "业务审核驳回";
                }
                if (finance) {
                    return "财务审核驳回";
                }
                return leader ? "领导审核驳回" : "审核驳回";
            default:
                return action;
        }
    }

    /** 阶段名称，详情页分组展示用 */
    public static String stageName(String stage) {
        if (STAGE_PROJECT.equals(stage)) {
            return "项目审核";
        }
        if (STAGE_BUSINESS.equals(stage)) {
            return "业务审核";
        }
        if (STAGE_FINANCE.equals(stage)) {
            return "财务审核";
        }
        if (STAGE_LEADER.equals(stage)) {
            return "领导审核";
        }
        return "审核";
    }

    public boolean reject() {
        return ACTION_REJECT.equals(action);
    }
}
