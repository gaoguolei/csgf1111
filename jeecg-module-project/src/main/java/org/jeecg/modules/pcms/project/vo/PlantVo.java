package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.StationListVo;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/9 上午8:53
 * @Version 1.5
 */
@Data
public class PlantVo {

    private List<StationListVo> plants;

    private Integer count;

}
