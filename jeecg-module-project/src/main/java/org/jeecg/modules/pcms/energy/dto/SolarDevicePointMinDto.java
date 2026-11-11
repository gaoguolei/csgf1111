package org.jeecg.modules.pcms.energy.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SolarDevicePointMinDto {

    @SerializedName("req_serial_num")
    public String reqSerialNum;

    @SerializedName("result_code")
    public String resultCode;

    @SerializedName("result_msg")
    public String resultMsg;

    @SerializedName("result_data")
    public Map<String, List<SolarDevicePointMinDto.DataPoint>> resultData;

    @Data
    public static class DataPoint {
        //装机功率
        @SerializedName("p83019") public String p83019;
        //电站日发电量 Wh
        @SerializedName("p83022") public String p83022;
        //电站累计发电 Wh
        @SerializedName("p83024") public String p83024;
        //电站等效小时  h
        @SerializedName("p83025") public String p83025;
        //电站功率
        @SerializedName("p83033") public String p83033;

        @SerializedName("time_stamp")
        public String timeStamp;
    }

}
