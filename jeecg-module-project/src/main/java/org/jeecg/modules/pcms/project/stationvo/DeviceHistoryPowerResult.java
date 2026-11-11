package org.jeecg.modules.pcms.project.stationvo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceHistoryPowerResult {

    private static final long serialVersionUID = 7522452492695971196L;

    @JsonProperty("error_code")
    private Long errorCode;

    @JsonProperty("error_msg")
    private String errorMsg;

    private DeviceHistoryPowerResult.PageData data;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData implements Serializable {

        private static final long serialVersionUID = 4395814471502926075L;

        @JsonProperty("next_page_start_id")
        private String nextPageStartId;

        private String sn;
        @JsonProperty("datalogger_sn")
        private String dataloggerSn;

        private List<DeviceHistoryPowerResult.PageData.HistoryDevice> datas;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HistoryDevice implements Serializable {

            private static final long serialVersionUID = -1727340659489324652L;

            private String vpv1;
            private String vpv3;
            @JsonProperty("total_energy")
            private String totalEnergy;
            private String vpv2;
            private String powerFactor;
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
            private Integer vac3;
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
            private String status;
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
            private Integer power;
            private String vString11;
            private String vString10;
            private String vString15;
            private String vString14;
            private String vString13;
            private String vString12;
            private String vString16;
            private Integer iac2;
            private String ppv;
            private String iac1;
            private String iac3;
            @JsonProperty("today_energy")
            private String todayEnergy;
            private String time;
        }

    }

}
