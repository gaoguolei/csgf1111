package org.jeecg.modules.pcms.project.stationvo;


import lombok.Data;

@Data
public class DeviceVo {


    private DeviceData data;

    private String errorCode;
    private String errorMsg;
}
