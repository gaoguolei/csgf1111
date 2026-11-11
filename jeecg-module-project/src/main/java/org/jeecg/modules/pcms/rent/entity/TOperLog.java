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
 * @Description: 操作日志记录表
 * @Version: V1.0
 */
@Data
@TableName("t_oper_log")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_oper_log对象", description = "操作日志记录表")
public class TOperLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ACTION_INSERT = "INSERT";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_IMPORT = "IMPORT";
    /** 生成/重新生成租金台账 */
    public static final String ACTION_GENERATE = "GENERATE";
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    /** 标记纠纷暂停支付 */
    public static final String ACTION_DISPUTE = "DISPUTE";
    /** 取消纠纷暂停支付 */
    public static final String ACTION_CANCEL_DISPUTE = "CANCEL_DISPUTE";
    public static final String ACTION_VOID = "VOID";
    public static final String ACTION_PAY = "PAY";
    /** 银行回单对账 */
    public static final String ACTION_RECONCILE = "RECONCILE";
    public static final String ACTION_UPLOAD = "UPLOAD";

    public static final String BIZ_FARMER = "农户信息";
    public static final String BIZ_INSTALL = "安装信息";
    public static final String BIZ_RENT = "租金";
    public static final String BIZ_IMPORT = "导入";
    public static final String BIZ_LEDGER = "租金台账";
    public static final String BIZ_ATTACH = "租金附件";
    /** 租金档位字典：按项目管理租金类型时的增删改 */
    public static final String BIZ_PRICE_TYPE = "租金档位";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "操作用户")
    private String operUser;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    private Date operTime;

    @ApiModelProperty(value = "动作 INSERT/UPDATE/DELETE/IMPORT")
    private String action;

    @ApiModelProperty(value = "业务类型 农户信息/安装信息/租金/导入")
    private String bizType;

    @ApiModelProperty(value = "业务主键")
    private Long bizId;

    @ApiModelProperty(value = "操作内容，改前改后JSON")
    private String operContent;

    @ApiModelProperty(value = "操作IP")
    private String ip;
}
