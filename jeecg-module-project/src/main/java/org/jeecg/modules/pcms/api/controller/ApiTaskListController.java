package org.jeecg.modules.pcms.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.pcms.aspect.annotation.PvOperateLog;
import org.jeecg.modules.pcms.common.DictOperateTypeEnum;
import org.jeecg.modules.pcms.common.DictProjectBeiJStatusEnum;
import org.jeecg.modules.pcms.common.DictProjectStatusEnum;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;
import org.jeecg.modules.pcms.energy.entity.DeviceMessage;
import org.jeecg.modules.pcms.project.entity.*;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationentity.PvStationsDetails;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;
import org.jeecg.modules.pcms.project.vo.*;
import org.jeecg.modules.pcms.util.AESUtils;
import org.jeecg.modules.pcms.util.FileUploadUtils;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @Description: 工作台
 * @Author: jun
 * @Date: 2023-03-07
 * @Version: V1.0
 */
@Api(tags = "光伏-工作台")
@RestController
@RequestMapping("/project/taskList")
@Slf4j
public class ApiTaskListController {
    @Autowired
    private IPvProjectInfoService pvProjectInfoService;
    @Autowired
    private IPvProjectBpmService pvProjectBpmService;
    @Autowired
    private IPvProjectFileService pvProjectFileService;

    @Autowired
    private IPvProjectFileOtherService pvProjectFileOtherService;

    @Autowired
    private IPvProjectRemarkService pvProjectRemarkService;
    @Autowired
    private IRegionsServiceOne iRegionsService;


    @Autowired
    private ISysInverterService iSysInverterService;

    @Autowired
    private ISysAssemblyService iSysAssemblyService;


    @Autowired
    private ISysModelService modelService;

    @Autowired
    private IPvCallbackService iPvCallbackService;

//    @Autowired
//    private IPvStationsService stationsService;

//    @Autowired
//    private IPvEnergyService iPvEnergyService;

    @Autowired
    private PvProjectKeyService pvProjectKeyService;


//    @Autowired
//    private IPvStationsViewService iPvStationsViewService;

//    @Autowired
//    private IPvStationsDetailsService iPvStationsDetailsService;

//    @Autowired
//    private IPvDeviceLastService iPvDeviceLastService;

    @Autowired
    private FileUploadUtils fileUploadUtils;

    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private IPvTjProjectInstallService iPvTjProjectInstallService;

    @Autowired
    private IDeviceMessageService iDeviceMessageService;

    @Autowired
    private IDeviceService iDeviceService;
    @Autowired
    private IDevicePointDayService iDevicePointDayService;

    @Autowired
    private IDeviceDetailGrowattService iDeviceDetailGrowattService;
    @Autowired
    private IDevicePointMonthService iDevicePointMonthService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    private static final Map<String, String> NUMBER_TO_CHINESE = new HashMap<>();

    static {
        NUMBER_TO_CHINESE.put("1", "自然人");
        NUMBER_TO_CHINESE.put("2", "法人");
        NUMBER_TO_CHINESE.put("3", "法人二期");
        NUMBER_TO_CHINESE.put("4", "法人三期");
        NUMBER_TO_CHINESE.put("5", "法人四期");
        NUMBER_TO_CHINESE.put("6", "法人五期");
        NUMBER_TO_CHINESE.put("7", "法人六期");
        NUMBER_TO_CHINESE.put("8", "法人七期");
        NUMBER_TO_CHINESE.put("9", "法人八期");
        NUMBER_TO_CHINESE.put("10", "法人九期");
        NUMBER_TO_CHINESE.put("11", "法人十期");
    }


    @Value(value = "${jeecg.path.upload}")
    private String uploadpath;

    @Value(value = "${renergy.envParam}")
    private String envParam;

    @Value(value = "${jeecg.domainUrl.allPath}")
    private String allPath;

    // 项目类型_增
    @AutoLog(value = "工作台--添加项目类型")
    @ApiOperation(value = "工作台--添加项目类型", notes = "工作台--添加项目类型")
//    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "String", paramType = "header")
    @PostMapping(value = "/addProjectType")
    public Result<String> addProjectType(@RequestParam(name = "tenantId", required = true) Integer tenantId, @RequestParam(name = "typeValue", required = true) String typeValue) {

        return this.pvProjectInfoService.addProjectType(tenantId, typeValue);
    }

    // 项目类型_删
    @AutoLog(value = "工作台--禁用项目类型")
    @ApiOperation(value = "工作台--禁用项目类型", notes = "工作台--禁用项目类型")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "String", paramType = "header")
    @DeleteMapping(value = "/delProjectType")
    public Result<String> delProjectType(@RequestBody UProjectType uProjectType) {
        return this.pvProjectInfoService.delProjectType(Arrays.asList(uProjectType.getIds().split(",")), uProjectType.getTenantId());
    }

    // 项目类型_删
    @AutoLog(value = "工作台--启用项目类型")
    @ApiOperation(value = "工作台--启用项目类型", notes = "工作台--启用项目类型")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "String", paramType = "header")
    @PostMapping(value = "/enableProjectType")
    public Result<String> enableProjectType(@RequestBody UProjectType uProjectType) {
        return this.pvProjectInfoService.enableProjectType(Arrays.asList(uProjectType.getIds().split(",")), uProjectType.getTenantId());
    }

    // 项目类型_改
    @AutoLog(value = "工作台--修改项目类型")
    @ApiOperation(value = "工作台--修改项目类型", notes = "工作台--修改项目类型")
//    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "String", paramType = "header")
    @PostMapping(value = "/updateProjectType")
    public Result<String> updateProjectType(@RequestBody ProjectTypeVo projectTypeVo) {
        return this.pvProjectInfoService.updateProjectType(projectTypeVo);
    }

    // 项目类型_查
    @AutoLog(value = "工作台--项目类型分页列表")
    @ApiOperation(value = "工作台--项目类型分页列表", notes = "工作台--项目类型分页列表")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/getProjectType")
    public Result<IPage<PvProjectType>> getProjectType(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "tenantId", required = true) Integer tenantId) {
        Page<PvProjectType> page = new Page<>(pageNo, pageSize);
        IPage<PvProjectType> pageList = pvProjectInfoService.pagePt(page, tenantId);
        return Result.OK(pageList);
    }

    // 项目类型_查
    @AutoLog(value = "工作台--项目类型列表")
    @ApiOperation(value = "工作台--项目类型列表", notes = "工作台--项目类型列表")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/selectProjectType")
    public Result<List<PvProjectType>> selectProjectType(@RequestParam(name = "tenantId", required = true) Integer tenantId) {
        List<PvProjectType> pageList = pvProjectInfoService.selectProjectType(tenantId);
        return Result.OK(pageList);
    }

    /**
     * 已办-分页列表查询
     *
     * @param pvProjectInfo
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */

    @AutoLog(value = "工作台-已办-分页列表查询")
    @ApiOperation(value = "工作台-已办-分页列表查询", notes = "工作台-已办-分页列表查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
//    @PermissionData(pageComponent = "pvo/doneTask/index")
    @GetMapping(value = "/completed")
    public Result<IPage<PvProjectInfo>> completed(PvProjectInfo pvProjectInfo,
                                                  @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                  HttpServletRequest req) {
        //获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String id = loginUser.getId();
        SysUser byId = sysUserService.getById(id);
        String createBy = byId.getCreateBy();
        String username = loginUser.getUsername();
        String relTenantIds = loginUser.getRelTenantIds();
        String renterId = pvProjectInfo.getRenterId();
        String orgCode = loginUser.getOrgCode();
        String roleId = iRegionsService.getRoleId(id);
        //北京项目
        if (renterId.equals("-2103508991")) {
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);

            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            String addressId = pvProjectInfo.getAddressId();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }

            //如果renterId 为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }
            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }

            //project_status > '10' and sys_org_code like '#{sys_org_code}%'
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.gt("project_status ", 10);
                queryWrapper.eq("create_by", username);
            }
            //施工管理员  project_status > '40'
            if (roleId.equals("1632852551321931777")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.gt("project_status ", 40);
            }
            //资料员  project_status > '20'
            if (roleId.equals("1632852197175873538")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.gt("project_status ", 30);
            }
            //施工队  project_status > '50' and constr_team='#{sys_org_code}'
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.gt("project_status ", 50);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                    List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                    allUsersCreatedBy.add(createBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }

            }
            //巡检员   project_status > '60'
            if (roleId.equals("1632852792637018113")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.gt("project_status ", 60);
            }
            //监理
            if (roleId.equals("1707015958923980802")) {
//                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
//                allUsersCreatedBy.add(createBy);
//                queryWrapper.in("create_by",allUsersCreatedBy);
                queryWrapper.gt("project_status ", 80);
            }
            //并网员 project_status > '70'
            if (roleId.equals("1632853052226686977")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.gt("project_status ", 70);
            }
            //如果是一级商或者二级商
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    allUsersCreatedBy.add(username);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //项目类型
                String pt = records.get(i).getProjectType();
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String regionPath = records.get(i).getAreaAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectBeiJStatusEnum dicProjectBeijBpmEnum = DictProjectBeiJStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
        //天津项目
        if (renterId.equals("102")) {
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            String addressId = pvProjectInfo.getAddressId();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }

            //relTenantIds 为空那么代表这是租户管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }
            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.gt("project_status ", 10);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.gt("project_status ", 20);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.gt("project_status ", 50);
                queryWrapper.eq("constr_team", orgCode);
            }
            //施工管理员
            if (roleId.equals("1632852551321931777")) {
                queryWrapper.gt("project_status ", 40);
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                queryWrapper.gt("project_status ", 60);
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.gt("project_status ", 70);
            }
            //如果是一级商或者二级商。那么进来的时候就 递归查询他所创建的用户写进集合
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String regionPath = records.get(i).getAreaAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
            }
            return Result.OK(pageList);
        }
        //徐州
        if (renterId.equals("118337537")) {
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            String addressId = pvProjectInfo.getAddressId();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }

            //relTenantIds 为空那么代表这是租户管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }
            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.gt("project_status ", 10);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.gt("project_status ", 20);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.gt("project_status ", 50);
                queryWrapper.eq("constr_team", orgCode);
            }
            //施工管理员
            if (roleId.equals("1632852551321931777")) {
                queryWrapper.gt("project_status ", 40);
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                queryWrapper.gt("project_status ", 60);
            }

            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.gt("project_status ", 70);
            }
            //如果是一级商或者二级商。那么进来的时候就 递归查询他所创建的用户写进集合
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String regionPath = records.get(i).getAreaAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dictProjectStatusEnum = DictProjectStatusEnum.get(projectStatus);
                records.get(i).setProjectStatus_dictText(dictProjectStatusEnum.getName());
            }
            return Result.OK(pageList);
        }
        //廊坊项目
        if (renterId.equals("-1759715327")) {
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            String addressId = pvProjectInfo.getAddressId();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }

            //relTenantIds 为空那么代表这是租户管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }
            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.gt("project_status ", 10);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.gt("project_status ", 20);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.gt("project_status ", 50);
                queryWrapper.eq("constr_team", orgCode);
            }
            //施工管理员
            if (roleId.equals("1632852551321931777")) {
                queryWrapper.gt("project_status ", 40);
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                queryWrapper.gt("project_status ", 60);
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.gt("project_status ", 70);
            }
            //如果是一级商或者二级商。那么进来的时候就 递归查询他所创建的用户写进集合
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String regionPath = records.get(i).getAreaAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
            }
            return Result.OK(pageList);
        }
        //其他项目， 如果后续有需求变化再整出来单独写逻辑
        else {
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            String addressId = pvProjectInfo.getAddressId();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }

            //relTenantIds 为空那么代表这是租户管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }
            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.gt("project_status ", 10);
                queryWrapper.eq("create_by", username);
            }
            //施工管理员
            if (roleId.equals("1632852551321931777")) {
                queryWrapper.gt("project_status ", 40);
            }
            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.gt("project_status ", 20);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.gt("project_status ", 50);
                queryWrapper.eq("constr_team", orgCode);
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                queryWrapper.gt("project_status ", 60);
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.gt("project_status ", 70);
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //项目类型
                String pt = records.get(i).getProjectType();
//               String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String regionPath = records.get(i).getAreaAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
            }
            return Result.OK(pageList);
        }

    }


//    @RequestMapping(value = "test", method = RequestMethod.GET)
//    public void test() {
//        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
//        String id = loginUser.getId();
//        SysUser byId = sysUserService.getById(id);
//        String createBy = byId.getCreateBy();
//        List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
//        System.err.println(allUsersCreatedBy);
//    }


    /**
     * 待办-分页列表查询
     *
     * @param pvProjectInfo
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */

    @AutoLog(value = "工作台-待办-分页列表查询")
    @ApiOperation(value = "工作台-待办-分页列表查询", notes = "工作台-待办-分页列表查询")
