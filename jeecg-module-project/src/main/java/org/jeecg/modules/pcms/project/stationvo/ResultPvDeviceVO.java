package org.jeecg.modules.pcms.project.stationvo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/26 下午5:24
 * @Version 1.5
 */
@Data
public class ResultPvDeviceVO {




    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;
    private String todayEnergy;
    private Integer year;
    private Integer month;
    private Integer day;

}
