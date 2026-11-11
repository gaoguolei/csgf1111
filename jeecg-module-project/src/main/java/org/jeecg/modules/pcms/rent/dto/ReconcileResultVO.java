package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 银行回单批量上传对账结果。需求稿第 13 条。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "回单对账结果", description = "回单对账结果")
public class ReconcileResultVO {

    @ApiModelProperty(value = "上传的回单张数")
    private int total;

    @ApiModelProperty(value = "对账一致的张数")
    private int okCount;

    @ApiModelProperty(value = "对账异常的张数，信息对不上但回单已存入附件")
    private int errorCount;

    @ApiModelProperty(value = "无法处理的张数：文件名解析不出、找不到台账、OCR失败")
    private int failCount;

    @ApiModelProperty(value = "每张回单的处理明细")
    private List<Item> items = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "回单对账明细", description = "回单对账明细")
    public static class Item {

        @ApiModelProperty(value = "上传的文件名")
        private String fileName;

        @ApiModelProperty(value = "从文件名解析出的电站名称")
        private String parsedName;

        @ApiModelProperty(value = "从备注里解析出的年度。备注没写年份时为空")
        private Integer parsedYear;

        @ApiModelProperty(value = "从备注里解析出的期数")
        private Integer parsedPeriod;

        @ApiModelProperty(value = "回单/清单行上识别出的备注原文（客户在这里写期数）")
        private String receiptRemark;

        @ApiModelProperty(value = "匹配到的台账ID")
        private Long ledgerId;

        @ApiModelProperty(value = "处理结果 OK已对账 ERROR对账异常 FAIL未能处理")
        private String result;

        @ApiModelProperty(value = "结果说明，异常时逐项写明哪里对不上")
        private String message;

        @ApiModelProperty(value = "回单上识别出的金额")
        private BigDecimal receiptAmount;

        @ApiModelProperty(value = "台账的本次支付金额 = min(本期应付, 本期实付)，回单金额就是跟它比对的")
        private BigDecimal systemAmount;

        @ApiModelProperty(value = "回单上识别出的收款账号")
        private String receiptAccount;

        @ApiModelProperty(value = "回单上识别出的收款人")
        private String receiptName;

        @ApiModelProperty(value = "1=回单已存入该台账附件")
        private Integer attached;
    }
}
