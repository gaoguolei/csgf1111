package org.jeecg.modules.pcms.energy.vo;

import lombok.Data;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointDayGrw;

import java.util.Map;
import java.util.Set;

@Data
public class DeviceEquipmentPointGrwFlat {


    private String psKey;
    private String type;
    private String psName;
    private String deviceEquipmentName;
    private String deviceEquipmentCode;
    private String deviceEquipmentStatus;
    private String deviceModelName;
    private Long psId;
    private String messageSN;

    private Map<String, DeviceEquipmentPointDayGrw> dayData; // key: timeStamp, value: 当天数据

    // 可以添加一些便捷方法
    public DeviceEquipmentPointDayGrw getDayData(Integer timeStamp) {
        return dayData.get(timeStamp);
    }

    public Set<String> getAllDates() {
        return dayData.keySet();
    }
}
