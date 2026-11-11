package org.jeecg.modules.pcms.energy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;


@Data
@TableName("device_detail_solar")
public class DeviceDetailSolar {

    //主键id
    @TableId(type = IdType.AUTO)
    private Integer id;

    //装机功率，单位为Wp
    public String designCapacity;

    //告警数量
    public Integer alarmCount;

    //电站key
    public String psKey;

    //纬度
    public String latitude;

    //电站简介
    public String description;

    //电站备注
    public String psRemarks;

    //电站当前每kWh的电价
    public String psPriceKwh;

    //电站故障状态 1：故障，2：告警，4：正常
    public Integer psFaultStatus;

    //电站类型名称
    public String psTypeName;

    //电站建设状态 0：未建，1：在建 2：并网，3：拟建，4：未接入
    public Integer buildStatus;

    //建站日期
    public String installDate;

    //电站类型：1：地面电站 3：分布式光伏 4：户用光伏 5：户用储能 6：村级电站 7：分布式储能 8：扶贫电站 9：风能电站 12：工商业储能
    public Integer psType;

    //业主用户的邮箱
    public String email;

    //经度
    public String longitude;

    //电价单位
    public String paramIncomeUnitName;

    //电站当前每Wh的电价
    public String psPrice;

    //电站名称
    public String psName;

    //电站的分享人类型：1：业主分享2：安装商分享 其他：非分享电站
    public String shareUserType;


    public String psLocation;

    //电站的分享类型 1：分享类型（浏览权限）2：分享类型（管理权限）0：非分享电站（本人电站）
    public String shareType;

    //电站当前时区
    public String psCurrentTimeZone;

    //业主用户手机号码
    public String userMobleTel;

    @JsonProperty("ps_id")
    public Long psId;

    //电站的通信设备SN信息
    public String communicationDevDetailList;

    //并网类型：1：全额上网 2：自发自用，余电上网 3：自发自用，无馈网 4：离网
    public Integer connectType;

    //电站状态 1：在线，0：离线
    public Integer psStatus;

    //故障数量
    public Integer faultCount;

    public Date createdAt;
}
