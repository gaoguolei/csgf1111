package org.jeecg.modules.pcms.energy.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevicePointVo {

    //设备psKey
    private String psKey;

    //测点分钟类型  1：:5 分钟  2：15分钟  3：:30分钟 4：:60分钟
    private String pointTimeType;

    //测点值
    private String pointValue;
    //开始时间
    private String startDate;

    //结束时间
    private String endDate;

}
