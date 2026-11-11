package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/11/1 下午6:40
 * @Version 1.5
 */
@Data

public class StationCountVo {

    //发电量
    private BigDecimal power;

    //收益
    private BigDecimal income;

    //发电小时
    private BigDecimal hour;

    private Integer key;
    private Integer index;
    //累计发电量
    private  BigDecimal totalPower;
    //累计发电量
    private  BigDecimal peakPower;

    private BigDecimal currentPower;



}
