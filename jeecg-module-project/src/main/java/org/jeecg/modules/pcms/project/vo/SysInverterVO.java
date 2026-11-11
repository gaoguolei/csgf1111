package org.jeecg.modules.pcms.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/7 下午2:42
 * @Version 1.5
 */
@Data
public class SysInverterVO {

    private java.lang.Integer Id;

//    @ApiModelProperty(value = "逆变器名称")
    @Excel(name = "逆变器名称", width = 15,needMerge = true)
    private java.lang.String inverterName;

    @ApiModelProperty(value = "项目id")
    private java.lang.Integer projectId;

//    @ApiModelProperty(value = "逆变器型号")
    @Excel(name = "逆变器型号", width = 15,needMerge = true)
    private java.lang.String model;


//    @ApiModelProperty(value = "逆变器sn码")
    @Excel(name = "逆变器sn码", width = 15,needMerge = true)
    private java.lang.String snNo;


//    @ApiModelProperty(value = "mppt数量")
    @Excel(name = "mppt数量", width = 15,needMerge = true)
    private java.lang.Integer mpptConut;



    @ApiModelProperty(value = "租户id")
    private java.lang.Integer tenantId;

    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;

//    @ExcelCollection(name = "接线信息")
    private List<SysInverterWireVO> sysInverterWireVOS;

    @Excel(name = "接线数量", width = 15,needMerge = true)
    private java.lang.Integer wireNum;

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
    private java.lang.Integer piece;

}
