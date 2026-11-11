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
 * @Description: 租金单价档位字典
 * @Version: V1.0
 */
@Data
@TableName("t_rent_price_type")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_price_type对象", description = "租金单价档位字典")
public class TRentPriceType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 按年切分：相对每户并网日 */
    public static final int SPLIT_BY_YEAR = 1;
    /** 按自然日期切分：全体农户统一时点 */
    public static final int SPLIT_BY_DATE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属大区")
    private String regionCode;

    @ApiModelProperty(value = "档位编码")
    private String typeCode;

    @ApiModelProperty(value = "档位名称")
    private String typeName;

    @ApiModelProperty(value = "切分方式 1按年 2按自然日期")
    private Integer splitMode;

    @ApiModelProperty(value = "起始年，含")
    private Integer startYear;

    @ApiModelProperty(value = "结束年，含")
    private Integer endYear;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效起日，NULL=从并网日起")
    private Date effectDateFrom;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效止日，NULL=到租期结束")
    private Date effectDateTo;

    private Integer sortNo;
}
