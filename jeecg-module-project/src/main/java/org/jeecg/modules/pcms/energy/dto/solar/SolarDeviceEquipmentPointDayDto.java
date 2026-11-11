package org.jeecg.modules.pcms.energy.dto.solar;


import lombok.Data;

import java.util.*;

@Data
public class SolarDeviceEquipmentPointDayDto {
    public String req_serial_num;
    public String result_code;
    public String result_msg;
    public Map<String, Map<String, List<Map<String, String>>>> result_data;
}
