package org.jeecg.modules.pcms.energy.dto.growatt;


import lombok.Data;

import java.util.List;

@Data
public class GrowattDeviceFaultDto {
    public String errorMsg;
    public AlarmData data;
    public int errorCode;

    @Data
    public static class AlarmData {
        public List<Alarm> alarms;
        public int count;
        public String sn;
    }

    @Data
    public static class Alarm {
        public String startTime;
        public String alarmMessage;
        public String endTime;
        public String faultType;
        public int alarmCode;
        public int status;
    }
}
