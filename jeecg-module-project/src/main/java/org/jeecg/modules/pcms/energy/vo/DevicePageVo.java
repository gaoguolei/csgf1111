package org.jeecg.modules.pcms.energy.vo;


import lombok.Data;

import java.util.List;

@Data
public class DevicePageVo {

    private Integer pageNo;

    private Integer pageSize;

    //1：全部电站，2：设备型号
    private Integer equipmentRequestType;

    //电站 类型
    private String deviceType;

    //电站名称或者sn码
    private String deviceNameOrSn;

    //等效小时最小
    private String equivalentHourMin;
    //等效小时最大
    private String equivalentHourMax;

    //关注
    private String concern;

    //电站故障状态：1-故障,2-告警,3-正常
    private String psFaultStatus;


    //离线
    private String psStatus;

    //接入中
    private String validFlag;

    private String tabType;

    private String plantId;

    //设备类型  1:逆变器 22：通讯设备
    private String equipmentType;

    //设备状态 1：故障，2：告警，4：正常
    private String equipmentStatus;

    //设备名称或者sn号码
    private String equipmentNameOrSn;

    //设备型号
    private String equipmentModelName;

    //告警名称
    private String faultName;

    //故障码
    private String faultCode;

    //故障码
    private String faultReason;

    //地区
    private String address;

    //设备sn码
    private String deviceEquipmentSn;


    //查询来源 1：阳光云  2：古瑞瓦特
    private Integer source;

    private Integer psSource;


    private String devicePsKey;

    private String deviceId;

    //设备psKey
    private String startDate;

    //设备psKey
    private String endDate;

    //未处理还是历史数据
    private String status;

    //设备psKey
    private List<String> psKey;

    //设备psKey
    private String psKeyOne;

    //取消选中的
    private List<String> notInPsKey;

    //查询的分钟类型  5 15 30 60
    private String minuteInterval;

    //分钟类型
    private String minType;

    //查询日期
    private String queryDate;

    //是否全选
    private String isAll;

    private String queryType;

    //派单号
    private String dispatchNo;

    //故障类型
    private String faultType;

    //故障类型
    private String dispatchType;

    //故障类型
    private String dispatchStatus;

    private String renterId;

    private String taskId;




}
