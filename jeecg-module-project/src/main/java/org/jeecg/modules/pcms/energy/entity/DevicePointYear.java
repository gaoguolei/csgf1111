package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("device_point_year")
public class DevicePointYear {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String psKey;

    private String p83019;
    //电站日发电量 Wh
    private String p83022;
    //电站累计发电 Wh
    private String p83024;
    //电站等效小时  h
    private String p83025;
    //电站功率
    private String p83033;

    //时间
    private String timeStamp;
    //来源
    private Integer source;


    @TableField(exist = false)
    private String formatTime;

    //电站名称
    @TableField(exist = false)
    private String psName;

    //建站日期
    @TableField(exist = false)
    private String createTime;

    //并网日期
    @TableField(exist = false)
    private String gridTime;

    //电站地址
    @TableField(exist = false)
    private String psAddress;

    @TableField(exist = false)
    private String designCapacity;
}
