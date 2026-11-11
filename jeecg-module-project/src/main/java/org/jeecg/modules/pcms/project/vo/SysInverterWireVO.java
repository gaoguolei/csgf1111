package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/7 下午2:44
 * @Version 1.5
 */
@Data
public class SysInverterWireVO {


    private java.lang.Integer Id;
    /**
     * 逆变器id
     */
    private java.lang.Integer inverterId;

    /**
     * 接线数量
     */
    @Excel(name = "接线数量",width = 15)
    private java.lang.Integer wireNum;

    /**
     * 设备名称
     */
    @Excel(name = "设备名称",width = 15)
    private java.lang.String wireName;

    @ExcelCollection(name = "mppt信息")
    private List<SysInverterWireMpptVO> sysInverterWireMpptVOS;


}
