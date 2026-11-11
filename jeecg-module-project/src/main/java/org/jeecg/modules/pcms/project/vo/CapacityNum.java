package org.jeecg.modules.pcms.project.vo;

/**
 * @Author gongzhou
 * @Date 2023/8/9 上午9:25
 * @Version 1.5
 */

import lombok.Data;

import java.math.BigDecimal;

/**
 * 户数和容量
 */
@Data
public class CapacityNum {


    //镇
    private String town;

    //户数（累计签约户数）
    private Integer houseNum;

    //本周签约户数
    private Integer weekHouseNum;


    //累计签约容量
    private BigDecimal capacity;


    //本周签约容量
    private BigDecimal weekCapacity;
}
