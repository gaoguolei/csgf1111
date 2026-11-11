package org.jeecg.modules.pcms.project.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/25 上午8:26
 * @Version 1.5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationInfoDataVo {

    private String status;

    private String stationType;

    private String createTime;

    //经度
    private String instLongitude;

    //纬度
    private String instLatitude;


    //等效绿植
    private String plants;

    //减排
    private String reduction;

    //实时功率
    private String nowPower;

    //月日发满小时数
    private String monthHour;

    //日发满小时数
    private String dayHour;

    //月收益
    private String monthIncome;

    //日收益
    private String dayIncome;


    //日发电量
    private String dayPower;

    //月发电量
    private String monthPower;

    //组件总容量
    private String total;

    private Double percentage;




}
