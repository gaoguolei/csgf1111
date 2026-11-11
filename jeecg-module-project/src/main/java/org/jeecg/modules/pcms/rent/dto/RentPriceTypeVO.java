package org.jeecg.modules.pcms.rent.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 租金档位列表项。管理界面用，比下拉选项多带了「被多少户在用」。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金档位", description = "租金档位")
public class RentPriceTypeVO {

    @ApiModelProperty(value = "档位ID")
    private Long id;

    @ApiModelProperty(value = "大区编码")
    private String regionCode;

    @ApiModelProperty(value = "档位编码，如 TJ_Y1_5")
    private String typeCode;

    @ApiModelProperty(value = "档位名称，如 1-5年")
    private String typeName;

    @ApiModelProperty(value = "年份区间描述，如 第1-5年")
    private String yearRangeDesc;

    @ApiModelProperty(value = "起始年，含")
    private Integer startYear;

    @ApiModelProperty(value = "结束年，含")
    private Integer endYear;

    @ApiModelProperty(value = "切分方式 1按年 2按自然日期")
    private Integer splitMode;

    @ApiModelProperty(value = "排序号")
    private Integer sortNo;

    @ApiModelProperty(value = "当前有多少条租金单价在引用这个档位（只算未删除的）")
    private Integer usedCount;

    @ApiModelProperty(value = "1=没人用，可以修改/删除；0=已被引用，改删会被拦下")
    private Integer editable;

    @ApiModelProperty(value = "不能改删时的原因，前端可直接提示")
    private String blockReason;
}