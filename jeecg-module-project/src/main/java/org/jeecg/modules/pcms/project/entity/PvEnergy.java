package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:14
 * @Version 1.5
 */
@Data
@TableName("pv_energy")
public class PvEnergy {


    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer plantId;

    private Integer year;

    private Integer month;

    private Integer day;

    private BigDecimal energy;
}
