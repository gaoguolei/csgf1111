package org.jeecg.modules.pcms.project.vo;

/**
 * @Author gongzhou
 * @Date 2023/7/31 上午11:09
 * @Version 1.5
 */

import lombok.Data;

import java.math.BigDecimal;

/**
 * 容量
 */
@Data
public class Capacity {
    //累计签约完成容量
    private BigDecimal signLoad;
    //本周签约容量
    private BigDecimal signWeekLoad;


    //累计施工容量
    private BigDecimal buildLoad;
    //本周施工容量
    private BigDecimal buildWeekLoad;


    //累计申报容量
    private BigDecimal reportLoad;
    //本周申报容量
    private BigDecimal reportWeekLoad;


    //累计并网容量
    private BigDecimal onGridLoad;
    //本周并网容量
    private BigDecimal onGridWeekLoad;

}
