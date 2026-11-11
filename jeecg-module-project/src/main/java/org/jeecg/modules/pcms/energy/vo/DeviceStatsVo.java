package org.jeecg.modules.pcms.energy.vo;


import lombok.Data;

@Data
public class DeviceStatsVo {
    private String monthlyEnergy;
    private String yearlyEnergy;
    private String todayEnergy;
}
