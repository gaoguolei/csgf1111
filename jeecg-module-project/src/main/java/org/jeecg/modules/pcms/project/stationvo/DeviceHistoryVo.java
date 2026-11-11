package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/12 下午1:39
 * @Version 1.5
 */
@Data
public class DeviceHistoryVo {
    private String time;
    private String sn;
    //日发电量
    private BigDecimal dayPower;
    //日发电小时数
    private BigDecimal dayHour;
}
