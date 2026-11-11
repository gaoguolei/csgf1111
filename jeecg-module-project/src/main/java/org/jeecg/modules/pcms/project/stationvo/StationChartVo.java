package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/1 下午6:43
 * @Version 1.5
 */
@Data
public class StationChartVo {

    private List<PvEnergyVo> pvEnergyVos;

    private StationCountVo stationCountVo;
}
