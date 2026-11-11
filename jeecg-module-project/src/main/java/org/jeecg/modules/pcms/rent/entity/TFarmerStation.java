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
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 农户电站信息表
 * @Version: V1.0
 */
@Data
@TableName("t_farmer_station")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_farmer_station对象", description = "农户电站信息表")
public class TFarmerStation implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 未删除时的固定占位值，与建表脚本 del_time 默认值一致 */
    public static final String NOT_DELETED = "1970-01-01 00:00:00";

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "农户名称，可能是村委会、小学等单位")
    @Excel(name = "农户名称", width = 15)
    private String farmerName;

    @ApiModelProperty(value = "身份证号，仅天津/廊坊有")
    @Excel(name = "身份证号", width = 20)
    private String idCard;

    @ApiModelProperty(value = "银行卡号，北京无")
    @Excel(name = "银行卡号", width = 25)
    private String bankCardNo;

    @ApiModelProperty(value = "银行卡姓名，北京无")
    @Excel(name = "银行卡姓名", width = 15)
    private String bankCardName;

    @ApiModelProperty(value = "电站安装地址")
    @Excel(name = "安装地址", width = 30)
    private String installAddr;

    @ApiModelProperty(value = "发电户号，字符串原样存，允许一格多号和 -1/-2 后缀")
    @Excel(name = "发电户号", width = 15)
    private String powerUserNo;

    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @ApiModelProperty(value = "大区编码，冗余")
    @Excel(name = "大区", width = 10)
    private String regionCode;

    @ApiModelProperty(value = "项目编号：北京编号/天津编码，徐州海南河南无")
    @Excel(name = "项目编号", width = 15)
    private String stationCode;

    @ApiModelProperty(value = "业务员，仅天津/廊坊")
    @Excel(name = "业务员", width = 10)
    private String salesman;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期")
    @Excel(name = "并网日期", width = 15, format = "yyyy-MM-dd")
    private Date gridDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际发电日期，仅河南")
    private Date actualGenDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "租金截止日期 = 并网日期 + 租期年限")
    private Date rentDeadline;

    @ApiModelProperty(value = "安装总容量，统一kW")
    @Excel(name = "安装容量(kW)", width = 15)
    private BigDecimal totalCapacity;

    @ApiModelProperty(value = "安装总块数")
    @Excel(name = "安装块数", width = 12)
    private Integer totalBlocks;

    private String remark;

    @ApiModelProperty(value = "0正常 1删除")
    private Integer delFlag;

    @ApiModelProperty(value = "删除时间，未删除固定为1970-01-01，参与唯一键")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date delTime;

    // ---------- 审核状态（三级审核：项目 → 业务 → 领导） ----------
    /** 正常，租金可以正常操作 */
    public static final int AUDIT_NORMAL = 0;

    /** 新增：待项目审核（项目经理/管理员） */
    public static final int AUDIT_INSERT_PENDING_PROJECT = 1;
    /** 新增：项目审核驳回 */
    public static final int AUDIT_INSERT_PROJECT_REJECTED = 2;
    /** 新增：项目审核通过，待业务审核 */
    public static final int AUDIT_INSERT_PENDING_BUSINESS = 3;
    /** 新增：业务审核驳回 */
    public static final int AUDIT_INSERT_BUSINESS_REJECTED = 4;
    /** 新增：业务审核通过，待领导审核 */
    public static final int AUDIT_INSERT_PENDING_LEADER = 5;
    /** 新增：领导审核驳回 */
    public static final int AUDIT_INSERT_LEADER_REJECTED = 6;

    /** 修改：待项目审核 */
    public static final int AUDIT_UPDATE_PENDING_PROJECT = 7;
    /** 修改：项目审核驳回 */
    public static final int AUDIT_UPDATE_PROJECT_REJECTED = 8;
    /** 修改：项目审核通过，待业务审核 */
    public static final int AUDIT_UPDATE_PENDING_BUSINESS = 9;
    /** 修改：业务审核驳回 */
    public static final int AUDIT_UPDATE_BUSINESS_REJECTED = 10;
    /** 修改：业务审核通过，待领导审核 */
    public static final int AUDIT_UPDATE_PENDING_LEADER = 11;
    /** 修改：领导审核驳回 */
    public static final int AUDIT_UPDATE_LEADER_REJECTED = 12;

    /** 所有"待审核"的状态值（新增/修改 × 三级），聚合筛选用 */
    public static final Integer[] PENDING_AUDIT_VALUES = {
            AUDIT_INSERT_PENDING_PROJECT, AUDIT_INSERT_PENDING_BUSINESS, AUDIT_INSERT_PENDING_LEADER,
            AUDIT_UPDATE_PENDING_PROJECT, AUDIT_UPDATE_PENDING_BUSINESS, AUDIT_UPDATE_PENDING_LEADER};
    /** 所有"驳回"的状态值，聚合筛选用 */
    public static final Integer[] REJECTED_AUDIT_VALUES = {
            AUDIT_INSERT_PROJECT_REJECTED, AUDIT_INSERT_BUSINESS_REJECTED, AUDIT_INSERT_LEADER_REJECTED,
            AUDIT_UPDATE_PROJECT_REJECTED, AUDIT_UPDATE_BUSINESS_REJECTED, AUDIT_UPDATE_LEADER_REJECTED};

    @ApiModelProperty(value = "审核状态 0正常 "
            + "1新增待项目审核 2新增项目驳回 3新增待业务审核 4新增业务驳回 5新增待领导审核 6新增领导驳回 "
            + "7修改待项目审核 8修改项目驳回 9修改待业务审核 10修改业务驳回 11修改待领导审核 12修改领导驳回")
    @Excel(name = "状态", width = 30,
            replace = {"正常_0",
                    "新增农户待项目审核_1", "新增农户项目审核驳回_2",
                    "新增农户项目审核通过（待业务审核）_3", "新增农户业务审核驳回_4",
                    "新增农户业务审核通过（待领导审核）_5", "新增农户领导审核驳回_6",
                    "修改农户待项目审核_7", "修改农户项目审核驳回_8",
                    "修改农户项目审核通过（待业务审核）_9", "修改农户业务审核驳回_10",
                    "修改农户业务审核通过（待领导审核）_11", "修改农户领导审核驳回_12"})
    private Integer auditStatus;

    @ApiModelProperty(value = "最近一次提交审核的时间。租金的锁定区间以它为准")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditSubmitTime;

    @ApiModelProperty(value = "最近一次提交审核的人")
    private String auditSubmitBy;

    /** 审核状态的中文名，列表和导出共用 */
    public static String auditStatusName(Integer status) {
        if (status == null) {
            return "正常";
        }
        switch (status) {
            case AUDIT_INSERT_PENDING_PROJECT:
                return "新增农户待项目审核";
            case AUDIT_INSERT_PROJECT_REJECTED:
                return "新增农户项目审核驳回";
            case AUDIT_INSERT_PENDING_BUSINESS:
                return "新增农户项目审核通过（待业务审核）";
            case AUDIT_INSERT_BUSINESS_REJECTED:
                return "新增农户业务审核驳回";
            case AUDIT_INSERT_PENDING_LEADER:
                return "新增农户业务审核通过（待领导审核）";
            case AUDIT_INSERT_LEADER_REJECTED:
                return "新增农户领导审核驳回";
            case AUDIT_UPDATE_PENDING_PROJECT:
                return "修改农户待项目审核";
            case AUDIT_UPDATE_PROJECT_REJECTED:
                return "修改农户项目审核驳回";
            case AUDIT_UPDATE_PENDING_BUSINESS:
                return "修改农户项目审核通过（待业务审核）";
            case AUDIT_UPDATE_BUSINESS_REJECTED:
                return "修改农户业务审核驳回";
            case AUDIT_UPDATE_PENDING_LEADER:
                return "修改农户业务审核通过（待领导审核）";
            case AUDIT_UPDATE_LEADER_REJECTED:
                return "修改农户领导审核驳回";
            default:
                return "正常";
        }
    }

    /** 是否处于审核中（待项目/待业务/待领导） */
    public boolean auditPending() {
        return pendingStageOf(auditStatus) != null;
    }

    /** 当前一审级别 PROJECT/BUSINESS/LEADER；不在审核中返回 null */
    public static String pendingStageOf(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case AUDIT_INSERT_PENDING_PROJECT:
            case AUDIT_UPDATE_PENDING_PROJECT:
                return "PROJECT";
            case AUDIT_INSERT_PENDING_BUSINESS:
            case AUDIT_UPDATE_PENDING_BUSINESS:
                return "BUSINESS";
            case AUDIT_INSERT_PENDING_LEADER:
            case AUDIT_UPDATE_PENDING_LEADER:
                return "LEADER";
            default:
                return null;
        }
    }

    /** 是否属于"新增农户"的审核状态 */
    public static boolean isInsertStatus(Integer status) {
        return status != null && status >= AUDIT_INSERT_PENDING_PROJECT
                && status <= AUDIT_INSERT_LEADER_REJECTED;
    }

    /** 是否属于"修改农户"的审核状态 */
    public static boolean isUpdateStatus(Integer status) {
        return status != null && status >= AUDIT_UPDATE_PENDING_PROJECT
                && status <= AUDIT_UPDATE_LEADER_REJECTED;
    }

    /** 某一级审核通过后进入的"待下一级"状态；最后一级通过恢复为正常 */
    public static int pendingStatusFor(boolean insert, String stage) {
        if ("PROJECT".equals(stage)) {
            return insert ? AUDIT_INSERT_PENDING_BUSINESS : AUDIT_UPDATE_PENDING_BUSINESS;
        }
        if ("BUSINESS".equals(stage)) {
            return insert ? AUDIT_INSERT_PENDING_LEADER : AUDIT_UPDATE_PENDING_LEADER;
        }
        return AUDIT_NORMAL;
    }

    /** 某一级审核驳回后的状态 */
    public static int rejectedStatusFor(boolean insert, String stage) {
        if ("PROJECT".equals(stage)) {
            return insert ? AUDIT_INSERT_PROJECT_REJECTED : AUDIT_UPDATE_PROJECT_REJECTED;
        }
        if ("BUSINESS".equals(stage)) {
            return insert ? AUDIT_INSERT_BUSINESS_REJECTED : AUDIT_UPDATE_BUSINESS_REJECTED;
        }
        return insert ? AUDIT_INSERT_LEADER_REJECTED : AUDIT_UPDATE_LEADER_REJECTED;
    }

    @ApiModelProperty(value = "租户ID，用于数据隔离。需求第14条")
    private Integer tenantId;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
