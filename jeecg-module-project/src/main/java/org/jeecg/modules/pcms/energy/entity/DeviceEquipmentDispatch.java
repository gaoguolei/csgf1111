package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;


/**
 * 工单表
 */
@Data
@TableName("device_equipment_dispatch")
public class DeviceEquipmentDispatch {

    //主键id
    @TableId(type = IdType.AUTO)
    private Long id;

    //派单号
    private String dispatchNo;


    //生成类型1：自动生成（异常工单）；2：手动添加
    private String dispatchType;

    //电站id
    private String deviceId;

    //'故障编码'
    private String faultCode;
    //'故障类型'
    private String faultType;
    //'故障名称'
    private String faultName;
    //'故障时间'
    private String faultTime;
    //派单服务厂商  1：阳光云；2：古瑞瓦特
    private String source;
    //'服务人员'
    private String userId;
    //工单状态0：待分配 ；1：已分配；3：已完成
    private String dispatchStatus;
    //'维修时间'
    private Date repairTime;
    //'维修时间'
    private Date createTime;
    //'更新时间'
    private Date updateTime;
    //所属项目
    private String renterId;

    @TableField(exist = false)
    private String userName;

    //电站名称
    private String psName;

    //地址
    @TableField(exist = false)
    private String address;

}
