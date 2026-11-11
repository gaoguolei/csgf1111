package org.jeecg.modules.pcms.energy.dto.growatt;


import lombok.Data;

@Data
public class GrowattDeviceDetailDto {



    @Data
    public static class DeviceDetail {
        public String peakPowerActual;
        public String monthlyEnergy;
        public String lastUpdateTime;
        public String currentPower;
        public String timezone;
        public String yearlyEnergy;
        public String todayEnergy;
        public String carbonOffset;
        public String efficiency;
        public String totalEnergy;
    }

    public DeviceDetail data;
    public int errorCode;
    public String errorMsg;
}
