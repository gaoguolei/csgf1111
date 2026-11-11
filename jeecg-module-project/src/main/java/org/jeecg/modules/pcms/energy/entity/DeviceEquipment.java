package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("device_equipment")
public class DeviceEquipment {


    //主键id
    @TableId(type = IdType.AUTO)
    private Integer id;
    //设备通道id
    private Integer chnnlId;
    //设备的ps_key(查询设备数据需要用到)
    private String psKey;
    //设备SN
    private String deviceSn;
    //设备当前在线离线状态：0：离线，1：在线
    private String devStatus;
    //设备类型编码
    private Integer deviceType;
    //生产厂家名
    private String factoryName;
    //设备UUID
    private Long uuid;
    //
    private String gridConnectionDate;
    //设备当前故障状态设备故障状态：1：故障，2：告警，4：正常
    private Integer devFaultStatus;
    //设备认领状态：0：未认领，1：认领
    private Integer relState;
    //设备地址编码
    private Integer deviceCode;
    //电站ID
    private Long psId;
    //设备型号ID
    private Long deviceModelId;
    //设备对应的通信设备的SN
    private String communicationDevSn;
    //设备型号名称
    private String deviceModelCode;

    //设备名称
    private String deviceName;

    //古瑞瓦特的设备状态0：断开；1：在线；2：待机；3：故障。other：待机
    private String grwStatus;

    //来源  1：阳光云，2：古瑞瓦特
    private String source;


    @TableField(exist = false)
    private String plantName;

    @TableField(exist = false)
    private String dayPower;

    @TableField(exist = false)
    private String totalActivePower;

}
