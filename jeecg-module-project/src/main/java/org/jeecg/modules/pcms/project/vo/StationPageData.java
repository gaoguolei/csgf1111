package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Author gongzhou
 * @Date 2023/10/24 下午9:58
 * @Version 1.5
 */
@Data
public class StationPageData {
    //电站其他数据
    private StationPageVo stationPageVo;

    //电站list
    private List<StatisticsVo> statisticsVos;

    //电站最近七天数据

    private List<StationCountVo> weekList;

    //电站最近6个月发电数据
    private List<StationCountVo> monthList;

}
