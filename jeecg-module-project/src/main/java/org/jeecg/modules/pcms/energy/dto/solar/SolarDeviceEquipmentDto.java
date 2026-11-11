package org.jeecg.modules.pcms.energy.dto.solar;


import lombok.Data;

import java.util.List;

@Data
public class SolarDeviceEquipmentDto {

    public String reqSerialNum;
    public String resultCode;
    public String resultMsg;
    public ResultData resultData;

    @Data
    public static class ResultData {
        public List<PageItem> pageList;
        public Integer rowCount;
    }

    @Data
    public static class PageItem {
        public Integer chnnlId;
        public String typeName;
        public String psKey;
        public String deviceSn;
        public String devStatus;
        public Integer deviceType;
        public String factoryName;
        public Long uuid;
        public String gridConnectionDate;
        public String deviceName;
        public Integer devFaultStatus;
        public Integer relState;
        public Integer deviceCode;
        public Long psId;
        public Long deviceModelId;
        public String communicationDevSn;
        public String deviceModelCode;
    }
}
