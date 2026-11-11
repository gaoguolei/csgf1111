package org.jeecg.modules.pcms.project.stationvo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:56
 * @Version 1.5
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryAlarmResultVo {

    private static final long serialVersionUID = 7529452792695971196L;

    @JsonProperty("error_code")
    private Long errorCode;

    @JsonProperty("error_msg")
    private String errorMsg;

    private HistoryAlarmResultVo.PageData data;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData implements Serializable {

        private static final long serialVersionUID = 4395854401502926075L;
        private String count;
        private String sn;
        private List<Ararm> alarms;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Ararm implements Serializable {

            private static final long serialVersionUID = -1727340639289324652L;

            @JsonProperty("alarm_code")
            private String alarmCode;
            private String status;
            @JsonProperty("end_time")
            private String endTime;
            @JsonProperty("start_time")
            private String startTime;
            @JsonProperty("alarm_message")
            private String alarmMessage;
            @JsonProperty("fault_type")
            private String faultType;

        }

    }
}
