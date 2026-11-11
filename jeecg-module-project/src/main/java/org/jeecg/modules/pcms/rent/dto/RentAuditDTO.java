package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 提交审核、审核通过、驳回、纠纷标记、作废共用的入参。
 *
 * 需求稿第 8 条：标记纠纷、取消纠纷、审核驳回这三种操作都必须填备注，
 * 校验放在 service 里，前端漏传也拦得住。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金审核操作", description = "租金审核操作")
public class RentAuditDTO {

    @ApiModelProperty(value = "台账ID，单条操作时用")
    private Long id;

    @ApiModelProperty(value = "台账ID集合，批量提交时用")
    private List<Long> ids = new ArrayList<>();

    @ApiModelProperty(value = "备注。驳回、标记纠纷、取消纠纷、作废时必填")
    private String remark;
}
