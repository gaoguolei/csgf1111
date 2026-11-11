package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * 古瑞瓦特的电站概览数据对应的表
 */
@Data
@TableName("device_detail_growatt")
public class DeviceDetailGrowatt {

    //主键id
    @TableId(type = IdType.AUTO)
    private Integer id;
    //实际峰值功率（kW）
    public String peakPowerActual;
    //当月发电量(kWh)
    public String monthlyEnergy;
    //数据最后接收时间
    public String lastUpdateTime;
    //当前功率（kW）
    public String currentPower;
    //时区
    public String timezone;
    //当年发电量(kWh)
    public String yearlyEnergy;
    //当天发电量(kWh)
    public String todayEnergy;
    //等效减少二氧化碳排放量
    public String carbonOffset;
    public String efficiency;
    //累计发电量(kWh)
    public String totalEnergy;

    //实际峰值功率（kW）
    public Integer plantId;

    //数据来源   1：光伏云  2：古瑞瓦特
    public Integer source;







}
