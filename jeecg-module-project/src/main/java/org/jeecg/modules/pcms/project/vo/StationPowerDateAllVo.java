package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/26 上午9:17
 * @Version 1.5
 */
@Data
public class StationPowerDateAllVo {
    //收益，发电量的统计
    private List<StationPowerDateVo> stationPowerDateVos;
}
