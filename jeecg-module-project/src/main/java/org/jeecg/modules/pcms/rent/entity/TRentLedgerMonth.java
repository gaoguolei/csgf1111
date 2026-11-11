package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 租金台账月明细，一期最多三行
 *
 * 需求稿第 7 条：租金详情页可录入本期内每月的实际发电天数，
 * 累计发电天数取这几行之和；没录入的月份按 rent_days 算。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_ledger_month")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_ledger_month对象", description = "租金台账月明细")
public class TRentLedgerMonth implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "台账ID")
    private Long ledgerId;

    @ApiModelProperty(value = "年月 yyyy-MM")
    private String ym;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "该月计租起日")
    private Date segBegin;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "该月计租止日")
    private Date segEnd;

    @ApiModelProperty(value = "当月自然天数，28/29/30/31")
    private Integer monthDays;

    @ApiModelProperty(value = "该月理论计租天数")
    private Integer rentDays;

    @ApiModelProperty(value = "该月实际发电天数，人工录入；为空表示未录入，按 rentDays 计算")
    private Integer genDays;

    @ApiModelProperty(value = "该月满月租金 = 该月适用档位年租金 ÷ 12")
    private BigDecimal monthRent;

    @ApiModelProperty(value = "该月理论金额，按 rentDays 算，不随人工录入变化，是折算发电天数时的基数")
    private BigDecimal theoryAmount;

    @ApiModelProperty(value = "该月金额 = 理论金额 ÷ 计租天数 × 实际发电天数，未录入时等于理论金额")
    private BigDecimal amount;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 实际参与计算的天数：录了发电天数用发电天数，没录用理论计租天数 */
    public int effectiveDays() {
        if (genDays != null) {
            return genDays;
        }
        return rentDays == null ? 0 : rentDays;
    }
}
