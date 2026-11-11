package org.jeecg.modules.pcms.rent.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 单个租金档位的表单。新增时按 RentPriceTypeSaveDTO 成组提交，修改时单独提交。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金档位表单", description = "租金档位表单")
public class RentPriceTypeFormDTO {

    @ApiModelProperty(value = "档位ID，修改时必填")
    private Long id;

    @ApiModelProperty(value = "档位名称，如 1-3年 / 前10年", required = true)
    private String typeName;

    @ApiModelProperty(value = "起始年，含。第1年从并网日算起", required = true)
    private Integer startYear;

    @ApiModelProperty(value = "结束年，含", required = true)
    private Integer endYear;

    @ApiModelProperty(value = "排序号，同一个大区内数字小的档位优先。不传时按起始年自动排")
    private Integer sortNo;
}