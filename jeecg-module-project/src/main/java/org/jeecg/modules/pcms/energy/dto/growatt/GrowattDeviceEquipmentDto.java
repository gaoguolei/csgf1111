package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

import java.util.List;

@Data
public class GrowattDeviceEquipmentDto {
    public String errorMsg;
    public DeviceEquipmentData data;
    public int errorCode;

    @Data
    public static class DeviceEquipmentData {
        public List<DeviceEquipmentDto> devices;
        public int count;
    }

    @Data
    public static class DeviceEquipmentDto {
        public String lastUpdateTime;
        public int deviceId;
        public String deviceSn;
        public boolean lost;
        public String model;
        public int type;
        public String dataloggerSn;
        public String manufacturer;
        public int status;

        public String getLost() {
            return lost ? "0" : "1";
        }
    }
}