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
 * 农户信息变更审核申请。需求 28~33。
 *
 * 一次"提交修改"对应一行：提交时把改前/改后整份快照存进来，
 * 审核弹窗直接展示这两份 JSON 的差异；驳回或取消时按 before_json 回滚。
 *
 * 【为什么存整份快照而不是字段级 diff】
 * 修改可能同时涉及农户主表、安装信息、租金档位三层。存字段级 diff 要处理
 * 新增/删除安装信息的场景，反而更复杂。整份快照直接用 Jackson 反序列化就能回滚，
 * 审核弹窗也能原样展示"改前/改后"。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_farmer_change_request")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_farmer_change_request对象", description = "农户信息变更审核申请")
public class TFarmerChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 新增农户 */
    public static final String TYPE_INSERT = "INSERT";
    /** 修改农户 */
    public static final String TYPE_UPDATE = "UPDATE";

    /** 待审核 */
    public static final String STATUS_PENDING = "PENDING";
    /** 审核通过 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 审核驳回，数据已回滚 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 提交人在审核前主动取消，数据已回滚 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** 审核阶段：项目审核（项目经理/管理员） */
    public static final String STAGE_PROJECT = "PROJECT";
    /** 审核阶段：业务审核（业务专员） */
    public static final String STAGE_BUSINESS = "BUSINESS";
    /** 审核阶段：领导审核（公司领导） */
    public static final String STAGE_LEADER = "LEADER";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "农户电站信息表ID")
    private Long stationId;

    @ApiModelProperty(value = "变更类型 INSERT新增 UPDATE修改")
    private String changeType;

    @ApiModelProperty(value = "状态 PENDING待审核 APPROVED通过 REJECTED驳回 CANCELLED取消")
    private String status;

    @ApiModelProperty(value = "当前审核阶段 PROJECT项目审核 BUSINESS业务审核 LEADER领导审核。"
            + "每通过一级往后走一级，领导审核通过后申请完结")
    private String auditStage;

    @ApiModelProperty(value = "修改前快照（农户主表 + 安装信息 + 租金档位）。新增时为空")
    private String beforeJson;

    @ApiModelProperty(value = "修改后快照")
    private String afterJson;

    @ApiModelProperty(value = "提交人")
    private String submitBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "提交审核时间。租金锁定区间以它为准")
    private Date submitTime;

    @ApiModelProperty(value = "最近一次审核人")
    private String auditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最近一次审核时间")
    private Date auditTime;

    @ApiModelProperty(value = "驳回原因")
    private String auditRemark;

    @ApiModelProperty(value = "项目审核人")
    private String projectAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "项目审核时间")
    private Date projectAuditTime;

    @ApiModelProperty(value = "业务审核人")
    private String businessAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "业务审核时间")
    private Date businessAuditTime;

    @ApiModelProperty(value = "领导审核人")
    private String leaderAuditBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "领导审核时间")
    private Date leaderAuditTime;

    private Date createTime;
    private Date updateTime;

    public boolean pending() {
        return STATUS_PENDING.equals(status);
    }

    /** 状态中文名，列表和审核弹窗共用 */
    public static String statusName(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待审核";
            case STATUS_APPROVED:
                return "审核通过";
            case STATUS_REJECTED:
                return "审核驳回";
            case STATUS_CANCELLED:
                return "已取消";
            default:
                return status;
        }
    }

    /** 变更类型中文名 */
    public static String typeName(String type) {
        if (TYPE_INSERT.equals(type)) {
            return "新增农户信息";
        }
        if (TYPE_UPDATE.equals(type)) {
            return "修改农户信息";
        }
        return type == null ? "" : type;
    }

    /** 审核阶段中文名 */
    public static String stageName(String stage) {
        if (STAGE_PROJECT.equals(stage)) {
            return "项目审核";
        }
        if (STAGE_BUSINESS.equals(stage)) {
            return "业务审核";
        }
        if (STAGE_LEADER.equals(stage)) {
            return "领导审核";
        }
        return "审核";
    }
}
