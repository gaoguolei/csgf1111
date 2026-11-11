package org.jeecg.modules.pcms.energy.vo;


import lombok.Data;

@Data
public class PsInfoVo {
    //建站日期
    private String createTime;

    //并网日期
    private String gridTime;

    //地址
    private String address;

    //电站名称
    private String psName;

    //时间
    private String date;

    //装机功率
    private String designCapacity;

    //总发电量
    private String totalEnergy;

    //总发电量单位
    private String totalEnergyUnit;

    //设备sn(设备序列号)
    private String deviceSn;

    private String communicationDevSn;

    private String deviceModelCode;

    private String deviceStatus;
    private String psKey;



}
