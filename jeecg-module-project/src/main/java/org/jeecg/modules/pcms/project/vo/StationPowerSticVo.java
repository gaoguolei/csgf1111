package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/25 下午7:45
 * @Version 1.5
 */
@Data
public class StationPowerSticVo {


    //当日功率(实时功率)
    private String currentPower;
    //日发电量
    private String generation;
    //累计发电量
    private String totalPower;
    //组件总容量（组件总容量）
    private String peakPower;
    //日发满小时数
    private String dayHour;
    //日发电量
    private String dayNum;
    //日收益
    private String dayIncome;
    //月发满小时
    private String monthHour;
    //月发电量
    private String monthPower;
    //月收益
    private String monthIncome;
    //节能减排
    private String carbonOffset;
    //节能减排总容量
    private String carbonOffsetNum;
    //电站类型
    private String plantType;
    //创建时间
    private Date createTime;
    //经度
    private String longitude;
    //纬度
    private String latitude;




}
