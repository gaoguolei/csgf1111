package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class DeviceEquipmentPointDayGrw {

    //主键id
    @TableId(type = IdType.AUTO)
    private Long id;

    // device_sn
    private String deviceSn;

    private String dataloggerSn;
    //时间
    private String timeStamp;

    //发电量
    private String p1;

}