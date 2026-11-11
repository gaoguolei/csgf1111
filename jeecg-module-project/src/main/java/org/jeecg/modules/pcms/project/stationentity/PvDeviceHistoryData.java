package org.jeecg.modules.pcms.project.stationentity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/11 下午4:29
 * @Version 1.5
 */
@Data
@TableName("pv_device_history_data")
public class PvDeviceHistoryData {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String plantId;
    private String dataloggerSn;
    private String sn;
    private String status;
    private String ipv1;
    private String ipv2;
    private String ipv3;
    private String ipv4;
    private String ipv5;
    private String ipv6;
    private String ipv7;
    private String ipv8;
    private String vpv1;
    private String vpv2;
    private String vpv3;
    private String vpv4;
    private String vpv5;
    private String vpv6;
    private String vpv7;
    private String vpv8;
    private String iac1;
    private String iac2;
    private String iac3;
    private String vac1;
    private String vac2;
    private String vac3;
    private String power;
    private String todayEnergy;
    private String totalEnergy;
    //温度
    private String temperature;
    private String fac;
    private String powerFactor;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    private Integer year;

    private Integer month;
    private Integer day;


}
