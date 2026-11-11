package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:07
 * @Version 1.5
 */
@Data
@TableName("pv_device_alarm")
@ApiModel(value = "pv_device_alarm对象", description = "pv_device_alarm")
public class PvDeviceAlarm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sn;

    private String plantName;

    private Integer plantId;

    private String alarmCode;

    private String alarmMessage;

    private String startTime;

    private String endTime;

    private String status;

    private BigDecimal alarmHour;

    @TableField(exist = false)
    private String hour;

    @TableField(exist = false)
    private String method;

}
