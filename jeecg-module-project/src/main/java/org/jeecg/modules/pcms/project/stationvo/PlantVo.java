package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

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
