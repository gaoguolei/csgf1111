package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2024/5/15 下午12:02
 * @Version 1.5
 */

@Data
@ApiModel(value = "pv_tj_project_install对象", description = "pv_tj_project_install")
@TableName("pv_tj_project_install")
public class PvTjProjectInstall implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "项目id")
    private Integer projectId;

    @Excel(name="房屋类型",width = 15)
//    @ApiModelProperty(value = "房屋类型")
    private String houseType;

    @Excel(name="安装角度",width = 15)
//    @ApiModelProperty(value = "安装角度")
    private String instAngle;

    //需要导出
    @Excel(name="组件安装数量（块）",width = 15)
//    @ApiModelProperty(value = "组件安装数量（块）")
    private Integer instCmptNumber;

    //需要导出
    @Excel(name="组件安装容量（瓦）",width = 15)
//    @ApiModelProperty(value = "组件安装容量（瓦）")
    private BigDecimal instCmptCapacity;

    @Excel(name="组件排布方式",width = 15)
//    @ApiModelProperty(value = "组件排布方式")
    private String instCmptArrange;

    @Excel(name="组件型号",width = 15)
//    @ApiModelProperty(value = "组件型号")
    private String cmptModel;

    @Excel(name="组件规格",width = 15)
//    @ApiModelProperty(value = "组件规格")
    private String cmptSpec;

//    @Excel(name="踏勘图id集合",width = 15)
//    @ApiModelProperty(value = "踏勘图id集合")
    private String fileId;

//    @Excel(name="安装备注id集合",width = 15)
//    @ApiModelProperty(value = "安装备注id集合")
    private String remakesId;

    @Excel(name="安装备注",width = 15)
    @TableField(exist = false)
    private String remake;

//    @Excel(name="创建时间",width = 15)
//    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd hh:mm:ss")
//    @DateTimeFormat(pattern="yyyy-MM-dd yyyy-MM-dd hh:mm:ss")
////    @ApiModelProperty(value = "创建日期")
//    private Date createDate;

    @TableField(exist = false)
    private List<PvProjectFile> pvProjectFile;

    @TableField(exist = false)
    private List<PvProjectRemark> pvProjectRemark;


}
