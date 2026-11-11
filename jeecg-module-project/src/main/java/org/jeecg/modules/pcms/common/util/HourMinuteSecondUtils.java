package org.jeecg.modules.pcms.common.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/3/27 下午12:57
 * @Version 1.5
 */
public class HourMinuteSecondUtils {
    public static Date setHourMinuteSecond(Date date, int hour, int minute, int second,int mill){
        if(null == date){
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, mill);
        return calendar.getTime();
    }
}
