package org.jeecg.modules.pcms.project.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/24 下午9:05
 * @Version 1.5
 */

@Data
public class StationPageVo {
    //实时功率
    private String totalCurrentPower;
    //组件总容量
    private String totalPeakPower;
    //电站总数
    private Integer stationCount;
    //累计发电量
    private String totalPower;
    //组件总数
    private BigDecimal section;
    //总收益
    private String income;
    //在线数量
    private Integer status1;
    //在线占比
    private BigDecimal online;
    //离线数量
    private Integer status0;
    //离线占比
    private BigDecimal offline;
     //节能减排(树)
    private BigDecimal emission;

    //电站累计发电量
    private String deviceTotalPower;
}
