package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/10/12 上午8:58
 * @Version 1.5
 */

@Data
public class StationReportVo {



    private String stationName;

    private String peakPower;

    private String income;


    private String dayPowerHour;

    private String dayPower;

    private String totalPower;

    private Integer plantId;
}
