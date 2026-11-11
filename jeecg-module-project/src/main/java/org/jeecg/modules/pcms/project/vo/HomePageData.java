package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/4/20 上午9:53
 * @Version 1.5
 */
@Data
public class HomePageData {
    /**
     * 用户签约
     */
    //累计签约数
    private  Integer signUp;

    //累计签约容量
    private BigDecimal signLoad;

    //今日签约户数
    private Integer signToday;

    //今日签约容量
    private BigDecimal signTodayLoad;

    /**
     * 施工进场
     */
    //累计完成户数
    private Integer complete;

    //累计完成容量
    private BigDecimal completeLoad;

    //今日完成户数
    private Integer completeToday;

    //今日完成容量
    private BigDecimal completeTodayLoad;

    /**
     *电网流程申报
     */

    //累计申报户数
    private Integer powerGrid;

    //累计申报容量
    private BigDecimal powerGridLoad;

    //今日申报户数
    private Integer powerGridRToday;

    //今日申报容量
    private BigDecimal powerGridLoadToday;

    //申报完成数
    private Integer powerComplete;

    //申报完成容量
    private BigDecimal powerCompleteLoad;

    /**
     * 并网
     */
    //累计完成户数
    private Integer onGrid;


    //累计完成容量
    private BigDecimal onGridLoad;


    //今日完成户数
    private Integer onGridToday;

    //今日完成容量
    private  BigDecimal onGridLoadToday;



    //用户签约合计完成（容量）
    private BigDecimal signTotal;

    //施工进场合计完成（容量）
    private BigDecimal workTotal;

    //电网申报合计完成（容量）
    private BigDecimal powerTotal;
    //并网合计完成（容量）
    private BigDecimal onGridTotal;

    //签约户数签约容量图表（展示最近6个月的数据）
    private List<ChartVo> chartVos;

}

