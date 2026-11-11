package org.jeecg.modules.pcms.rent.ocr.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 银行回单/付款回单的识别结果。
 *
 * 阿里云对 PaymentRecord 这类单据没有公开固定的字段名（文档写的是"支持自定义抽取字段"），
 * 不同银行的回单版式也不一样，所以这里的做法是：
 *   1. rawFields  —— 原样返回阿里云识别出的全部键值对，一个不丢
 *   2. 上面那批具名字段 —— 按语义从 rawFields 里归类出来，能认出几个算几个
 *   3. content    —— 整张图的文本，前面两样都没认出来时人工照着看
 *
 * 前端拿具名字段回填表单，同时把 rawFields 也展示出来，
 * 遇到没归类上的字段可以手动挑，也方便反过来补充归类规则。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "银行回单识别结果", description = "银行回单识别结果")
public class PaymentReceiptVO {

    @ApiModelProperty(value = "阿里云请求ID，出问题时提工单用得上")
    private String requestId;

    @ApiModelProperty(value = "付款人/付方户名")
    private String payerName;

    @ApiModelProperty(value = "付款账号")
    private String payerAccount;

    @ApiModelProperty(value = "收款人/收方户名，这一栏才是农户")
    private String payeeName;

    @ApiModelProperty(value = "收款账号，用来跟农户的银行卡号比对")
    private String payeeAccount;

    @ApiModelProperty(value = "金额，已去掉 ¥ 和千分位")
    private BigDecimal amount;

    @ApiModelProperty(value = "大写金额，原样返回不做转换")
    private String amountInWords;

    @ApiModelProperty(value = "交易日期，原样返回字符串，格式各银行不统一")
    private String tradeTime;

    @ApiModelProperty(value = "流水号/凭证号")
    private String serialNo;

    @ApiModelProperty(value = "用途/摘要/附言")
    private String remark;

    @ApiModelProperty(value = "开户行/付款行")
    private String bankName;

    @ApiModelProperty(value = "阿里云识别出的全部键值对，key为字段名")
    private Map<String, FieldVO> rawFields = new LinkedHashMap<>();

    @ApiModelProperty(value = "整张图的文本")
    private String content;

    @ApiModelProperty(value = "按收款人姓名和账号匹配到的候选农户，供前端选择关联")
    private List<FarmerCandidateVO> candidates = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "识别字段", description = "识别字段")
    public static class FieldVO {
        @ApiModelProperty(value = "阿里云给出的字段名")
        private String keyName;

        @ApiModelProperty(value = "识别值")
        private String value;

        @ApiModelProperty(value = "置信度 0-100，低于80的建议前端标黄提示人工核对")
        private Integer confidence;
    }

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "候选农户", description = "候选农户")
    public static class FarmerCandidateVO {
        @ApiModelProperty(value = "农户ID")
        private Long id;

        @ApiModelProperty(value = "农户姓名")
        private String farmerName;

        @ApiModelProperty(value = "银行卡姓名")
        private String bankCardName;

        @ApiModelProperty(value = "银行卡号")
        private String bankCardNo;

        @ApiModelProperty(value = "发电户号")
        private String powerUserNo;

        @ApiModelProperty(value = "所属项目ID")
        private Long projectId;

        @ApiModelProperty(value = "所属项目名称")
        private String projectName;

        @ApiModelProperty(value = "匹配方式：ACCOUNT=按卡号精确匹配 NAME=按姓名匹配")
        private String matchBy;
    }
}
