package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
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
 * @Description: 租金纠纷暂停支付记录，一次纠纷一行
 *
 * 与审核记录一动作一行不同，纠纷是"暂停 → 取消"成对出现的，
 * 所以标记时插入一行、取消时回填同一行，一行就是一次完整的纠纷过程。
 * cancel_time 为空表示这次纠纷还没解除，正在暂停中。
 *
 * 建这张表的原因和 t_rent_audit_record 一样：台账上只存得下最近一次。
 * 原来反复标记几次，dispute_time/dispute_cancel_time 只剩最后一次，
 * dispute_remark 还把取消原因用 | 拼在标记原因后面，几轮下来就糊成一团了。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_dispute_record")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_dispute_record对象", description = "租金纠纷暂停支付记录")
public class TRentDisputeRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "台账ID")
    private Long ledgerId;

    @ApiModelProperty(value = "第几次纠纷，从1开始")
    private Integer disputeRound;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "标记纠纷暂停支付的时间")
    private Date disputeTime;

    @ApiModelProperty(value = "标记纠纷的原因，必填")
    private String disputeRemark;

    @ApiModelProperty(value = "标记人")
    private String disputeBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "取消纠纷的时间，为空表示这次纠纷还在暂停中")
    private Date cancelTime;

    @ApiModelProperty(value = "取消纠纷的原因，必填")
    private String cancelRemark;

    @ApiModelProperty(value = "取消人")
    private String cancelBy;

    /** 这次纠纷是否还在暂停中 */
    public boolean ongoing() {
        return cancelTime == null;
    }
}