//    @PermissionData(pageComponent = "pvo/todoTasks/index")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/toDo")
    public Result<IPage<PvProjectInfo>> toDo(PvProjectInfo pvProjectInfo,
                                             @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                             HttpServletRequest req) {
        String renterId = pvProjectInfo.getRenterId();
        //如果是北京的项目，需要根据不同的角色来展示页面的数据
        if (renterId.equals("-2103508991")) {
            //获取当前登录用户
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            SysUser byId = sysUserService.getById(id);
            String createBy = byId.getCreateBy();
            String relTenantIds = loginUser.getRelTenantIds();
            String username = loginUser.getUsername();
            String orgCode = loginUser.getOrgCode();
            String roleId = iRegionsService.getRoleId(id);
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            //获取租户id
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            String addressId = pvProjectInfo.getAddressId();
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }
            //如果renterId 不为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }

            //如果 renterId 不为空则代表是普通用户登录取他租户id即可
            else if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员（只看自己创建的项目）
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.in("project_status ", 10, 21);
                queryWrapper.eq("create_by", username);
            }
            //施工管理员(查看权限：查看二级商创建的项目，还能查看二级商创建的销售员创建的项目)
            if (roleId.equals("1632852551321931777")) {
                //createBy 是二级商  把二级商所创建的销售员全部查询出来
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.in("project_status ", 40, 70);

            }
            //资料员
            if (roleId.equals("1632852197175873538")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
                queryWrapper.in("project_status ", 20, 30, 100);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.in("project_status ", 50, 61);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                    List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                    allUsersCreatedBy.add(createBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }
            //巡检员
            if (roleId.equals("1632852792637018113")) {
                queryWrapper.in("project_status ", 60);
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
            }
            //监理
            if (roleId.equals("1707015958923980802")) {
                queryWrapper.in("project_status ", 80);
//                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
//                allUsersCreatedBy.add(createBy);
//                queryWrapper.in("create_by",allUsersCreatedBy);
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.in("project_status ", 90);
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(createBy);
                allUsersCreatedBy.add(createBy);
                queryWrapper.in("create_by", allUsersCreatedBy);
            }
            //如果是一级商或者二级商
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    allUsersCreatedBy.add(username);
                    queryWrapper.ne("project_status", 110);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
//                String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }

                String regionPath = records.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    String constrTeam = records.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsService.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            records.get(i).setConstrTeamName(nameByCode);
                        }
                    }
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                if ("-2103508991".equals(renterId)) {
                    String projectStatus = records.get(i).getProjectStatus();
                    DictProjectBeiJStatusEnum dicProjectBeijBpmEnum = DictProjectBeiJStatusEnum.get(projectStatus);
                    if (dicProjectBeijBpmEnum != null) {
                        records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                    }
                }
            }
            return Result.OK(pageList);
        }
        //天津
        if (renterId.equals("102")) {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            String relTenantIds = loginUser.getRelTenantIds();
            String orgCode = loginUser.getOrgCode();
            String username = loginUser.getUsername();
            String roleId = iRegionsService.getRoleId(id);
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            //获取租户id
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            String addressId = pvProjectInfo.getAddressId();
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }
            //relTenantIds 为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }

            //如果 renterId 为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.in("project_status ", 10, 21);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.in("project_status ", 20, 30, 80);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.in("project_status ", 50, 61);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                }
            }
            //施工管理员，巡检员，并网员三个角色分别都能操作各自状态的数据，需要在待办里面显示
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632852551321931777") || roleId.equals("1632853052226686977")) {
                queryWrapper.in("project_status ", 40, 60, 70);
            }

            //如果是一级商或者二级商
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                    queryWrapper.ne("project_status", 90);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
//                String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
                String regionPath = records.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    String constrTeam = records.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsService.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            records.get(i).setConstrTeamName(nameByCode);
                        }
                    }
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }

        //徐州
        if (renterId.equals("118337537")) {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            String relTenantIds = loginUser.getRelTenantIds();
            String orgCode = loginUser.getOrgCode();
            String username = loginUser.getUsername();
            String roleId = iRegionsService.getRoleId(id);
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            //获取租户id
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            String addressId = pvProjectInfo.getAddressId();
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }
            //relTenantIds 为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }

            //如果 renterId 为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.in("project_status ", 10, 21);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.in("project_status ", 20, 30, 80);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.in("project_status ", 50, 61);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                }
            }
            //施工管理员，巡检员，并网员三个角色分别都能操作各自状态的数据，需要在待办里面显示
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632852551321931777") || roleId.equals("1632853052226686977")) {
                queryWrapper.in("project_status ", 40, 60, 70);
            }

            //如果是一级商或者二级商
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                    queryWrapper.ne("project_status", 90);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
//                String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
                String regionPath = records.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    String constrTeam = records.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsService.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            records.get(i).setConstrTeamName(nameByCode);
                        }
                    }
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }

        //廊坊项目
        if (renterId.equals("-1759715327")) {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            String relTenantIds = loginUser.getRelTenantIds();
            String orgCode = loginUser.getOrgCode();
            String username = loginUser.getUsername();
            String roleId = iRegionsService.getRoleId(id);
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            //获取租户id
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            String addressId = pvProjectInfo.getAddressId();
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }
            //relTenantIds 为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }

            //如果 renterId 为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.in("project_status ", 10, 21);
                queryWrapper.eq("create_by", username);
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.in("project_status ", 20, 30, 80);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.in("project_status ", 50, 61);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                }
            }
            //施工管理员，巡检员，并网员三个角色分别都能操作各自状态的数据，需要在待办里面显示
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632852551321931777") || roleId.equals("1632853052226686977")) {
                queryWrapper.in("project_status ", 40, 60, 70);
            }

            //如果是一级商或者二级商
            if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
                List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
                if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                    pvProjectInfo.setUserIds(allUsersCreatedBy);
                    queryWrapper.in("create_by", allUsersCreatedBy);
                    queryWrapper.ne("project_status", 90);
                }
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //预计安装容量赋值
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                //项目类型
                String pt = records.get(i).getProjectType();
