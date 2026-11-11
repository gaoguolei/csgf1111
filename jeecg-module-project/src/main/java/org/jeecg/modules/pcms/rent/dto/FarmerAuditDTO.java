package org.jeecg.modules.pcms.rent.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 农户变更审核操作的入参。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "农户变更审核", description = "农户变更审核")
public class FarmerAuditDTO {

    @ApiModelProperty(value = "审核申请ID（不是农户ID）", required = true)
    private Long requestId;

    @ApiModelProperty(value = "备注。驳回时必填（驳回原因），通过时可选")
    private String remark;
}
