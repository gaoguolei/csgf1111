package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.pcms.project.vo.HouseTypeVo;
import org.jeecg.modules.pcms.project.vo.SysAssemblyVO;
import org.jeecg.modules.pcms.project.vo.SysInverterVO;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @Description: pv_project_info
 * @Author: jun
 * @Date: 2023-03-07
 * @Version: V1.0
 */
@ApiModel(value = "pv_project_info对象", description = "pv_project_info")
@Data
@TableName("pv_project_info")
public class PvProjectInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private java.lang.Integer id;
    /**
     * 项目编号
     */
    @Excel(name = "项目编号", width = 15)
    @ApiModelProperty(value = "项目编号")
    private java.lang.String projectCode;
    /**
     * 项目状态
     */
    @Excel(name = "项目状态", width = 15, dicCode = "dict_project_status")
//    @Dict(dicCode = "dict_project_status")
    @ApiModelProperty(value = "项目状态")
    private java.lang.String projectStatus;

    @TableField(exist = false)
    private String projectStatus_dictText;
    /**
     * 销售员ID
     */
    @Excel(name = "销售员ID", width = 15)
    @ApiModelProperty(value = "销售员ID")
    private java.lang.String salespersonId;
    /**
     * 户主姓名
     */
    @Excel(name = "户主姓名", width = 15)
    @ApiModelProperty(value = "户主姓名")
    private java.lang.String customerName;
    /**
     * 身份证号
     */
    @Excel(name = "身份证号", width = 15)
    @ApiModelProperty(value = "身份证号")
    private java.lang.String idNumber;
    /**
     * 联系电话
     */
    @Excel(name = "联系电话", width = 15)
    @ApiModelProperty(value = "联系电话")
    private java.lang.String customerMobile;
    /**
     * 电站安装地址
     */
    @Excel(name = "电站安装地址", width = 15)
    @ApiModelProperty(value = "电站安装地址")
    private java.lang.String areaAddress;
    /**
     * 详细地址
     */
    @Excel(name = "详细地址", width = 15)
    @ApiModelProperty(value = "详细地址")
    private java.lang.String fullAddress;
    /**
     * 房屋类型
     */
    @Excel(name = "房屋类型", width = 15)
    @ApiModelProperty(value = "房屋类型")
    private java.lang.String houseType;
    /**
     * 房屋尺寸
     */
    @Excel(name = "房屋尺寸", width = 15)
    @ApiModelProperty(value = "房屋尺寸")
    private java.lang.String houseSize;
    /**
     * 距离并网点距离(米)
     */
    @Excel(name = "距离并网点距离(米)", width = 15)
    @ApiModelProperty(value = "距离并网点距离(米)")
    private java.math.BigDecimal houseDistance;
    /**
     * 安装位置
     */
    @Excel(name = "安装位置", width = 15)
    @ApiModelProperty(value = "安装位置")
    private java.lang.String instLocation;
    /**
     * 经度
     */
    @Excel(name = "经度", width = 15)
    @ApiModelProperty(value = "经度")
    private java.lang.String instLongitude;
    /**
     * 纬度
     */
    @Excel(name = "纬度", width = 15)
    @ApiModelProperty(value = "纬度")
    private java.lang.String instLatitude;
    /**
     * 组件排布方式
     */
    @Excel(name = "组件排布方式", width = 15)
    @ApiModelProperty(value = "组件排布方式")
    private java.lang.String instCmptArrange;
    /**
     * 组件品牌(北京项目)
     */
    @Excel(name = "组件品牌", width = 15)
    @ApiModelProperty(value = "组件品牌")
    private java.lang.String instCmptBrand;
    /**
     * 组件型号(北京项目)
     */
    @Excel(name = "组件型号", width = 15)
    @ApiModelProperty(value = "组件型号")
    private java.lang.String instCmptModel;
    /**
     * 组件安装数量（块）
     */
    @Excel(name = "组件安装数量（块）", width = 15)
    @ApiModelProperty(value = "组件安装数量（块）")
    private java.lang.Integer instCmptNumber;
    /**
     * 组件安装容量（瓦）
     */
    @Excel(name = "组件安装容量（瓦）", width = 15)
    @ApiModelProperty(value = "组件安装容量（瓦）")
    private java.math.BigDecimal instCmptCapacity;

    /**
     * 总容量
     */
    @ApiModelProperty(value = "组件规格乘以块数计算的总和")
    private String totalCapacity;
    /**
     * 合同号
     */
    @Excel(name = "合同号", width = 15)
    @ApiModelProperty(value = "合同号")
    private java.lang.String conCode;
    /**
     * 银行卡持有人
     */
    @Excel(name = "银行卡持有人", width = 15)
    @ApiModelProperty(value = "银行卡持有人")
    private java.lang.String conBankCardHolders;
    /**
     * 施工队
     */
    @Excel(name = "施工队", width = 15)
    @ApiModelProperty(value = "施工队")
    private java.lang.String constrTeam;
    /**
     * 组件品牌(施工)(北京项目)
     */
    @Excel(name = "组件品牌(施工)", width = 15)
    @ApiModelProperty(value = "组件品牌(施工)")
    private java.lang.String instAssemblyBrand;
    /**
     * 组件规格(施工)(北京项目)
     */
    @Excel(name = "组件规格(施工)", width = 15)
    @ApiModelProperty(value = "组件规格(施工)")
    private java.lang.String instAssemblySpec;
    /**
     * 删除状态(0-正常,1-已删除)
     */
    @Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
    @ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    @TableLogic
    private java.lang.Integer delFlag;
    /**
     * 创建人
     */
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
    /**
     * 所属部门
     */
    @ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
    /**
     * 所属租户
     */
    @Excel(name = "所属租户", width = 15)
    @ApiModelProperty(hidden = true)
    private java.lang.Integer tenantId;

    @ApiModelProperty(hidden = true,value = "1：自然人；2：法人；3:法人二期；4：法人三期；5：法人四期；6：法人五期；")
    private java.lang.String projectType;

    @ApiModelProperty(hidden = true,value = "安装角度")
    private java.lang.String instAngle;


    @ApiModelProperty(hidden = true,value = "实际组件安装数量(块)")
    private java.lang.Integer instCmptNumberFact;

    @ApiModelProperty(hidden = true,value = "实际组件安装容量（瓦）")
    private BigDecimal instCmptCapacityFact;

    /**
     * 驳回类型
     */
    @ApiModelProperty(value = "驳回类型")
    private String rejectionType;


    @ApiModelProperty(value = "客户发电户号")
    private String accountNumber;

    @ApiModelProperty(value = "房屋（类型，数量，尺寸）集合")
    private String houseTypeGroup;

    @TableField(exist = false)
    private List<HouseTypeVo> typeList;


    @ApiModelProperty(value = "房主姓名（北京项目）")
    private String houseOwner;
    @ApiModelProperty(value = "房本编号(北京项目)")
    private String houseNo;
    @ApiModelProperty(value = "关系说明（北京项目）")
    private String relationInfo;
    @ApiModelProperty(value = "电卡姓名（北京项目）")
    private String electricityOwner;
    @ApiModelProperty(value = "电卡编号（北京项目）")
    private String electricityCardNo;
    @ApiModelProperty(value = "期望开卡时间（北京项目）")
    private String openCardTime;
    @ApiModelProperty(value = "期望安装时间（北京项目）")
    private String workTime;
    @ApiModelProperty(value = "返费卡号（北京项目）")
    private String returnFeeDebitCardNo;
    @ApiModelProperty(value = "返费卡姓名（北京项目）")
    private String returnFeeDebitCardOwner;
    @ApiModelProperty(value = "返费卡开户行（北京项目）")
    private String returnFeeDebitCardBank;

    @ApiModelProperty(value = "签署状态0：未签署，1签署中，2签署完成")
    private String isUpload;


    @TableField(exist = false)
    private java.lang.String rejectionStatus;


    @TableField(exist = false)
    private String address;

    @TableField(exist = false)
    private String constrTeamName;

    //开始时间
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginDate;

    //结束时间
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    @TableField(exist = false)
    private String renterId;

    @TableField(exist = false)
    private String addressId;

    @TableField(exist = false)
    private List<Integer> selections;

    @TableField(exist = false)
    private Integer bpmNodeId;

    @TableField(exist = false)
    private String timeStatus;

    //是否是普通角色。（是否是销售员）
    @TableField(exist = false)
    private String isSeller;

    //操作人
    @TableField(exist = false)
    private String operator;

    @TableField(exist = false)
    private List<Integer> statusIds;


    @TableField(exist = false)
    private List<String> userIds;

    @TableField(exist = false)
    private List<PvTjProjectInstall> projectInstalls;

    @TableField(exist = false)
    private Integer pageNo;

    @TableField(exist = false)
    private Integer pageSize;

    /**
     * 逆变器
     */
    @TableField(exist = false)
    List<SysInverterVO> sysInverterVOS;

    /**
     * 组件
     */
    @TableField(exist = false)
    List<SysAssemblyVO> sysAssemblyVOS;

    //是否后补施工
//    @TableField(exist = false)
    private String buildAfter;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setBeginDate(String beginDate) {
        if (beginDate == null || beginDate.trim().isEmpty()) {
            return;
        }
        LocalDate localDate = LocalDate.from(formatter.parse(beginDate));
        this.beginDate = LocalDateTime.of(localDate, LocalTime.of(0, 0, 0));
    }

    public void setEndDate(String endDate) {
        if (endDate == null || endDate.trim().isEmpty()) {
            return;
        }
        LocalDate localDate = LocalDate.from(formatter.parse(endDate));
        this.endDate = LocalDateTime.of(localDate, LocalTime.of(23, 59, 59));
    }


}
