package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

@Data
public class LastUpdateTimeVo {

    private String time;

    private String minutes;
    private String seconds;
    private String hours;
    private String month;
    private String year;
    private String timezoneOffset;
    private String day;
    private Integer date;


}

