package org.jeecg.modules.pcms.rent.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 操作日志展示项。需求稿第 11 条要求日志能看出改了什么，
 * 所以 summary 里给的是"安装块数 由 10 改为 30"这种人话，
 * 原始 JSON 仍放在 detail 里备查。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "操作日志", description = "操作日志")
public class OperLogVO {

    @ApiModelProperty(value = "日志ID")
    private Long id;

    @ApiModelProperty(value = "操作用户")
    private String operUser;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    private Date operTime;

    @ApiModelProperty(value = "动作编码")
    private String action;

    @ApiModelProperty(value = "动作名称，如 修改、提交审核")
    private String actionName;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "业务主键")
    private Long bizId;

    @ApiModelProperty(value = "变更摘要，逐条列出改了哪个字段、从什么改成什么")
    private String summary;

    @ApiModelProperty(value = "原始内容，改前改后JSON")
    private String detail;

    @ApiModelProperty(value = "操作IP")
    private String ip;
}
