package org.jeecg.modules.pcms.project.stationentity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 电站详情
 */

@Data
@TableName("pv_stations_details")
public class PvStationsDetails {


    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer plantId;
    private String name;
    private Integer userId;
    private String country;
    private String longitude;
    private String latitude;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;
    private String imageUrl;
    private String peakPower;
    private String currency;
    private String timezone;
    private Integer plantType;
    private String address1;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Integer tenantId;
    private String status;

}
