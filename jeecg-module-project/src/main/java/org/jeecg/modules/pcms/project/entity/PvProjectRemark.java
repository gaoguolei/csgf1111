package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;

/**
 * @Description: pv_project_remark
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
@ApiModel(value="pv_project_remark对象", description="pv_project_remark")
@Data
@TableName("pv_project_remark")
public class PvProjectRemark implements Serializable {
    private static final long serialVersionUID = 1L;

	/**id*/
	@TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;
	/**客户项目ID*/
    @ApiModelProperty(value = "客户项目ID")
    private java.lang.Integer projectId;
	/**参数组ID*/
	@Excel(name = "参数组ID", width = 15)
    @ApiModelProperty(value = "参数组ID")
    private java.lang.Integer paramGroupId;
	/**参数项ID*/
	@Excel(name = "参数项ID", width = 15)
    @ApiModelProperty(value = "参数项ID")
    private java.lang.Integer paramItmeId;
	/**标签文本*/
	@Excel(name = "标签文本", width = 15)
    @ApiModelProperty(value = "标签文本")
    private java.lang.String labelText;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private java.lang.String remark;
	/**是否必填*/
	@Excel(name = "是否必填", width = 15, dicCode = "dict_yes_no")
    @ApiModelProperty(value = "是否必填")
    private java.lang.String isRequired;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sortNo;
	/**删除状态(0-正常,1-已删除)*/
	@Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
//    @TableLogic
    private java.lang.Integer delFlag;
	/**所属租户*/
	@Excel(name = "所属租户", width = 15)
    @ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;
}
