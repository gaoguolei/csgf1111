package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 租金详情的可改项。需求稿第 7 条：详情页只允许改两样东西——
 * 每月实际发电天数、本期实付金额，其余全部系统计算。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金详情编辑", description = "租金详情编辑")
public class RentLedgerEditDTO {

    @ApiModelProperty(value = "台账ID，必填")
    private Long id;

    @ApiModelProperty(value = "本期实付金额。不传表示不调整，仍取本期应付金额")
    private BigDecimal periodPaid;
    @ApiModelProperty(value = "本期应付金额，手工调整用。不传=沿用之前的口径："
            + "之前没调整过就按月明细重算，之前调整过就保持人工值不变。"
            + "传了就以传的为准，并锁住不再被月明细重算覆盖；"
            + "想恢复成系统计算值，把系统算出来的那个数原样传回来即可（会自动解除锁定）")
    private BigDecimal periodPayable;
    @ApiModelProperty(value = "月明细的实际发电天数。只传要改的月份即可，"
            + "genDays 传 null 表示清空该月录入值、回到按日期理论计算")
    private List<MonthItem> months = new ArrayList<>();

    @ApiModelProperty(value = "备注")
    private String remark;

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "月明细录入项", description = "月明细录入项")
    public static class MonthItem {

        @ApiModelProperty(value = "月明细ID，与 ym 二选一")
        private Long id;

        @ApiModelProperty(value = "年月 yyyy-MM，没有ID时按这个匹配")
        private String ym;

        @ApiModelProperty(value = "实际发电天数，不能为负也不能超过当月自然天数")
        private Integer genDays;

        @ApiModelProperty(value = "实际发电天数（与 genDays 同义）。"
                + "前端详情页这一列一直用 rentDays 这个字段名取值，"
                + "为了不改前端，这里同时接受它；两个都传时以 genDays 为准")
        private Integer rentDays;
    }
}
