package org.jeecg.modules.pcms.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.pcms.project.entity.PvTjProjectInstall;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectExportVOXuZ {
    //id
    private Integer id;
    //项目编号
    @Excel(name = "项目编号",width = 15,needMerge = true)
    private String projectCode;

    @Excel(name = "项目类型", width = 15,needMerge = true)
    private String projectType;

    @Excel(name = "创建人", width = 15,needMerge = true)
    private String createBy;

    //项目状态
    private String projectStatus;
    @Excel(name = "项目状态", width = 15,needMerge = true)
    private String status;
    //户主
    @Excel(name = "户主", width = 15,needMerge = true)
    private String customerName;

    @Excel(name = "身份证号", width = 15,needMerge = true)
    private String idNumber;

    @Excel(name = "手机号", width = 15,needMerge = true)
    private String customerMobile;

    @Excel(name = "创建时间", width = 15, format = "yyyy-MM-dd HH:mm:ss",needMerge = true)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private java.util.Date createTime;


    @Excel(name = "更新时间", width = 15, format = "yyyy-MM-dd HH:mm:ss",needMerge = true)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private java.util.Date updateTime;

    //安装地址
    private String areaAddress;
    @Excel(name = "安装地址", width = 15,needMerge = true)
    private String address;
    private String fullAddress;

    //客户信息备注
    @Excel(name = "客户信息备注", width = 15,needMerge = true)
    private String customerNotes;

    //房屋类型
    @Excel(name = "房屋类型", width = 15,needMerge = true)
    private String houseType;

    //房屋尺寸
    @Excel(name = "房屋尺寸(长，宽)", width = 15,needMerge = true)
    private String houseSize;

    //距离
    @Excel(name = "距离并网点距离(米)", width = 15,needMerge = true)
    private BigDecimal houseDistance;

    //房屋信息备注
    @Excel(name = "房屋信息备注", width = 15,needMerge = true)
    private String houseNotes;

    //组件排布方式
    @Excel(name = "组件排布方式", width = 15,needMerge = true)
    private String instCmptArrange;

    @Excel(name = "安装角度", width = 15,needMerge = true)
    private System instAngle;

    //组件安装数量(块)
    @Excel(name = "组件安装数量(块)", width = 15,needMerge = true)
    private  Integer instCmptNumber;

    //组件安装容量(瓦)
    @Excel(name = "组件安装容量(瓦)", width = 15,needMerge = true)
    private BigDecimal instCmptCapacity;

    @Excel(name = "实际组件安装容量(瓦)", width = 15,needMerge = true)
    private BigDecimal instCmptCapacityFact;

    //安装备注
    @Excel(name = "安装备注", width = 15,needMerge = true)
    private String installNotes;

    //银行卡持有人
    @Excel(name = "银行卡持有人", width = 15,needMerge = true)
    private String conBankCardHolders;

    //资料员审核结果
    @Excel(name = "资料员审核结果", width = 15,needMerge = true)
    private String documenterEnd;

    //资料员审核意见
    @Excel(name = "资料员审核意见", width = 15,needMerge = true)
    private String documenterComments;

    //供电所备注
    @Excel(name = "供电所备注", width = 15,needMerge = true)
    private String powerNotes;

    //施工队
    private String constrTeam;
    @Excel(name = "施工队", width = 15,needMerge = true)
    private String contrTeamName;

    //施工队管理员备注
    @Excel(name = "施工队管理员备注", width = 15,needMerge = true)
    private String contrTeamAdminNotes;

    //施工队备注
    @Excel(name = "施工队备注", width = 15,needMerge = true)
    private String contrTeamNotes;

    //施工巡检结果
    @Excel(name = "施工巡检结果", width = 15,needMerge = true)
    private String contrTeamEnd;

    //施工巡检意见
    @Excel(name = "施工巡检意见", width = 15,needMerge = true)
    private String inspection;

    //并网备注
    @Excel(name = "并网备注", width = 15,needMerge = true)
    private String onGridNotes;

    //结项备注
    @Excel(name = "结项备注", width = 15,needMerge = true)
    private String endNotes;

    @Excel(name = "发电户号", width = 15,needMerge = true)
    private String accountNumber;

    @ExcelCollection(name = "安装信息")
    private List<PvTjProjectInstall> projectInstalls;

    /**
     * 组件
     */
//    @ExcelCollection(name = "组件信息")
//    @TableField(exist = false)
    private List<SysAssemblyVO> sysAssemblyVOS;

    /**
     * 逆变器
     */
//    @ExcelCollection(name = "逆变器信息")
//    @TableField(exist = false)
    private List<SysInverterVO> sysInverterVOS;
}
