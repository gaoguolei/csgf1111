package org.jeecg.modules.pcms.project.stationvo;


import lombok.Data;

@Data
public class DeviceInfo {

    //设备在线与否状态(0:在线， 1:断开)
    private String lost;
    //设备状态，status(0：等待，1：正常，3：故障）)
    private Integer status;
    //别名
    private String alias;
    //地址
    private String location;
    //所属采集器序列号
    private String dataLogSn;
    //额定功率
    private String nominalPower;
    //最后更新时间
//    private String lastUpdateTime;
    private LastUpdateTimeVo lastUpdateTime;
    //服务器地址
    private String tcpServerIp;
    //逆变器版本
    private String fwVersion;
}
