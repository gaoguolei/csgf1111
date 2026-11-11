package org.jeecg.modules.pcms.energy.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDataVo {


    String psKey;
    String formattedInstallDate;
    String installDate;
}
