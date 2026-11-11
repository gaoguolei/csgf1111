package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * 租金导出汇总：按「项目 + 期数」分组统计条数和金额。
 *
 * 项目经理要求导出时在表格前面带一份汇总，看一眼就知道这次导出了多少条、
 * 每个分期的钱合计多少，不用自己在 Excel 里再拉一遍数据透视。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金导出汇总", description = "租金导出汇总")
public class RentLedgerSummaryVO {

    @ApiModelProperty(value = "项目名称")
    @Excel(name = "项目", width = 28)
    private String projectName;

    @ApiModelProperty(value = "期数名称，如 2026年4期")
    @Excel(name = "期数", width = 16)
    private String periodName;

    @ApiModelProperty(value = "该分期下的租金条数")
    @Excel(name = "租金条数", width = 12)
    private Integer count;

    @ApiModelProperty(value = "该分期的本期应付合计")
    @Excel(name = "本期应付合计(元)", width = 18)
    private BigDecimal payableSum;

    @ApiModelProperty(value = "该分期的本期实付合计")
    @Excel(name = "本期实付合计(元)", width = 18)
    private BigDecimal paidSum;
}