package org.jeecg.modules.pcms.common.set.entity;

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
 * @Description: set_regions
 * @Author: jeecg-boot
 * @Date:   2023-03-12
 * @Version: V1.0
 */
@Data
@TableName("set_regions")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="set_regions对象", description="set_regions")
public class SetRegions implements Serializable {
    private static final long serialVersionUID = 1L;

	/**地区id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "地区id")
    private java.lang.Integer id;
	/**父地区id*/
	@Excel(name = "父地区id", width = 15)
    @ApiModelProperty(value = "父地区id")
    private java.lang.Integer parentId;
	/**路径*/
	@Excel(name = "路径", width = 15)
    @ApiModelProperty(value = "路径")
    private java.lang.String regionPath;
	/**级别*/
	@Excel(name = "级别", width = 15)
    @ApiModelProperty(value = "级别")
    private java.lang.Integer regionGrade;
	/**名称*/
	@Excel(name = "名称", width = 15)
    @ApiModelProperty(value = "名称")
    private java.lang.String localName;
	/**邮编*/
	@Excel(name = "邮编", width = 15)
    @ApiModelProperty(value = "邮编")
    private java.lang.String zipcode;
	/**是否支持货到付款*/
	@Excel(name = "是否支持货到付款", width = 15)
    @ApiModelProperty(value = "是否支持货到付款")
    private java.lang.String cod;
}
