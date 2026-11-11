package org.jeecg.modules.pcms.energy.dto.solar;

import lombok.Data;

import java.util.List;


@Data
public class SolarDeviceFaultDto {

    private String reqSerialNum;
    private String resultCode;
    private String resultMsg;
    private ResultData resultData;

    // 内部静态类 - ResultData
    @Data
    public static class ResultData {
        private List<PageListItem> pageList;
        private Integer rowCount;

    }

    // 内部静态类 - PageListItem
    @Data
    public static class PageListItem {
        private Integer psId;
        private String psKey;
        private Integer faultTypeCode;
        private String faultCode;
        private Integer faultType;
        private Integer faultLevel;
        private String faultDesc;
        private Integer processStatus;
        private String createTime;
        private Long processTime;
        private String faultReason;
        private Integer gridConnectionStatus;
        private String deviceModelCode;
        private String deviceModel;
        private String deviceName;
        private Integer deviceType;
        private String psName;
        private Integer uuid;
        private String typeName;
        private String faultName;
        private String overTime;
    }
}