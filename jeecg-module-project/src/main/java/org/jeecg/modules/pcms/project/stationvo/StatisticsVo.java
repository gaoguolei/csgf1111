package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/10/9 下午3:21
 * @Version 1.5
 */
@Data
public class StatisticsVo {

    //实时总功率
    private String totalPower;

    //当日电量
    private String totalTodayPower;

    //当月电量
    private String totalMonthPower;

    //累计电量
    private String totalEfficiency;

    //累计峰值功率
    private String totalPeakPower;

    private Integer count;

    private Integer status0;

    private Integer status1;

    private Integer status3;

}
