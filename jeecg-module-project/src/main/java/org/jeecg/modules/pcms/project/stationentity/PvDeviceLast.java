package org.jeecg.modules.pcms.project.stationentity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;


/**
 * 逆变器最新数据
 */
@Data
@TableName("pv_device_last")
public class PvDeviceLast {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String plantId;

    private String inverterId;
    private String dataloggerSn;
    private String status;
    private String ipv1;
    private String ipv2;
    private String ipv3;
    private String ipv4;
    private String ipv5;
    private String ipv6;
    private String ipv7;
    private String ipv8;
    private String vpv1;
    private String vpv2;
    private String vpv3;
    private String vpv4;
    private String vpv5;
    private String vpv6;
    private String vpv7;
    private String vpv8;
    private String ppv1;
    private String ppv2;
    private String ppv3;
    private String ppv4;
    private String ppv5;
    private String ppv6;
    private String ppv7;
    private String ppv8;
    private String iacr;
    private String iacs;
    private String iact;
    private String vacr;
    private String vacs;
    private String vact;
    private String pacr;
    private String pacs;
    private String pact;
    private String ppv;
    private String pac;
    private String powerToday;
    private String powerTotal;
    private String temperature;
    private String fac;
    private String pf;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    private String faultType;
    private String timeTotal;
    private String ipmTemperature;
    private String epv1Today;
    private String epv1Total;
    private String epv2Today;
    private String epv2Total;
    private String epvTotal;
    private String eRacToday;
    private String eRacTotal;
    private String pBusVoltage;
    private String nBusVoltage;
    private String dwStringWarningValue1;
    private String wStringStatusValue;
    private String wPIDFaultValue;
    private String vPidPvape;
    private String iPidPvape;
    private String pidStatus;
    private String vPidPvbpe;
    private String iPidPvbpe;
    private String strFault;
    private String vString1;
    private String vString2;
    private String vString3;
    private String vString4;
    private String vString5;
    private String vString6;
    private String vString7;
    private String vString8;
    private String vString9;
    private String vString10;
    private String vString11;
    private String vString12;
    private String vString13;
    private String vString14;
    private String vString15;
    private String vString16;
    private String currentString1;
    private String currentString2;
    private String currentString3;
    private String currentString4;
    private String currentString5;
    private String currentString6;
    private String currentString7;
    private String currentString8;
    private String currentString9;
    private String currentString10;
    private String currentString11;
    private String currentString12;
    private String currentString13;
    private String currentString14;
    private String currentString15;
    private String currentString16;
    private String warnCode;

    @TableField(exist = false)
    private PvDevice pvDevice;
}
