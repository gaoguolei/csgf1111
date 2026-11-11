package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

@Data
public class GrowattEnergyResponseDto {
    public String errorMsg;
    public ReturnData data;
    public int errorCode;

    @Data
    public static class ReturnData {
        public EnergyItem[] energys;
        public int count;
        public String timeUnit;
    }
    @Data
    public static class EnergyItem {
        public String date;
        public String energy;
    }
}