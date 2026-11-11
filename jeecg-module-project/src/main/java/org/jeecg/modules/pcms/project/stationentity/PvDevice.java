package org.jeecg.modules.pcms.project.stationentity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.LastUpdateTimeVo;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("pv_device")
public class PvDevice {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String plantId;

    private String deviceSn;

    private String serialNum;

    private String dataloggerSn;

    private String lost;

    private Integer status;
    private String alias;

    private String location;
    private String dataLogSn;

    private String nominalPower;
    private String lastUpdateTime;

    @TableField(exist = false)
    private LastUpdateTimeVo lastUpdateTimeVo;

    private String tcpServerIp;
    private String fwVersion;



    private String updateStatus;


    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;


    private Integer tenantId;


    //逆变器sn
    @TableField(exist = false)
    private String sn;
    //额定功率
    @TableField(exist = false)
    private BigDecimal ratedPower;
    //实时功率
    @TableField(exist = false)
    private BigDecimal timePower;
    //当日电量
    @TableField(exist = false)
    private BigDecimal dayPower;
    //累计电量
    @TableField(exist = false)
    private BigDecimal totalPower;
    //所属电站
    @TableField(exist = false)
    private String plantName;
    //质保到期时间
    @TableField(exist = false)
    private Date endTime;
    //总数
    @TableField(exist = false)
    private Integer count;

}
