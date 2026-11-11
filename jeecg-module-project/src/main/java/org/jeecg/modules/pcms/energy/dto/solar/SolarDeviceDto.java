package org.jeecg.modules.pcms.energy.dto.solar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SolarDeviceDto {
    @JsonProperty("req_serial_num")
    private String reqSerialNum;

    @JsonProperty("result_code")
    private String resultCode;

    @JsonProperty("result_msg")
    private String resultMsg;

    @JsonProperty("result_data")
    private ResultData resultData;

    /**
     * 结果数据内部类
     */
    @Data
    public static class ResultData {
        @JsonProperty("pageList")
        private List<PageItem> pageList;

        @JsonProperty("rowCount")
        private int rowCount;
    }

    /**
     * 分页数据项内部类
     */
    @Data
    public static class PageItem {
        @JsonProperty("total_energy")
        private EnergyData totalEnergy;

        @JsonProperty("alarm_count")
        private int alarmCount;

        @JsonProperty("latitude")
        private double latitude;

        @JsonProperty("description")
        private String description;

        @JsonProperty("total_income_update_time")
        private String totalIncomeUpdateTime;

        @JsonProperty("valid_flag")
        private int validFlag;

        @JsonProperty("curr_power")
        private EnergyData currPower;

        @JsonProperty("ps_fault_status")
        private int psFaultStatus;

        @JsonProperty("district_name")
        private String districtName;

        @JsonProperty("co2_reduce_update_time")
        private String co2ReduceUpdateTime;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("install_date")
        private String installDate;

        @JsonProperty("build_status")
        private int buildStatus;

        @JsonProperty("today_energy_update_time")
        private String todayEnergyUpdateTime;

        @JsonProperty("year_income_update_time")
        private String yearIncomeUpdateTime;

        @JsonProperty("total_energy_update_time")
        private String totalEnergyUpdateTime;

        @JsonProperty("ps_type")
        private int psType;

        @JsonProperty("longitude")
        private double longitude;

        @JsonProperty("total_capcity_update_time")
        private String totalCapcityUpdateTime;

        @JsonProperty("equivalent_hour_update_time")
        private String equivalentHourUpdateTime;

        @JsonProperty("ps_name")
        private String psName;

        @JsonProperty("co2_reduce_total")
        private EnergyData co2ReduceTotal;

        @JsonProperty("curr_power_update_time")
        private String currPowerUpdateTime;

        @JsonProperty("today_income")
        private EnergyData todayIncome;

        @JsonProperty("grid_connection_status")
        private int gridConnectionStatus;

        @JsonProperty("month_income_update_time")
        private String monthIncomeUpdateTime;

        @JsonProperty("equivalent_hour")
        private EnergyData equivalentHour;

        @JsonProperty("co2_reduce_total_update_time")
        private String co2ReduceTotalUpdateTime;

        @JsonProperty("province_name")
        private String provinceName;

        @JsonProperty("ps_location")
        private String psLocation;

        @JsonProperty("total_income")
        private EnergyData totalIncome;

        @JsonProperty("total_capcity")
        private EnergyData totalCapcity;

        @JsonProperty("share_type")
        private String shareType;

        @JsonProperty("year_income")
        private EnergyData yearIncome;

        @JsonProperty("ps_current_time_zone")
        private String psCurrentTimeZone;

        @JsonProperty("today_income_update_time")
        private String todayIncomeUpdateTime;

        @JsonProperty("ps_id")
        private int psId;

        @JsonProperty("grid_connection_time")
        private Long gridConnectionTime;

        @JsonProperty("month_income")
        private EnergyData monthIncome;

        @JsonProperty("connect_type")
        private int connectType;

        @JsonProperty("today_energy")
        private EnergyData todayEnergy;

        @JsonProperty("ps_status")
        private int psStatus;

        @JsonProperty("co2_reduce")
        private EnergyData co2Reduce;

        @JsonProperty("fault_count")
        private int faultCount;
    }

    /**
     * 能源数据通用内部类（包含单位和数值）
     */
    @Data
    public static class EnergyData {
        @JsonProperty("unit")
        private String unit;

        @JsonProperty("value")
        private String value;
    }
}



