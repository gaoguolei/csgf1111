package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@TableName("device")
public class Device {


    //主键id

    @TableId(type = IdType.AUTO)
    private Integer id;
    //电站id（两厂商同有字段）
    private Integer deviceId;
    //电站名称
    private String deviceName;
    // 告警数量
    private int alarmCount;
    //经度
    private Double latitude;
    //纬度
    private Double longitude;
    //省
    private String provinceName;
    //市
    private String cityName;
    //区
    private String districtName;

    //电站简介
    private String description;
    //电站状态，1：正常，2：停用，3：接入中
    private int validFlag;
    //当前功率
    private String currPower;
    //电站故障状态，1：故障，2：告警，3 :正常
    private int psFaultStatus;

    //建站时间 格式：yyyy-MM-dd HH:mm:ss
    private String installDate;

    //电站建设状态 0：未建，1：在建 2：并网，3：拟建，4：未接入
    private int buildStatus;
    //电站类型：（多个使用英文逗号隔开传入，默认查询所有） 1：地面电站 3：分布式光伏 4：户用光伏 5：户用储能 6：村级电站 7：分布式储能 8：扶贫电站 9：风能电站 10：地面储能电站 12：工商业储能电站
    private int psType;
    //累计CO₂减排
    private String co2ReduceTotal;
    private String todayIncome;
    //并网状态 1-已并网 0-未并网
    private int gridConnectionStatus;
    //今日等效小时
    private String equivalentHour;
    //电站位置
    private String psLocation;
    //累计收益<value,unit>（当电站为储能类型时，收益为储能收益）
    private String totalIncome;
    //年收益
    private String yearIncome;
    //月收益
    private String monthIncome;
    //总装机量
    private String totalCapcity;
    //电站的分享类型 1：分享类型（浏览权限） 2：分享类型（管理权限） 0：非分享电站（本人电站）
    private String shareType;
    //电站当前时区
    private String psCurrentTimeZone;
    //并网时间
    private String gridConnectionTime;
    //并网类型：1：全额上网 2：自发自用，余电上网 3：自发自用，无馈网 4：离网
    private int connectType;
    //今日发电量
    private String todayEnergy;
    //今日发电量单位
    private String todayEnergyUnit;
    //总发电量
    private String totalEnergy;
    //总发电量单位
    private String totalEnergyUnit;

    //项目类型
    private String projectType;

    //电站状态（阳光云） 1：在线，0：离线    （古瑞瓦特）1正常，3异常  -1：离线
    private int psStatus;
    //今日CO₂减排
    private String co2Reduce;
    //故障数量
    private int faultCount;

    //来源  1：阳光云，2：古瑞瓦特
    private int source;

    //是否被关注：0：未关注，1：已关注
    private String isConcern;



    //安装商（古瑞瓦特专用字段）
    private String installer;


    @TableField(exist = false)
    private String monthEnergy;
    @TableField(exist = false)
    private String yearEnergy;


    @TableField(exist = false)
    Map<Integer, List<DeviceEquipment>> integerListMap;

    @TableField(exist = false)
    List<DeviceFault> deviceFaults;

    @TableField(exist = false)
    private String psKey;

    @TableField(exist = false)
    private List<String> equipmentSnList;

}
