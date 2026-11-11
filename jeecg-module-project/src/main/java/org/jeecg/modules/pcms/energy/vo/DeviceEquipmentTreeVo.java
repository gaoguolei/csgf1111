package org.jeecg.modules.pcms.energy.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
public class DeviceEquipmentTreeVo {

    private String name;        // 节点名称
    private String type;        // 节点类型：station, category, inverter, communication
    private List<DeviceEquipmentTreeVo> children; // 子节点

    // 构造方法
    public DeviceEquipmentTreeVo(String name, String type) {
        this.name = name;
        this.type = type;
        this.children = new ArrayList<>();
    }

    // 添加子节点
    public void addChild(DeviceEquipmentTreeVo child) {
        this.children.add(child);
    }


}
