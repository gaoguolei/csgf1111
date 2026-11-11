package org.jeecg.modules.pcms.project.stationentity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;


/**
 * 电站概览
 */
@Data
@TableName("pv_stations_view")
public class PvStationsView {

    @TableId(type = IdType.AUTO)
    private Integer id;

    //电站id
    private Integer plantId;
    //实际峰值功率（kW）(组件容量)
    private String peakPowerActual;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;
    //实时功率
    private String currentPower;
    //当天发电量(kWh)
    private String todayEnergy;
    //当月发电量(kWh)
    private String monthlyEnergy;
    //当年发电量(kWh)
    private String yearlyEnergy;
    //累计发电量(kWh)
    private String totalEnergy;
    //等效减少二氧化碳排放量
    private String carbonOffset;
    //电站地址
    @TableField(exist = false)
    private String address;
    //电站图片轮播图
    @TableField(exist = false)
    private List<String> imgUrl;
    @TableField(exist = false)
    //经度
    private String longitude;
    @TableField(exist = false)
    //维度
    private String latitude;
    //电站名称
    @TableField(exist = false)
    private String name;

    private String timezone;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Integer tenantId;
    private String status;

}
