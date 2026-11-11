package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午7:22
 * @Version 1.5
 */
@Data
@TableName("pv_day_power")
public class PvDayPower {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer plantId;

    private Integer year;

    private Integer month;

    private Integer day;

    private Integer hour;

    private Integer min;

    private BigDecimal power;
}
