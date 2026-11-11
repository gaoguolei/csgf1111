package org.jeecg.modules.pcms.project.stationvo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午6:53
 * @Version 1.5
 */

@Data
public class StationAlarmVo {

    //报警等级
    private String grade;
    //电站id
    private Integer plantId;
    //电站名称
    private String plantName;
    //逆变器sn
    private String sn;
    //报警内容
    private String alarmMessage;
    //报警代码
    private String alarmCode;
    //建议处理方式
    private String method;
    //报警时长
    private BigDecimal alarmHour;
    //报警时间
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTime;
    //恢复时间
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date restoreTime;
}
