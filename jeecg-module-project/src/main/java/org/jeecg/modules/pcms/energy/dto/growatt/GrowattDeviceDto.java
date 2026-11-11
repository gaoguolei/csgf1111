package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

import java.util.List;

@Data
public class GrowattDeviceDto {
    private String errorMsg;
    private PlantData data;
    private int errorCode;

    @Data
    public static class PlantData {
        private List<Plant> plants;
        private int count;
    }

    @Data
    public static class Plant {
        private String totalEnergy;
        private int plantId;
        private String country;
        private Object latitudeF;
        private Object latitudeD;
        private String city;
        private String imageUrl;
        private String latitude;
        private String currentPower;
        private String locale;
        private String peakPower;
        private String operator;
        private String installer;
        private String userId;
        private String name;
        private String createDate;
        private int status;
        private String longitude;
    }
}



