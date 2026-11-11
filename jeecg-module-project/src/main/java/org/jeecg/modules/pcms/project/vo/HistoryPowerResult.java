package org.jeecg.modules.pcms.project.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.experimental.Accessors;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:19
 * @Version 1.5
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryPowerResult implements Serializable{

    private static final long serialVersionUID = 7529452792695971196L;

    @JsonProperty("error_code")
    private Long errorCode;

    @JsonProperty("error_msg")
    private String errorMsg;

    private PageData data;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData implements Serializable {

        private static final long serialVersionUID = 4395854401502926075L;

        private Long count;

        @JsonProperty("time_unit")
        private String timeUnit;

        private List<Energys> energys;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Energys implements Serializable {

            private static final long serialVersionUID = -1727340639289324652L;

            private LocalDate date;

            private BigDecimal energy;

        }

    }
}
