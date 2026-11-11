package org.jeecg.modules.pcms.energy.export;


import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class DevicePointMonthExportVO {


    @Excel(name = "电站名称",width = 15,needMerge = true)
    private String psName;

    @Excel(name = "装机功率",width = 15,needMerge = true)
    private String peakPower;

    @Excel(name = "日期",width = 15,needMerge = true)
    private String date;

    @Excel(name = "当日发电量",width = 15,needMerge = true)
    private String dayPower;

    @Excel(name = "当月发电量",width = 15,needMerge = true)
    private String monthPower;


    @Excel(name = "累计发电量",width = 15,needMerge = true)
    private String totalPower;
}
