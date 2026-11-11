package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/10 上午8:51
 * @Version 1.5
 */
@Data
public class DeviceListPageVo {


    //状态
    private String status;
    //逆变器sn
    private String sn;
    //额定功率
    private BigDecimal ratedPower;
    //实时功率
    private BigDecimal timePower;
    //当日电量
    private BigDecimal dayPower;
    //累计电量
    private BigDecimal totalPower;
    //所属电站
    private String plantName;
    //质保到期时间
    private Date endTime;
    //更新时间
    private Date updateTime;
    //总数
    private Integer count;
}
