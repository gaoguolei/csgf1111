package org.jeecg.modules.pcms.project.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author gongzhou
 * @Date 2023/10/9 下午3:21
 * @Version 1.5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsVo {

    //状态
    private String status;

    //电站id
    private String plantId;
    //电站名称
    private String plantName;


    //总功率
    private String Power;

    //当日电量
    private String totalTodayPower;

    //组件总容量
    private String incomeVolume;

//    //当月电量
//    private String totalMonthPower;

    //累计电量
    private String totalEfficiency;
}
