package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

import java.util.List;


@Data
public class PvDataloggerVo {

    private Integer count;

    private List<PvDataloggerRuselt> dataloggers;

}
