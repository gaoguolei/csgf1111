package org.jeecg.modules.pcms.energy.vo;


import lombok.Data;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMinSixty;

import java.util.Map;
import java.util.Set;

@Data
public class DeviceEquipmentPointMinSixtyFlat {

    private String psKey;
    private String type;
    private String psName;
    private String deviceEquipmentName;
    private String deviceEquipmentCode;
    private String deviceEquipmentStatus;
    private String deviceModelName;
    private Long psId;
    private String messageSN;

    private Map<String, DeviceEquipmentPointMinSixty> dayData; // key: timeStamp, value: 当天数据

    // 可以添加一些便捷方法
    public DeviceEquipmentPointMinSixty getDayData(Integer timeStamp) {
        return dayData.get(timeStamp);
    }

    public Set<String> getAllDates() {
        return dayData.keySet();
    }
}
