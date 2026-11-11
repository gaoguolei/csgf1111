package org.jeecg.modules.pcms.project.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.UnsupportedEncodingException;

/**
 * @Description: pv_project_bpm
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
@ApiModel(value="pv_project_bpm对象", description="pv_project_bpm")
@Data
@TableName("pv_project_bpm")
public class PvProjectBpm implements Serializable {
    private static final long serialVersionUID = 1L;

	/**id*/
	@TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;
	/**项目ID*/
    @ApiModelProperty(value = "项目ID")
    private java.lang.Integer projectId;
	/**节点*/
	@Excel(name = "节点", width = 15, dicCode = "dict_project_bpm")
    @ApiModelProperty(value = "节点")
    private java.lang.String bpmNodeId;
	/**审核人*/
	@Excel(name = "审核人", width = 15)
    @ApiModelProperty(value = "审核人")
    private java.lang.String approver;
	/**审核结果*/
	@Excel(name = "审核结果", width = 15, dicCode = "dict_approval_result")
    @ApiModelProperty(value = "审核结果")
    private java.lang.String approvalResult;
	/**审核意见*/
	@Excel(name = "审核意见", width = 15)
    @ApiModelProperty(value = "审核意见")
    private java.lang.String approvalRemark;
	/**审核时间*/
	@Excel(name = "审核时间", width = 15, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "审核时间")
    private java.util.Date approvalTime;
	/**是否已提交*/
	@Excel(name = "是否已提交", width = 15, dicCode = "dict_yes_no")
    @ApiModelProperty(value = "是否已提交")
    private java.lang.String isCommitted;
	/**操作描述*/
	@Excel(name = "操作描述", width = 15)
    @ApiModelProperty(value = "操作描述")
    private java.lang.String operDescription;
    /**删除状态(0-正常,1-已删除)*/
    @Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    @TableLogic
    private java.lang.Integer delFlag;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**所属租户*/
	@Excel(name = "所属租户", width = 15)
    @ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;
}
