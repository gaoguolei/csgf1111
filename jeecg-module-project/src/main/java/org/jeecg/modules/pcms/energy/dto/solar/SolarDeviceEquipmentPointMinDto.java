package org.jeecg.modules.pcms.energy.dto.solar;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SolarDeviceEquipmentPointMinDto {
    @SerializedName("req_serial_num")
    public String reqSerialNum;

    @SerializedName("result_code")
    public String resultCode;

    @SerializedName("result_msg")
    public String resultMsg;

    @SerializedName("result_data")
    public Map<String, List<DataPoint>> resultData;



    @Data
    public static class DataPoint {
        @SerializedName("p1") public String p1;
        @SerializedName("p2") public String p2;
        @SerializedName("p3") public String p3;
        @SerializedName("p4") public String p4;
        @SerializedName("p5") public String p5;
        @SerializedName("p6") public String p6;
        @SerializedName("p7") public String p7;
        @SerializedName("p8") public String p8;
        @SerializedName("p9") public String p9;
        @SerializedName("p10") public String p10;
        @SerializedName("p14") public String p14;
        @SerializedName("p18") public String p18;
        @SerializedName("p19") public String p19;
        @SerializedName("p20") public String p20;
        @SerializedName("p21") public String p21;
        @SerializedName("p22") public String p22;
        @SerializedName("p23") public String p23;
        @SerializedName("p24") public String p24;
        @SerializedName("p25") public String p25;
        @SerializedName("p26") public String p26;
        @SerializedName("p27") public String p27;
        @SerializedName("p43") public String p43;
        @SerializedName("p45") public String p45;
        @SerializedName("p46") public String p46;
        @SerializedName("p70") public String p70;
        @SerializedName("p71") public String p71;
        @SerializedName("p72") public String p72;
        @SerializedName("p73") public String p73;
        @SerializedName("p74") public String p74;
        @SerializedName("p75") public String p75;
        @SerializedName("p76") public String p76;
        @SerializedName("p77") public String p77;
        @SerializedName("p87") public String p87;
        @SerializedName("p88") public String p88;
        @SerializedName("p90") public String p90;
        @SerializedName("p94") public String p94;
        @SerializedName("p95") public String p95;
        @SerializedName("p96") public String p96;
        @SerializedName("p97") public String p97;
        @SerializedName("p98") public String p98;
        @SerializedName("p99") public String p99;
        @SerializedName("p100") public String p100;
        @SerializedName("p101") public String p101;
        @SerializedName("p102") public String p102;
        @SerializedName("p103") public String p103;

        @SerializedName("time_stamp")
        public String timeStamp;
    }
}