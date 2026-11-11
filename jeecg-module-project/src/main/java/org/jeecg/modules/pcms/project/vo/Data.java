package org.jeecg.modules.pcms.project.vo;

import org.jeecg.modules.pcms.project.stationvo.StationListVo;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/9/18 上午9:32
 * @Version 1.5
 */
@lombok.Data
public class Data {

    private Integer count;

    private List<StationListVo> stationListVos;
}
