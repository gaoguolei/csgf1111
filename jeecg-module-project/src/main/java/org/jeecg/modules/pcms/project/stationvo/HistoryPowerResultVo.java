package org.jeecg.modules.pcms.project.stationvo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午7:25
 * @Version 1.5
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryPowerResultVo {

    private static final long serialVersionUID = 7529452792695971196L;

    @JsonProperty("error_code")
    private Long errorCode;

    @JsonProperty("error_msg")
    private String errorMsg;

    private HistoryPowerResultVo.PageData data;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData implements Serializable {

        private static final long serialVersionUID = 4395854401502926075L;

        private Long count;


        private List<Energys> powers;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Energys implements Serializable {

            private static final long serialVersionUID = -1727340639289324652L;

            private String time;

            private BigDecimal power;

        }

    }


}
