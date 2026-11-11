package org.jeecg.modules.pcms.project.stationentity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecg.modules.pcms.project.stationvo.StationReportVo;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("pv_stations")
public class PvStations {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer plantId;

    private String name;

    private Integer userId;

    private String country;

    private String longitude;

    private String latitude;

    private String peakPower;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    private String installer;

    private String currentPower;

    private String totalEnergy;

    private String latitudeD;

    private String latitudeF;

    private Integer tenantId;

    private String status;


    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;


    //逆变器sn
    @TableField(exist = false)
    private String sn;
    //额定功率
    @TableField(exist = false)
    private BigDecimal ratedPower;
    //实时功率
    @TableField(exist = false)
    private BigDecimal timePower;
    //当日电量
    @TableField(exist = false)
    private BigDecimal dayPower;
    //累计电量
    @TableField(exist = false)
    private BigDecimal totalPower;
    //所属电站
    @TableField(exist = false)
    private String plantName;
    //质保到期时间
    @TableField(exist = false)
    private Date endTime;
    //总数
    @TableField(exist = false)
    private Integer count;
    //逆变器数量
    @TableField(exist = false)
    private String deviceNum;

    //当日电量(kwh)
    @TableField(exist = false)
    private String totalTodayPower;
    //累计电量(Mwh)
    @TableField(exist = false)
    private String totalEfficiency;
    //日发满小时数
    @TableField(exist = false)
    private String dayHour;
    //实时功率
    @TableField(exist = false)
    private String nowPower;
    //组件总容量
    @TableField(exist = false)
    private String incomeVolume;


    //业主
    @TableField(exist = false)
    private String userName;
    //电站联系人
    @TableField(exist = false)
    private String phone;
    //已分配组织
    @TableField(exist = false)
    private String stage;
    //组件数量
    @TableField(exist = false)
    private Integer assemblyNum;
    //每度电收益
    @TableField(exist = false)
    private BigDecimal income;
    //日发电量
    @TableField(exist = false)
    private BigDecimal dayNum;
    //月发满小时
    @TableField(exist = false)
    private BigDecimal monthPower;
    //月发电量
    @TableField(exist = false)
    private BigDecimal monthNum;
    //年发满小时
    @TableField(exist = false)
    private BigDecimal yearPower;
    //年发电量
    @TableField(exist = false)
    private BigDecimal yearNum;
    //年收益
    @TableField(exist = false)
    private BigDecimal yearIncome;
    //累计
    @TableField(exist = false)
    private BigDecimal power;
    //累计
    @TableField(exist = false)
    private BigDecimal num;
    //累计收益
    @TableField(exist = false)
    private BigDecimal totalIncome;
    //报表数据
    @TableField(exist = false)
    private List<PvEnergyVo> energyVos;

    @TableField(exist = false)
    private BigDecimal totalPowerNum;

}
