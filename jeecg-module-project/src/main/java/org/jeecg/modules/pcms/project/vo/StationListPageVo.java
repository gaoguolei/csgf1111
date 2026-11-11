package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/10/27 上午9:41
 * @Version 1.5
 */
@Data
public class StationListPageVo {
    //电站id
    private String plantId;
    //状态
    private String status;
    //电站名称
    private String plantName;
    //逆变器数量
    private String deviceNum;
    //当日电量(kwh)
    private String totalTodayPower;
    //累计电量(Mwh)
    private String totalEfficiency;
    //日发满小时数
    private String dayHour;
    //实时功率
    private String nowPower;
    //组件总容量
    private String incomeVolume;
    //更新时间
    private Date updateTime;

}
