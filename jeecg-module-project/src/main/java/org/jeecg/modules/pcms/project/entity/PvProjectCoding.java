package org.jeecg.modules.pcms.project.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: pv_project_coding
 * @Author: jun
 * @Date:   2023-03-10
 * @Version: V1.0
 */
@Data
@TableName("pv_project_coding")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="pv_project_coding对象", description="pv_project_coding")
public class PvProjectCoding implements Serializable {
    private static final long serialVersionUID = 1L;

	/**地区id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "地区id")
    private java.lang.Integer id;
	/**年份*/
	@Excel(name = "年份", width = 15, format = "yyyy-MM-dd")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "年份")
    private java.util.Date codeDate;
	/**省份id*/
	@Excel(name = "省份id", width = 15)
    @ApiModelProperty(value = "省份id")
    private java.lang.Integer provinceId;
	/**市区ID*/
	@Excel(name = "市区ID", width = 15)
    @ApiModelProperty(value = "市区ID")
    private java.lang.Integer cityId;
	/**前缀*/
	@Excel(name = "前缀", width = 15)
    @ApiModelProperty(value = "前缀")
    private java.lang.String prefix;
	/**编号*/
	@Excel(name = "编号", width = 15)
    @ApiModelProperty(value = "编号")
    private java.lang.Integer bizcode;
}
