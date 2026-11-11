package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.StatisticsVo;
import org.jeecg.modules.pcms.project.vo.StationListPageVo;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/27 上午10:10
 * @Version 1.5
 */
@Data
public class StationResultVo {

    private List<StationListPageVo> stationListPageVos;

}
