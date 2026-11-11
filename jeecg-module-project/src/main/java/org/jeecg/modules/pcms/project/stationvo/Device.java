package org.jeecg.modules.pcms.project.stationvo;


import lombok.Data;

import java.text.DecimalFormat;

@Data
public class Device {


    private String plantId;
    private String deviceId;
    private String dataloggerSn;
    private String deviceSn;
    private String manufacturer;
    private String model;
    private String type;
    private String lastUpdateTime;
    private String lost;
    private String status;

}
