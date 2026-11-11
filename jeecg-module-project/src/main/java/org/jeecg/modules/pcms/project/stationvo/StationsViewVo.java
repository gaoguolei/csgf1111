package org.jeecg.modules.pcms.project.stationvo;


import lombok.Data;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;

@Data
public class StationsViewVo {

    private String errorCode;


    private String errorMsg;


    private PvStationsView data;


}
