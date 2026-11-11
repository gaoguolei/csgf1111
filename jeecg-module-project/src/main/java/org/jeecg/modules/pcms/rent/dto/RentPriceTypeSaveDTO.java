package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 新增租金档位。需求：一个项目下的档位成对新增，合计覆盖 1-25 年。
 *
 * 为什么成对：按年切分的档位是"前 N 年一个价、之后一个价"，
 * 单独加一条（比如只加 1-3 年）会让 4-25 年这段没有单价，租金直接算不出来。
 * 所以强制一次把整段覆盖齐，比如 1-3 年 + 4-25 年。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "新增租金档位", description = "新增租金档位")
public class RentPriceTypeSaveDTO {

    @ApiModelProperty(value = "大区编码，如 TJLF / BJ / HAIN", required = true)
    private String regionCode;

    @ApiModelProperty(value = "要新增的档位，合计必须从第1年连续覆盖到第25年。"
            + "正常传两条，如 [{1-3年}, {4-25年}]", required = true)
    private List<RentPriceTypeFormDTO> types = new ArrayList<>();
}