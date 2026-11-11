package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 古瑞瓦特电站的采集器
 */
@Data
@TableName("device_gather_grw")
public class DeviceGatherGrw {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String lost;

    //采集器类型
    private String model;

    //采集器SN
    private String sn;

    //
    private String type;

    //
    private String netmode;


    //采集器厂家
    private String manufacturer;

    private Integer psId;



    @TableField(exist = false)
    private String plantName;


    //设备名称
    @TableField(exist = false)
    private String equipmentName;

    @TableField(exist = false)
    private String source;






}
