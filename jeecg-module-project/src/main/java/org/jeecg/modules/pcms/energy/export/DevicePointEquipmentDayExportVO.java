package org.jeecg.modules.pcms.energy.export;


import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class DevicePointEquipmentDayExportVO {
    //2、设备报表：同样分日报表、月报表、年报表、总报表四级，包含电站报表的所有字段，增加设序列号、通信设备（采集器）序列号、设备型号、状态字段。

    @Excel(name = "电站名称",width = 15,needMerge = true)
    private String psName;

    @Excel(name = "装机功率",width = 15,needMerge = true)
    private String peakPower;

    @Excel(name = "日期",width = 15,needMerge = true)
    private String date;

    @Excel(name = "当日发电量",width = 15,needMerge = true)
    private String dayPower;

    @Excel(name = "累计发电量",width = 15,needMerge = true)
    private String totalPower;

    @Excel(name = "设备序列号",width = 15,needMerge = true)
    private String equipmentSn;

    @Excel(name = "通信设备（采集器）序列号",width = 15,needMerge = true)
    private String messageSn;

    @Excel(name = "设备型号",width = 15,needMerge = true)
    private String equipmentModelSn;

    @Excel(name = "设备状态",width = 15,needMerge = true)
    private String equipmentStatus;





}
