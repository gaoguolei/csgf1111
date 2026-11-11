package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

import java.util.List;

@Data
public class GrowattDeviceEquipmentPointMinDto {

    private String errorMsg;
    private int errorCode;
    private GrwPointMinData data;

    @Data
    public static class GrwPointMinData {
        private int nextPageStartId;
        private List<InverterData> datas;
        private String sn;
        private String dataloggerSn;
    }


    @Data
    public static class InverterData {
        private String vpv3;
        private String total_energy;
        private String vpv2;
        private String vpv1;
        private String power_factor;
        private String fac;
        private String vpv8;
        private String vpv7;
        private String vpv6;
        private String vpv5;
        private String vpv4;
        private String currentString1;
        private String currentString5;
        private String currentString4;
        private String currentString3;
        private String currentString2;
        private String currentString9;
        private String currentString8;
        private String currentString7;
        private String currentString6;
        private String vString8;
        private String vString7;
        private String vString9;
        private String vac3;
        private String vac2;
        private String vac1;
        private String vString2;
        private String vString1;
        private String ppv6;
        private String vString4;
        private String ppv7;
        private String vString3;
        private String ppv8;
        private String vString6;
        private String vString5;
        private int status;
        private String currentString16;
        private String currentString15;
        private String currentString14;
        private String currentString13;
        private String currentString12;
        private String currentString11;
        private String currentString10;
        private String ppv2;
        private String ppv3;
        private String ppv4;
        private String ppv5;
        private String ppv1;
        private String ipv2;
        private String ipv1;
        private String ipv4;
        private String ipv3;
        private String ipv6;
        private String ipv5;
        private String ipv8;
        private String temperature;
        private String ipv7;
        private String power;
        private String vString11;
        private String vString10;
        private String vString15;
        private String vString14;
        private String vString13;
        private String vString12;
        private String vString16;
        private String iac2;
        private String ppv;
        private String iac1;
        private String iac3;
        private String today_energy;
        private String time;
    }
}