//                String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
                String regionPath = records.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    String constrTeam = records.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsService.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            records.get(i).setConstrTeamName(nameByCode);
                        }
                    }
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        } else {
            //获取当前登录用户

            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            String relTenantIds = loginUser.getRelTenantIds();
            String orgCode = loginUser.getOrgCode();
            String username = loginUser.getUsername();
            String roleId = iRegionsService.getRoleId(id);
//            String customerName = pvProjectInfo.getCustomerName();
//            if (StringUtils.isNotBlank(customerName)) {
//                String customerNameNew = customerName.replaceAll("\\*", "");
//                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//                String passwordKey = pvProjectKey.getPasswordKey();
//                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//                pvProjectInfo.setCustomerName(encrypt);
//            }
            //获取租户id
            HashMap<String, String[]> stringHashMap = new HashMap<>();
            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, stringHashMap);
            LocalDateTime beginDate = pvProjectInfo.getBeginDate();
            LocalDateTime endDate = pvProjectInfo.getEndDate();
            if (beginDate != null && endDate != null) {
                queryWrapper.ge("create_time", beginDate);
                queryWrapper.le("create_time", endDate);
            }
            String addressId = pvProjectInfo.getAddressId();
            if (StringUtils.isNotBlank(addressId)) {
                queryWrapper.like("area_address", addressId + ",");
            }
            //relTenantIds 为空那么代表这是租户项目管理员或者超级管理员使用
            if (StringUtils.isBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            String projectType = pvProjectInfo.getProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                queryWrapper.eq("project_type", projectType);
            }

            //如果 renterId 为空则代表是普通用户登录取他租户id即可
            if (StringUtils.isNotBlank(relTenantIds)) {
                queryWrapper.eq("tenant_id", relTenantIds);
            }
            //销售员
            if (roleId.equals("1632852352834883585")) {
                queryWrapper.in("project_status ", 10, 21);
                queryWrapper.eq("create_by", username);
            }
            //施工管理员
            if (roleId.equals("1632852551321931777")) {
                queryWrapper.in("project_status ", 40);
            }
            //资料员
            if (roleId.equals("1632852197175873538")) {
                queryWrapper.in("project_status ", 20, 30, 80);
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                queryWrapper.in("project_status ", 50, 61);
                if (StringUtils.isNotBlank(orgCode)) {
                    queryWrapper.eq("constr_team", orgCode);
                }
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                queryWrapper.in("project_status ", 50, 60);
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                queryWrapper.in("project_status ", 70);
            }

            queryWrapper.orderByDesc("update_time");
            queryWrapper.eq("del_flag", 0);
            Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo, pageSize);
            IPage<PvProjectInfo> pageList = pvProjectInfoService.page(page, queryWrapper);
            List<PvProjectInfo> records = pageList.getRecords();
            for (int i = 0; i < records.size(); i++) {
                //项目类型
                String pt = records.get(i).getProjectType();
//                String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(pt, null);
                PvProjectType pvProjectType = new PvProjectType();
                if (StringUtils.isNotBlank(pt)) {
                    pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(pt), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    records.get(i).setProjectType(chineseDescription);
                } else {
                    records.get(i).setProjectType("");
                }
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
                String regionPath = records.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(regionPath)) {
                    String fullAddress = records.get(i).getFullAddress();
                    String constrTeam = records.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsService.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            records.get(i).setConstrTeamName(nameByCode);
                        }
                    }
                    regionPath = regionPath.substring(1, regionPath.length());
                    String[] regionPathArray = regionPath.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsService.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    records.get(i).setAddress(stringBuilder.toString() + fullAddress);
                }

                // 解码关键信息返回前端
                PvProjectInfo projectInfo = records.get(i);
                this.decrypt(projectInfo);

            }
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }

    }

    // 解码关键信息返回前端
    private void decrypt(PvProjectInfo projectInfo) {
        // 电话号码
        String customerMobile = projectInfo.getCustomerMobile();
        // 身份号码
        String idNumber = projectInfo.getIdNumber();
        // 银行卡持有人
        String conBankCardHolders = projectInfo.getConBankCardHolders();
        // 客户姓名
//        String customerName = projectInfo.getCustomerName();
        // 户主姓名
        String houseOwner = projectInfo.getHouseOwner();
        // 电卡姓名
        String electricityOwner = projectInfo.getElectricityOwner();
        String electricityCardNo = projectInfo.getElectricityCardNo();
        // 返费人
        String returnFeeDebitCardOwner = projectInfo.getReturnFeeDebitCardOwner();
        // 返费卡号
        String returnFeeDebitCardNo = projectInfo.getReturnFeeDebitCardNo();
        // 项目id
        Integer projectInfoId = projectInfo.getId();
        // 租户id
        Integer tenantId = projectInfo.getTenantId();
        // 获取秘钥
        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
        if (pvProjectKey != null) {
            // 秘钥
            String passwordKey = pvProjectKey.getPasswordKey();
            if (StringUtils.isNotBlank(passwordKey)) {
                SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                // 电话号码
                if (StringUtils.isNotBlank(customerMobile)) {
                    String decrypt = AESUtils.decrypt(customerMobile, secretKey);
                    projectInfo.setCustomerMobile(decrypt);
                }
                // 身份号码
                if (StringUtils.isNotBlank(idNumber)) {
                    String decrypt = AESUtils.decrypt(idNumber, secretKey);
                    projectInfo.setIdNumber(decrypt);
                }
                // 银行卡持有人
                if (StringUtils.isNotBlank(conBankCardHolders)) {
                    String decrypt = AESUtils.decrypt(conBankCardHolders, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setConBankCardHolders(decrypt);
                    }
                }
                // 客户姓名
//                if (StringUtils.isNotBlank(customerName)) {
//                    String decrypt = AESUtils.decrypt(customerName, secretKey);
//                    projectInfo.setCustomerName(decrypt);
//                }
                // 户主姓名
                if (StringUtils.isNotBlank(houseOwner)) {
                    String decrypt = AESUtils.decrypt(houseOwner, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setHouseOwner(decrypt);
                    }
                }
                // 电卡姓名
                if (StringUtils.isNotBlank(electricityOwner)) {
                    String decrypt = AESUtils.decrypt(electricityOwner, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setElectricityOwner(decrypt);
                    }
                }
                // 电卡姓名
                if (StringUtils.isNotBlank(electricityCardNo)) {
                    String decrypt = AESUtils.decrypt(electricityOwner, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setElectricityCardNo(decrypt);
                    }
                }
                // 电卡姓名
                if (StringUtils.isNotBlank(returnFeeDebitCardOwner)) {
                    String decrypt = AESUtils.decrypt(returnFeeDebitCardOwner, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setReturnFeeDebitCardOwner(decrypt);
                    }
                }
                // 电卡姓名
                if (StringUtils.isNotBlank(returnFeeDebitCardNo)) {
                    String decrypt = AESUtils.decrypt(returnFeeDebitCardNo, secretKey);
                    if (StringUtils.isNotBlank(decrypt)) {
                        projectInfo.setReturnFeeDebitCardNo(decrypt);
                    }
                }
            }

        }
    }


    /**
     * 台账
     *
     * @param pvProjectInfo
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "台账")
    @ApiOperation(value = "台账", notes = "台账")
    @GetMapping(value = "/ledger")
    public Result<IPage<PvProjectInfo>> ledger(PvProjectInfo pvProjectInfo,
                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(name = "renterId") String renterId,
                                               HttpServletRequest req) {

//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, req.getParameterMap());
//        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
//        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
//        List<String> statusIds = pvProjectInfo.getStatusIds();
//        if( statusIds != null && !statusIds.isEmpty()){
//            queryWrapper.in("project_status",statusIds);
//        }
//        LocalDateTime endDate = pvProjectInfo.getEndDate();
//        if(beginDate !=null && endDate != null){
//            queryWrapper.ge("create_time",beginDate);
//            queryWrapper.le("create_time",endDate);
//        }
//        String addressId = pvProjectInfo.getAddressId();
//        if(StringUtils.isNotBlank(addressId)){
//            queryWrapper.like("area_address",addressId+",");
//        }
//        queryWrapper.orderByDesc("update_time");

        //获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String relTenantIds = loginUser.getRelTenantIds();
        String username = loginUser.getUsername();
//        String customerName = pvProjectInfo.getCustomerName();
//        if (StringUtils.isNotBlank(customerName)) {
//            String customerNameNew = customerName.replaceAll("\\*", "");
//            PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//            String passwordKey = pvProjectKey.getPasswordKey();
//            SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//            String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
//            pvProjectInfo.setCustomerName(encrypt);
//        }
        //获取租户id
//        String renterId = pvProjectInfo.getRenterId();
        if (StringUtils.isNotBlank(renterId)) {
            pvProjectInfo.setRenterId(renterId);
        }
        //如果 renterId 为空则代表是普通用户登录取他租户id即可
        else {
            pvProjectInfo.setRenterId(relTenantIds);
        }
        String id = loginUser.getId();
        String roleId = iRegionsService.getRoleId(id);
        pvProjectInfo.setOperator(loginUser.getUsername());
        //销售员查看
        if (roleId.equals("1632852352834883585")) {
            pvProjectInfo.setIsSeller("Y");
        }
        //如果一级商或者是二级商的角色
        if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
            List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
            if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                pvProjectInfo.setUserIds(allUsersCreatedBy);
            }
        }


        Page<PvProjectInfo> page = new Page<PvProjectInfo>(pageNo.longValue(), pageSize.longValue());
        IPage<PvProjectInfo> pageList = pvProjectInfoService.getLedger(page, pvProjectInfo);
        List<PvProjectInfo> records = pageList.getRecords();
        for (int i = 0; i < records.size(); i++) {
            //项目类型
            String projectType = records.get(i).getProjectType();
//            String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(projectType, null);
            PvProjectType pvProjectType = new PvProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
            } else {
                records.get(i).setProjectType("");
            }
            String regionPath = records.get(i).getAreaAddress();
            if (StringUtils.isNotBlank(regionPath)) {
                String fullAddress = records.get(i).getFullAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                regionPath = regionPath.substring(1, regionPath.length());
                String[] regionPathArray = regionPath.split(",");
                //给地区赋值
                StringBuilder stringBuilder = new StringBuilder();
                for (String path : regionPathArray) {
                    Regions region = iRegionsService.getModel(Integer.valueOf(path));
                    if (region == null) {
                        throw new IllegalArgumentException("地区不合法");
                    }
                    String localName = region.getLocalName();
                    stringBuilder.append(localName);
                }
                records.get(i).setAddress(stringBuilder.toString() + fullAddress);
            }

            // 解码关键信息返回前端
            PvProjectInfo projectInfo = records.get(i);
            this.decrypt(projectInfo);

        }
        if ("-2103508991".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectBeiJStatusEnum dicProjectBeijBpmEnum = DictProjectBeiJStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
        if ("102".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
        //徐州
        if ("118337537".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }   //预计安装容量赋值
        else {
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
    }


    public List<PvTjProjectInstall> getPvTjProjectInstalls(Integer projectId) {
        QueryWrapper<PvTjProjectInstall> pvTjProjectInstallQueryWrapper = new QueryWrapper<>();
        pvTjProjectInstallQueryWrapper.eq("project_id", projectId);
        pvTjProjectInstallQueryWrapper.orderByDesc("create_date");
        List<PvTjProjectInstall> list = iPvTjProjectInstallService.list(pvTjProjectInstallQueryWrapper);
        if (list != null & list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                PvTjProjectInstall pvTjProjectInstall = list.get(i);
                String remakesId = pvTjProjectInstall.getRemakesId();
                String fileId = pvTjProjectInstall.getFileId();
                if (StringUtils.isNotBlank(remakesId)) {
                    String[] remakesIds = remakesId.split(",");
                    QueryWrapper<PvProjectRemark> remarkQueryWrapper = new QueryWrapper<>();
                    List<String> lists = Arrays.asList(remakesIds);
                    remarkQueryWrapper.in("id", lists);
                    List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.list(remarkQueryWrapper);
                    if (pvProjectRemarks != null) {
                        list.get(i).setPvProjectRemark(pvProjectRemarks);
                    }
                }
                if (StringUtils.isNotBlank(fileId)) {
                    String[] fileIds = fileId.split(",");
                    QueryWrapper<PvProjectFile> fileQueryWrapper = new QueryWrapper<>();
                    List<String> lists1 = Arrays.asList(fileIds);
                    fileQueryWrapper.in("id", lists1);
                    List<PvProjectFile> pvProjectFileList = pvProjectFileService.list(fileQueryWrapper);
                    if (pvProjectFileList != null && pvProjectFileList.size() > 0) {
                        list.get(i).setPvProjectFile(pvProjectFileList);
                    }
                }
            }
            return list;
        }
        return new ArrayList<>();
    }

    public List<PvTjProjectInstall> getPvTjProjectInstallsOld(Integer projectId) {
        try {
            PvProjectInfo pvProjectInfo = pvProjectInfoService.getById(projectId);
            List<PvTjProjectInstall> objects = new ArrayList<>();
            //天津项目，所以参数固定
            QueryWrapper<PvProjectFile> pvProjectFileQueryWrapper = new QueryWrapper<>();
            pvProjectFileQueryWrapper.eq("project_id", projectId);
            //此处查询的是已存在的存量数据，估暂时先这么写
            pvProjectFileQueryWrapper.eq("label_text", "踏勘草图");
            PvProjectFile pvProjectFile = pvProjectFileService.getOne(pvProjectFileQueryWrapper);
            QueryWrapper<PvProjectRemark> remarkQueryWrapper = new QueryWrapper<>();
            remarkQueryWrapper.eq("project_id", projectId);
            remarkQueryWrapper.eq("label_text", "安装备注");
            PvProjectRemark pvProjectRemark = pvProjectRemarkService.getOne(remarkQueryWrapper);
            PvTjProjectInstall pvTjProjectInstall = new PvTjProjectInstall();
            BeanUtils.copyProperties(pvProjectInfo, pvTjProjectInstall);
//        String houseType = pvProjectInfo.getHouseType();
//        String instAngle = pvProjectInfo.getInstAngle();
//        BigDecimal instCmptCapacity = pvProjectInfo.getInstCmptCapacity();
//        Integer instCmptNumber = pvProjectInfo.getInstCmptNumber();
//        String instCmptArrange = pvProjectInfo.getInstCmptArrange();
            List<PvProjectFile> pvProjectFiles = new ArrayList<>();
            if (pvProjectFile != null) {
                pvProjectFiles.add(pvProjectFile);
                pvTjProjectInstall.setPvProjectFile(pvProjectFiles);
            } else {
                pvTjProjectInstall.setPvProjectFile(null);
            }

            List<PvProjectRemark> pvProjectRemarks = new ArrayList<>();
            if (pvProjectRemark != null) {
                pvProjectRemarks.add(pvProjectRemark);
                pvTjProjectInstall.setPvProjectRemark(pvProjectRemarks);
            } else {
                pvTjProjectInstall.setPvProjectRemark(null);
            }
            objects.add(pvTjProjectInstall);
            return objects;
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }


    /**
     * 回收站
     *
     * @param pvProjectInfo
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @ApiOperation(value = "回收站", notes = "回收站")
    @GetMapping(value = "/recycle")
    public Result<IPage<PvProjectInfo>> recycle(PvProjectInfo pvProjectInfo,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest req) {

        //获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String relTenantIds = loginUser.getRelTenantIds();
        String customerName = pvProjectInfo.getCustomerName();
        if (StringUtils.isNotBlank(customerName)) {
            String customerNameNew = customerName.replaceAll("\\*", "");
//            PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
//            String passwordKey = pvProjectKey.getPasswordKey();
//            SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//            String encrypt = AESUtils.encrypt(customerNameNew, secretKey);
            pvProjectInfo.setCustomerName(customerNameNew);
        }
        //获取租户id
        String renterId = pvProjectInfo.getRenterId();
        if (StringUtils.isNotBlank(renterId)) {
            pvProjectInfo.setRenterId(renterId);
        }
        //如果 renterId 为空则代表是普通用户登录取他租户id即可
        else {
            pvProjectInfo.setRenterId(relTenantIds);
        }

        pvProjectInfo.setOperator(loginUser.getUsername());
        String username = loginUser.getUsername();
        String id = loginUser.getId();
        String roleId = iRegionsService.getRoleId(id);
        //销售员查看
        if (roleId.equals("1632852352834883585")) {
            pvProjectInfo.setIsSeller("Y");
        }
        //如果一级商或者是二级商的角色
        if (roleId.equals("1791047313644818434") || roleId.equals("1791047421807529985")) {
            List<String> allUsersCreatedBy = this.findAllUsersCreatedBy(username);
            if (allUsersCreatedBy != null && allUsersCreatedBy.size() > 0) {
                pvProjectInfo.setUserIds(allUsersCreatedBy);
            }
        }

        Page<PvProjectInfo> page = new Page<>(pageNo, pageSize);
        IPage<PvProjectInfo> pageList = pvProjectInfoService.getRecycle(page, pvProjectInfo);
        List<PvProjectInfo> records = pageList.getRecords();
        for (int i = 0; i < records.size(); i++) {
            //项目类型
            String projectType = records.get(i).getProjectType();
//            String chineseDescription = NUMBER_TO_CHINESE.getOrDefault(projectType, null);
            PvProjectType pvProjectType = new PvProjectType();
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.pvProjectInfoService.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                records.get(i).setProjectType(chineseDescription);
            } else {
                records.get(i).setProjectType("");
            }
            String regionPath = records.get(i).getAreaAddress();
            if (StringUtils.isNotBlank(regionPath)) {
                String fullAddress = records.get(i).getFullAddress();
                String constrTeam = records.get(i).getConstrTeam();
                if (StringUtils.isNotBlank(constrTeam)) {
                    String nameByCode = iRegionsService.getNameByCode(constrTeam);
                    if (StringUtils.isNotBlank(nameByCode)) {
                        records.get(i).setConstrTeamName(nameByCode);
                    }
                }
                regionPath = regionPath.substring(1, regionPath.length());
                String[] regionPathArray = regionPath.split(",");
                //给地区赋值
                StringBuilder stringBuilder = new StringBuilder();
                for (String path : regionPathArray) {
                    Regions region = iRegionsService.getModel(Integer.valueOf(path));
                    if (region == null) {
                        throw new IllegalArgumentException("地区不合法");
                    }
                    String localName = region.getLocalName();
                    stringBuilder.append(localName);
                }
                records.get(i).setAddress(stringBuilder.toString() + fullAddress);
            }

            // 解码关键信息返回前端
            PvProjectInfo projectInfo = records.get(i);
            this.decrypt(projectInfo);

        }
        if ("-2103508991".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectBeiJStatusEnum dicProjectBeijBpmEnum = DictProjectBeiJStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
        if ("102".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
        //徐州
        if ("118337537".equals(renterId)) {
            for (int i = 0; i < records.size(); i++) {
                String totalCapacity = records.get(i).getTotalCapacity();
                BigDecimal instCmptCapacity = records.get(i).getInstCmptCapacity();
                if (StringUtils.isNotBlank(totalCapacity) || instCmptCapacity != null) {
                    records.get(i).setTotalCapacity(StringUtils.isNotBlank(totalCapacity) ? totalCapacity : instCmptCapacity.toString());
                }
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }   //预计安装容量赋值
        else {
            for (int i = 0; i < records.size(); i++) {
                String projectStatus = records.get(i).getProjectStatus();
                DictProjectStatusEnum dicProjectBeijBpmEnum = DictProjectStatusEnum.get(projectStatus);
                if (dicProjectBeijBpmEnum != null) {
                    records.get(i).setProjectStatus_dictText(dicProjectBeijBpmEnum.getName());
                }
            }
            return Result.OK(pageList);
        }
    }

    /**
     * 新增用户卡
     *
     * @param pvProjectInfoPage
     * @returnqueryCompletedById
     */
    @PvOperateLog(operateType = DictOperateTypeEnum.ADD)
    @AutoLog(value = "用户卡-新增")
    @ApiOperation(value = "工作台-新增",
            notes = "1、属性 id、projectId、projectCode、delFlag、createBy、createTime、updateBy、updateTime、sysOrgCode为平台自动生成，前端不用填写；<br>" +
                    "2、调用本接口时需去掉 pvProjectInfoPage.tenantId、pvProjectBpmList.tenantId、pvProjectFileList.tenantId、pvProjectRemarkList.tenantId 四个 tenantId 属性；<br>" +
                    "3、前端点击 保存 时需将 pvProjectBpmList.isCommitted 设置为 'N'；<br>" +
                    "4、前端点击 提交 时需将 pvProjectBpmList.isCommitted 设置为 'Y'。")

    //@RequiresPermissions("project:pv_project_info:add")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody PvProjectInfoPage pvProjectInfoPage) {
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("")) {
            return Result.error("请填写pvProjectBpm.isCommitted: Y/N; ");
        }
        PvProjectInfo pvProjectInfo = new PvProjectInfo();
        BeanUtils.copyProperties(pvProjectInfoPage, pvProjectInfo);
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectInfoService.saveMain(pvProjectInfo, pvProjectInfoPage.getPvProjectBpmList(), pvProjectInfoPage.getPvProjectFileList(), pvProjectInfoPage.getPvProjectRemarkList());
        //记日志使用
        BeanUtils.copyProperties(pvProjectInfo, pvProjectInfoPage);
        //Y提交，N保存
        //保存一个操作消息
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMsgType("1");
        deviceMessage.setCreateTime(new Date());
        deviceMessage.setPriority("1");
        deviceMessage.setUserId(loginUser.getId());
        if ((pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("Y"))) {
            deviceMessage.setMsgTitle("工作台-新增项目成功");
            iDeviceMessageService.save(deviceMessage);
            return Result.OK("提交成功！");
        } else {
            deviceMessage.setMsgTitle("工作台-保存项目成功");
            iDeviceMessageService.save(deviceMessage);
            return Result.OK("保存成功！");
        }
    }

    // 批量修改
    @PvOperateLog(operateType = DictOperateTypeEnum.EDIT)
    @AutoLog(value = "工作台--批量修改")
    @ApiOperation(value = "工作台--批量修改", notes = "工作台--批量修改")
    @PostMapping(value = "/updateBatch")
    public Result<String> updateBatch(@RequestBody updateBatchVo updateBatchVo1) {
        this.pvProjectInfoService.updateBatchMain(updateBatchVo1.getIds(), updateBatchVo1.getTypeId());
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMsgType("1");
        deviceMessage.setCreateTime(new Date());
        deviceMessage.setPriority("2");
        deviceMessage.setUserId(loginUser.getId());
        deviceMessage.setMsgTitle("工作台--批量修改成功");
        iDeviceMessageService.save(deviceMessage);

        return Result.OK("批量修改成功！");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @PvOperateLog(operateType = DictOperateTypeEnum.DELETE)
    @AutoLog(value = "工作台--通过id删除")
    @ApiOperation(value = "工作台--通过id删除", notes = "工作台--通过id删除")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")

    //@RequiresPermissions("project:pv_project_info:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMsgType("1");
        deviceMessage.setCreateTime(new Date());
        deviceMessage.setPriority("1");
        deviceMessage.setUserId(loginUser.getId());
        deviceMessage.setMsgTitle("工作台--删除成功");
        iDeviceMessageService.save(deviceMessage);


        pvProjectInfoService.delMain(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @PvOperateLog(operateType = DictOperateTypeEnum.DELETE)
    @AutoLog(value = "工作台--批量删除")
    @ApiOperation(value = "工作台--批量删除", notes = "工作台--批量删除")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    //@RequiresPermissions("project:pv_project_info:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.pvProjectInfoService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 待办-通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "工作台-待办-通过id查询")
    @ApiOperation(value = "工作台-待办-通过id查询", notes = "工作台-待办-通过id查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryToDoById")
    public Result<PvProjectInfoPage> queryToDoById(@RequestParam(name = "id", required = true) Integer id, @RequestParam(required = false) Integer status) {
        PvProjectInfo pvProjectInfo = pvProjectInfoService.getById(id);
        if (pvProjectInfo == null) {
            return Result.error("未找到对应数据");
        }
//        if (pvProjectInfo.getProjectStatus().equals(DictProjectStatusEnum.PROJECT_COMPLETED.getCode())) {
//            return Result.error("该项目已完成");
//        }
        String houseTypeGroup = pvProjectInfo.getHouseTypeGroup();
        if (StringUtils.isNotBlank(houseTypeGroup)) {
            List<HouseTypeVo> houseTypeVos = JSONObject.parseArray(houseTypeGroup, HouseTypeVo.class);
            pvProjectInfo.setTypeList(houseTypeVos);
        }

        // 解密数据
        this.decrypt(pvProjectInfo);

        PvProjectInfoPage pvProjectInfoPage = new PvProjectInfoPage();
        BeanUtils.copyProperties(pvProjectInfo, pvProjectInfoPage);
        pvProjectInfoPage.setPvProjectFileList(pvProjectFileService.selectByMainId(id));
        pvProjectInfoPage.setPvProjectRemarkList(pvProjectRemarkService.selectByMainId(id));
        //组件
        pvProjectInfoPage.setSysAssemblyVOS(iSysAssemblyService.getSysInverterVOS(id));
        //逆变器
        pvProjectInfoPage.setSysInverterVOS(iSysInverterService.getSysInverterVOS(id));
        Integer tenantId = pvProjectInfoPage.getTenantId();
        if (tenantId == -2103508991) {
            //天津的项目需要把数据重新封装给前端
            List<PvTjProjectInstall> pvTjProjectInstalls = this.getPvTjProjectInstalls(id);
            if (pvTjProjectInstalls != null && pvTjProjectInstalls.size() > 0) {
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstalls);
            } else {
                //旧数据重新封装
                List<PvTjProjectInstall> pvTjProjectInstallsOld = this.getPvTjProjectInstallsOld(id);
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstallsOld);
            }
            if (status != null) {
                pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByProjectIdBeiJ(pvProjectInfo.getId(), "100"));
            } else {
                pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByProjectIdBeiJ(pvProjectInfo.getId(), pvProjectInfo.getProjectStatus()));
            }
        } else {
            if (status != null) {
                pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByProjectId(pvProjectInfo.getId(), "80"));
            } else {
                pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByProjectId(pvProjectInfo.getId(), pvProjectInfo.getProjectStatus()));
            }
        }
        return Result.OK(pvProjectInfoPage);
    }

    /**
     * 通过id查询待办
     *
     * @param id
     * @return
     */
    @AutoLog(value = "工作台-已办-通过id查询")
    @ApiOperation(value = "工作台-已办-通过id查询", notes = "工作台-已办-通过id查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryCompletedById")
    public Result<PvProjectInfoPage> queryCompletedById(@RequestParam(name = "id", required = true) Integer id) {
        PvProjectInfo pvProjectInfo = pvProjectInfoService.getById(id);
        Integer tenantId = pvProjectInfo.getTenantId();
        if (pvProjectInfo == null) {
            return Result.error("未找到对应数据");
        }
        //这里先查询自己项目是否可以下载的状态，如果是2就直接返回，如果是1 则去查询e签宝的合同下载状态接口，如果查询到是可以下载，则更新项目的状态为可下载
        String isUpload = pvProjectInfo.getIsUpload();
        if (StringUtils.isNotBlank(isUpload) && !"2".equals(isUpload)) {
            //调用e签宝查询合同的接口
            String signFlowId = iPvCallbackService.getSignFlowId(id);
            if (StringUtils.isNotBlank(signFlowId)) {
                fileUploadUtils.getAndUpdateSignStatus(signFlowId, id);
            }
        }

        String houseTypeGroup = pvProjectInfo.getHouseTypeGroup();
        if (StringUtils.isNotBlank(houseTypeGroup)) {
            List<HouseTypeVo> houseTypeVos = JSONObject.parseArray(houseTypeGroup, HouseTypeVo.class);
            pvProjectInfo.setTypeList(houseTypeVos);
        }

        // 解密数据
        this.decrypt(pvProjectInfo);

        PvProjectInfoPage pvProjectInfoPage = new PvProjectInfoPage();
        BeanUtils.copyProperties(pvProjectInfo, pvProjectInfoPage);
        //组件
        pvProjectInfoPage.setSysAssemblyVOS(iSysAssemblyService.getSysInverterVOS(id));
        //逆变器
        pvProjectInfoPage.setSysInverterVOS(iSysInverterService.getSysInverterVOS(id));
        pvProjectInfoPage.setPvProjectFileList(pvProjectFileService.selectByMainId(id));
        pvProjectInfoPage.setPvProjectRemarkList(pvProjectRemarkService.selectByMainId(id));
        pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByMainId(id));
        //如果是天津的，需要重新封装安装信息那一块
        if (tenantId == 102 || tenantId == -1759715327) {
            List<PvTjProjectInstall> pvTjProjectInstalls = this.getPvTjProjectInstalls(id);
            if (pvTjProjectInstalls != null && pvTjProjectInstalls.size() > 0) {
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstalls);
            } else {
                //旧数据重新封装
                List<PvTjProjectInstall> pvTjProjectInstallsOld = this.getPvTjProjectInstallsOld(id);
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstallsOld);
            }

        }
        if (tenantId == 118337537) {
            List<PvTjProjectInstall> pvTjProjectInstalls = this.getPvTjProjectInstalls(id);
            if (pvTjProjectInstalls != null && pvTjProjectInstalls.size() > 0) {
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstalls);
            } else {
                //旧数据重新封装
                List<PvTjProjectInstall> pvTjProjectInstallsOld = this.getPvTjProjectInstallsOld(id);
                pvProjectInfoPage.setProjectInstalls(pvTjProjectInstallsOld);
            }
        }

        return Result.OK(pvProjectInfoPage);
    }

    /**
     * 业务办理
     *
     * @param pvProjectInfoPage
     * @return
     */
