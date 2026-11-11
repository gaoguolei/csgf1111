package org.jeecg.modules.pcms.project.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.UnsupportedEncodingException;

/**
 * @Description: pv_project_file
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
@ApiModel(value="pv_project_file对象", description="pv_project_file")
@Data
@TableName("pv_project_file")
@AllArgsConstructor
@NoArgsConstructor
public class PvProjectFile implements Serializable {
    private static final long serialVersionUID = 1L;

	/**id*/
	@TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;
	/**项目ID*/
    @ApiModelProperty(value = "项目ID")
    private java.lang.Integer projectId;
	/**参数组ID*/
	@Excel(name = "参数组ID", width = 15)
    @ApiModelProperty(value = "参数组ID")
    private java.lang.Integer paramGroupId;
	/**参数ID*/
	@Excel(name = "参数ID", width = 15)
    @ApiModelProperty(value = "参数ID")
    private java.lang.Integer paramItmeId;
	/**位置名称*/
	@Excel(name = "位置名称", width = 15)
    @ApiModelProperty(value = "位置名称")
    private java.lang.String labelText;
	/**文件URL*/
	@Excel(name = "文件URL", width = 15)
    @ApiModelProperty(value = "文件URL")
    private java.lang.String fileUrl;
	/**缩略图*/
	@Excel(name = "缩略图", width = 15)
    @ApiModelProperty(value = "缩略图")
    private java.lang.String thumbUrl;
	/**文件类型*/
	@Excel(name = "文件类型", width = 15, dicCode = "dict_file_type")
    @ApiModelProperty(value = "文件类型")
    private java.lang.String fileType;
	/**是否必填*/
	@Excel(name = "是否必填", width = 15, dicCode = "dict_yes_no")
    @ApiModelProperty(value = "是否必填")
    private java.lang.String isRequired;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sortNo;
    /**排序*/
    @Excel(name = "原图是否已删除", width = 15, dicCode = "dict_yes_no")
    @ApiModelProperty(value = "原图是否已删除")
    private java.lang.String isOriginalDeleted;

    /**删除状态(0-正常,1-已删除)*/
	@Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    @TableLogic
    private java.lang.Integer delFlag;
	/**所属租户*/
	@Excel(name = "所属租户", width = 15)
    @ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;
}
