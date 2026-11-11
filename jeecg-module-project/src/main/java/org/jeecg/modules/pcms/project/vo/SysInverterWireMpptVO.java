package org.jeecg.modules.pcms.project.vo;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Author gongzhou
 * @Date 2023/7/7 下午3:46
 * @Version 1.5
 */
@Data
public class SysInverterWireMpptVO {

    private java.lang.Integer Id;

    /**
     * 接线id
     */
    private java.lang.Integer inverterWireId;

    /**
     * 串
     */
    @Excel(name = "串",width = 15)
//    @ExcelProperty(value = "串")
    private java.lang.String strand;

    /**
     * 块
     */
    @Excel(name = "块",width = 15)
    //    @ExcelProperty(value = "块")
    private java.lang.Integer piece;

    /**
     * 安装容量
     */
    @Excel(name = "安装容量",width = 15)
    private java.lang.String capacity;

    /**
     * 安装角度
     */
    @Excel(name = "安装角度",width = 15)
    private java.lang.String angle;

    /**
     * 组串电压
     */
    @Excel(name = "组串电压",width = 15)
    private java.lang.String voltage;

}
