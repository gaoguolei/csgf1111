package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceLast;

@Data
public class DeviceLastVo {

    private String deviceSn;

    private String dataloggerSn;


    private PvDeviceLast data;

    private String errorCode;


    private String errorMsg;
}
