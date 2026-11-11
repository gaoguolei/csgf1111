package org.jeecg.modules.pcms.energy.dto.solar;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SolarPlantDetailDto {
    @JsonProperty("req_serial_num")
    public String reqSerialNum;

    @JsonProperty("result_code")
    public String resultCode;

    @JsonProperty("result_msg")
    public String resultMsg;

    @JsonProperty("result_data")
    public ResultData resultData;

    @Data
    public static class ResultData {
        @JsonProperty("code")
        public String code;

        @JsonProperty("dataList")
        public List<PlantInfo> dataList;
    }

    @Data
    public static class PlantInfo {
        @JsonProperty("design_capacity")
        public String designCapacity;

        @JsonProperty("alarm_count")
        public Integer alarmCount;

        @JsonProperty("ps_key")
        public String psKey;

        public String latitude;

        public String description;

        @JsonProperty("ps_remarks")
        public Map<String,String> psRemarks;

        @JsonProperty("ps_price_kwh")
        public String psPriceKwh;

        @JsonProperty("ps_fault_status")
        public Integer psFaultStatus;

        @JsonProperty("ps_type_name")
        public String psTypeName;

        @JsonProperty("build_status")
        public Integer buildStatus;

        @JsonProperty("install_date")
        public String installDate;

        @JsonProperty("ps_type")
        public Integer psType;

        public String email;

        public String longitude;

        @JsonProperty("param_income_unit_name")
        public String paramIncomeUnitName;

        @JsonProperty("ps_price")
        public String psPrice;

        @JsonProperty("ps_name")
        public String psName;

        @JsonProperty("share_user_type")
        public String shareUserType;

        @JsonProperty("ps_location")
        public String psLocation;

        @JsonProperty("share_type")
        public String shareType;

        @JsonProperty("ps_current_time_zone")
        public String psCurrentTimeZone;

        @JsonProperty("user_moble_tel")
        public String userMobleTel;

        @JsonProperty("ps_id")
        public Long psId;

        @JsonProperty("communication_dev_detail_list")
        public List<CommunicationDevDetail> communicationDevDetailList;

        @JsonProperty("connect_type")
        public Integer connectType;

        @JsonProperty("ps_status")
        public Integer psStatus;

        @JsonProperty("fault_count")
        public Integer faultCount;
    }

    @Data
    public static class CommunicationDevDetail {
        @JsonProperty("is_enable")
        public Integer isEnable;

        @JsonProperty("sn")
        public String sn;
    }
}
