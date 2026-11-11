package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;
import org.jeecg.modules.pcms.project.stationentity.PvStationsDetails;

@Data
public class StationsVo {

    private String errorCode;


    private String errorMsg;

    private PvStationsDetails data;

}
