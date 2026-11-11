package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

import java.util.List;


@Data
public class PowerDataResponseDto {
    private String errorMsg;
    private DataBeanReturn data;
    private int errorCode;


    @Data
    public static class DataBeanReturn {
        private int count;
        private List<PowersBean> powers;

        @Data
        public static class PowersBean {
            private String time;
            private String power;
        }
    }
}