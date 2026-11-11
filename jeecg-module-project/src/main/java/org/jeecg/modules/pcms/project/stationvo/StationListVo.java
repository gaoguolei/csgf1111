package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

import javax.security.sasl.SaslServer;

/**
 * @Author gongzhou
 * @Date 2023/9/12 下午2:59
 * @Version 1.5
 */
@Data
public class StationListVo {


    private Integer plantId;

    private String name;

    private Integer userId;

    private String country;

    private String longitude;

    private String latitude;

    private String peakPower;

    private String createDate;

    private String installer;

    private String currentPower;

    private String totalEnergy;

    private String latitudeD;

    private String latitudeF;

    //当日电量
    private String todayPower;


    //日满小时数
    private String hour;



}
