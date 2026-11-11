package org.jeecg.modules.pcms.energy.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEquipmentPointVo {

    //设备key
    private String psKey;
    //投运时间
    private String gridConnectionDate;
    //格式化之后的投运时间
    private String connectionDate;
    //传入的截止时间节点  如果传了这个时间表示是从投运时间到这个时间点 内的所有数据
    private String endTime;
    //获取时间集合的类型
    private Integer type;
    private List<String> psKeyList;



}
