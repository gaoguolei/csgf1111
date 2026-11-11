package org.jeecg.modules.pcms.project.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Author gongzhou
 * @Date 2023/7/8 上午8:19
 * @Version 1.5
 */
@Data
public class SysAssemblyVO {

    private java.lang.Integer Id;

//    @ApiModelProperty(value = "组件名称")
    @Excel(name = "组件名称",width = 15)
    private java.lang.String assemblyName;

//    @ApiModelProperty(value = "组件规格")
    @Excel(name = "组件规格",width = 15)
    private java.lang.String assemblySpec;

//    @ApiModelProperty(value = "组件型号")
    @Excel(name = "组件型号",width = 15)
    private java.lang.String assemblyModel;

    @ApiModelProperty(value = "项目id")
    private java.lang.Integer projectId;

    @ApiModelProperty(value = "租户id")
    private java.lang.Integer tenantId;

}
