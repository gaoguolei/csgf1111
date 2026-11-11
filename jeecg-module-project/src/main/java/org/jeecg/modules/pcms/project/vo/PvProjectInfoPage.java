package org.jeecg.modules.pcms.project.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import org.jeecg.modules.pcms.project.entity.PvProjectFile;
import org.jeecg.modules.pcms.project.entity.PvProjectRemark;
import org.jeecg.modules.pcms.project.entity.PvTjProjectInstall;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description: pv_project_info
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
@Data
@ApiModel(value="pv_project_infoPage对象", description="pv_project_info")
public class PvProjectInfoPage {

	/**id*/
//	@ApiModelProperty(value = "id")
    private java.lang.Integer id;
	/**项目编号*/
	@Excel(name = "项目编号", width = 15)
	@ApiModelProperty(value = "项目编号")
    private java.lang.String projectCode;
	/**项目状态*/
	@Excel(name = "项目状态", width = 15, dicCode = "dict_project_status")
    @Dict(dicCode = "dict_project_status")
	@ApiModelProperty(value = "项目状态")
    private java.lang.String projectStatus;
	/**销售员ID*/
	@Excel(name = "销售员ID", width = 15)
	@ApiModelProperty(value = "销售员ID")
    private java.lang.String salespersonId;
	/**户主姓名*/
	@Excel(name = "户主姓名", width = 15)
	@ApiModelProperty(value = "户主姓名")
    private java.lang.String customerName;
	/**身份证号*/
	@Excel(name = "身份证号", width = 15)
	@ApiModelProperty(value = "身份证号")
    private java.lang.String idNumber;
	/**联系电话*/
	@Excel(name = "联系电话", width = 15)
	@ApiModelProperty(value = "联系电话")
    private java.lang.String customerMobile;
	/**电站安装地址*/
	@Excel(name = "电站安装地址", width = 15)
	@ApiModelProperty(value = "电站安装地址")
    private java.lang.String areaAddress;
	/**详细地址*/
	@Excel(name = "详细地址", width = 15)
	@ApiModelProperty(value = "详细地址")
    private java.lang.String fullAddress;
	/**房屋类型*/
	@Excel(name = "房屋类型", width = 15)
	@ApiModelProperty(value = "房屋类型")
    private java.lang.String houseType;
	/**房屋尺寸*/
	@Excel(name = "房屋尺寸", width = 15)
	@ApiModelProperty(value = "房屋尺寸")
    private java.lang.String houseSize;
	/**距离并网点距离(米)*/
	@Excel(name = "距离并网点距离(米)", width = 15)
	@ApiModelProperty(value = "距离并网点距离(米)")
    private java.math.BigDecimal houseDistance;
	/**安装位置*/
	@Excel(name = "安装位置", width = 15)
	@ApiModelProperty(value = "安装位置")
    private java.lang.String instLocation;
	/**经度*/
	@Excel(name = "经度", width = 15)
	@ApiModelProperty(value = "经度")
    private java.lang.String instLongitude;
	/**纬度*/
	@Excel(name = "纬度", width = 15)
	@ApiModelProperty(value = "纬度")
    private java.lang.String instLatitude;
	/**组件排布方式*/
	@Excel(name = "组件排布方式", width = 15)
	@ApiModelProperty(value = "组件排布方式")
    private java.lang.String instCmptArrange;
	@Excel(name = "组件品牌(北京项目)", width = 15)
	@ApiModelProperty(value = "组件品牌")
	private java.lang.String instCmptBrand;
	@Excel(name = "组件型号(北京项目)", width = 15)
	@ApiModelProperty(value = "组件型号")
	private java.lang.String instCmptModel;
	/**组件安装数量（块）*/
	@Excel(name = "组件安装数量（块）", width = 15)
	@ApiModelProperty(value = "组件安装数量（块）")
    private java.lang.Integer instCmptNumber;
	/**组件安装容量（瓦）*/
	@Excel(name = "组件安装容量（瓦）", width = 15)
	@ApiModelProperty(value = "组件安装容量（瓦）")
    private java.math.BigDecimal instCmptCapacity;
	/**合同号*/
	@Excel(name = "合同号", width = 15)
	@ApiModelProperty(value = "合同号")
    private java.lang.String conCode;
	/**银行卡持有人*/
	@Excel(name = "银行卡持有人", width = 15)
	@ApiModelProperty(value = "银行卡持有人")
    private java.lang.String conBankCardHolders;
	/**施工队*/
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
	/**删除状态(0-正常,1-已删除)*/
	@Excel(name = "删除状态(0-正常,1-已删除)", width = 15)
//	@ApiModelProperty(value = "删除状态(0-正常,1-已删除)")
    private java.lang.Integer delFlag;
	/**创建人*/
	@ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd hh:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd yyyy-MM-dd hh:mm:ss")
	@ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
	@ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd hh:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd hh:mm:ss")
	@ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
	@ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**所属租户*/
	@Excel(name = "所属租户", width = 15)
	@ApiModelProperty(value = "所属租户")
    private java.lang.Integer tenantId;

	@ApiModelProperty(value = "项目类型")
	private java.lang.String projectType;

	@TableField(exist = false)
	private String address;

	@ExcelCollection(name="pv_project_bpm")
	@ApiModelProperty(value = "pv_project_bpm")
	private List<PvProjectBpm> pvProjectBpmList;
	@ExcelCollection(name="pv_project_file")
	@ApiModelProperty(value = "pv_project_file")
	private List<PvProjectFile> pvProjectFileList;
	@ExcelCollection(name="pv_project_remark")
	@ApiModelProperty(value = "pv_project_remark")
	private List<PvProjectRemark> pvProjectRemarkList;


	/**
	 * 驳回类型
	 */
	@ApiModelProperty(value = "驳回类型")
	private String rejectionType;

	@ApiModelProperty(value = "驳回状态")
	private java.lang.String rejectionStatus;

	@ApiModelProperty(value = "组件规格乘以块数计算的总和")
	private String totalCapacity;


	@ApiModelProperty(value = "房屋（类型，数量，尺寸）集合")
	private String houseTypeGroup;

	private List<HouseTypeVo> typeList;


	List<SysInverterVO> sysInverterVOS;

	List<SysAssemblyVO> sysAssemblyVOS;

	private List<PvTjProjectInstall> projectInstalls;

	@ApiModelProperty(hidden = true,value = "实际组件安装数量(块)")
	private java.lang.Integer instCmptNumberFact;

	@ApiModelProperty(hidden = true,value = "实际组件安装容量（瓦）")
	private BigDecimal instCmptCapacityFact;

	@ApiModelProperty(value = "客户发电户号")
	private String accountNumber;

	@ApiModelProperty(hidden = true,value = "安装角度")
	private java.lang.String instAngle;

	//是否可下载
	@TableField(exist = false)
	private String isUpload;

	//是否后补施工
//	@TableField(exist = false)
	private String buildAfter;

}