//    @PvOperateLog(operateType = DictOperateTypeEnum.EDIT)
    @AutoLog(value = "工作台-业务办理")
    @ApiOperation(value = "工作台-业务办理", notes =
            "1、本接口处理的业务包括：销售员修改项目信息、资料员审核、施工管理员分配施工队、施工队上传施工图片、巡检员审核、并网员并网、资料员补充资料等业务；<br>" +
                    "2、调用本接口前调用 待办-通过id查询 接口获取客户项目待办相关信息；<br>" +
                    "3、如果点击 提交 按钮， 需要将 pvProjectBpmList.isCommitted 设置为 'Y'；")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    //@RequiresPermissions("project:pv_project_info:edit")
    @RequestMapping(value = "/businessHandling", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> businessHandling(@RequestBody PvProjectInfoPage pvProjectInfoPage) {
        //业务办理
        PvProjectInfo pvProjectInfo = new PvProjectInfo();
        BeanUtils.copyProperties(pvProjectInfoPage, pvProjectInfo);
        PvProjectInfo pvProjectInfoEntity = pvProjectInfoService.getById(pvProjectInfo.getId());
        Integer tenantId = pvProjectInfo.getTenantId();
        String projectStatus = pvProjectInfoEntity.getProjectStatus();

        //判断一下传入的状态与当前操作的角色是否匹配
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String id = loginUser.getId();
        String roleId = iRegionsService.getRoleId(id);
        Boolean aBoolean = this.checkUserRole(roleId, projectStatus, String.valueOf(tenantId));
        if (!aBoolean) {
            return Result.error("当前操作项目状态已变更，请刷新页面！");
        }
        if (pvProjectInfoEntity == null) {
            return Result.error("未找到对应数据");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult().equals("")) {
            return Result.error("请填写pvProjectBom.ApprovalResult: approved/rejected; ");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("")) {
            return Result.error("请填写pvProjectBpm.isCommitted: Y/N; ");
        }
        pvProjectInfoService.updateMain(pvProjectInfo, pvProjectInfoPage.getPvProjectBpmList(), pvProjectInfoPage.getPvProjectFileList(), pvProjectInfoPage.getPvProjectRemarkList());
        if ((pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("Y"))) {
            return Result.OK("提交成功！");
        } else {
            return Result.OK("保存成功！");
        }
    }


    /**
     * 校验角色（防止重复提交）
     */
    public Boolean checkUserRole(String roleId, String projectStatus, String renterId) {
        if (renterId.equals("-2103508991")) {
            //销售员只能操作销售员
            if (roleId.equals("1632852352834883585")) {
                if (projectStatus.equals("10") || projectStatus.equals("21")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工管理员
            if (roleId.equals("1705793382893690881")) {
                if (projectStatus.equals("50")) {
                    return true;
                } else {
                    return false;
                }
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                if (projectStatus.equals("20") || projectStatus.equals("30") || projectStatus.equals("100")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工队
            if (roleId.equals("1632852669798436865")) {
                if (projectStatus.equals("50") || projectStatus.equals("61")) {
                    return true;
                } else {
                    return false;
                }
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                if (projectStatus.equals("60") || projectStatus.equals("61") || projectStatus.equals("80")) {
                    return true;
                } else {
                    return false;
                }
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                if (projectStatus.equals("70") || projectStatus.equals("90")) {
                    return true;
                } else {
                    return false;
                }
            }

        }
        if (renterId.equals("102")) {
            //销售员只能操作销售员应该操作的状态
            if (roleId.equals("1632852352834883585")) {
                if (projectStatus.equals("10") || projectStatus.equals("21")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工管理员
            if (roleId.equals("1705793382893690881") || roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632853052226686977")) {
                if (projectStatus.equals("40") || projectStatus.equals("60") || projectStatus.equals("70") || projectStatus.equals("90")) {
                    return true;
                } else {
                    return false;
                }
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                if (projectStatus.equals("20") || projectStatus.equals("30") || projectStatus.equals("80")) {
                    return true;
                } else {
                    return false;
                }
            }

            //施工队
            if (roleId.equals("1632852669798436865")) {
                if (projectStatus.equals("50") || projectStatus.equals("61")) {
                    return true;
                } else {
                    return false;
                }
            }
            //巡检员  监理
//            if(roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")){
//                if(projectStatus.equals("60")){
//                    return true;
//                }else {
//                    return false;
//                }
//            }
            //并网员
//            if(roleId.equals("1632853052226686977")){
//                if(projectStatus.equals("70") || projectStatus.equals("90")){
//                    return true;
//                }else {
//                    return false;
//                }
//            }


        }
        //廊坊
        if (renterId.equals("-1759715327")) {
            //销售员只能操作销售员应该操作的状态
            if (roleId.equals("1632852352834883585")) {
                if (projectStatus.equals("10") || projectStatus.equals("21")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工管理员
            if (roleId.equals("1705793382893690881") || roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632853052226686977")) {
                if (projectStatus.equals("40") || projectStatus.equals("60") || projectStatus.equals("70") || projectStatus.equals("90")) {
                    return true;
                } else {
                    return false;
                }
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                if (projectStatus.equals("20") || projectStatus.equals("30") || projectStatus.equals("80")) {
                    return true;
                } else {
                    return false;
                }
            }

            //施工队
            if (roleId.equals("1632852669798436865")) {
                if (projectStatus.equals("50") || projectStatus.equals("61")) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        //徐州
        if (renterId.equals("118337537")) {
            //销售员只能操作销售员应该操作的状态
            if (roleId.equals("1632852352834883585")) {
                if (projectStatus.equals("10") || projectStatus.equals("21")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工管理员
            if (roleId.equals("1705793382893690881") || roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802") || roleId.equals("1632853052226686977")) {
                if (projectStatus.equals("40") || projectStatus.equals("60") || projectStatus.equals("70") || projectStatus.equals("90")) {
                    return true;
                } else {
                    return false;
                }
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                if (projectStatus.equals("20") || projectStatus.equals("30") || projectStatus.equals("80")) {
                    return true;
                } else {
                    return false;
                }
            }

            //施工队
            if (roleId.equals("1632852669798436865")) {
                if (projectStatus.equals("50") || projectStatus.equals("61")) {
                    return true;
                } else {
                    return false;
                }
            }
            //巡检员  监理
//            if(roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")){
//                if(projectStatus.equals("60")){
//                    return true;
//                }else {
//                    return false;
//                }
//            }
            //并网员
//            if(roleId.equals("1632853052226686977")){
//                if(projectStatus.equals("70") || projectStatus.equals("90")){
//                    return true;
//                }else {
//                    return false;
//                }
//            }
        } else {
            //销售员只能操作销售员应该操作的状态
            if (roleId.equals("1632852352834883585")) {
                if (projectStatus.equals("10") || projectStatus.equals("21")) {
                    return true;
                } else {
                    return false;
                }
            }
            //施工管理员
            if (roleId.equals("1705793382893690881")) {
                if (projectStatus.equals("40")) {
                    return true;
                } else {
                    return false;
                }
            }

            //资料员
            if (roleId.equals("1632852197175873538")) {
                if (projectStatus.equals("20") || projectStatus.equals("30") || projectStatus.equals("80")) {
                    return true;
                } else {
                    return false;
                }
            }

            //施工队
            if (roleId.equals("1632852669798436865")) {
                if (projectStatus.equals("50") || projectStatus.equals("61")) {
                    return true;
                } else {
                    return false;
                }
            }
            //巡检员  监理
            if (roleId.equals("1632852792637018113") || roleId.equals("1707015958923980802")) {
                if (projectStatus.equals("60")) {
                    return true;
                } else {
                    return false;
                }
            }
            //并网员
            if (roleId.equals("1632853052226686977")) {
                if (projectStatus.equals("70") || projectStatus.equals("90")) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * 业务办理
     *
     * @param pvProjectInfoPage
     * @return
     */
//    @PvOperateLog(operateType = DictOperateTypeEnum.EDIT)
    @AutoLog(value = "工作台-操作人员保存用")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @RequestMapping(value = "/keepProjectInfo", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> keepProjectInfo(@RequestBody PvProjectInfoPage pvProjectInfoPage) {
        PvProjectInfo pvProjectInfo = new PvProjectInfo();
        BeanUtils.copyProperties(pvProjectInfoPage, pvProjectInfo);
        PvProjectInfo pvProjectInfoEntity = pvProjectInfoService.getById(pvProjectInfo.getId());
        if (pvProjectInfoEntity == null) {
            return Result.error("未找到对应数据");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult().equals("")) {
            return Result.error("请填写pvProjectBom.ApprovalResult: approved/rejected; ");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("")) {
            return Result.error("请填写pvProjectBpm.isCommitted: Y/N; ");
        }
        pvProjectInfoService.keepProjectInfo(pvProjectInfo, pvProjectInfoPage.getPvProjectBpmList(), pvProjectInfoPage.getPvProjectFileList(), pvProjectInfoPage.getPvProjectRemarkList());
        if ((pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("Y"))) {
            return Result.OK("提交成功！");
        } else {
            return Result.OK("保存成功！");
        }
    }


    /**
     * 归档员编辑
     *
     * @param pvProjectInfoPage
     * @return
     */
//    @PvOperateLog(operateType = DictOperateTypeEnum.EDIT)
    @AutoLog(value = "归档员编辑")
    @ApiOperation(value = "工作台-业务办理", notes =
            "1、本接口处理的业务包括：销售员修改项目信息、资料员审核、施工管理员分配施工队、施工队上传施工图片、巡检员审核、并网员并网、资料员补充资料等业务；<br>" +
                    "2、调用本接口前调用 待办-通过id查询 接口获取客户项目待办相关信息；<br>" +
                    "3、如果点击 提交 按钮， 需要将 pvProjectBpmList.isCommitted 设置为 'Y'；")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    //@RequiresPermissions("project:pv_project_info:edit")
    @RequestMapping(value = "/updateFile", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> updateFile(@RequestBody PvProjectInfoPage pvProjectInfoPage) {
        PvProjectInfo pvProjectInfo = new PvProjectInfo();
        BeanUtils.copyProperties(pvProjectInfoPage, pvProjectInfo);
        PvProjectInfo pvProjectInfoEntity = pvProjectInfoService.getById(pvProjectInfo.getId());
        if (pvProjectInfoEntity == null) {
            return Result.error("未找到对应数据");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult().equals("")) {
            return Result.error("请填写pvProjectBom.ApprovalResult: approved/rejected; ");
        }
        if (pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted() == null || pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted().equals("")) {
            return Result.error("请填写pvProjectBpm.isCommitted: Y/N; ");
        }
        pvProjectInfoService.updateMainFile(pvProjectInfo, pvProjectInfoPage.getPvProjectBpmList(), pvProjectInfoPage.getPvProjectFileList(), pvProjectInfoPage.getPvProjectRemarkList());

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMsgType("1");
        deviceMessage.setCreateTime(new Date());
        deviceMessage.setPriority("2");
        deviceMessage.setUserId(loginUser.getId());
        deviceMessage.setMsgTitle("工作台--归档编辑");
        iDeviceMessageService.save(deviceMessage);

        return Result.OK("业务办理成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "项目业务流程-通过主表ID查询")
    @ApiOperation(value = "项目业务流程-通过主表ID查询", notes = "项目业务流程-通过主表ID查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryPvProjectBpmByMainId")
    public Result<List<PvProjectBpm>> queryPvProjectBpmListByMainId(@RequestParam(name = "id", required = true) Integer id) {
        List<PvProjectBpm> pvProjectBpmList = pvProjectBpmService.selectByMainId(id);
        if (pvProjectBpmList == null || pvProjectBpmList.size() == 0) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(pvProjectBpmList);
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "项目文件查询-通过主表ID查询")
    @ApiOperation(value = "项目文件查询-通过主表ID查询", notes = "项目文件查询-通过主表ID查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryPvProjectFileByMainId")

    public Result<List<PvProjectFile>> queryPvProjectFileListByMainId(@RequestParam(name = "id", required = true) Integer id) {
        List<PvProjectFile> pvProjectFileList = pvProjectFileService.selectByMainId(id);
        if (pvProjectFileList == null || pvProjectFileList.size() == 0) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(pvProjectFileList);
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "项目备注查询-通过主表ID查询")
    @ApiOperation(value = "项目备注查询-通过主表ID查询", notes = "项目备注查询-通过主表ID查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryPvProjectRemarkByMainId")
    public Result<List<PvProjectRemark>> queryPvProjectRemarkListByMainId(@RequestParam(name = "id", required = true) Integer id) {
        List<PvProjectRemark> pvProjectRemarkList = pvProjectRemarkService.selectByMainId(id);
        if (pvProjectRemarkList == null || pvProjectRemarkList.size() == 0) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(pvProjectRemarkList);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param pvProjectInfo
     */
    //@RequiresPermissions("project:pv_project_info:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        // Step.1 组装查询条件查询数据
        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, request.getParameterMap());
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        //配置选中数据查询条件
        String selections = request.getParameter("selections");
        if (oConvertUtils.isNotEmpty(selections)) {
            List<String> selectionList = Arrays.asList(selections.split(","));
            queryWrapper.in("id", selectionList);
        }
        //Step.2 获取导出数据
        List<PvProjectInfo> pvProjectInfoList = pvProjectInfoService.list(queryWrapper);

        // Step.3 组装pageList
        List<PvProjectInfoPage> pageList = new ArrayList<PvProjectInfoPage>();
        for (PvProjectInfo main : pvProjectInfoList) {
            PvProjectInfoPage vo = new PvProjectInfoPage();
            BeanUtils.copyProperties(main, vo);
            List<PvProjectBpm> pvProjectBpmList = pvProjectBpmService.selectByMainId(main.getId());
            vo.setPvProjectBpmList(pvProjectBpmList);
            List<PvProjectFile> pvProjectFileList = pvProjectFileService.selectByMainId(main.getId());
            vo.setPvProjectFileList(pvProjectFileList);
            List<PvProjectRemark> pvProjectRemarkList = pvProjectRemarkService.selectByMainId(main.getId());
            vo.setPvProjectRemarkList(pvProjectRemarkList);
            pageList.add(vo);
        }

        // Step.4 AutoPoi 导出Excel
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目管理列表");
        mv.addObject(NormalExcelConstants.CLASS, PvProjectInfoPage.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMsgType("1");
        deviceMessage.setCreateTime(new Date());
        deviceMessage.setPriority("1");
        deviceMessage.setUserId(loginUser.getId());
        deviceMessage.setMsgTitle("导出excel");
        iDeviceMessageService.save(deviceMessage);

        return mv;
    }


    // 1. 触发导出（立即返回任务ID）
    @GetMapping("/start")
    public Result startExport(HttpServletRequest request, PvProjectInfo pvProjectInfo, HttpServletResponse response) {
        String taskId = "TASK_" + System.currentTimeMillis();
        String renterId = pvProjectInfo.getRenterId();
        // 天津
        if ("102".equals(renterId)) {
            pvProjectInfoService.getList(taskId,request, pvProjectInfo);
        }
        // 北京
        if ("-2103508991".equals(renterId)) {
            pvProjectInfoService.getListBeiJ(taskId,request, pvProjectInfo);
        }
        // 徐州
        if ("118337537".equals(renterId)) {
            pvProjectInfoService.getListXuZ(taskId,request, pvProjectInfo);
        }
        // 廊坊
        if ("-1759715327".equals(renterId)) {
            pvProjectInfoService.getListLF(taskId,request, pvProjectInfo);
        }
        // 河南
        if ("507514882".equals(renterId)) {
            pvProjectInfoService.getListHeN(taskId,request, pvProjectInfo);
        }
        // 海南
        if ("-624947198".equals(renterId)) {
            pvProjectInfoService.getListHaiN(taskId,request, pvProjectInfo);
        }
        // 海南二期
        if ("1964826626".equals(renterId)) {
            pvProjectInfoService.getListHaiN2(taskId,request, pvProjectInfo);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("taskId",taskId);
        resultMap.put("message","操作成功");
        return Result.ok(resultMap);
    }

    // 2. 状态查询接口
    @GetMapping("/status/{taskId}")
    public Result getStatus(@PathVariable String taskId) {
        List<Object> taskStatus = pvProjectInfoService.getTaskStatus(taskId);
        return Result.ok(taskStatus);
    }
    /**
     * 3.导出
     *
     * @param request
     * @param pvProjectInfo
     */
    //@RequiresPermissions("project:pv_project_info:exportXls")
    @RequestMapping(value = "/pvpExportXls")
    public ModelAndView pvpExportXls(String taskId,HttpServletRequest request, PvProjectInfo pvProjectInfo, HttpServletResponse response) {
        // Step.1 组装查询条件查询数据
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String tenantId = pvProjectInfo.getRenterId();
        //天津
        if (tenantId.equals("102")) {
            List<ProjectExportVO> list = pvProjectInfoService.getList(taskId,request, pvProjectInfo);
//            Map<Object, Object> entries = redisTemplate.opsForHash().entries("export:task:" + taskId);
//            List<ProjectExportVO> list = entries.values().stream()
//                    .map(obj -> (ProjectExportVO) obj)
//                    .collect(Collectors.toList());
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVO> collect = range.stream()
                    .map(obj -> (ProjectExportVO) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            if (collect.size() > 0) {
                for (ProjectExportVO projectExportVO : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVO, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVO.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVO.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVO.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVO.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
//            System.err.println(collect);
            // Step.4 AutoPoi 导出Excel
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVO.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }
        //徐州
        if (tenantId.equals("118337537")) {
//            List<ProjectExportVOXuZ> list = pvProjectInfoService.getListXuZ(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOXuZ> collect = range.stream()
                    .map(obj -> (ProjectExportVOXuZ) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            // Step.4 AutoPoi 导出Excel
            if (collect.size() > 0) {
                for (ProjectExportVOXuZ projectExportVOXuZ : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOXuZ, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOXuZ.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOXuZ.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOXuZ.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOXuZ.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOXuZ.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }
        //北京
        if (tenantId.equals("-2103508991")) {
//            List<ProjectExportVOBeij> list = pvProjectInfoService.getListBeiJ(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOBeij> collect = range.stream()
                    .map(obj -> (ProjectExportVOBeij) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            if (collect.size() > 0) {
                for (ProjectExportVOBeij projectExportVOBeij : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOBeij, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOBeij.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOBeij.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOBeij.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOBeij.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                    projectExportVOBeij.setHouseOwner(pvProjectInfoNew.getHouseOwner());
                    projectExportVOBeij.setElectricityOwner(pvProjectInfoNew.getElectricityOwner());
                    projectExportVOBeij.setElectricityCardNo(pvProjectInfoNew.getElectricityCardNo());
                    projectExportVOBeij.setReturnFeeDebitCardOwner(pvProjectInfoNew.getReturnFeeDebitCardOwner());
                    projectExportVOBeij.setReturnFeeDebitCardNo(pvProjectInfoNew.getReturnFeeDebitCardNo());
                }
            }
            // Step.4 AutoPoi 导出Excel
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOBeij.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }
        //海南
        if (tenantId.equals("-624947198")) {
//            List<ProjectExportVOHaiN> list = pvProjectInfoService.getListHaiN(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOHaiN> collect = range.stream()
                    .map(obj -> (ProjectExportVOHaiN) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            // Step.4 AutoPoi 导出Excel
            if (collect.size() > 0) {
                for (ProjectExportVOHaiN projectExportVOHaiN : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOHaiN, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOHaiN.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOHaiN.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOHaiN.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOHaiN.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOHaiN.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }
        //海南二期
        if (tenantId.equals("1964826626")) {
//            List<ProjectExportVOHaiN2> list = pvProjectInfoService.getListHaiN2(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOHaiN2> collect = range.stream()
                    .map(obj -> (ProjectExportVOHaiN2) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            // Step.4 AutoPoi 导出Excel
            if (collect.size() > 0) {
                for (ProjectExportVOHaiN2 projectExportVOHaiN2 : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOHaiN2, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOHaiN2.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOHaiN2.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOHaiN2.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOHaiN2.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOHaiN2.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }

        //河南
        if (tenantId.equals("507514882")) {
//            List<ProjectExportVOHeN> list = pvProjectInfoService.getListHeN(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOHeN> collect = range.stream()
                    .map(obj -> (ProjectExportVOHeN) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            // Step.4 AutoPoi 导出Excel
            if (collect.size() > 0) {
                for (ProjectExportVOHeN projectExportVOHeN : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOHeN, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOHeN.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOHeN.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOHeN.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOHeN.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOHeN.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }

        //廊坊
        if (tenantId.equals("-1759715327")) {
//            List<ProjectExportVOLF> list = pvProjectInfoService.getListLF(taskId, request, pvProjectInfo);
            List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
            redisTemplate.delete("Task:" + taskId);
            List<ProjectExportVOLF> collect = range.stream()
                    .map(obj -> (ProjectExportVOLF) obj)  // 强制类型转换
                    .collect(Collectors.toList());
            // Step.4 AutoPoi 导出Excel
            if (collect.size() > 0) {
                for (ProjectExportVOLF projectExportVOLF : collect) {
                    PvProjectInfo pvProjectInfoNew = new PvProjectInfo();
                    pvProjectInfoNew.setTenantId(Integer.valueOf(tenantId));
                    BeanUtils.copyProperties(projectExportVOLF, pvProjectInfoNew);
                    this.decrypt(pvProjectInfoNew);
//                    projectExportVOLF.setCustomerName(pvProjectInfoNew.getCustomerName());
                    projectExportVOLF.setIdNumber(pvProjectInfoNew.getIdNumber());
                    projectExportVOLF.setCustomerMobile(pvProjectInfoNew.getCustomerMobile());
                    projectExportVOLF.setConBankCardHolders(pvProjectInfoNew.getConBankCardHolders());
                }
            }
            ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
            mv.addObject(NormalExcelConstants.FILE_NAME, "客户项目列表");
            mv.addObject(NormalExcelConstants.CLASS, ProjectExportVOLF.class);
            mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("客户项目管理数据", "导出人:" + sysUser.getRealname(), "客户项目管理"));
            mv.addObject(NormalExcelConstants.DATA_LIST, collect);
            return mv;
        }
        return new ModelAndView();

    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    //@RequiresPermissions("project:pv_project_info:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            // 获取上传文件对象
            MultipartFile file = entity.getValue();
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<PvProjectInfoPage> list = ExcelImportUtil.importExcel(file.getInputStream(), PvProjectInfoPage.class, params);
                for (PvProjectInfoPage page : list) {
                    PvProjectInfo po = new PvProjectInfo();
                    BeanUtils.copyProperties(page, po);
                    pvProjectInfoService.saveMain(po, page.getPvProjectBpmList(), page.getPvProjectFileList(), page.getPvProjectRemarkList());
                }
                return Result.OK("文件导入成功！数据行数:" + list.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.OK("文件导入失败！");
    }

    /**
     * 首页数据
     *
     * @return
     */
    //@RequiresPermissions("project:pv_project_info:importExcel")
    @RequestMapping(value = "/homeData", method = RequestMethod.GET)
    public Result<?> homeData(PvProjectInfo pvProjectInfo) {
        HomePageData homePageData = pvProjectInfoService.getHomePageData(pvProjectInfo);
        return Result.ok(homePageData);
    }


    /**
     * 获取村名
     *
     * @return
     */
    //@RequiresPermissions("project:pv_project_info:importExcel")
    @RequestMapping(value = "/getVillage/{id}", method = RequestMethod.GET)
    public Result<?> getVillage(@PathVariable Integer id) {
        Regions model = iRegionsService.getModel(id);
        if (model != null) {
            return Result.okVillage(model.getLocalName());
        }
        return Result.ok();
    }


    /**
     * 首页地图签约数量
     *
     * @return
     */
    @RequestMapping(value = "/homeDataCount", method = RequestMethod.GET)
    public Result<?> homeDataCount(PvProjectInfo pvProjectInfo) {
        List<LocationVO> list = pvProjectInfoService.getHomeDataCount(pvProjectInfo);
        return Result.ok(list);
    }


    /**
     * 首页地图签约数量
     *
     * @return
     */
    @RequestMapping(value = "/updateAreaAddress", method = RequestMethod.POST)
    public Result<?> updateAreaAddress(@RequestBody PvProjectInfo pvProjectInfo) {
        Result result = pvProjectInfoService.updateAreaAddress(pvProjectInfo);
        return Result.ok(result);
    }

    /**
     * 获取租户项目id
     *
     * @return
     */
    @RequestMapping(value = "/getRenterId", method = RequestMethod.GET)
    public Result<?> getRenterId() {
        Result result = pvProjectInfoService.getRenterId();
        return Result.ok(result);
    }


//    @Value(value = "${jeecg.path.upload}")
//    private String uploadpath;
//
//    @Value(value = "${jeecg.domainUrl.pdf}")
//    private String outPdf;

    /**
     * 生成pdf
     *
     * @param pdfVO
     * @return
     * @throws Exception
     */
    @PostMapping("/complexPDF")
    public Result<?> complexPDF(@RequestBody PdfVO pdfVO) throws Exception {

        //获取图片路径，通过传来的路径获取需要封装的图片
        List<String> newPath = new ArrayList<>();
        List<Long> imgIds = pdfVO.getImgIds();
        for (int i = 0; i < imgIds.size(); i++) {
            String imgUrl = iRegionsService.getImgUrl(imgIds.get(i));
            newPath.add(imgUrl);
        }
        String projectCode = pdfVO.getProjectCode();
        String p = allPath + projectCode + ".pdf";
        //先判断是否有这个文件
        File file = new File(uploadpath + "/" + projectCode + ".pdf");
        if (!file.exists()) {
            try {
                Document document = new Document(PageSize.A4, 10, 10, 10, 10);
                //相对路径生成pdf
                PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(uploadpath, projectCode + ".pdf")));
                document.open();
                Rectangle pageSize = document.getPageSize();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                for (int i = 0; i < newPath.size(); i++) {
                    Path pathImg = Paths.get(uploadpath + "/" + newPath.get(i));
                    Image image = Image.getInstance(pathImg.toString());
                    try {
                        image.setAlignment(Image.ALIGN_CENTER);
                        image.scaleToFit(pageWidth - 20, pageHeight - 20);
                        document.add(image);
                    } catch (Exception e) {
                    }

                }
                document.close();
                //返回给前端全路径
                return Result.downLoadOk(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //返回给前端全路径
        return Result.downLoadOk(p);
    }


    /**
     * 批量导出pdf
     *
     * @param pdfVO
     * @return
     * @throws Exception
     */
    @PostMapping("/batchPDF")
    public Result<?> batchPDF(@RequestBody PdfVO pdfVO) throws Exception {

        //获取项目id。根据每个项目id查询该项目里面所有的图片，再导出pdf
        List<Integer> projectIds = pdfVO.getProjectId();
        if (projectIds != null) {


            List<String> strings = new ArrayList<>();
            for (int i = 0; i < projectIds.size(); i++) {
                //获取到单个项目里面的所有图片的路径。用于导出
                PvProjectInfo byId = pvProjectInfoService.getById(projectIds.get(i));
                String projectCode = byId.getProjectCode();
                String customerName = byId.getCustomerName();

                List<String> collect = pvProjectFileService.selectFilesId(projectIds.get(i));
                List<String> pvProjectFiles = collect.stream().filter(l -> StringUtils.isNotBlank(l)).collect(Collectors.toList());
                String p = allPath + customerName + "-" + projectCode + ".pdf";
                //先判断是否有这个文件
                File file = new File(uploadpath + "/" + customerName + "-" + projectCode + ".pdf");
                if (file.exists()) {
                    file.delete();
                }
                if (!file.exists()) {
                    try {
                        Document document = new Document(PageSize.A4, 10, 10, 10, 10);
                        //相对路径生成pdf
                        PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(uploadpath, customerName + "-" + projectCode + ".pdf")));
                        document.open();
                        Rectangle pageSize = document.getPageSize();
                        float pageWidth = pageSize.getWidth();
                        float pageHeight = pageSize.getHeight();
                        //先来个判断（如果该项目一张图片都没有。那么就返回当前项目没有图片的提示）
                        List<File> files = new ArrayList<>();
                        for (int j = 0; j < pvProjectFiles.size(); j++) {
                            File img = new File(uploadpath + "/" + pvProjectFiles.get(j));
                            if (img.exists()) {
                                files.add(img);
                            }
                        }
                        if (files != null && files.size() > 0) {
                            for (int j = 0; j < pvProjectFiles.size(); j++) {
                                Path pathImg = Paths.get(uploadpath + "/" + pvProjectFiles.get(j));
                                try {
                                    Image image = Image.getInstance(pathImg.toString());
                                    image.setAlignment(Image.ALIGN_CENTER);
                                    image.scaleToFit(pageWidth - 20, pageHeight - 20);
                                    document.add(image);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            System.err.println(document);
                            document.close();
                            //返回给前端全路径
                            strings.add(p);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return Result.OK(strings);
        }
        return Result.ok();
    }


    /**
     * 获取租户项目id
     *
     * @return
     */
    @RequestMapping(value = "/getMods/{id}", method = RequestMethod.GET)
    public Result<?> getMods(@PathVariable Integer id) {
        List<SysModel> models = modelService.getModels(id);
        return Result.ok(models);
    }


    /**
     * 恢复
     *
     * @return
     */
    @RequestMapping(value = "/updateByProjectId", method = RequestMethod.POST)
    public Result<?> updateByProjectId(@RequestBody PdfVO pdfVO) {
        List<Integer> projectId = pdfVO.getProjectId();
        if (!projectId.isEmpty()) {
            return Result.ok(pvProjectInfoService.updateByProjectId(projectId));
        } else {
            return Result.error("更新失败！");
        }
    }


    /**
     * 回收站
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "回收站-通过id查询", notes = "回收站-通过id查询")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/queryRecycleId")
    public Result<PvProjectInfoPage> queryRecycleId(@RequestParam(name = "id", required = true) Integer id) {
        QueryWrapper<PvProjectInfo> pvProjectInfoQueryWrapper = new QueryWrapper<>();
        PvProjectInfo pvProjectInfo = pvProjectInfoService.getByProjectId(id);
        if (pvProjectInfo == null) {
            return Result.error("未找到对应数据");
        }
        // 解码关键信息返回前端
        this.decrypt(pvProjectInfo);
        PvProjectInfoPage pvProjectInfoPage = new PvProjectInfoPage();
        BeanUtils.copyProperties(pvProjectInfo, pvProjectInfoPage);
        //组件
        pvProjectInfoPage.setSysAssemblyVOS(iSysAssemblyService.getSysInverterVOS(id));
        //逆变器
        pvProjectInfoPage.setSysInverterVOS(iSysInverterService.getSysInverterVOS(id));
        pvProjectInfoPage.setPvProjectFileList(pvProjectFileService.selectByMainId(id));
        pvProjectInfoPage.setPvProjectRemarkList(pvProjectRemarkService.selectByMainId(id));
        pvProjectInfoPage.setPvProjectBpmList(pvProjectBpmService.selectByMainId(id));
        return Result.OK(pvProjectInfoPage);
    }


    /**
     * 生成pdf
     *
     * @param pdfVO
     * @return
     * @throws Exception
     */
    @PostMapping("/imgFormPDF")
    public List<String> imgFormPDF(@RequestBody PdfVO pdfVO) throws Exception {

        //获取项目的id
        List<Integer> projectId = pdfVO.getProjectId();
        List<String> strings = new ArrayList<>();
        if (!projectId.isEmpty()) {
            for (int i = 0; i < projectId.size(); i++) {
                Integer aLong = projectId.get(i);
                PvProjectInfo byId = pvProjectInfoService.getById(aLong);
                //项目编号
                String projectCode = byId.getProjectCode();
                //拿到该项目的所有文件路径
                List<PvProjectFile> pvProjectFiles = pvProjectFileService.selectByMainId(aLong);
                if (!pvProjectFiles.isEmpty()) {
                    List<String> impUrls = new ArrayList<>();
                    pvProjectFiles.stream().forEach(j -> {
                        String fileUrl = j.getFileUrl();
                        impUrls.add(fileUrl);
                    });
                    String p = allPath + projectCode + ".pdf";
                    //先判断是否有这个文件
                    File file = new File(uploadpath + "/" + projectCode + ".pdf");
                    if (file.isFile() && file.exists()) {
                        file.delete();
                    }
                    if (!file.exists()) {
                        //先删除再生成
                        try {
                            Document document = new Document(PageSize.A4, 10, 10, 10, 10);
                            //相对路径生成pdf
                            PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(uploadpath, projectCode + ".pdf")));
                            document.open();
                            Rectangle pageSize = document.getPageSize();
                            float pageWidth = pageSize.getWidth();
                            float pageHeight = pageSize.getHeight();
                            for (int k = 0; k < impUrls.size(); k++) {
                                Path pathImg = Paths.get(uploadpath + "/" + impUrls.get(k));
                                Image image = Image.getInstance(pathImg.toString());
                                try {
                                    image.setAlignment(Image.ALIGN_CENTER);
                                    image.scaleToFit(pageWidth - 20, pageHeight - 20);
                                    document.add(image);
                                } catch (Exception e) {
                                    new ArrayList<String>();
                                }
                            }
                            document.close();
                            //返回给前端全路径
                            strings.add(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return strings;
        }
//        List<String> newPath = new ArrayList<>();
//        List<Long> imgIds = pdfVO.getImgIds();
//        for (int i = 0; i < imgIds.size(); i++) {
//            String imgUrl = iRegionsService.getImgUrl(imgIds.get(i));
//            newPath.add(imgUrl);
//        }
//        String projectCode = pdfVO.getProjectCode();
//        String p=allPath+projectCode+".pdf";
//        //先判断是否有这个文件
//        File file = new File(uploadpath+"/" + projectCode + ".pdf");
//        if (!file.exists()) {
//            //先删除再生成
//            Path path = Paths.get(p);
//            Files.delete(path);
//            try {
//                Document document = new Document(PageSize.A4, 10, 10, 10, 10);
//                //相对路径生成pdf
//                PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(uploadpath, projectCode + ".pdf")));
//                document.open();
//                Rectangle pageSize = document.getPageSize();
//                float pageWidth = pageSize.getWidth();
//                float pageHeight = pageSize.getHeight();
//
//                for (int i = 0; i < newPath.size(); i++) {
//                    Path pathImg = Paths.get(uploadpath+"/"+newPath.get(i));
//                    Image image = Image.getInstance(pathImg.toString());
//                    try {
//                        image.setAlignment(Image.ALIGN_CENTER);
//                        image.scaleToFit(pageWidth - 20, pageHeight - 20);
//                        document.add(image);
//                    } catch (Exception e) {
//                    }
//
//                }
//                document.close();
//                //返回给前端全路径
//                return p;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        //返回给前端全路径
//        return  p;
        return new ArrayList<String>();
    }

    /**
     * 首页统计
     *
     * @return
     */
    @ApiOperation(value = "首页统计", notes = "首页统计")
    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @GetMapping(value = "/homePageCount")
    public Result<HomePageCountVo> homePageCount(PvProjectInfo pvProjectInfo) {
        return pvProjectInfoService.homePageCountNew(pvProjectInfo);
    }


    public List<String> findAllUsersCreatedBy(String createBy) {
        List<String> users = new ArrayList<>();
        findUsersRecursively(createBy, users);
        return users;
    }

    private void findUsersRecursively(String createBy, List<String> users) {
        if (StringUtils.isNotBlank(createBy)) {
            users.add(createBy); // 添加当前用户到列表中

            // 递归地查找该用户创建的所有用户
            List<String> createdUsers = sysUserService.getUserIds(createBy);
            for (String createdUser : createdUsers) {
                findUsersRecursively(createdUser, users); // 递归调用
            }
        }
    }

    /**
     * 电站列表
     *
     * @return
     */
    @GetMapping(value = "/getStationInfo")
    public Result getStationInfo(PageVo pageVo) {
        String renterId = pageVo.getRenterId();
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("project_type",renterId);
        List<Device> list = iDeviceService.list(deviceQueryWrapper);
        if(list != null ){
            for (int i = 0; i < list.size(); i++) {
                int source = list.get(i).getSource();
                //如果是阳光的，就除以1000 保留俩位小数
                if(source == 2){
                    String currPower = list.get(i).getCurrPower();
                    String totalCapcity = list.get(i).getTotalCapcity();
                    if(StringUtils.isNotBlank(totalCapcity)){
                        String bd = "";
                        bd = new BigDecimal(totalCapcity).setScale(2, RoundingMode.HALF_UP).toString();
                        list.get(i).setTotalCapcity(bd);
                    }
                    if(StringUtils.isNotBlank(currPower)){
                        String divide = new BigDecimal(currPower).divide(new BigDecimal(1000)).setScale(2,RoundingMode.UP).toString();
                        list.get(i).setCurrPower(divide);
                    }
                }
                Integer deviceId = list.get(i).getDeviceId();
                DeviceDetailGrowatt deviceDetail = iDeviceDetailGrowattService.getDeviceDetail(deviceId.toString());
                if(deviceDetail != null){
                    String todayEnergy = deviceDetail.getTodayEnergy();
                    list.get(i).setTodayEnergy(todayEnergy);
                }
            }
        }
        return Result.ok(list);
    }


    /**
     * 首页点击地图进入市级大屏页面
     *
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/getStationInfoMarket")
    public Result getStationInfoMarket(PageVo pageVo) {
//        if (pageVo.getAddressId() == 3) {
//            List<PvStationsView> list = device.list();
//            //电站总数
//            StationPageData stationPageData = new StationPageData();
//            BigDecimal totalEnergy = new BigDecimal(0.00);
//            //节能减排
//            BigDecimal emission = new BigDecimal(0.00);
//            //实时功率
//            BigDecimal totalCurrentPower = new BigDecimal(0.00);
//            //组件总容量
//            BigDecimal totalPeakPower = new BigDecimal(0.00);
//
//            List<StatisticsVo> statisticsVos = new ArrayList<>();
//            for (int i = 0; i < list.size(); i++) {
//                Integer plantId = list.get(i).getPlantId();
//                String carbonOffset = list.get(i).getCarbonOffset();
//                if (StringUtils.isNotBlank(carbonOffset)) {
//                    emission = emission.add(new BigDecimal(carbonOffset));
//                }
//                QueryWrapper<PvStations> pvStationsQueryWrapper = new QueryWrapper<>();
//                pvStationsQueryWrapper.eq("plant_id", plantId);
//                PvStations one = stationsService.getOne(pvStationsQueryWrapper);
//                String status = stationsService.getStatus(plantId);
//                String todayEnergy = list.get(i).getTodayEnergy();
//                String peakPower = list.get(i).getPeakPowerActual();
//                String currentPower = list.get(i).getCurrentPower();
//                StatisticsVo statisticsVo = new StatisticsVo();
//                String name = one.getName();
//                statisticsVo.setPlantName(StringUtils.isNotBlank(name) ? name : "");
//                statisticsVo.setPlantId(plantId.toString());
//                statisticsVo.setPower(new BigDecimal(currentPower).setScale(2, RoundingMode.HALF_UP).toString());
//                statisticsVo.setTotalTodayPower(todayEnergy);
//                statisticsVo.setIncomeVolume(peakPower);
//                statisticsVo.setStatus(status);
//                statisticsVos.add(statisticsVo);
//                totalEnergy = totalEnergy.add(new BigDecimal(list.get(i).getTotalEnergy()));
//                totalCurrentPower = totalCurrentPower.add(new BigDecimal(currentPower));
//                totalPeakPower = totalPeakPower.add(new BigDecimal(peakPower));
//            }
//
//            StationPageVo stationPageVo = new StationPageVo();
//            //总容量
//            stationPageVo.setTotalPeakPower(totalPeakPower.setScale(2, RoundingMode.HALF_UP).toString());
//            //总数
//            stationPageVo.setStationCount(list.size());
//            //总发电量
//            stationPageVo.setTotalPower(totalEnergy.setScale(2, RoundingMode.HALF_UP).toString());
//            stationPageVo.setIncome(totalEnergy.multiply(new BigDecimal(1.1).setScale(2, RoundingMode.HALF_UP)).toString());
//            stationPageData.setStationPageVo(stationPageVo);
//            stationPageData.setStatisticsVos(statisticsVos);
//            return Result.OK(stationPageData);
//        }
        return Result.ok();
    }

    @GetMapping(value = "/getStationInfoOther")
    public Result getStationInfoOther(PageVo pageVo) {
        String renterId = pageVo.getRenterId();
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("project_type",renterId);
        List<Device> list = iDeviceService.list(deviceQueryWrapper);
        //电站总数
        int size = list.size();
        StationPageData stationPageData = new StationPageData();
        StationPageVo stationPageVo = new StationPageVo();
        stationPageVo.setStationCount(size);
        BigDecimal totalEnergy = new BigDecimal(0.00);
        //节能减排
        BigDecimal emission = new BigDecimal(0.00);
        //实时功率
        BigDecimal totalCurrentPowerSol= new BigDecimal(0.00);
        BigDecimal totalCurrentPowerGrw = new BigDecimal(0.00);
        BigDecimal totalCurrentPower=new BigDecimal(0.00);
        //组件总容量
        BigDecimal totalPeakPower = new BigDecimal(0.00);
        for (int i = 0; i < list.size(); i++) {
            int source = list.get(i).getSource();
            String peakPower = list.get(i).getTotalCapcity();
            String currentPower = list.get(i).getCurrPower();

            String carbonOffset = list.get(i).getCo2Reduce();
            if (StringUtils.isNotBlank(carbonOffset)) {
                emission = emission.add(new BigDecimal(carbonOffset));
            }
            totalEnergy = totalEnergy.add(new BigDecimal(list.get(i).getTotalEnergy()));
            if(this.isPositiveNumber(currentPower)){
                if(source == 2){
                    totalCurrentPowerGrw = totalCurrentPowerGrw.add(new BigDecimal(currentPower).divide(new BigDecimal(1000)));
                }
                if(source == 1){
                    BigDecimal bigDecimal = this.convertToWatt(currentPower);
                    totalCurrentPowerSol = totalCurrentPowerSol.add(bigDecimal);
                }
            }

            if(this.isPositiveNumber(peakPower)){
                totalPeakPower = totalPeakPower.add(new BigDecimal(peakPower));
            }

        }
        BigDecimal divide = totalCurrentPowerSol.divide(new BigDecimal(1000));
        totalCurrentPower = totalCurrentPowerGrw.add(divide);
        stationPageVo.setTotalPower(totalEnergy.setScale(2, RoundingMode.HALF_UP).toString());
        stationPageVo.setIncome(totalEnergy.multiply(new BigDecimal(1.1)).setScale(2, RoundingMode.HALF_UP).toString());
        //离线数量
        int size1 = iDeviceService.getUnLineDeviceNumSolar(renterId,0);
        stationPageVo.setStatus1(size1);
        //节能减排
        stationPageVo.setSection(emission.setScale(2, RoundingMode.HALF_UP));
        //在线数量
        int size0 = iDeviceService.getUnLineDeviceNumGrw(renterId,1);
        stationPageVo.setStatus0(size0);
        //组件总数
        stationPageVo.setEmission(emission.divide(new BigDecimal(7), BigDecimal.ROUND_CEILING).setScale(0, RoundingMode.HALF_UP));
        //实时功率
        stationPageVo.setTotalCurrentPower(totalCurrentPower.setScale(2, RoundingMode.HALF_UP).toString());
        //总容量
        stationPageVo.setTotalPeakPower(totalPeakPower.setScale(2, RoundingMode.HALF_UP).toString());
        String s = iDeviceService.countTotalPower(renterId);
        stationPageVo.setDeviceTotalPower(s);
        //在线数量
        stationPageData.setStationPageVo(stationPageVo);

        //总的累计发电量
        //封装电站7天数据

        List<StationCountVo> weeks = iDevicePointDayService.weeks(renterId);
        if (weeks != null && weeks.size() > 0) {
            List<StationCountVo> collect = weeks.stream().sorted(Comparator.comparing(StationCountVo::getIndex).reversed()).collect(Collectors.toList());
            stationPageData.setWeekList(collect);
        }
        //封装电站6个月数据
        List<StationCountVo> month = iDevicePointMonthService.month(renterId);
        if (month != null && month.size() > 0) {
            List<StationCountVo> collect = month.stream().sorted(Comparator.comparing(StationCountVo::getIndex).reversed()).collect(Collectors.toList());
            stationPageData.setMonthList(collect);
        }
        return Result.OK(stationPageData);

    }

    public BigDecimal convertToWatt(String rawPower) {
        if (rawPower == null || rawPower.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String cleaned = rawPower.trim();

        // 尝试解析为数值
        try {
            // 判断是否包含小数点
            if (cleaned.contains(".")) {
                BigDecimal value = new BigDecimal(cleaned);
                // 小数 → 当作 kW，转为 W
                return value.multiply(BigDecimal.valueOf(1000));
            } else {
                // 整数 → 当作 W
                return new BigDecimal(cleaned);
            }
        } catch (NumberFormatException e) {
            System.err.println("无效功率值: " + cleaned);
            return BigDecimal.ZERO;
        }
    }

//
//    /**
//     * 电站列表进详情
//     *
//     * @return
//     */
//    @GetMapping(value = "/getStationInfoData")
//    public Result getStationInfoData(PageVo pageVo) {
//        Integer plantId = pageVo.getPlantId();
//        StationPowerSticVo stationPowerSticVo = new StationPowerSticVo();
//        QueryWrapper<Device> objectQueryWrapper = new QueryWrapper<>();
//        objectQueryWrapper.eq("plant_id", plantId);
//        Device one = iDeviceService.getOne(objectQueryWrapper);
//        QueryWrapper<PvStationsDetails> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("plant_id", plantId);
//        String longitude = one.getLongitude().toString();
//        String latitude = one.getLatitude().toString();
//        String createDate = one.getInstallDate();
//        Integer plantType = one.getPsType();
//        if (one != null) {
//            String currentPower = one.getCurrPower();
////            String carbonOffset = one.getCarbonOffset();
//            String peakPowerActual = one.getTotalCapcity();
//            String todayEnergy = one.getTodayEnergy();
//            String monthlyEnergy = one.getMonthlyEnergy();
//            if (StringUtils.isNotBlank(currentPower)) {
//                stationPowerSticVo.setCurrentPower(new BigDecimal(currentPower).setScale(2, RoundingMode.UP).toString());
//            }
//            if (StringUtils.isNotBlank(carbonOffset)) {
//                stationPowerSticVo.setCarbonOffset(new BigDecimal(carbonOffset).setScale(2, RoundingMode.UP).toString());
//                stationPowerSticVo.setCarbonOffsetNum(new BigDecimal(carbonOffset).multiply(new BigDecimal(7)).setScale(2, RoundingMode.UP).toString());
//            }
//            if (StringUtils.isNotBlank(peakPowerActual)) {
//                stationPowerSticVo.setPeakPower(new BigDecimal(peakPowerActual).setScale(2, RoundingMode.UP).toString());
//            }
//            if (StringUtils.isNotBlank(todayEnergy) && StringUtils.isNotBlank(peakPowerActual)) {
//                stationPowerSticVo.setDayNum(new BigDecimal(todayEnergy).setScale(2, RoundingMode.UP).toString());
//                stationPowerSticVo.setDayIncome(new BigDecimal(todayEnergy).multiply(new BigDecimal(1.1).setScale(2, RoundingMode.HALF_UP)).toString());
//                stationPowerSticVo.setDayHour(new BigDecimal(todayEnergy).divide(new BigDecimal(peakPowerActual), BigDecimal.ROUND_CEILING).toString());
//            }
//            if (StringUtils.isNotBlank(monthlyEnergy) && StringUtils.isNotBlank(peakPowerActual)) {
//                stationPowerSticVo.setMonthPower(new BigDecimal(monthlyEnergy).setScale(2, RoundingMode.UP).toString());
//                stationPowerSticVo.setMonthIncome(new BigDecimal(monthlyEnergy).multiply(new BigDecimal(1.1).setScale(2, RoundingMode.HALF_UP)).toString());
//                stationPowerSticVo.setMonthHour(new BigDecimal(monthlyEnergy).divide(new BigDecimal(peakPowerActual), BigDecimal.ROUND_CEILING).toString());
//            }
//        }
//        if (pvStationsDetails != null) {
//            if (createDate != null) {
//                stationPowerSticVo.setCreateTime(createDate);
//            }
//            if (StringUtils.isNotBlank(longitude)) {
//                stationPowerSticVo.setLongitude(longitude);
//            }
//            if (StringUtils.isNotBlank(latitude)) {
//                stationPowerSticVo.setLatitude(latitude);
//            }
//            if (plantType != null && plantType == 0) {
//                stationPowerSticVo.setPlantType("家用电站");
//            }
//        }
//        return Result.ok(stationPowerSticVo);
//    }


    /**
     * 详情统计
     *
     * @param
     * @return
     */
//    @GetMapping(value = "/getStationInfoStatistics")
//    public Result getStationInfoStatistics(PageVo pageVo) {
//        Result stationInfoStatistics = iPvEnergyService.getStationInfoStatistics(pageVo);
//        return stationInfoStatistics;
//    }
//
//    /**
//     * 详情 年 月 总 统计
//     *
//     * @param pageVo
//     * @return
//     */
//    @GetMapping(value = "/getStationInfoStatisticsAll")
//    public Result getStationInfoStatisticsAll(PageVo pageVo) {
//        //假数据，天津
//        return iPvEnergyService.getStationChart(pageVo);
//    }


    public  boolean isPositiveNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.trim().matches("\\d+(\\.\\d+)?");
    }


    /**
     * 获取环境参数
     *
     * @param
     * @return
     */
    @GetMapping(value = "/getEnvParam")
    public Result getEnvParam() {
        return Result.ok(envParam);
    }

////     处理旧数据 (加密)
//    @PostMapping(value = "/encipherDate")
//    public Result encipherDate() {
//        try {
////            LambdaQueryWrapper<PvProjectInfo> pvProjectInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
////            List<PvProjectInfo> list = pvProjectInfoService.list(pvProjectInfoLambdaQueryWrapper);
//            List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
//            if (list != null && list.size() > 0) {
//                for (PvProjectInfo pvProjectInfo : list) {
//                    // 项目id
//                    Integer projectInfoId = pvProjectInfo.getId();
//                    // 租户id
//                    Integer tenantId = pvProjectInfo.getTenantId();
//                    // 获取秘钥
//                    SecretKey secretKey = AESUtils.generateSecretKey();
//                    if (secretKey != null) {
//                        // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
//                        // 电话号码
//                        String customerMobile = pvProjectInfo.getCustomerMobile();
//                        // 身份号码
//                        String idNumber = pvProjectInfo.getIdNumber();
//                        // 银行卡持有人
//                        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
//                        // 客户姓名
//                        String customerName = pvProjectInfo.getCustomerName();
//                        // 秘钥转base4
//                        String keyStr = AESUtils.encryptKeyFromBase64(secretKey);
//                        // 存秘钥表
//                        // 更新秘钥表
//                        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectId(projectInfoId, tenantId);
//                        if (pvProjectKey != null) {
//                            Integer id = pvProjectKey.getId();
//                            pvProjectKeyService.updatePasswordKey(keyStr, id);
//                        } else {
//                            // 存秘钥表
//                            PvProjectKey pvProjectKeyNew = new PvProjectKey();
//                            pvProjectKeyNew.setPasswordKey(keyStr);
//                            pvProjectKeyNew.setCreateTime(new Date());
//                            pvProjectKeyNew.setUpdateTime(new Date());
//                            pvProjectKeyNew.setRowStatus("1");
//                            pvProjectKeyNew.setProjectId(projectInfoId);
//                            pvProjectKeyNew.setTenantId(tenantId);
//                            pvProjectKeyService.save(pvProjectKeyNew);
//                        }
//                        // 加密
//                        // 电话号码
//                        if (StringUtils.isNotBlank(customerMobile)) {
//                            String encryptMobile = AESUtils.encrypt(customerMobile, secretKey);
//                            pvProjectInfo.setCustomerMobile(encryptMobile);
//                        }
//                        // 身份号码
//                        if (StringUtils.isNotBlank(idNumber)) {
//                            String encryptIdNumber = AESUtils.encrypt(idNumber, secretKey);
//                            pvProjectInfo.setIdNumber(encryptIdNumber);
//                        }
//                        // 银行卡持有人
//                        if (StringUtils.isNotBlank(conBankCardHolders)) {
//                            String encryptBankCardHolders = AESUtils.encrypt(conBankCardHolders, secretKey);
//                            pvProjectInfo.setConBankCardHolders(encryptBankCardHolders);
//                        }
//                        // 客户姓名
//                        if (StringUtils.isNotBlank(customerName)) {
//                            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//                            pvProjectInfo.setCustomerName(encryptCustomerName);
//                        }
//                        // 更新remark信息
//                        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNew(projectInfoId);
//                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
//                            for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
//                                if (tenantId != null && tenantId == -2103508991) {
//                                    Integer paramItmeId = pvProjectRemark.getParamItmeId();
//                                    Integer groupId = pvProjectRemark.getParamGroupId();
//                                    if (paramItmeId == 238 && groupId == 57) {
//                                        //户主姓名
//                                        String remark = pvProjectRemark.getRemark();
//                                        if (StringUtils.isNotBlank(remark)) {
//                                            // 加密
//                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                            pvProjectRemark.setRemark(encryptRemark);
//                                            pvProjectInfo.setHouseOwner(encryptRemark);
//                                        }
//                                    }
//                                    if (paramItmeId == 242 && groupId == 58) {
//                                        // 电卡姓名
//                                        String remark = pvProjectRemark.getRemark();
//                                        if (StringUtils.isNotBlank(remark)) {
//                                            // 加密
//                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                            pvProjectRemark.setRemark(encryptRemark);
//                                            pvProjectInfo.setElectricityOwner(encryptRemark);
//                                        }
//                                    }
//                                    if (paramItmeId == 363 && groupId == 61) {
//                                        // 电费收益卡号
//                                        String remark = pvProjectRemark.getRemark();
//                                        if (StringUtils.isNotBlank(remark)) {
//                                            // 加密
//                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                            pvProjectRemark.setRemark(encryptRemark);
//                                            pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
//                                        }
//                                    }
//                                    if (paramItmeId == 367 && groupId == 61) {
//                                        // 租金收益卡号
//                                        String remark = pvProjectRemark.getRemark();
//                                        if (StringUtils.isNotBlank(remark)) {
//                                            // 加密
//                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                            pvProjectRemark.setRemark(encryptRemark);
//                                            pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
//                                        }
//                                    }
//                                    if (paramItmeId == 366 && groupId == 61) {
//                                        // 租金收益卡持有人
//                                        String remark = pvProjectRemark.getRemark();
//                                        if (StringUtils.isNotBlank(remark)) {
//                                            // 加密
//                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                            pvProjectRemark.setRemark(encryptRemark);
//                                            pvProjectInfo.setReturnFeeDebitCardNo(encryptRemark);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                        // 更新项目信息
//                        pvProjectInfoService.updateById(pvProjectInfo);
//                    }
//                }
//            }
//            return Result.ok("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            return Result.error("fail");
//        }
//    }

//     处理旧数据 (解密)
    @PostMapping(value = "/decryptDate")
    public Result decryptDate() {
//        try {
        List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
        if (list != null && list.size() > 0) {
            for (PvProjectInfo pvProjectInfo : list) {
                Integer tenantId = pvProjectInfo.getTenantId();
                Integer projectInfoId = pvProjectInfo.getId();
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectId(projectInfoId, tenantId);
                if (pvProjectKey != null) {
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        String customerName = pvProjectInfo.getCustomerName();
                        if (StringUtils.isNotBlank(customerName)) {
                            String decrypt = AESUtils.decrypt(customerName, secretKey);
                            if (StringUtils.isNotBlank(decrypt)) {
                                pvProjectInfo.setCustomerName(decrypt);
                            }
                        }
                        String idNumber = pvProjectInfo.getIdNumber();
                        if (StringUtils.isNotBlank(idNumber)) {
                            String decrypt1 = AESUtils.decrypt(idNumber, secretKey);
                            if (StringUtils.isNotBlank(decrypt1)) {
                                pvProjectInfo.setIdNumber(decrypt1);
                            }
                        }
                        String customerMobile = pvProjectInfo.getCustomerMobile();
                        if (StringUtils.isNotBlank(customerMobile)) {
                            String decrypt2 = AESUtils.decrypt(customerMobile, secretKey);
                            if (StringUtils.isNotBlank(decrypt2)) {
                                pvProjectInfo.setCustomerMobile(decrypt2);
                            }
                        }
                        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
                        if (StringUtils.isNotBlank(conBankCardHolders)) {
                            String decrypt3 = AESUtils.decrypt(conBankCardHolders, secretKey);
                            if (StringUtils.isNotBlank(decrypt3)) {
                                pvProjectInfo.setConBankCardHolders(decrypt3);
                            }
                        }
                        String houseOwner = pvProjectInfo.getHouseOwner();
                        if (StringUtils.isNotBlank(houseOwner)) {
                            String decrypt4 = AESUtils.decrypt(houseOwner, secretKey);
                            if (StringUtils.isNotBlank(decrypt4)) {
                                pvProjectInfo.setHouseOwner(decrypt4);
                            }
                        }
                        String electricityOwner = pvProjectInfo.getElectricityOwner();
                        if (StringUtils.isNotBlank(electricityOwner)) {
                            String decrypt5 = AESUtils.decrypt(electricityOwner, secretKey);
                            if (StringUtils.isNotBlank(decrypt5)) {
                                pvProjectInfo.setElectricityOwner(decrypt5);
                            }
                        }
                        pvProjectInfoService.updateByIdNew(pvProjectInfo);
                        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNew(projectInfoId);
                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                            for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                                pvProjectRemarkService.updateByIdNew(pvProjectRemark);
                            }
                        }
                    }
                }
            }
        }
        return Result.ok("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            return Result.error("fail");
//        }
    }

    //  处理旧数据 (解密)
    @PostMapping(value = "/decryptDateNew")
    public Result decryptDateTwo() {
//        try {
        List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
        if (list != null && list.size() > 0) {
            for (PvProjectInfo pvProjectInfo : list) {
                Integer tenantId = pvProjectInfo.getTenantId();
                Integer projectInfoId = pvProjectInfo.getId();
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNew(tenantId);
                if (pvProjectKey != null) {
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        String customerName = pvProjectInfo.getCustomerName();
                        if (StringUtils.isNotBlank(customerName)) {
                            String decrypt = AESUtils.decrypt(customerName, secretKey);
                            if (StringUtils.isNotBlank(decrypt)) {
                                pvProjectInfo.setCustomerName(decrypt);
                            }
                        }
                        String idNumber = pvProjectInfo.getIdNumber();
                        if (StringUtils.isNotBlank(idNumber)) {
                            String decrypt1 = AESUtils.decrypt(idNumber, secretKey);
                            if (StringUtils.isNotBlank(decrypt1)) {
                                pvProjectInfo.setIdNumber(decrypt1);
                            }
                        }
                        String customerMobile = pvProjectInfo.getCustomerMobile();
                        if (StringUtils.isNotBlank(customerMobile)) {
                            String decrypt2 = AESUtils.decrypt(customerMobile, secretKey);
                            if (StringUtils.isNotBlank(decrypt2)) {
                                pvProjectInfo.setCustomerMobile(decrypt2);
                            }
                        }
                        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
                        if (StringUtils.isNotBlank(conBankCardHolders)) {
                            String decrypt3 = AESUtils.decrypt(conBankCardHolders, secretKey);
                            if (StringUtils.isNotBlank(decrypt3)) {
                                pvProjectInfo.setConBankCardHolders(decrypt3);
                            }
                        }
                        String houseOwner = pvProjectInfo.getHouseOwner();
                        if (StringUtils.isNotBlank(houseOwner)) {
                            String decrypt4 = AESUtils.decrypt(houseOwner, secretKey);
                            if (StringUtils.isNotBlank(decrypt4)) {
                                pvProjectInfo.setHouseOwner(decrypt4);
                            }
                        }
                        String electricityOwner = pvProjectInfo.getElectricityOwner();
                        if (StringUtils.isNotBlank(electricityOwner)) {
                            String decrypt5 = AESUtils.decrypt(electricityOwner, secretKey);
                            if (StringUtils.isNotBlank(decrypt5)) {
                                pvProjectInfo.setElectricityOwner(decrypt5);
                            }
                        }
                        pvProjectInfoService.updateByIdNew(pvProjectInfo);
                        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNewNewNew(projectInfoId);
                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                            for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                                pvProjectRemarkService.updateByIdNew(pvProjectRemark);
                            }
                        }
                    }
                }
            }
        }
        return Result.ok("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            return Result.error("fail");
//        }
    }

    // 处理旧数据 (加密)
//    @PostMapping(value = "/encipherDateNew")
//    public Result encipherDateNew() {
////        try {
//            List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
//            if (list != null && list.size() > 0) {
//                for (PvProjectInfo pvProjectInfo : list) {
//                    // 项目id
//                    Integer projectInfoId = pvProjectInfo.getId();
//                    // 租户id
//                    Integer tenantId = pvProjectInfo.getTenantId();
//                    // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
//                    // 电话号码
//                    String customerMobile = pvProjectInfo.getCustomerMobile();
//                    // 身份号码
//                    String idNumber = pvProjectInfo.getIdNumber();
//                    // 银行卡持有人
//                    String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
//                    // 客户姓名
//                    String customerName = pvProjectInfo.getCustomerName();
//                    // 存秘钥表
//                    // 更新秘钥表
//                    PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNew(tenantId);
//                    if (pvProjectKey != null) {
//                        String passwordKey = pvProjectKey.getPasswordKey();
//                        if (StringUtils.isNotBlank(passwordKey)) {
//                            SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
//                            // 加密
//                            // 电话号码
//                            if (StringUtils.isNotBlank(customerMobile)) {
//                                String encryptMobile = AESUtils.encrypt(customerMobile, secretKey);
//                                pvProjectInfo.setCustomerMobile(encryptMobile);
//                            }
//                            // 身份号码
//                            if (StringUtils.isNotBlank(idNumber)) {
//                                String encryptIdNumber = AESUtils.encrypt(idNumber, secretKey);
//                                pvProjectInfo.setIdNumber(encryptIdNumber);
//                            }
//                            // 银行卡持有人
//                            if (StringUtils.isNotBlank(conBankCardHolders)) {
//                                String encryptBankCardHolders = AESUtils.encrypt(conBankCardHolders, secretKey);
//                                pvProjectInfo.setConBankCardHolders(encryptBankCardHolders);
//                            }
//                            // 客户姓名
//                            if (StringUtils.isNotBlank(customerName)) {
//                                String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//                                pvProjectInfo.setCustomerName(encryptCustomerName);
//                            }
//                            // 更新remark信息
//                            List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNew(projectInfoId);
//                            if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
//                                for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
//                                    if (tenantId != null && tenantId == -2103508991) {
//                                        Integer paramItmeId = pvProjectRemark.getParamItmeId();
//                                        Integer groupId = pvProjectRemark.getParamGroupId();
//                                        if (paramItmeId == 238 && groupId == 57) {
//                                            //户主姓名
//                                            String remark = pvProjectRemark.getRemark();
//                                            if (StringUtils.isNotBlank(remark)) {
//                                                // 加密
//                                                String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                                pvProjectRemark.setRemark(encryptRemark);
//                                                pvProjectInfo.setHouseOwner(encryptRemark);
//                                            }
//                                        }
//                                        if (paramItmeId == 242 && groupId == 58) {
//                                            // 电卡姓名
//                                            String remark = pvProjectRemark.getRemark();
//                                            if (StringUtils.isNotBlank(remark)) {
//                                                // 加密
//                                                String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                                pvProjectRemark.setRemark(encryptRemark);
//                                                pvProjectInfo.setElectricityOwner(encryptRemark);
//                                            }
//                                        }
//                                        if (paramItmeId == 363 && groupId == 61) {
//                                            // 电费收益卡号
//                                            String remark = pvProjectRemark.getRemark();
//                                            if (StringUtils.isNotBlank(remark)) {
//                                                // 加密
//                                                String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                                pvProjectRemark.setRemark(encryptRemark);
//                                                pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
//                                            }
//                                        }
//                                        if (paramItmeId == 367 && groupId == 61) {
//                                            // 租金收益卡号
//                                            String remark = pvProjectRemark.getRemark();
//                                            if (StringUtils.isNotBlank(remark)) {
//                                                // 加密
//                                                String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                                pvProjectRemark.setRemark(encryptRemark);
//                                                pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
//                                            }
//                                        }
//                                        if (paramItmeId == 366 && groupId == 61) {
//                                            // 租金收益卡持有人
//                                            String remark = pvProjectRemark.getRemark();
//                                            if (StringUtils.isNotBlank(remark)) {
//                                                // 加密
//                                                String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                                                pvProjectRemark.setRemark(encryptRemark);
//                                                pvProjectInfo.setReturnFeeDebitCardNo(encryptRemark);
//                                            }
//                                        }
//                                        pvProjectRemarkService.updateById(pvProjectRemark);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    // 更新项目信息
//                    pvProjectInfoService.updateById(pvProjectInfo);
//                }
//            }
//            return Result.ok("ok");
////        } catch (Exception e) {
////            log.error(e.getMessage());
////            return Result.error("fail");
////        }
//    }

    // 处理旧数据 (加密)
    @PostMapping(value = "/encipherDateNewNew")
    public Result encipherDateNewNew() {
//        try {
        List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
        if (list != null && list.size() > 0) {
            for (PvProjectInfo pvProjectInfo : list) {
                // 项目id
                Integer projectInfoId = pvProjectInfo.getId();
                // 租户id
                Integer tenantId = pvProjectInfo.getTenantId();
                // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
                // 电话号码
                String customerMobile = pvProjectInfo.getCustomerMobile();
                // 身份号码
                String idNumber = pvProjectInfo.getIdNumber();
                // 银行卡持有人
                String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
                // 客户姓名
                String customerName = pvProjectInfo.getCustomerName();
                // 存秘钥表
                // 更新秘钥表
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
                if (pvProjectKey != null) {
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        // 加密
                        // 电话号码
                        if (StringUtils.isNotBlank(customerMobile)) {
                            String encryptMobile = AESUtils.encrypt(customerMobile, secretKey);
                            pvProjectInfo.setCustomerMobile(encryptMobile);
                        }
                        // 身份号码
                        if (StringUtils.isNotBlank(idNumber)) {
                            String encryptIdNumber = AESUtils.encrypt(idNumber, secretKey);
                            pvProjectInfo.setIdNumber(encryptIdNumber);
                        }
                        // 银行卡持有人
                        if (StringUtils.isNotBlank(conBankCardHolders)) {
                            String encryptBankCardHolders = AESUtils.encrypt(conBankCardHolders, secretKey);
                            pvProjectInfo.setConBankCardHolders(encryptBankCardHolders);
                        }
                        // 客户姓名
                        if (StringUtils.isNotBlank(customerName)) {
                            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
                            pvProjectInfo.setCustomerName(encryptCustomerName);
                        }
                        // 更新remark信息
                        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNewNewNewNew(projectInfoId);
                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                            for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                                if (tenantId != null && tenantId == -2103508991) {
                                    Integer paramItmeId = pvProjectRemark.getParamItmeId();
                                    Integer groupId = pvProjectRemark.getParamGroupId();
                                    if (paramItmeId == 238 && groupId == 57) {
                                        //户主姓名
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                            pvProjectInfo.setHouseOwner(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 242 && groupId == 58) {
                                        // 电卡姓名
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                            pvProjectInfo.setElectricityOwner(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 363 && groupId == 61) {
                                        // 电费收益卡号
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                            pvProjectInfo.setElectricityCardNo(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 367 && groupId == 61) {
                                        // 租金收益卡号
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                            pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 366 && groupId == 61) {
                                        // 租金收益卡持有人
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                            pvProjectInfo.setReturnFeeDebitCardNo(encryptRemark);
                                        }
                                    }
                                    pvProjectRemarkService.updateByIdNew(pvProjectRemark);
                                }
                            }
                        }
                    }
                }
                // 更新项目信息
                pvProjectInfoService.updateByIdNew(pvProjectInfo);
            }
        }
        return Result.ok("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            return Result.error("fail");
//        }
    }


    // 处理旧数据 (加密)
    @PostMapping(value = "/encipherDateNewNewNew")
    public Result encipherDateNewNewNew() {
//        try {
        List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
        if (list != null && list.size() > 0) {
            for (PvProjectInfo pvProjectInfo : list) {
                // 项目id
                Integer projectInfoId = pvProjectInfo.getId();
                // 租户id
                Integer tenantId = pvProjectInfo.getTenantId();
                // 存秘钥表
                // 更新秘钥表
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
                if (pvProjectKey != null) {
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        // 加密
                        // 更新remark信息
                        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkService.selectByMainIdNewNewNewNewNew(projectInfoId);
                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                            for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                                if (tenantId != null && tenantId == -2103508991) {
                                    Integer paramItmeId = pvProjectRemark.getParamItmeId();
                                    Integer groupId = pvProjectRemark.getParamGroupId();
                                    if (paramItmeId == 238 && groupId == 57) {
                                        //户主姓名
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 242 && groupId == 58) {
                                        // 电卡姓名
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 363 && groupId == 61) {
                                        // 电费收益卡号
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 367 && groupId == 61) {
                                        // 租金收益卡号
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                    if (paramItmeId == 366 && groupId == 61) {
                                        // 租金收益卡持有人
                                        String remark = pvProjectRemark.getRemark();
                                        if (StringUtils.isNotBlank(remark)) {
                                            // 加密
                                            String encryptRemark = AESUtils.encrypt(remark, secretKey);
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                    pvProjectRemarkService.updateByIdNew(pvProjectRemark);
                                }
                            }
                        }
                    }
                }
            }
        }
        return Result.ok("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            return Result.error("fail");
//        }
    }


    // 解密 (姓名)
    @PostMapping(value = "/decryptDateName")
    public Result decryptDateName() {
        try {
            List<PvProjectInfo> list = pvProjectInfoService.selectProjectInfoList();
            PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
            String passwordKey = pvProjectKey.getPasswordKey();
            SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
            if (list != null && list.size() > 0) {
                for (PvProjectInfo pvProjectInfo : list) {
                    Integer projectInfoId = pvProjectInfo.getId();
                    String customerName = pvProjectInfo.getCustomerName();
                    if (StringUtils.isNotBlank(customerName)) {
                        String decrypt = AESUtils.decrypt(customerName, secretKey);
                        if (StringUtils.isNotBlank(decrypt)) {
                            pvProjectInfoService.updateCustomerName(decrypt,projectInfoId);
                        }
                    }
                }
            }
            return Result.ok("666");
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.error("error");
        }
    }

    // 项目额外文件（上传）
    @PostMapping(value = "/addProjectInfoOtherFile")
    public Result addProjectInfoOtherFile(@RequestBody PvProjectFileOther pvProjectFileOther) {
        //获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        Integer projectId = pvProjectFileOther.getProjectId(); // 项目id
        PvProjectInfo pvProjectInfo = pvProjectInfoService.getById(projectId);
        if (pvProjectInfo == null) {
            return Result.error("项目不存在");
        }
        pvProjectFileOtherService.save(pvProjectFileOther);
        // 添加日志
        pvProjectInfoService.addLogToOtherFile(pvProjectFileOther, loginUser, "0");
        return Result.ok(String.valueOf(pvProjectFileOther.getId()),"保存成功");
    }

    // 项目额外文件（删除）
    @DeleteMapping(value = "/delProjectInfoOtherFile")
    public Result delProjectInfoOtherFile(@RequestParam(name = "id", required = true) Integer id) {
        //获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        PvProjectFileOther pvProjectFileOther = pvProjectFileOtherService.getById(id);
        if (pvProjectFileOther == null) {
            return Result.error("文件不存在");
        }
        // 删除服务器数据
        String fileUrl = pvProjectFileOther.getFileUrl();
        File file = new File(uploadpath + File.separator + fileUrl);
        if (!file.exists()) {
            return Result.error("文件不存在");
        }
        boolean delete = file.delete();
        // 删除数据库数据
        pvProjectFileOtherService.delProjectInfoOtherFile(id);
        // 添加日志
        pvProjectInfoService.addLogToOtherFile(pvProjectFileOther, loginUser, "1");
        return Result.ok("删除成功");
    }

    // 项目额外文件（反显）
    @GetMapping(value = "/getProjectInfoOtherFiles")
    public Result getProjectInfoOtherFiles(@RequestParam Integer projectId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoService.getById(projectId);
        if (pvProjectInfo == null) {
            return Result.error("项目不存在");
        }
        LambdaQueryWrapper<PvProjectFileOther> pvProjectFileOtherLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pvProjectFileOtherLambdaQueryWrapper.eq(PvProjectFileOther::getProjectId,projectId);
        pvProjectFileOtherLambdaQueryWrapper.eq(PvProjectFileOther::getDelFlag,0);
        List<PvProjectFileOther> pvProjectFileOthers = pvProjectFileOtherService.list(pvProjectFileOtherLambdaQueryWrapper);
        return Result.ok(pvProjectFileOthers);
    }

}
