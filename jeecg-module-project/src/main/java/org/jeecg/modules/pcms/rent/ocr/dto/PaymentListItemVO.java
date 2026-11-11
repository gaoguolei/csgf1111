package org.jeecg.modules.pcms.rent.ocr.dto;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 付款清单里的一行。
 *
 * 客户的"付款明细"是批量代发清单（序号|收款人|收款账号|代发金额|…），
 * 一张表几十笔，跟一行一笔的回单版式完全不同，所以单独建个模型。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "付款清单行", description = "付款清单行")
public class PaymentListItemVO {

    @ApiModelProperty(value = "表里的行号（从表头下一行开始数），报错时用来定位是哪一行")
    private Integer rowNo;

    @ApiModelProperty(value = "收款人")
    private String payeeName;

    @ApiModelProperty(value = "收款账号，定位农户用")
    private String payeeAccount;

    @ApiModelProperty(value = "代发金额，已去掉千分位")
    private BigDecimal amount;

    @ApiModelProperty(value = "备注列的内容。清单末尾会加一列备注写期数，对账时优先按期数定位")
    private String remark;

    @ApiModelProperty(value = "整行原始文本，排查识别问题时用")
    private String raw;
}