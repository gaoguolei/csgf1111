package org.jeecg.modules.pcms.project.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecgframework.poi.excel.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: pv_project_file
 * @Author: jun
 * @Date:   2025-08-26
 * @Version: V1.0
 */
@ApiModel(value="pv_project_file_other对象", description="pv_project_file_other")
@Data
@TableName("pv_project_file_other")
@AllArgsConstructor
@NoArgsConstructor
public class PvProjectFileOther implements Serializable {
    private static final long serialVersionUID = 1L;

    /**id*/
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;
    /**项目ID*/
    @ApiModelProperty(value = "项目ID")
    private java.lang.Integer projectId;
    /**文件URL*/
    @Excel(name = "文件URL", width = 15)
    @ApiModelProperty(value = "文件URL")
    private java.lang.String fileUrl;
    /**缩略图*/
    @Excel(name = "缩略图", width = 15)
    @ApiModelProperty(value = "缩略图")
    private java.lang.String thumbUrl;
    /**删除状态(0-正常,1-已删除)*/
    @Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    @TableLogic
    private java.lang.Integer delFlag;
    /**所属租户*/
    @Excel(name = "所属租户", width = 15)
    @ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;
    /**文件类型*/
    /*
        0、租赁合同
        1、购售电合同
        2、农户省份证
        3、用电权属证明
        4、蓝图
        5、荷载与地勘报告
        6、三级验收单
        7、可研报告（村委会）
        8、组织机构代码（村委会）
        9、法人公户信息（村委会）
    */
    @Excel(name = "文件类型", width = 15)
    @ApiModelProperty(value = "文件类型")
    private java.lang.String fileType;
    @Excel(name = "文件名字", width = 15)
    @ApiModelProperty(value = "文件名字")
    private java.lang.String fileName;
    @Excel(name = "文件大小", width = 15)
    @ApiModelProperty(value = "文件大小")
    private java.lang.Long fileSize;
    @Excel(name = "类型", width = 15)
    @ApiModelProperty(value = "类型")
    private java.lang.String type;
}
