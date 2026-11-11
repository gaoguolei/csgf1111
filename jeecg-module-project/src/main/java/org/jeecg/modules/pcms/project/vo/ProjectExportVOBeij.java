package org.jeecg.modules.pcms.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/9/5 上午7:48
 * @Version 1.5
 */
@Data
public class ProjectExportVOBeij {


    private Integer id;

    @Excel(name = "项目编号", width = 15)
    private String projectCode;

    @Excel(name = "项目类型", width = 15)
    private String projectType;

    @Excel(name = "创建人", width = 15)
    private String createBy;

    //项目状态
    private String projectStatus;
    @Excel(name = "项目状态", width = 15)
    private String status;
    //户主
    @Excel(name = "户主", width = 15)
    private String customerName;

    @Excel(name = "身份证号", width = 20)
    private String idNumber;

    @Excel(name = "手机号", width = 15)
    private String customerMobile;


    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private java.util.Date createTime;


    @Excel(name = "更新时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private java.util.Date updateTime;

    //安装地址
    private String areaAddress;
    @Excel(name = "安装地址", width = 15)
    private String address;
    private String fullAddress;

    //客户信息备注
    @Excel(name = "客户信息备注", width = 15)
    private String customerNotes;

    //房屋类型
//    @Excel(name = "房屋类型", width = 15)
//    private String houseType;

    @Excel(name = "发电户号", width = 15)
    private String accountNumber;

    @Excel(name = "房主姓名", width = 15)
    private String houseOwner;
    @Excel(name = "房本编号", width = 15)
    private String houseNo;
    @Excel(name = "关系说明", width = 15)
    private String relationInfo;
    @Excel(name = "电卡姓名", width = 15)
    private String electricityOwner;
    @Excel(name = "电卡编号", width = 15)
    private String electricityCardNo;
    @Excel(name = "返费卡号", width = 15)
    private String returnFeeDebitCardNo;
    @Excel(name = "返费卡姓名", width = 15)
    private String returnFeeDebitCardOwner;
    @Excel(name = "电卡姓名", width = 15)
    private String returnFeeDebitCardBank;


    //房屋尺寸
//    @Excel(name = "房屋尺寸(长，宽)", width = 15)
//    private String houseSize;

    private List<HouseTypeVo> typeList;

    @Excel(name = "房屋尺寸(长，宽)", width = 15)
    private String houseTypes;

//    @Excel(name = "房屋尺寸(长，宽)", width = 15)
//    private String houseSize;


    //距离
    @Excel(name = "距离并网点距离(米)", width = 15)
    private BigDecimal houseDistance;

    //房屋信息备注
    @Excel(name = "房屋信息备注", width = 15)
    private String houseNotes;

    @Excel(name = "组件品牌（安装）", width = 15)
    private java.lang.String instCmptBrand;
    @Excel(name = "组件型号（安装）", width = 15)
    private java.lang.String instCmptModel;

    //组件排布方式
//    @Excel(name = "组件排布方式", width = 15)
//    private String instCmptArrange;
//
//    @Excel(name = "安装角度", width = 15)
//    private System instAngle;

    //组件安装数量(块)
    @Excel(name = "组件安装数量(块)", width = 15)
    private  Integer instCmptNumber;

    //组件安装容量(瓦)
    @Excel(name = "组件安装容量(瓦)", width = 15)
    private BigDecimal instCmptCapacity;

    @Excel(name = "实际组件安装容量(瓦)", width = 15)
    private BigDecimal instCmptCapacityFact;


    //安装备注
    @Excel(name = "安装备注", width = 15)
    private String installNotes;

    //银行卡持有人
    @Excel(name = "银行卡持有人", width = 15)
    private String conBankCardHolders;

    //资料员审核结果
    @Excel(name = "资料员审核结果", width = 15)
    private String documenterEnd;

    //资料员审核意见
    @Excel(name = "资料员审核意见", width = 15)
    private String documenterComments;

    //供电所备注
    @Excel(name = "供电所备注", width = 15)
    private String powerNotes;

    @Excel(name = "组件品牌(施工)", width = 15)
    private java.lang.String instAssemblyBrand;
    @Excel(name = "组件规格(施工)", width = 15)
    private java.lang.String instAssemblySpec;

    //施工队
    private String constrTeam;
    @Excel(name = "施工队", width = 15)
    private String contrTeamName;

    //施工队管理员备注
    @Excel(name = "施工队管理员备注", width = 15)
    private String contrTeamAdminNotes;


    //施工队备注
    @Excel(name = "施工队备注", width = 15)
    private String contrTeamNotes;

    //施工巡检结果
    @Excel(name = "施工巡检结果", width = 15)
    private String contrTeamEnd;

    //施工巡检意见
    @Excel(name = "施工巡检意见", width = 15)
    private String inspection;

    //并网备注
    @Excel(name = "并网备注", width = 15)
    private String onGridNotes;

    //结项备注
    @Excel(name = "结项备注", width = 15)
    private String endNotes;

}
