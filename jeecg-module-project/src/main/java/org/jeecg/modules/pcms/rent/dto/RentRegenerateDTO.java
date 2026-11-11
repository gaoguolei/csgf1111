package org.jeecg.modules.pcms.rent.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 重新生成租金的请求体。
 *
 * 前端以 JSON body 提交（{"stationId":4268,"year":2026}），
 * 所以这里用 @RequestBody 接收，而不是 @RequestParam——
 * 后者只从 query string 或 form 表单里取，读不到 JSON body。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "重新生成租金请求", description = "重新生成租金请求")
public class RentRegenerateDTO {

    @ApiModelProperty(value = "电站ID，必填")
    private Long stationId;

    @ApiModelProperty(value = "租金年度，不传取当年")
    private Integer year;

    @ApiModelProperty(value = "只重出这一期（1~12）。不传则按 fromDate（或今天）落在哪一期来定。"
            + "需求第20条：详情页的重新生成按钮只重出当期")
    private Integer periodNo;

    @ApiModelProperty(value = "按哪天算当期，格式 yyyy-MM-dd，不传取当天。"
            + "需求第21条：安装信息修改后按修改当日所在期重算")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private Date fromDate;

    @ApiModelProperty(value = "1=生成本年度全额（第1期一直算到年末）。"
            + "需求第25条：农户详情的「生成租金」按钮走这个。"
            + "不传时按 periodNo / fromDate 决定范围："
            + "传 periodNo 只重出该期，传 fromDate 从该日所在期算到年末")
    private Integer wholeYear;
}
