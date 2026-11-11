package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/10/9 上午10:20
 * @Version 1.5
 */
@Data
public class PowerVo {

    private  String  peakPowerActual;

    private String lastUpdateTime;

    private String currentPower;

    private String todayEnergy;

    private String monthlyEnergy;

    private String yearlyEnergy;

    private String totalEnergy;

    private String carbonOffset;

    private String timezone;


}
