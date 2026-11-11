package org.jeecg.modules.pcms.energy.dto.solar;

import lombok.Data;

import java.util.List;

@Data
public class SolarDeviceEquipmentPointDto {



    private String reqSerialNum;
    private String resultCode;
    private String resultMsg;
    private ResultData resultData;

    @Data
    public static class ResultData {
        private List<String> failPsKeyList;
        private List<DevicePointList> devicePointList;
    }

    @Data
    public static class DevicePointList {
        private DevicePoint devicePoint;
    }

    @Data
    public static class DevicePoint {
        // 设备名称
        private String deviceName;
        // 设备故障状态
        private Integer devFaultStatus;
        // PS密钥
        private String psKey;
        // 设备序列号
        private String deviceSn;
        // 设备状态
        private Integer devStatus;
        // PS标识
        private Integer psId;
        // 通信设备序列号
        private String communicationDevSn;
        // 唯一标识
        private Integer uuid;
        // ========================= 一、发电量相关 =========================
        /** 当日发电量，单位：Wh */
        private String p1;
        /** 当月发电量，单位：Wh */
        private String p87;
        /** 当年发电量，单位：Wh */
        private String p88;
        /** 总发电量，单位：Wh */
        private String p2;
        /** 日理论发电量，单位：Wh */
        private String p67;
        /** 总运行时间，单位：h */
        private String p7356;
        /** 总并网运行时间，单位：h */
        private String p3;

        // ========================= 二、功率与功率因数 =========================
        /** 总有功功率，单位：W */
        private String p24;
        /** 总无功功率，单位：var */
        private String p25;
        /** 总直流功率，单位：W */
        private String p14;
        /** 总视在功率，单位：VA */
        private String p43;
        /** 总功率因数，无单位 */
        private String p26;
        /** 限功率实际值，单位：W */
        private String p41;
        /** 无功调节实际值，单位：va */
        private String p42;

        // ========================= 三、电网电气参数 =========================
        /** A相电压，单位：V */
        private String p18;
        /** B相电压，单位：V */
        private String p19;
        /** C相电压，单位：V */
        private String p20;
        /** A相电流，单位：A */
        private String p21;
        /** B相电流，单位：A */
        private String p22;
        /** C相电流，单位：A */
        private String p23;
        /** A-B线电压，单位：V */
        private String p15;
        /** B-C线电压，单位：V */
        private String p16;
        /** C-A线电压，单位：V */
        private String p17;
        /** 电网频率，单位：Hz */
        private String p27;


        // ========================= 四、绝缘与阻抗 =========================
        /** 方阵绝缘阻抗，单位：kΩ */
        private String p94;
        /** 正极对地阻抗值，单位：Ω */
        private String p39;
        /** 负极对地阻抗值，单位：Ω */
        private String p40;
        /** 母线电压，单位：V */
        private String p95;
        /** 负极对地电压，单位：V */
        private String p90;

        // ========================= 五、故障与温度 =========================
        /** AFCI故障次数，无单位 */
        private String p120;
        /** 机内空气温度，单位：℃ */
        private String p4;
        /** 模块1温度，单位：℃ */
        private String p33;
        /** 模块2温度，单位：℃ */
        private String p34;
        /** 模块3温度，单位：℃ */
        private String p35;
        /** 模块4温度，单位：℃ */
        private String p36;
        /** 模块5温度，单位：℃ */
        private String p37;
        /** 模块6温度，单位：℃ */
        private String p38;

        // ========================= 六、MPPT电压（1-10路） =========================
        /** MPPT1电压，单位：V */
        private String p5;
        /** MPPT2电压，单位：V */
        private String p7;
        /** MPPT3电压，单位：V */
        private String p9;
        /** MPPT4电压，单位：V */
        private String p45;
        /** MPPT5电压，单位：V */
        private String p47;
        /** MPPT6电压，单位：V */
        private String p49;
        /** MPPT7电压，单位：V */
        private String p51;
        /** MPPT8电压，单位：V */
        private String p53;
        /** MPPT9电压，单位：V */
        private String p55;
        /** MPPT10电压，单位：V */
        private String p57;

        // ========================= 七、MPPT电流（1-10路） =========================
        /** MPPT1电流，单位：A */
        private String p6;
        /** MPPT2电流，单位：A */
        private String p8;
        /** MPPT3电流，单位：A */
        private String p10;
        /** MPPT4电流，单位：A */
        private String p46;
        /** MPPT5电流，单位：A */
        private String p48;
        /** MPPT6电流，单位：A */
        private String p50;
        /** MPPT7电流，单位：A */
        private String p52;
        /** MPPT8电流，单位：A */
        private String p54;
        /** MPPT9电流，单位：A */
        private String p56;
        /** MPPT10电流，单位：A */
        private String p58;


        // ========================= 八、组串电压（1-10路） =========================
        /** 组串1电压，单位：V */
        private String p96;
        /** 组串2电压，单位：V */
        private String p97;
        /** 组串3电压，单位：V */
        private String p98;
        /** 组串4电压，单位：V */
        private String p99;
        /** 组串5电压，单位：V */
        private String p100;
        /** 组串6电压，单位：V */
        private String p101;
        /** 组串7电压，单位：V */
        private String p102;
        /** 组串8电压，单位：V */
        private String p103;
        /** 组串9电压，单位：V */
        private String p104;
        /** 组串10电压，单位：V */
        private String p105;

        // ========================= 九、组串电流（1-10路） =========================
        /** 组串1电流，单位：A */
        private String p70;
        /** 组串2电流，单位：A */
        private String p71;
        /** 组串3电流，单位：A */
        private String p72;
        /** 组串4电流，单位：A */
        private String p73;
        /** 组串5电流，单位：A */
        private String p74;
        /** 组串6电流，单位：A */
        private String p75;
        /** 组串7电流，单位：A */
        private String p76;
        /** 组串8电流，单位：A */
        private String p77;
        /** 组串9电流，单位：A */
        private String p78;
        /** 组串10电流，单位：A */
        private String p79;
        // 设备时间（格式：yyyyMMddHHmmss）
        private String deviceTime;
    }








    }