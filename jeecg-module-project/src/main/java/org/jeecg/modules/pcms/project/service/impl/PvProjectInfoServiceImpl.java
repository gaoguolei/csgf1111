package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.bpm.BpmApprovalStrategy;
import org.jeecg.modules.pcms.bpm.BpmApprovalStrategyFactory;
import org.jeecg.modules.pcms.bpm.BpmBusinessCategoryEnum;
import org.jeecg.modules.pcms.common.*;
import org.jeecg.modules.pcms.common.sys.entity.PvTenant;
import org.jeecg.modules.pcms.common.sys.mapper.PvTenantMapper;
import org.jeecg.modules.pcms.project.entity.*;
import org.jeecg.modules.pcms.project.mapper.*;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.vo.*;
import org.jeecg.modules.pcms.util.AESUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: pv_project_info
 * @Author: jun
 * @Date: 2023-03-07
 * @Version: V1.0
 */
@Service
public class PvProjectInfoServiceImpl extends ServiceImpl<PvProjectInfoMapper, PvProjectInfo> implements IPvProjectInfoService {

    private final String ORIGINAL_PATH = "original";
    @Autowired
    private PvProjectInfoMapper pvProjectInfoMapper;
    @Autowired
    private PvProjectBpmMapper pvProjectBpmMapper;
    @Autowired
    private IPvProjectBpmService pvProjectBpmService;
    @Autowired
    private PvProjectFileMapper pvProjectFileMapper;
    @Autowired
    private PvProjectRemarkMapper pvProjectRemarkMapper;
    @Autowired
    private IPvProjectCodingService pvProjectCodingService;
    @Autowired
    private ISysInverterService iSysInverterService;
    @Autowired
    private ISysAssemblyService iSysAssemblyService;
    @Autowired
    private IRegionsServiceOne iRegionsServiceOne;
    @Autowired
    private PvOperateLogMapper pvOperateLogMapper;
    @Autowired
    private PvProjectKeyMapper pvProjectKeyMapper;
    @Autowired
    private PvProjectKeyService pvProjectKeyService;
    @Autowired
    private PvTjProjectInstallMapper pvTjProjectInstallMapper;
    @Autowired
    private PvProjectTypeMapper pvProjectTypeMapper;
    @Autowired
    private PvTenantMapper sysTenantMapper;
    @Autowired
    private ISysInverterWireService iSysInverterWireService;
    @Autowired
    private ISysInverterWireMpptService iSysInverterWireMpptService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Value(value = "${jeecg.path.upload}")
    private String uploadpath;
    @Value(value = "${jeecg.image.expirationDays}")
    private int imageExpirationDays;
    //    private static final Map<String, String> NUMBER_TO_CHINESE = new HashMap<>();
//
//    static {
//        NUMBER_TO_CHINESE.put("1", "自然人");
//        NUMBER_TO_CHINESE.put("2", "法人");
//        NUMBER_TO_CHINESE.put("3", "法人二期");
//        NUMBER_TO_CHINESE.put("4", "法人三期");
//        NUMBER_TO_CHINESE.put("5", "法人四期");
//        NUMBER_TO_CHINESE.put("6", "法人五期");
//    }
    private List<SetParamGroup> setParamGroupList = new ArrayList<SetParamGroup>(
            Arrays.asList(
                    new SetParamGroup(1, 12),
                    new SetParamGroup(1, 16),
                    new SetParamGroup(4, 20),
                    new SetParamGroup(4, 23),
                    new SetParamGroup(4, 24),
                    new SetParamGroup(12, 57),
                    new SetParamGroup(12, 58),
                    new SetParamGroup(25, 47)
            )
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMain(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList, List<PvProjectFile> pvProjectFileList, List<PvProjectRemark> pvProjectRemarkList) {
        Integer tenantId = pvProjectInfo.getTenantId();
        pvProjectInfo.setProjectStatus(DictProjectStatusEnum.DATA_ENTRY.getCode());
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectInfo.setSalespersonId(loginUser.getId());
        pvProjectInfo.setUpdateTime(new Date());
        pvProjectInfo.setCreateTime(new Date());
        PvTenant sysTenant = sysTenantMapper.selectById(tenantId);
        String name = sysTenant.getName();
        if (loginUser.getRelTenantIds() == null || loginUser.getRelTenantIds().equals("")) {
            pvProjectInfo.setProjectCode(pvProjectCodingService.generateProjectCode(sysTenant.getAreaAddress(), name));
        } else {
            pvProjectInfo.setProjectCode(pvProjectCodingService.generateProjectCode(sysTenant.getAreaAddress(), name));
        }
        pvProjectInfo.setConCode(pvProjectInfo.getProjectCode());
        // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
        // 电话号码
        String customerMobile = pvProjectInfo.getCustomerMobile();
        // 身份号码
        String idNumber = pvProjectInfo.getIdNumber();
        // 银行卡持有人
        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
        // 客户姓名
//        String customerName = pvProjectInfo.getCustomerName();
        // 获取秘钥
        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
        String passwordKey = pvProjectKey.getPasswordKey();
        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
        // 秘钥转base4
        String keyStr = AESUtils.encryptKeyFromBase64(secretKey);
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
//        if (StringUtils.isNotBlank(customerName)) {
//            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//            pvProjectInfo.setCustomerName(encryptCustomerName);
//        }
        //如果是北京项目需要把数据配置信息表里面的数据存储到项目表
        if (tenantId != null && tenantId == -2103508991) {
            //把房屋类型分组设置成多个
            List<HouseTypeVo> typeList = pvProjectInfo.getTypeList();
            if (typeList != null && !typeList.isEmpty()) {
                String string = JSONObject.toJSONString(typeList);
                pvProjectInfo.setHouseTypeGroup(string);
            }
            for (int i = 0; i < pvProjectRemarkList.size(); i++) {
                Integer paramItmeId = pvProjectRemarkList.get(i).getParamItmeId();
                Integer groupId = pvProjectRemarkList.get(i).getParamGroupId();
                if (paramItmeId == 238 && groupId == 57) {
                    //户主姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setHouseOwner(encryptRemark);
                    }
                }
                if (paramItmeId == 239 && groupId == 57) {
                    //房本编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setHouseNo(remark);
                }
                if (paramItmeId == 240 && groupId == 57) {
                    //关系说明
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setRelationInfo(remark);
                }
                if (paramItmeId == 242 && groupId == 58) {
                    //电卡姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setElectricityOwner(encryptRemark);
                    }
                }
                if (paramItmeId == 243 && groupId == 58) {
                    //电卡编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setElectricityCardNo(remark);
                }
                if (paramItmeId == 244 && groupId == 58) {
                    //期望开卡时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setOpenCardTime(remark);
                }
                if (paramItmeId == 245 && groupId == 58) {
                    //期望安装时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setWorkTime(remark);
                }
//                if (paramItmeId == 364 && groupId == 61) {
//                    //返费卡号主
//                    String remark = pvProjectRemarkList.get(i).getRemark();
//                    if (StringUtils.isNotBlank(remark)) {
//                        // 加密
//                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
//                        pvProjectInfo.setReturnFeeDebitCardOwner(encryptRemark);
//                    }
//                }
//                if (paramItmeId == 365 && groupId == 61) {
//                    //返费卡号
//                    String remark = pvProjectRemarkList.get(i).getRemark();
//                    if (StringUtils.isNotBlank(remark)) {
//                        // 加密
//                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
//                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
//                        pvProjectInfo.setReturnFeeDebitCardNo(encryptRemark);
//                    }
//                }
//                if (paramItmeId == 366 && groupId == 61) {
//                    //返费开户行
//                    String remark = pvProjectRemarkList.get(i).getRemark();
//                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
//                }
                if (paramItmeId == 363 && groupId == 61) {
                    // 电费收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
//                if (paramItmeId == 364 && groupId == 61) {
//                    // 电费收益卡号开户行
//                    String remark = pvProjectRemarkList.get(i).getRemark();
//                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
//                }
                if (paramItmeId == 367 && groupId == 61) {
                    // 租金收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 366 && groupId == 61) {
                    // 租金收益卡持有人
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
//                if (paramItmeId == 368 && groupId == 61) {
//                    // 租金收益卡开户行
//                    String remark = pvProjectRemarkList.get(i).getRemark();
//                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
//                }
            }

        }
        String totalCapacity = pvProjectInfo.getTotalCapacity();
        BigDecimal instCmptCapacity = pvProjectInfo.getInstCmptCapacity();
        if (instCmptCapacity != null) {
            pvProjectInfo.setInstCmptCapacity(instCmptCapacity);
        }
        if (StringUtils.isNotBlank(totalCapacity)) {
            pvProjectInfo.setInstCmptCapacity(new BigDecimal(totalCapacity));
        }
        pvProjectInfoMapper.insert(pvProjectInfo);
        // 存秘钥表
        // 更新秘钥表
        // 项目id
        Integer projectInfoId = pvProjectInfo.getId();
//        pvProjectFileMapper.deleteByProjectId(pvProjectInfo.getId());
//        pvProjectRemarkMapper.deleteByProjectId(pvProjectInfo.getId());
        pvTjProjectInstallMapper.deleteByProjectId(pvProjectInfo.getId());
        List<PvTjProjectInstall> projectInstalls = pvProjectInfo.getProjectInstalls();
        Integer id = pvProjectInfo.getId();
        //天津项目的文件和备注单独存储
        if (projectInstalls != null && projectInstalls.size() > 0) {
            if (projectInstalls != null && projectInstalls.size() > 0) {
                for (int i = 0; i < projectInstalls.size(); i++) {
                    PvTjProjectInstall pvTjProjectInstall = projectInstalls.get(i);
                    List<PvProjectFile> pvProjectFiles = projectInstalls.get(i).getPvProjectFile();
                    if (pvProjectFiles != null && pvProjectFiles.size() > 0) {
                        StringBuffer fileIds = new StringBuffer();
                        for (int j = 0; j < pvProjectFiles.size(); j++) {
                            PvProjectFile pvProjectFile = pvProjectFiles.get(j);
                            pvProjectFile.setProjectId(id);
                            pvProjectFileMapper.insert(pvProjectFile);
                            Integer projectFileId = pvProjectFile.getId();
                            fileIds.append(projectFileId + ",");
                        }
                        pvTjProjectInstall.setFileId(fileIds.toString().substring(0, fileIds.toString().length() - 1));
                    }
                    //获取到文件id
                    List<PvProjectRemark> pvProjectRemarks = projectInstalls.get(i).getPvProjectRemark();

                    if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                        StringBuffer remakesIds = new StringBuffer();
                        for (int j = 0; j < pvProjectRemarks.size(); j++) {
                            PvProjectRemark pvProjectRemark = pvProjectRemarks.get(j);
                            pvProjectRemark.setProjectId(id);
                            pvProjectRemarkMapper.insert(pvProjectRemark);
                            Integer remarkId = pvProjectRemark.getId();
                            remakesIds.append(remarkId + ",");
                        }
                        pvTjProjectInstall.setRemakesId(remakesIds.toString().substring(0, remakesIds.toString().length() - 1));
                    }
                    pvTjProjectInstall.setProjectId(id);
//                    pvTjProjectInstall.setCreateDate(new Date());
                    pvTjProjectInstallMapper.insert(pvTjProjectInstall);
                }
            }
        }
        if (pvProjectFileList != null && pvProjectFileList.size() > 0) {
            for (PvProjectFile entity : pvProjectFileList) {
                //外键设置
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectFileMapper.insert(entity);
            }
        }
        if (pvProjectRemarkList != null && pvProjectRemarkList.size() > 0) {
            for (PvProjectRemark entity : pvProjectRemarkList) {
                //外键设置
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectRemarkMapper.insert(entity);
            }
        }

        if (tenantId != null && tenantId == -2103508991) {
            if (pvProjectBpmList != null && pvProjectBpmList.size() > 0) {
                pvProjectBpmList.get(0).setProjectId(pvProjectInfo.getId());
                pvProjectBpmList.get(0).setBpmNodeId(DicProjectBeijBpmEnum.DATA_ENTRY.getCode());
                pvProjectBpmList.get(0).setApprovalResult(DictApprovalResultEnum.APPROVED.getCode());
            } else {
                PvProjectBpm pvProjectBpm = new PvProjectBpm();
                pvProjectBpm.setProjectId(pvProjectInfo.getId());
                pvProjectBpm.setBpmNodeId(DicProjectBeijBpmEnum.DATA_ENTRY.getCode());
                pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
                pvProjectBpmList.add(pvProjectBpm);
            }
            PvProjectBpm pvProjectBpm = pvProjectBpmService.startProjectBpmBeiJ(pvProjectInfo.getId());
            BpmApprovalStrategy bpmApprovalStrategy = BpmApprovalStrategyFactory.getBpmApprovalStrategy(BpmBusinessCategoryEnum.PROJECT.getCode());
            bpmApprovalStrategy.approval(pvProjectInfo, pvProjectBpmList.get(0));
            pvProjectBpm = pvProjectBpm;
        } else {
            if (pvProjectBpmList != null && pvProjectBpmList.size() > 0) {
                pvProjectBpmList.get(0).setProjectId(pvProjectInfo.getId());
                pvProjectBpmList.get(0).setBpmNodeId(DictProjectBpmEnum.DATA_ENTRY.getCode());
                pvProjectBpmList.get(0).setApprovalResult(DictApprovalResultEnum.APPROVED.getCode());
            } else {
                PvProjectBpm pvProjectBpm = new PvProjectBpm();
                pvProjectBpm.setProjectId(pvProjectInfo.getId());
                pvProjectBpm.setBpmNodeId(DictProjectBpmEnum.DATA_ENTRY.getCode());
                pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
                pvProjectBpmList.add(pvProjectBpm);
            }
            PvProjectBpm pvProjectBpm = pvProjectBpmService.startProjectBpm(pvProjectInfo.getId());
            BpmApprovalStrategy bpmApprovalStrategy = BpmApprovalStrategyFactory.getBpmApprovalStrategy(BpmBusinessCategoryEnum.PROJECT.getCode());
            bpmApprovalStrategy.approval(pvProjectInfo, pvProjectBpmList.get(0));
            pvProjectBpm = pvProjectBpm;
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMain(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList, List<PvProjectFile> pvProjectFileList, List<PvProjectRemark> pvProjectRemarkList) {
        Integer tenantId = pvProjectInfo.getTenantId();
        Integer projectInfoId = pvProjectInfo.getId();
        PvProjectInfo projectInfoById = pvProjectInfoMapper.selectById(projectInfoId);
        String buildAfter = "";
        if (BeanUtil.isNotEmpty(projectInfoById)) {
            String buildAfter1 = projectInfoById.getBuildAfter();
            if (StringUtils.isNotBlank(buildAfter1)) {
                buildAfter = buildAfter1;
            }
        }
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        //如果是北京项目需要把数据配置信息表里面的数据存储到项目表
        // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
        // 电话号码
        String customerMobile = pvProjectInfo.getCustomerMobile();
        // 身份号码
        String idNumber = pvProjectInfo.getIdNumber();
        // 银行卡持有人
        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
        // 客户姓名
//        String customerName = pvProjectInfo.getCustomerName();

        // 获取秘钥
        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
        String passwordKey = pvProjectKey.getPasswordKey();
        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
        // 秘钥转base4
        String keyStr = AESUtils.encryptKeyFromBase64(secretKey);
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
//        if (StringUtils.isNotBlank(customerName)) {
//            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//            System.err.println(encryptCustomerName);
//            pvProjectInfo.setCustomerName(encryptCustomerName);
//        }




        if (tenantId != null && tenantId == -2103508991) {
//            pvProjectInfo.setSalespersonId(loginUser.getId());
            pvProjectInfo.setUpdateTime(new Date());
            List<HouseTypeVo> typeList = pvProjectInfo.getTypeList();
            if (typeList != null && !typeList.isEmpty()) {
                String string = JSONObject.toJSONString(typeList);
                pvProjectInfo.setHouseTypeGroup(string);
            }






            for (int i = 0; i < pvProjectRemarkList.size(); i++) {
                Integer paramItmeId = pvProjectRemarkList.get(i).getParamItmeId();
                Integer groupId = pvProjectRemarkList.get(i).getParamGroupId();
                if (paramItmeId == 238 && groupId == 57) {
                    //户主姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setHouseOwner(encryptRemark);
                    }
                }
                if (paramItmeId == 239 && groupId == 57) {
                    //房本编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setHouseNo(remark);
                }
                if (paramItmeId == 240 && groupId == 57) {
                    //关系说明
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setRelationInfo(remark);

                }
                if (paramItmeId == 242 && groupId == 58) {
                    //电卡姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setElectricityOwner(encryptRemark);
                    }

                }
                if (paramItmeId == 243 && groupId == 58) {
                    //电卡编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setElectricityCardNo(remark);
                }
                if (paramItmeId == 244 && groupId == 58) {
                    //期望开卡时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setOpenCardTime(remark);
                }
                if (paramItmeId == 245 && groupId == 58) {
                    //期望安装时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setWorkTime(remark);
                }
                if (paramItmeId == 363 && groupId == 61) {
                    // 电费收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 364 && groupId == 61) {
                    // 电费收益卡号开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
                if (paramItmeId == 367 && groupId == 61) {
                    // 租金收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 366 && groupId == 61) {
                    // 租金收益卡持有人
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 368 && groupId == 61) {
                    // 租金收益卡开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
            }

        }

        pvProjectInfoMapper.updateById(pvProjectInfo);
        pvProjectInfoMapper.updateBuildAfter(projectInfoId,buildAfter);
//        pvProjectInfoMapper.updateProjectInfo(pvProjectInfo);
        //1.先删除子表数据
        pvProjectFileMapper.physicalDeleteByMainId(pvProjectInfo.getId());
        pvProjectRemarkMapper.physicalDeleteByMainId(pvProjectInfo.getId());
        pvProjectBpmMapper.physicalDeleteByMainId(pvProjectBpmList.get(0));
        //删除逆变器子表数据
        iSysInverterService.deleteByProjectId(pvProjectInfo.getId());
        //插入逆变器
        List<SysInverterVO> sysInverterVOS = pvProjectInfo.getSysInverterVOS();
        iSysInverterService.addSysInverterVOS(sysInverterVOS);

        //删除组件数据
        iSysAssemblyService.deleteByProjectId(pvProjectInfo.getId());
        //插入组件
        List<SysAssemblyVO> sysAssemblyVOS = pvProjectInfo.getSysAssemblyVOS();
        iSysAssemblyService.addSysInverterVOS(sysAssemblyVOS);


//        //如果踏勘草图不为空。那么原来旧数据表结构中的踏勘草图以及踏勘备注模块过滤掉不需要更新
        List<PvTjProjectInstall> projectInstalls = pvProjectInfo.getProjectInstalls();


//        2.子表数据重新插入
        if (pvProjectFileList != null && pvProjectFileList.size() > 0) {
            for (PvProjectFile entity : pvProjectFileList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectFileMapper.insert(entity);
            }
        }
        if (pvProjectRemarkList != null && pvProjectRemarkList.size() > 0) {
            for (PvProjectRemark entity : pvProjectRemarkList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectRemarkMapper.insert(entity);
            }
        }
        Integer id = pvProjectInfo.getId();
        //先删除在插入
        pvTjProjectInstallMapper.deleteByProjectId(id);
        if (projectInstalls != null && projectInstalls.size() > 0) {
            String totalCapacity = pvProjectInfo.getTotalCapacity();
            BigDecimal instCmptCapacity = pvProjectInfo.getInstCmptCapacity();
            if (StringUtils.isNotBlank(totalCapacity)) {
                PvProjectInfo projectInfo = new PvProjectInfo();
                projectInfo.setId(id);
                projectInfo.setTotalCapacity(totalCapacity);
                if (instCmptCapacity != null) {
                    projectInfo.setInstCmptCapacity(instCmptCapacity);
                }
//                pvProjectInfoMapper.updateProjectInfo(pvProjectInfo);
                pvProjectInfoMapper.updateById(pvProjectInfo);
                pvProjectInfoMapper.updateBuildAfter(projectInfoId,buildAfter);
            }

            //获取踏勘草图模块，与原数据做兼容
            if (projectInstalls != null && projectInstalls.size() > 0) {
                for (int i = 0; i < projectInstalls.size(); i++) {
                    PvTjProjectInstall pvTjProjectInstall = projectInstalls.get(i);
                    List<PvProjectFile> pvProjectFiles = projectInstalls.get(i).getPvProjectFile();
                    if (pvProjectFiles != null && pvProjectFiles.size() > 0) {
                        StringBuffer fileIds = new StringBuffer();
                        for (int j = 0; j < pvProjectFiles.size(); j++) {
                            PvProjectFile pvProjectFile = pvProjectFiles.get(j);
                            if(pvProjectFile != null ){
                                pvProjectFile.setProjectId(id);
                                pvProjectFile.setId(null);
                                pvProjectFileMapper.insert(pvProjectFile);
                                Integer projectFileId = pvProjectFile.getId();
                                fileIds.append(projectFileId + ",");
                            }
                        }
                        pvTjProjectInstall.setFileId(fileIds.toString().substring(0, fileIds.toString().length() - 1));
                    }
                    //获取到文件id
                    List<PvProjectRemark> pvProjectRemarks = projectInstalls.get(i).getPvProjectRemark();

                    if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                        StringBuffer remakesIds = new StringBuffer();
                        for (int j = 0; j < pvProjectRemarks.size(); j++) {
                            PvProjectRemark pvProjectRemark = pvProjectRemarks.get(j);
                            if(pvProjectRemark != null){
                                pvProjectRemark.setProjectId(id);
                                pvProjectRemark.setId(null);
                                pvProjectRemarkMapper.insert(pvProjectRemark);
                                Integer remarkId = pvProjectRemark.getId();
                                remakesIds.append(remarkId + ",");
                            }

                        }
                        pvTjProjectInstall.setRemakesId(remakesIds.toString().substring(0, remakesIds.toString().length() - 1));
                    }
                    pvTjProjectInstall.setProjectId(id);
                    pvTjProjectInstall.setId(null);
                    pvTjProjectInstallMapper.insert(pvTjProjectInstall);
                }
            }
        }

        if (pvProjectBpmList != null && pvProjectBpmList.size() > 0) {
            for (PvProjectBpm entity : pvProjectBpmList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                entity.setApprover(loginUser.getUsername());
                // 非审核环节默认为审核通过
                if (entity.getApprovalResult() == null || entity.getApprovalResult().equals("")) {
                    entity.setApprovalResult(DictApprovalResultEnum.APPROVED.getCode());
                }
                entity.setApprovalTime(new Date());
                pvProjectBpmMapper.insert(entity);
            }
        }

        BpmApprovalStrategy bpmApprovalStrategy = BpmApprovalStrategyFactory.getBpmApprovalStrategy(BpmBusinessCategoryEnum.PROJECT.getCode());

        bpmApprovalStrategy.approval(pvProjectInfo, pvProjectBpmList.get(0));
        String approvalResult = pvProjectBpmList.get(0).getApprovalResult();
        String isCommitted = pvProjectBpmList.get(0).getIsCommitted();


        if (tenantId != null && tenantId == -2103508991) {
            //如果rejectionType为1 代表项目经理驳回 则处理项目状态和日志
            String rejectionType = pvProjectInfo.getRejectionType();
            String rejectionStatus = pvProjectInfo.getRejectionStatus();
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.REJECTED.getCode()) && ("Y".equals(isCommitted))) {
                pvProjectInfoMapper.updateStatusById(rejectionStatus, pvProjectInfo.getId());
            }

            //如果rejectionStatus为2则代表是非项目经理驳回之后的提交
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.APPROVED.getCode()) && ("Y".equals(isCommitted))) {
                //需要先查询项目最新的状态
                PvProjectBpm status = pvProjectBpmMapper.queryStatusById(pvProjectInfo.getId());
                if (BeanUtil.isNotEmpty(status)) {
                    pvProjectInfoMapper.updateBefore(status.getBpmNodeId(), pvProjectInfo.getId());
                }
            }

            //添加日志  驳回的添加日志
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.REJECTED.getCode()) && ("Y".equals(isCommitted))) {
                this.addBeiJPvOperateLog(pvProjectInfo, pvProjectBpmList);
                return;
            }
            //添加项目经理驳回之后  操作员提交提交日志
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.APPROVED.getCode()) && ("Y".equals(isCommitted))) {
                PvOperateLog pvOperateLog = new PvOperateLog();
                //获取驳回到的状态
                String projectStatus = pvProjectInfo.getProjectStatus();
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.queryStatusById(pvProjectInfo.getId());
                pvOperateLog.setProjectId(pvProjectInfo.getId());
                if ((pvProjectBpmList == null || pvProjectBpmList.size() > 0) && pvProjectBpm != null) {
                    DictProjectBeiJStatusEnum dictProjectBeiJStatusEnum = DictProjectBeiJStatusEnum.get(projectStatus);
                    DictProjectBeiJStatusEnum dictProjectBeiJStatusEnum1 = DictProjectBeiJStatusEnum.get(pvProjectBpm.getBpmNodeId());
                    if (dictProjectBeiJStatusEnum != null && dictProjectBeiJStatusEnum1 != null) {
                        pvOperateLog.setOperDescription(dictProjectBeiJStatusEnum.getName() + " " + "提交" + " " + dictProjectBeiJStatusEnum1.getName());
                    }
                }
                pvOperateLog.setOperName(loginUser.getUsername());
                pvOperateLog.setDeptName(loginUser.getOrgCode());
                pvOperateLog.setOperTime(new Date());
                pvOperateLogMapper.insert(pvOperateLog);
            }
            //添加普通提交日志
            else {

                PvOperateLog pvOperateLog = new PvOperateLog();

                pvOperateLog.setProjectId(pvProjectInfo.getId());
                String editDesc = this.getEditDesc(pvProjectInfo, pvProjectBpmList);
                pvOperateLog.setOperDescription(editDesc);
                pvOperateLog.setOperName(loginUser.getUsername());
                pvOperateLog.setDeptName(loginUser.getOrgCode());
                pvOperateLog.setOperTime(new Date());
                pvOperateLogMapper.insert(pvOperateLog);
            }
        } else {
            //如果rejectionType为1 代表项目经理驳回 则处理项目状态和日志
            String rejectionType = pvProjectInfo.getRejectionType();
            String rejectionStatus = pvProjectInfo.getRejectionStatus();
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.REJECTED.getCode()) && ("Y".equals(isCommitted))) {
                pvProjectInfoMapper.updateStatusById(rejectionStatus, pvProjectInfo.getId());
            }

            //如果rejectionStatus为2则代表是被驳回之后的提交
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.APPROVED.getCode()) && ("Y".equals(isCommitted))) {
                //需要先查询项目最新的状态
                PvProjectBpm status = pvProjectBpmMapper.queryStatusById(pvProjectInfo.getId());
                if (pvProjectInfo.getProjectStatus().equals(DictProjectStatusEnum.POWER_STATION_APPROVAL.getCode())) {
                    // 选择候补施工，驳回提交到并网
                    PvProjectInfo projectInfo = pvProjectInfoMapper.selectById(pvProjectInfo.getId());
                    if (pvProjectInfo.getBuildAfter().equals("1")) {
                        // 该项目原本为候补施工项目,驳回提交到之前节点
                        if (projectInfo.getBuildAfter().equals("1")) {
                            pvProjectInfoMapper.updateBefore(status.getBpmNodeId(), pvProjectInfo.getId());
                        } else if (projectInfo.getBuildAfter().equals("0")) {
                            // 该项目原本为非候补施工项目,驳回提交到并网
                            pvProjectInfoMapper.updateBefore(DictProjectStatusEnum.PROJECT_CONNECTED.getCode(), pvProjectInfo.getId());
                        }
                    }
                    // 选择非候补施工，判断驳回之前是否为候补施工 (根据是否分配施工队来判断)
                    else if (pvProjectInfo.getBuildAfter().equals("0")) {
                        // 该项目原本为候补施工项目,驳回提交到分配施工队
                        if (projectInfo.getBuildAfter().equals("1")) {
                            pvProjectInfoMapper.updateBefore(DictProjectStatusEnum.ASSIGN_CONSTR_TEAMS.getCode(), pvProjectInfo.getId());
                        } else if (projectInfo.getBuildAfter().equals("0")) {
                            // 该项目原本为非候补施工项目,驳回提交到之前节点
                            pvProjectInfoMapper.updateBefore(status.getBpmNodeId(), pvProjectInfo.getId());
                        }
                    }
                    // 其他情况走正常流程
                } else {
                    pvProjectInfoMapper.updateBefore(status.getBpmNodeId(), pvProjectInfo.getId());
                }
            }
            //添加日志  驳回的添加日志
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.REJECTED.getCode())) {
                this.addPvOperateLog(pvProjectInfo, pvProjectBpmList);
                return;
            }
            //添加项目经理驳回之后  操作员提交提交日志
            if (StringUtils.isNotBlank(rejectionType) && approvalResult.equals(DictApprovalResultEnum.APPROVED.getCode()) && ("Y".equals(isCommitted))) {
                PvOperateLog pvOperateLog = new PvOperateLog();
                //获取驳回到的状态
                String projectStatus = pvProjectInfo.getProjectStatus();
                PvProjectBpm status = pvProjectBpmMapper.queryStatusById(pvProjectInfo.getId());
                String name = DictProjectStatusEnum.get(status.getBpmNodeId()).getName();
                pvOperateLog.setProjectId(pvProjectInfo.getId());
                PvProjectInfo projectInfo = pvProjectInfoMapper.selectById(pvProjectInfo.getId());
                if (pvProjectInfo.getProjectStatus().equals(DictProjectBpmEnum.POWER_STATION_APPROVAL.getCode())) {
                    if ("1".equals(pvProjectInfo.getBuildAfter())) {
                        if ("0".equals(projectInfo.getBuildAfter())) {
                            name = DictProjectStatusEnum.PROJECT_CONNECTED.getName();
                        }
                    } else if ("0".equals(pvProjectInfo.getBuildAfter())) {
                        if ("1".equals(projectInfo.getBuildAfter())) {
                            name = DictProjectStatusEnum.ASSIGN_CONSTR_TEAMS.getName();
                        }
                    }
                }
                if (pvProjectBpmList == null || pvProjectBpmList.size() > 0) {
                    pvOperateLog.setOperDescription(DictProjectStatusEnum.get(projectStatus).getName() + " " + "提交" + " " + name);
                }
                pvOperateLog.setOperName(loginUser.getUsername());
                pvOperateLog.setDeptName(loginUser.getOrgCode());
                pvOperateLog.setOperTime(new Date());
                pvOperateLogMapper.insert(pvOperateLog);
            }
            //添加普通提交日志
            else {

                PvOperateLog pvOperateLog = new PvOperateLog();

                pvOperateLog.setProjectId(pvProjectInfo.getId());
                String editDesc = this.getEditDesc(pvProjectInfo, pvProjectBpmList);
                pvOperateLog.setOperDescription(editDesc);
                pvOperateLog.setOperName(loginUser.getUsername());
                pvOperateLog.setDeptName(loginUser.getOrgCode());
                pvOperateLog.setOperTime(new Date());
                pvOperateLogMapper.insert(pvOperateLog);
            }
        }
        PvProjectInfo projectInfo = pvProjectInfoMapper.selectById(pvProjectBpmList.get(0).getProjectId());
        if (BeanUtil.isNotEmpty(projectInfo)) {
            projectInfo.setBuildAfter(pvProjectInfo.getBuildAfter());
            pvProjectInfoMapper.updateById(projectInfo);
        }
    }

    /**
     * 保存接口
     *
     * @param pvProjectInfo
     * @param pvProjectBpmList
     * @param pvProjectFileList
     * @param pvProjectRemarkList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void keepProjectInfo(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList, List<PvProjectFile> pvProjectFileList, List<PvProjectRemark> pvProjectRemarkList) {
        Integer tenantId = pvProjectInfo.getTenantId();
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectInfo.setUpdateTime(new Date());
        // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
        // 电话号码
        String customerMobile = pvProjectInfo.getCustomerMobile();
        // 身份号码
        String idNumber = pvProjectInfo.getIdNumber();
        // 银行卡持有人
        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
        // 客户姓名
//        String customerName = pvProjectInfo.getCustomerName();

        // 获取秘钥
        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
        String passwordKey = pvProjectKey.getPasswordKey();
        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
        // 秘钥转base4
        String keyStr = AESUtils.encryptKeyFromBase64(secretKey);
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
//        if (StringUtils.isNotBlank(customerName)) {
//            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//            pvProjectInfo.setCustomerName(encryptCustomerName);
//        }
        //如果是北京项目需要把数据配置信息表里面的数据存储到项目表
        if (tenantId != null && tenantId == -2103508991) {
//            pvProjectInfo.setSalespersonId(loginUser.getId());
            List<HouseTypeVo> typeList = pvProjectInfo.getTypeList();
            if (typeList != null && !typeList.isEmpty()) {
                String string = JSONObject.toJSONString(typeList);
                pvProjectInfo.setHouseTypeGroup(string);
            }
            for (int i = 0; i < pvProjectRemarkList.size(); i++) {
                Integer paramItmeId = pvProjectRemarkList.get(i).getParamItmeId();
                Integer groupId = pvProjectRemarkList.get(i).getParamGroupId();
                if (paramItmeId == 238 && groupId == 57) {
                    //户主姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setHouseOwner(encryptRemark);
                    }
                }
                if (paramItmeId == 239 && groupId == 57) {
                    //房本编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setHouseNo(remark);
                }
                if (paramItmeId == 240 && groupId == 57) {
                    //关系说明
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setRelationInfo(remark);

                }
                if (paramItmeId == 242 && groupId == 58) {
                    //电卡姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setElectricityOwner(encryptRemark);
                    }

                }
                if (paramItmeId == 243 && groupId == 58) {
                    //电卡编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setElectricityCardNo(remark);
                }
                if (paramItmeId == 244 && groupId == 58) {
                    //期望开卡时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setOpenCardTime(remark);
                }
                if (paramItmeId == 245 && groupId == 58) {
                    //期望安装时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setWorkTime(remark);
                }
                if (paramItmeId == 363 && groupId == 61) {
                    // 电费收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 364 && groupId == 61) {
                    // 电费收益卡号开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
                if (paramItmeId == 367 && groupId == 61) {
                    // 租金收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 366 && groupId == 61) {
                    // 租金收益卡持有人
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 368 && groupId == 61) {
                    // 租金收益卡开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
            }

        }
        pvProjectInfoMapper.updateById(pvProjectInfo);

        //1.先删除子表数据
        pvProjectFileMapper.physicalDeleteByMainId(pvProjectInfo.getId());
        pvProjectRemarkMapper.physicalDeleteByMainId(pvProjectInfo.getId());
        pvProjectBpmMapper.physicalDeleteByMainId(pvProjectBpmList.get(0));
        //删除逆变器子表数据
        iSysInverterService.deleteByProjectId(pvProjectInfo.getId());
        //插入逆变器
        List<SysInverterVO> sysInverterVOS = pvProjectInfo.getSysInverterVOS();
        iSysInverterService.addSysInverterVOS(sysInverterVOS);

        //删除组件数据
        iSysAssemblyService.deleteByProjectId(pvProjectInfo.getId());
        //插入组件
        List<SysAssemblyVO> sysAssemblyVOS = pvProjectInfo.getSysAssemblyVOS();
        iSysAssemblyService.addSysInverterVOS(sysAssemblyVOS);


        //2.子表数据重新插入
        if (pvProjectFileList != null && pvProjectFileList.size() > 0) {
            for (PvProjectFile entity : pvProjectFileList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectFileMapper.insert(entity);
            }
        }
        if (pvProjectRemarkList != null && pvProjectRemarkList.size() > 0) {
            for (PvProjectRemark entity : pvProjectRemarkList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                pvProjectRemarkMapper.insert(entity);
            }
        }

        if (pvProjectBpmList != null && pvProjectBpmList.size() > 0) {
            for (PvProjectBpm entity : pvProjectBpmList) {
                //外键设置
                entity.setId(null);
                entity.setProjectId(pvProjectInfo.getId());
                entity.setApprover(loginUser.getUsername());
                // 非审核环节默认为审核通过
                if (entity.getApprovalResult() == null || entity.getApprovalResult().equals("")) {
                    entity.setApprovalResult(DictApprovalResultEnum.APPROVED.getCode());
                }
                entity.setApprovalTime(new Date());
                pvProjectBpmMapper.insert(entity);
            }
        }
        BpmApprovalStrategy bpmApprovalStrategy = BpmApprovalStrategyFactory.getBpmApprovalStrategy(BpmBusinessCategoryEnum.PROJECT.getCode());
        bpmApprovalStrategy.approval(pvProjectInfo, pvProjectBpmList.get(0));

        PvOperateLog pvOperateLog = new PvOperateLog();

        pvOperateLog.setProjectId(pvProjectInfo.getId());
        String editDesc = this.getEditDesc(pvProjectInfo, pvProjectBpmList);
        pvOperateLog.setOperDescription(editDesc);
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        pvOperateLogMapper.insert(pvOperateLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMainFile(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList, List<PvProjectFile> pvProjectFileList, List<PvProjectRemark> pvProjectRemarkList) {
        Integer tenantId = pvProjectInfo.getTenantId();
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectInfo.setUpdateTime(new Date());
        // 加密： 1、电话号码 2、身份号码 3、银行卡号 4、客户姓名 5、银行卡持有人
        // 电话号码
        String customerMobile = pvProjectInfo.getCustomerMobile();
        // 身份号码
        String idNumber = pvProjectInfo.getIdNumber();
        // 银行卡持有人
        String conBankCardHolders = pvProjectInfo.getConBankCardHolders();
        // 客户姓名
//        String customerName = pvProjectInfo.getCustomerName();

        // 获取秘钥
        PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
        String passwordKey = pvProjectKey.getPasswordKey();
        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
        // 秘钥转base4
        String keyStr = AESUtils.encryptKeyFromBase64(secretKey);
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
//        if (StringUtils.isNotBlank(customerName)) {
//            String encryptCustomerName = AESUtils.encrypt(customerName, secretKey);
//            pvProjectInfo.setCustomerName(encryptCustomerName);
//        }
        //如果是北京项目需要把数据配置信息表里面的数据存储到项目表
        if (tenantId != null && tenantId == -2103508991) {
//            pvProjectInfo.setSalespersonId(loginUser.getId());
            List<HouseTypeVo> typeList = pvProjectInfo.getTypeList();
            if (typeList != null && !typeList.isEmpty()) {
                String string = JSONObject.toJSONString(typeList);
                pvProjectInfo.setHouseTypeGroup(string);
            }
            for (int i = 0; i < pvProjectRemarkList.size(); i++) {
                Integer paramItmeId = pvProjectRemarkList.get(i).getParamItmeId();
                Integer groupId = pvProjectRemarkList.get(i).getParamGroupId();
                if (paramItmeId == 238 && groupId == 57) {
                    //户主姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setHouseOwner(encryptRemark);
                    }
                }
                if (paramItmeId == 239 && groupId == 57) {
                    //房本编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setHouseNo(remark);
                }
                if (paramItmeId == 240 && groupId == 57) {
                    //关系说明
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setRelationInfo(remark);

                }
                if (paramItmeId == 242 && groupId == 58) {
                    //电卡姓名
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        // 加密
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                        pvProjectInfo.setElectricityOwner(encryptRemark);
                    }

                }
                if (paramItmeId == 243 && groupId == 58) {
                    //电卡编号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setElectricityCardNo(remark);
                }
                if (paramItmeId == 244 && groupId == 58) {
                    //期望开卡时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setOpenCardTime(remark);
                }
                if (paramItmeId == 245 && groupId == 58) {
                    //期望安装时间
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setWorkTime(remark);
                }
                if (paramItmeId == 363 && groupId == 61) {
                    // 电费收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 364 && groupId == 61) {
                    // 电费收益卡号开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
                if (paramItmeId == 367 && groupId == 61) {
                    // 租金收益卡号
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 366 && groupId == 61) {
                    // 租金收益卡持有人
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    if (StringUtils.isNotBlank(remark)) {
                        String encryptRemark = AESUtils.encrypt(remark, secretKey);
                        pvProjectRemarkList.get(i).setRemark(encryptRemark);
                    }
                }
                if (paramItmeId == 368 && groupId == 61) {
                    // 租金收益卡开户行
                    String remark = pvProjectRemarkList.get(i).getRemark();
                    pvProjectInfo.setReturnFeeDebitCardBank(remark);
                }
            }
            pvProjectInfoMapper.updateById(pvProjectInfo);
        }
        pvProjectInfoMapper.updateById(pvProjectInfo);
        //天津项目特殊处理
        if (tenantId != null && tenantId == 102) {
            Integer id = pvProjectInfo.getId();
            //先删除在插入
            pvTjProjectInstallMapper.deleteByProjectId(id);
            List<PvTjProjectInstall> projectInstalls = pvProjectInfo.getProjectInstalls();
            if (projectInstalls != null && projectInstalls.size() > 0) {
                String totalCapacity = pvProjectInfo.getTotalCapacity();
                BigDecimal instCmptCapacity = pvProjectInfo.getInstCmptCapacity();
                PvProjectInfo projectInfo = new PvProjectInfo();
                projectInfo.setId(id);
                if (StringUtils.isNotBlank(totalCapacity)) {
                    projectInfo.setTotalCapacity(totalCapacity);
                }
                if (instCmptCapacity != null) {
                    projectInfo.setInstCmptCapacity(instCmptCapacity);
                }
                pvProjectInfoMapper.updateById(pvProjectInfo);
                if (projectInstalls != null && projectInstalls.size() > 0) {
                    for (int i = 0; i < projectInstalls.size(); i++) {
                        PvTjProjectInstall pvTjProjectInstall = projectInstalls.get(i);
                        List<PvProjectFile> pvProjectFiles = projectInstalls.get(i).getPvProjectFile();
                        if (pvProjectFiles != null && pvProjectFiles.size() > 0) {
                            StringBuffer fileIds = new StringBuffer();
                            for (int j = 0; j < pvProjectFiles.size(); j++) {
                                PvProjectFile pvProjectFile = pvProjectFiles.get(j);
                                pvProjectFile.setProjectId(id);
                                pvProjectFile.setId(null);
                                pvProjectFileMapper.insert(pvProjectFile);
                                Integer projectFileId = pvProjectFile.getId();
                                fileIds.append(projectFileId + ",");
                            }
                            pvTjProjectInstall.setFileId(fileIds.toString().substring(0, fileIds.toString().length() - 1));
                        }
                        //获取到文件id
                        List<PvProjectRemark> pvProjectRemarks = projectInstalls.get(i).getPvProjectRemark();

                        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                            StringBuffer remakesIds = new StringBuffer();
                            for (int j = 0; j < pvProjectRemarks.size(); j++) {
                                PvProjectRemark pvProjectRemark = pvProjectRemarks.get(j);
                                pvProjectRemark.setProjectId(id);
                                pvProjectRemark.setId(null);
                                pvProjectRemarkMapper.insert(pvProjectRemark);
                                Integer remarkId = pvProjectRemark.getId();
                                remakesIds.append(remarkId + ",");
                            }
                            pvTjProjectInstall.setRemakesId(remakesIds.toString().substring(0, remakesIds.toString().length() - 1));
                        }
                        pvTjProjectInstall.setProjectId(id);
//                    pvTjProjectInstall.setCreateDate(new Date());
                        pvTjProjectInstallMapper.insert(pvTjProjectInstall);
                    }
                }

            }
            //直接修改原来数据
//        pvProjectFileMapper.physicalDeleteByMainId(pvProjectInfo.getId());
//        pvProjectRemarkMapper.physicalDeleteByMainId(pvProjectInfo.getId());
//        pvProjectBpmMapper.physicalDeleteByMainId(pvProjectBpmList.get(0));
        }
        for (int i = 0; i < pvProjectBpmList.size(); i++) {
            pvProjectBpmMapper.updateById(pvProjectBpmList.get(i));
        }
        for (int i = 0; i < pvProjectFileList.size(); i++) {
            pvProjectFileMapper.updateById(pvProjectFileList.get(i));
        }
        for (int i = 0; i < pvProjectRemarkList.size(); i++) {
            pvProjectRemarkMapper.updateById(pvProjectRemarkList.get(i));
        }
        //删除逆变器子表数据
        iSysInverterService.deleteByProjectId(pvProjectInfo.getId());
        //插入逆变器
        List<SysInverterVO> sysInverterVOS = pvProjectInfo.getSysInverterVOS();
        iSysInverterService.addSysInverterVOS(sysInverterVOS);

        //删除组件数据
        iSysAssemblyService.deleteByProjectId(pvProjectInfo.getId());
        //插入组件
        List<SysAssemblyVO> sysAssemblyVOS = pvProjectInfo.getSysAssemblyVOS();
        iSysAssemblyService.addSysInverterVOS(sysAssemblyVOS);

        PvOperateLog pvOperateLog = new PvOperateLog();

        pvOperateLog.setProjectId(pvProjectInfo.getId());
//        String editDesc = this.getEditDesc(pvProjectInfo, pvProjectBpmList);
        pvOperateLog.setOperDescription("归档编辑");
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        pvOperateLogMapper.insert(pvOperateLog);

    }

    /**
     * 项目经理驳回添加日志
     *
     * @param pvProjectInfo
     * @param pvProjectBpmList
     */
    public void addPvOperateLog(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList) {
        //通过或者驳回
        PvOperateLog pvOperateLog = new PvOperateLog();
        String name = null;
        //获取驳回到的状态
        name = DictProjectStatusEnum.get(pvProjectInfo.getRejectionStatus()).getName();
        String nowStatus = DictProjectStatusEnum.get(pvProjectInfo.getProjectStatus()).getName();
        pvOperateLog.setProjectId(pvProjectInfo.getId());
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (pvProjectBpmList == null || pvProjectBpmList.size() > 0) {
            pvOperateLog.setOperDescription(nowStatus + " " + DictApprovalResultEnum.REJECTED.getName() + " " + name);
        }
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        pvOperateLogMapper.insert(pvOperateLog);

    }

    public void addBeiJPvOperateLog(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList) {
        //通过或者驳回
        PvOperateLog pvOperateLog = new PvOperateLog();
        String name = null;
        //获取驳回到的状态
        String rejectionStatus = pvProjectInfo.getRejectionStatus();
        DictProjectBeiJStatusEnum dictProjectBeiJStatusEnum = DictProjectBeiJStatusEnum.get(rejectionStatus);
        if (StringUtils.isNotBlank(rejectionStatus) && dictProjectBeiJStatusEnum != null) {
            name = dictProjectBeiJStatusEnum.getName();
        }
        String nowStatus = DictProjectBeiJStatusEnum.get(pvProjectInfo.getProjectStatus()).getName();
        pvOperateLog.setProjectId(pvProjectInfo.getId());
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (pvProjectBpmList == null || pvProjectBpmList.size() > 0) {
            pvOperateLog.setOperDescription(nowStatus + " " + DictApprovalResultEnum.REJECTED.getName() + " " + name);
        }
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        pvOperateLogMapper.insert(pvOperateLog);

    }

    private String getEditDesc(PvProjectInfo pvProjectInfo, List<PvProjectBpm> pvProjectBpmList) {
        String renterId = pvProjectInfo.getRenterId();
        Integer tenantId = pvProjectInfo.getTenantId();
        if ((tenantId != null && tenantId == -2103508991) || ((StringUtils.isNotBlank(renterId) && renterId.equals("-2103508991")))) {
            String desc = null;
            DictProjectBeiJStatusEnum projectStatus = DictProjectBeiJStatusEnum.get(pvProjectInfo.getProjectStatus());

            desc = projectStatus.getName();
            if (pvProjectBpmList == null || pvProjectBpmList.size() == 0) {
                return desc;
            }

            DictYesNoEnum isCommitted = DictYesNoEnum.get(pvProjectBpmList.get(0).getIsCommitted());
            DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(pvProjectBpmList.get(0).getApprovalResult());

            if (projectStatus == DictProjectBeiJStatusEnum.DATA_APPROVAL || projectStatus == DictProjectBeiJStatusEnum.CONSTR_INSPECTION) {
                switch (approvalResult) {
                    case APPROVED:
                        desc += " " + DictApprovalResultEnum.APPROVED.getName();
                        break;
                    case REJECTED:
                        desc += " " + DictApprovalResultEnum.REJECTED.getName();
                        break;
                    default:
                        break;
                }
            } else if (isCommitted == DictYesNoEnum.YES) {
                desc += " 提交";
            } else {
                desc += " 修改";
            }
            return desc;
        } else {
            String desc = null;
            DictProjectStatusEnum projectStatus = DictProjectStatusEnum.get(pvProjectInfo.getProjectStatus());

            desc = projectStatus.getName();
            if (pvProjectBpmList == null || pvProjectBpmList.size() == 0) {
                return desc;
            }

            DictYesNoEnum isCommitted = DictYesNoEnum.get(pvProjectBpmList.get(0).getIsCommitted());
            DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(pvProjectBpmList.get(0).getApprovalResult());

            if (projectStatus == DictProjectStatusEnum.DATA_APPROVAL || projectStatus == DictProjectStatusEnum.POWER_STATION_APPROVAL || projectStatus == DictProjectStatusEnum.CONSTR_INSPECTION) {
                switch (approvalResult) {
                    case APPROVED:
                        desc += " " + DictApprovalResultEnum.APPROVED.getName();
                        break;
                    case REJECTED:
                        desc += " " + DictApprovalResultEnum.REJECTED.getName();
                        break;
                    default:
                        break;
                }
            } else if (isCommitted == DictYesNoEnum.YES) {
                desc += " 提交";
            } else {
                desc += " 修改";
            }
            return desc;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(Integer id) {
        pvProjectBpmMapper.deleteByMainId(id);
        pvProjectFileMapper.deleteByMainId(id);
        pvProjectRemarkMapper.deleteByMainId(id);
        pvProjectInfoMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            pvProjectBpmMapper.deleteByMainId(Integer.valueOf(id.toString()));
            pvProjectFileMapper.deleteByMainId(Integer.valueOf(id.toString()));
            pvProjectRemarkMapper.deleteByMainId(Integer.valueOf(id.toString()));
//            pvProjectInfoMapper.deleteById(id);

            pvProjectInfoMapper.restoreByprojectId(Integer.valueOf(id.toString()), new Date());
        }
    }

    @Override
    public void setProjectStatus(Integer projectId, String status) {
        PvProjectInfo projectInfo = getById(projectId);
        projectInfo.setProjectStatus(status);
        baseMapper.updateById(projectInfo);
    }

    @Override
    public void setProjectBeiJStatus(Integer projectId, String status) {
        PvProjectInfo projectInfo = getById(projectId);
        projectInfo.setProjectStatus(status);
        baseMapper.updateById(projectInfo);
    }

    @Override
    public PvProjectInfo getLogicalDeletedById(Integer id) {
        return pvProjectInfoMapper.getLogicalDeletedById(id);
    }

    @Override

    public void deleteExpiredImages() {
        List<PvProjectInfo> pvProjectInfoList = pvProjectInfoMapper.getExpirationList(imageExpirationDays);
        for (PvProjectInfo entity : pvProjectInfoList) {
            List<PvProjectFile> pvProjectFileList = pvProjectFileMapper.getExpirationList(entity.getId());
            for (PvProjectFile pvProjectFile : pvProjectFileList) {
                if (isAllowDeletion(pvProjectFile.getParamGroupId(), pvProjectFile.getParamItmeId())) {
                    deleteImages(pvProjectFile.getFileUrl());
                    pvProjectFile.setIsOriginalDeleted(DictYesNoEnum.YES.getCode());
                    pvProjectFileMapper.updateById(pvProjectFile);
                }
            }
        }
    }



    @Override
    public List<Object> getTaskStatus(String taskId) {
       return redisTemplate.opsForList().range("Task:"+taskId, 0, -1);
    }

    private boolean isAllowDeletion(int groupId, int paramId) {
        boolean isAllowDeletion = true;
        for (SetParamGroup setParamGroup : setParamGroupList) {
            if (setParamGroup.getGroupId() == groupId && setParamGroup.getParamId() == paramId) {
                isAllowDeletion = false;
                break;
            }
        }
        return isAllowDeletion;
    }

    private void deleteImages(String fileUrls) {
        String[] fileUrlArray = fileUrls.split(",");
        for (String fileUrl : fileUrlArray) {
            File file = new File(uploadpath + "/" + ORIGINAL_PATH + "/" + fileUrl);
            file.delete();
        }
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVO> getList(String taskId,HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper); // 指定租户的所有项目信息
            List<ProjectExportVO> projectExportVOS = getProjectExportVOS(renterId, projectList);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, projectExportVOS.toArray());
            return projectExportVOS;

        } else {

            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String id = loginUser.getId();
            String roleId = iRegionsServiceOne.getRoleId(id);

            List<PvProjectInfo> projectList = pvProjectInfoMapper.selectList(new LambdaQueryWrapper<PvProjectInfo>().eq(PvProjectInfo::getTenantId, renterId));
            List<ProjectExportVO> projectExportVOS = getProjectExportVOS(renterId, projectList);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, projectExportVOS.toArray());
            return projectExportVOS;
        }
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOXuZ> getListXuZ(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
//            List<PvProjectInfo> projectList = this.list(queryWrapper); // 指定租户的所有项目信息
//            List<ProjectExportVOXuZ> projectExportVOS = getProjectExportVOSXuZ(renterId, projectList);
//            redisTemplate.opsForList().leftPushAll("Task:"+taskId, projectExportVOS.toArray());
//            return projectExportVOS;
            List<PvProjectInfo> projectList = this.list(queryWrapper); // 指定租户的所有项目信息
            List<ProjectExportVOXuZ> projectExportVOS = getProjectExportVOSXuZ(renterId, projectList);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, projectExportVOS.toArray());
            return projectExportVOS;
        } else {
            List<PvProjectInfo> projectList = pvProjectInfoMapper.selectList(new LambdaQueryWrapper<PvProjectInfo>().eq(PvProjectInfo::getTenantId, renterId));
            List<ProjectExportVOXuZ> projectExportVOS = getProjectExportVOSXuZ(renterId, projectList);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, projectExportVOS.toArray());
            return projectExportVOS;
        }
    }

    @Async("exportTaskExecutor")
    private List<ProjectExportVOXuZ> getProjectExportVOSXuZ(String renterId, List<PvProjectInfo> projectList) {
        List<ProjectExportVOXuZ> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            Integer projectId = pv.getId();
            // 安装信息
            LambdaQueryWrapper<PvTjProjectInstall> pvTjProjectInstallLambdaQueryWrapper = new LambdaQueryWrapper<>();
            pvTjProjectInstallLambdaQueryWrapper.eq(PvTjProjectInstall::getProjectId, projectId);
            List<PvTjProjectInstall> pvTjProjectInstallList = pvTjProjectInstallMapper.selectList(pvTjProjectInstallLambdaQueryWrapper);
//            if (CollectionUtil.isNotEmpty(pvTjProjectInstallList)) {
            StringJoiner joiner = new StringJoiner(",");
            for (PvTjProjectInstall pvTjProjectInstall : pvTjProjectInstallList) {
                String remakesId = pvTjProjectInstall.getRemakesId();
                if (StringUtils.isNotBlank(remakesId)) {
                    List<String> remakeIdList = Arrays.asList(remakesId.split(","));
                    if (CollectionUtil.isNotEmpty(remakeIdList)) {
                        remakeIdList.forEach(remakeId -> {
                            PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectById(remakeId);
                            if (BeanUtil.isNotEmpty(pvProjectRemark)) {
                                joiner.add(pvProjectRemark.getRemark());
                            }
                        });
                    }
                }
                pvTjProjectInstall.setRemake(String.valueOf(joiner));
            }
            pv.setProjectInstalls(pvTjProjectInstallList);
//            }
            // 施工信息
            // 1. 组件信息
//            List<SysAssembly> sysAssemblyList = iSysAssemblyService.list(new LambdaQueryWrapper<SysAssembly>().eq(SysAssembly::getProjectId, projectId));
////            if (CollectionUtil.isNotEmpty(sysAssemblyList)) {
//                List<SysAssemblyVO> sysAssemblyVOList = sysAssemblyList.stream().map(sysAssembly -> {
//                    SysAssemblyVO sysAssemblyVO = new SysAssemblyVO();
//                    BeanUtils.copyProperties(sysAssembly, sysAssemblyVO);
//                    return sysAssemblyVO;
//                }).collect(Collectors.toList());
//                pv.setSysAssemblyVOS(sysAssemblyVOList);
////            }
//            // 2. 逆变器信息 (接线信息 + mppt信息)
//            List<SysInverter> sysInverterList = iSysInverterService.list(new LambdaQueryWrapper<SysInverter>().eq(SysInverter::getProjectId, projectId));
////            if (CollectionUtil.isNotEmpty(sysInverterList)) {
//                List<SysInverterVO> sysInverterVOList = sysInverterList.stream().map(sysInverter -> {
//                    List<SysInverterWire> sysInverterWireList = iSysInverterWireService.list(new LambdaQueryWrapper<SysInverterWire>().eq(SysInverterWire::getInverterId, sysInverter.getId()));
////                    if (CollectionUtil.isNotEmpty(sysInverterWireList)) {
//                        List<SysInverterWireVO> sysInverterWireVOList = sysInverterWireList.stream().map(sysInverterWire -> {
//                            List<SysInverterWireMppt> sysInverterWireMpptList = iSysInverterWireMpptService.list(new LambdaQueryWrapper<SysInverterWireMppt>().eq(SysInverterWireMppt::getInverterWireId, sysInverterWire.getId()));
////                            if (CollectionUtil.isNotEmpty(sysInverterWireMpptList)) {
//                                List<SysInverterWireMpptVO> sysInverterWireMpptVOList = sysInverterWireMpptList.stream().map(sysInverterWireMppt -> {
//                                    SysInverterWireMpptVO sysInverterWireMpptVO = new SysInverterWireMpptVO();
//                                    BeanUtils.copyProperties(sysInverterWireMppt, sysInverterWireMpptVO);
//                                    return sysInverterWireMpptVO;
//                                }).collect(Collectors.toList());
//                                SysInverterWireVO sysInverterWireVO = new SysInverterWireVO();
//                                BeanUtils.copyProperties(sysInverterWire, sysInverterWireVO);
//                                sysInverterWireVO.setSysInverterWireMpptVOS(sysInverterWireMpptVOList);
//                                return sysInverterWireVO;
////                            }
////                            return null;
//                        }).collect(Collectors.toList());
//                        SysInverterVO sysInverterVO = new SysInverterVO();
//                        BeanUtils.copyProperties(sysInverter, sysInverterVO);
//                        sysInverterVO.setSysInverterWireVOS(sysInverterWireVOList);
//                        return sysInverterVO;
////                    }
////                    return null;
//                }).collect(Collectors.toList());

//                Integer strand = 0; // 串
//                Integer piece = 0; // 块
//                Integer wireNum = 0; // 接线数量
//                for (SysInverterVO sysInverterVO : sysInverterVOList) {
//                    wireNum = 0;
//                    strand = 0;
//                    piece = 0;
//                    for (SysInverterWireVO sysInverterWireVO : sysInverterVO.getSysInverterWireVOS()) {
//                        wireNum += sysInverterWireVO.getWireNum();
//                        for (SysInverterWireMpptVO sysInverterWireMpptVO : sysInverterWireVO.getSysInverterWireMpptVOS()) {
//                            strand += Integer.parseInt(sysInverterWireMpptVO.getStrand());
//                            piece += sysInverterWireMpptVO.getPiece();
//                        }
//                    }
//                    sysInverterVO.setStrand(String.valueOf(strand));
//                    sysInverterVO.setPiece(piece);
//                    sysInverterVO.setWireNum(wireNum);
//                }

//                pv.setSysInverterVOS(sysInverterVOList);
//            }

            ProjectExportVOXuZ projectExportVO = new ProjectExportVOXuZ();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }

            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVOXuZ> data = this.getDataXuZ(projectExportVOS);
        return data;
    }

    public List<PvProjectInfo> replicateList(List<PvProjectInfo> originalList, int times) {
        if (originalList == null || originalList.isEmpty() || times <= 0) {
            return new ArrayList<>(); // 防御性返回空 List
        }

        List<PvProjectInfo> result = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            result.addAll(originalList); // 逐次追加
        }
        return result;
    }
    // 获取导出类
    @Async("exportTaskExecutor")
    private List<ProjectExportVO> getProjectExportVOS(String renterId, List<PvProjectInfo> projectList) {
//        List<PvProjectInfo> projectList = this.replicateList(projectList1, 2);
        ArrayList<ProjectExportVO> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            Integer projectId = pv.getId();
            // 安装信息
            LambdaQueryWrapper<PvTjProjectInstall> pvTjProjectInstallLambdaQueryWrapper = new LambdaQueryWrapper<>();
            pvTjProjectInstallLambdaQueryWrapper.eq(PvTjProjectInstall::getProjectId, projectId);
            List<PvTjProjectInstall> pvTjProjectInstallList = pvTjProjectInstallMapper.selectList(pvTjProjectInstallLambdaQueryWrapper);
//            if (CollectionUtil.isNotEmpty(pvTjProjectInstallList)) {
            StringJoiner joiner = new StringJoiner(",");
            for (PvTjProjectInstall pvTjProjectInstall : pvTjProjectInstallList) {
                String remakesId = pvTjProjectInstall.getRemakesId();
                if (StringUtils.isNotBlank(remakesId)) {
                    List<String> remakeIdList = Arrays.asList(remakesId.split(","));
                    if (CollectionUtil.isNotEmpty(remakeIdList)) {
                        remakeIdList.forEach(remakeId -> {
                            PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectById(remakeId);
                            if (BeanUtil.isNotEmpty(pvProjectRemark)) {
                                joiner.add(pvProjectRemark.getRemark());
                            }
                        });
                    }
                }
                pvTjProjectInstall.setRemake(String.valueOf(joiner));
            }
            pv.setProjectInstalls(pvTjProjectInstallList);

//            }
            // 施工信息
            // 1. 组件信息
//            List<SysAssembly> sysAssemblyList = iSysAssemblyService.list(new LambdaQueryWrapper<SysAssembly>().eq(SysAssembly::getProjectId, projectId));
////            if (CollectionUtil.isNotEmpty(sysAssemblyList)) {
//                List<SysAssemblyVO> sysAssemblyVOList = sysAssemblyList.stream().map(sysAssembly -> {
//                    SysAssemblyVO sysAssemblyVO = new SysAssemblyVO();
//                    BeanUtils.copyProperties(sysAssembly, sysAssemblyVO);
//                    return sysAssemblyVO;
//                }).collect(Collectors.toList());
//                pv.setSysAssemblyVOS(sysAssemblyVOList);
////            }
//            // 2. 逆变器信息 (接线信息 + mppt信息)
//            List<SysInverter> sysInverterList = iSysInverterService.list(new LambdaQueryWrapper<SysInverter>().eq(SysInverter::getProjectId, projectId));
////            if (CollectionUtil.isNotEmpty(sysInverterList)) {
//                List<SysInverterVO> sysInverterVOList = sysInverterList.stream().map(sysInverter -> {
//                    List<SysInverterWire> sysInverterWireList = iSysInverterWireService.list(new LambdaQueryWrapper<SysInverterWire>().eq(SysInverterWire::getInverterId, sysInverter.getId()));
////                    if (CollectionUtil.isNotEmpty(sysInverterWireList)) {
//                        List<SysInverterWireVO> sysInverterWireVOList = sysInverterWireList.stream().map(sysInverterWire -> {
//                            List<SysInverterWireMppt> sysInverterWireMpptList = iSysInverterWireMpptService.list(new LambdaQueryWrapper<SysInverterWireMppt>().eq(SysInverterWireMppt::getInverterWireId, sysInverterWire.getId()));
////                            if (CollectionUtil.isNotEmpty(sysInverterWireMpptList)) {
//                                List<SysInverterWireMpptVO> sysInverterWireMpptVOList = sysInverterWireMpptList.stream().map(sysInverterWireMppt -> {
//                                    SysInverterWireMpptVO sysInverterWireMpptVO = new SysInverterWireMpptVO();
//                                    BeanUtils.copyProperties(sysInverterWireMppt, sysInverterWireMpptVO);
//                                    return sysInverterWireMpptVO;
//                                }).collect(Collectors.toList());
//                                SysInverterWireVO sysInverterWireVO = new SysInverterWireVO();
//                                BeanUtils.copyProperties(sysInverterWire, sysInverterWireVO);
//                                sysInverterWireVO.setSysInverterWireMpptVOS(sysInverterWireMpptVOList);
//                                return sysInverterWireVO;
////                            }
////                            return null;
//                        }).collect(Collectors.toList());
//                        SysInverterVO sysInverterVO = new SysInverterVO();
//                        BeanUtils.copyProperties(sysInverter, sysInverterVO);
//                        sysInverterVO.setSysInverterWireVOS(sysInverterWireVOList);
//                        return sysInverterVO;
////                    }
////                    return null;
//                }).collect(Collectors.toList());

//                Integer strand = 0; // 串
//                Integer piece = 0; // 块
//                Integer wireNum = 0; // 接线数量
//                for (SysInverterVO sysInverterVO : sysInverterVOList) {
//                    wireNum = 0;
//                    strand = 0;
//                    piece = 0;
//                    for (SysInverterWireVO sysInverterWireVO : sysInverterVO.getSysInverterWireVOS()) {
//                        wireNum += sysInverterWireVO.getWireNum();
//                        for (SysInverterWireMpptVO sysInverterWireMpptVO : sysInverterWireVO.getSysInverterWireMpptVOS()) {
//                            strand += Integer.parseInt(sysInverterWireMpptVO.getStrand());
//                            piece += sysInverterWireMpptVO.getPiece();
//                        }
//                    }
//                    sysInverterVO.setStrand(String.valueOf(strand));
//                    sysInverterVO.setPiece(piece);
//                    sysInverterVO.setWireNum(wireNum);
//                }

//                pv.setSysInverterVOS(sysInverterVOList);
//            }

            ProjectExportVO projectExportVO = new ProjectExportVO();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }

            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVO> data = this.getData(projectExportVOS);
        return data;
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOBeij> getListBeiJ(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper);
            ArrayList<ProjectExportVOBeij> projectExportVOS = new ArrayList<>();
            projectList.forEach(pv -> {
                ProjectExportVOBeij projectExportVO = new ProjectExportVOBeij();
                BeanUtils.copyProperties(pv, projectExportVO);
                //创建时间
                Date createTime = pv.getCreateTime();
                if (createTime != null) {
                    projectExportVO.setCreateTime(createTime);
                }
                //更新时间
                Date updateTime = pv.getUpdateTime();
                if (updateTime != null) {
                    projectExportVO.setUpdateTime(updateTime);
                }
                String projectType = pv.getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(projectType)) {
                    pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    projectExportVO.setProjectType(chineseDescription);
                } else {
                    projectExportVO.setProjectType("");
                }
                String houseTypeGroup = pv.getHouseTypeGroup();
                List<HouseTypeVo> houseTypeVos = JSONObject.parseArray(houseTypeGroup, HouseTypeVo.class);

                if (houseTypeVos != null) {
                    StringBuffer houseTypes = new StringBuffer();
                    for (int i = 0; i < houseTypeVos.size(); i++) {
                        String houseType = houseTypeVos.get(i).getHouseType();
                        Integer num = houseTypeVos.get(i).getNum();
                        BigDecimal length = houseTypeVos.get(i).getLength();
                        BigDecimal width = houseTypeVos.get(i).getWidth();
                        houseTypes.append("房屋类型：" + houseType + ",").append("房屋数量：" + num + ",").append("尺寸（长）：" + length + "米，").append("尺寸（宽）：" + width + "米，").append("；");

                    }
                    projectExportVO.setHouseTypes(houseTypes.toString());
                }


                projectExportVOS.add(projectExportVO);
            });
            List<ProjectExportVOBeij> data = getDataBeiJ(projectExportVOS);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
//            return projectExportVOS;
//            List<ProjectExportVOBeij> data = this.getDataBeiJ(projectExportVOS);
            return data;
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String id = loginUser.getId();
        String roleId = iRegionsServiceOne.getRoleId(id);


//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, request.getParameterMap());
        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        String addressId = pvProjectInfo.getAddressId();
        if (StringUtils.isNotBlank(addressId)) {
            queryWrapper.like("area_address", "," + addressId);
        }
        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
        LocalDateTime endDate = pvProjectInfo.getEndDate();
        if (beginDate != null && endDate != null) {
            queryWrapper.ge("create_time", beginDate);
            queryWrapper.le("create_time", endDate);

        }
        List<Integer> statusIds = pvProjectInfo.getStatusIds();
        if (statusIds != null && !statusIds.isEmpty()) {
            queryWrapper.in("project_status", statusIds);
        }
        if (renterId != null) {
            queryWrapper.eq("tenant_id", renterId);
        }
        List<PvProjectInfo> projectList = this.list(queryWrapper);
        ArrayList<ProjectExportVOBeij> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            ProjectExportVOBeij projectExportVO = new ProjectExportVOBeij();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }
            projectExportVOS.add(projectExportVO);
        });
//        List<ProjectExportVOBeij> data = this.getDataBeiJ(projectExportVOS);
        List<ProjectExportVOBeij> data = getDataBeiJ(projectExportVOS);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
        return data;
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOHaiN> getListHaiN(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper);
            ArrayList<ProjectExportVOHaiN> projectExportVOS = new ArrayList<>();
            projectList.forEach(pv -> {
                ProjectExportVOHaiN projectExportVO = new ProjectExportVOHaiN();
                BeanUtils.copyProperties(pv, projectExportVO);
                //创建时间
                Date createTime = pv.getCreateTime();
                if (createTime != null) {
                    projectExportVO.setCreateTime(createTime);
                }
                //更新时间
                Date updateTime = pv.getUpdateTime();
                if (updateTime != null) {
                    projectExportVO.setUpdateTime(updateTime);
                }
                String projectType = pv.getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(projectType)) {
                    pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    projectExportVO.setProjectType(chineseDescription);
                } else {
                    projectExportVO.setProjectType("");
                }
                projectExportVOS.add(projectExportVO);
            });
            List<ProjectExportVOHaiN> data = this.getDataHaiN(projectExportVOS);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
            return data;
        }


//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, request.getParameterMap());
        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        String addressId = pvProjectInfo.getAddressId();
        if (StringUtils.isNotBlank(addressId)) {
            queryWrapper.like("area_address", "," + addressId);
        }
        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
        LocalDateTime endDate = pvProjectInfo.getEndDate();
        if (beginDate != null && endDate != null) {
            queryWrapper.ge("create_time", beginDate);
            queryWrapper.le("create_time", endDate);

        }
        List<Integer> statusIds = pvProjectInfo.getStatusIds();
        if (statusIds != null && !statusIds.isEmpty()) {
            queryWrapper.in("project_status", statusIds);
        }
        if (renterId != null) {
            queryWrapper.eq("tenant_id", renterId);
        }
        List<PvProjectInfo> projectList = this.list(queryWrapper);
        ArrayList<ProjectExportVOHaiN> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            ProjectExportVOHaiN projectExportVO = new ProjectExportVOHaiN();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }
            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVOHaiN> data = this.getDataHaiN(projectExportVOS);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
        return data;
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOHeN> getListHeN(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper);
            ArrayList<ProjectExportVOHeN> projectExportVOS = new ArrayList<>();
            projectList.forEach(pv -> {
                ProjectExportVOHeN projectExportVO = new ProjectExportVOHeN();
                BeanUtils.copyProperties(pv, projectExportVO);
                //创建时间
                Date createTime = pv.getCreateTime();
                if (createTime != null) {
                    projectExportVO.setCreateTime(createTime);
                }
                //更新时间
                Date updateTime = pv.getUpdateTime();
                if (updateTime != null) {
                    projectExportVO.setUpdateTime(updateTime);
                }
                String projectType = pv.getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(projectType)) {
                    pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    projectExportVO.setProjectType(chineseDescription);
                } else {
                    projectExportVO.setProjectType("");
                }
                projectExportVOS.add(projectExportVO);
            });
            List<ProjectExportVOHeN> data = this.getDataHeN(projectExportVOS);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
            return data;
        }


//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, request.getParameterMap());
        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        String addressId = pvProjectInfo.getAddressId();
        if (StringUtils.isNotBlank(addressId)) {
            queryWrapper.like("area_address", "," + addressId);
        }
        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
        LocalDateTime endDate = pvProjectInfo.getEndDate();
        if (beginDate != null && endDate != null) {
            queryWrapper.ge("create_time", beginDate);
            queryWrapper.le("create_time", endDate);

        }
        List<Integer> statusIds = pvProjectInfo.getStatusIds();
        if (statusIds != null && !statusIds.isEmpty()) {
            queryWrapper.in("project_status", statusIds);
        }
        if (renterId != null) {
            queryWrapper.eq("tenant_id", renterId);
        }
        List<PvProjectInfo> projectList = this.list(queryWrapper);
        ArrayList<ProjectExportVOHeN> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            ProjectExportVOHeN projectExportVO = new ProjectExportVOHeN();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }
            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVOHeN> data = this.getDataHeN(projectExportVOS);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
        return data;
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOLF> getListLF(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper);
            ArrayList<ProjectExportVOLF> projectExportVOS = new ArrayList<>();
            projectList.forEach(pv -> {
                ProjectExportVOLF projectExportVO = new ProjectExportVOLF();
                BeanUtils.copyProperties(pv, projectExportVO);
                //创建时间
                Date createTime = pv.getCreateTime();
                if (createTime != null) {
                    projectExportVO.setCreateTime(createTime);
                }
                //更新时间
                Date updateTime = pv.getUpdateTime();
                if (updateTime != null) {
                    projectExportVO.setUpdateTime(updateTime);
                }
                String projectType = pv.getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(projectType)) {
                    pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    projectExportVO.setProjectType(chineseDescription);
                } else {
                    projectExportVO.setProjectType("");
                }
                projectExportVOS.add(projectExportVO);
            });
            List<ProjectExportVOLF> data = this.getDataLF(projectExportVOS);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
            return data;
        }

        Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        String addressId = pvProjectInfo.getAddressId();
        if (StringUtils.isNotBlank(addressId)) {
            queryWrapper.like("area_address", "," + addressId);
        }
        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
        LocalDateTime endDate = pvProjectInfo.getEndDate();
        if (beginDate != null && endDate != null) {
            queryWrapper.ge("create_time", beginDate);
            queryWrapper.le("create_time", endDate);

        }
        List<Integer> statusIds = pvProjectInfo.getStatusIds();
        if (statusIds != null && !statusIds.isEmpty()) {
            queryWrapper.in("project_status", statusIds);
        }
        if (renterId != null) {
            queryWrapper.eq("tenant_id", renterId);
        }
        List<PvProjectInfo> projectList = this.list(queryWrapper);
        ArrayList<ProjectExportVOLF> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            ProjectExportVOLF projectExportVO = new ProjectExportVOLF();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }
            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVOLF> data = this.getDataLF(projectExportVOS);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
        return data;
    }

    @Override
    public HomePageData getHomePageData(PvProjectInfo pvProjectInfo) {

        //区分一下如果是
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String relTenantIds = loginUser.getRelTenantIds();
        //获取租户id
        String renterId = pvProjectInfo.getRenterId();
        if (StringUtils.isNotBlank(renterId)) {
            pvProjectInfo.setRenterId(renterId);
        }
        //如果 renterId 为空则代表是普通用户登录取他租户id即可
        else {
            pvProjectInfo.setRenterId(relTenantIds);
        }
        // 获取项目id
        String projectInfoRenterId = pvProjectInfo.getRenterId();

        HomePageData homePageData = new HomePageData();
        //累计签约户数
        pvProjectInfo.setBpmNodeId(10);
        Integer count = pvProjectInfoMapper.getHomePageData(pvProjectInfo, 10);
        homePageData.setSignUp(count == null ? 0 : count);
        //累计签约容量
        BigDecimal sum = pvProjectInfoMapper.getHomePageDataLoad(pvProjectInfo, 10);
        if (sum != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setSignLoad(sum.divide(new BigDecimal(1000)));
            }else {
                homePageData.setSignLoad(sum);
            }

        }


        //今日签约户数
        Integer sinUpToday = pvProjectInfoMapper.getHomePageDatatToday(pvProjectInfo, 10);
        homePageData.setSignToday(sinUpToday);
        //今日累计签约量
        BigDecimal signTodayLoad = pvProjectInfoMapper.getHomePageDataLoadToday(pvProjectInfo, 10);
        if (signTodayLoad != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setSignTodayLoad(signTodayLoad.divide(new BigDecimal(1000)));
            }else{
                homePageData.setSignTodayLoad(signTodayLoad);
            }

        }

        //施工进场（销售员）
        Integer work = pvProjectInfoMapper.getHomePageData(pvProjectInfo, 60);
        homePageData.setComplete(work);
        //累计完成容量
        BigDecimal workSum = pvProjectInfoMapper.getHomePageDataLoad(pvProjectInfo, 60);
        if (workSum != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setCompleteLoad(workSum.divide(new BigDecimal(1000)));
            }else {
                homePageData.setCompleteLoad(workSum);
            }

        }


        //今日完成户数
        Integer homePageDatatToday = pvProjectInfoMapper.getHomePageDatatToday(pvProjectInfo, 60);
        homePageData.setCompleteToday(homePageDatatToday);
        //今日累计完成容量
        BigDecimal homePageDataLoadToday = pvProjectInfoMapper.getHomePageDataLoadToday(pvProjectInfo, 60);
        if (homePageDataLoadToday != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setCompleteTodayLoad(homePageDataLoadToday.divide(new BigDecimal(1000)));
            }else {
                homePageData.setCompleteTodayLoad(homePageDataLoadToday);
            }

        }

        // 并网户数
        int onGridBpmNodeId = 0;
        // 申报数
        int powerGridLoad = 0;
        // 申报完成数
        int powerCompleteBpmNodeId = 0;
        // 北京项目
        if ("-2103508991".equals(projectInfoRenterId)) {
            powerCompleteBpmNodeId = 110;
            onGridBpmNodeId = 100;
            powerGridLoad = 40;
        } else {
            // 其他项目
            powerCompleteBpmNodeId = 90;
            onGridBpmNodeId = 80;
            powerGridLoad = 30;
        }

        //累计申报户数
        Integer folder = pvProjectInfoMapper.getHomePageData(pvProjectInfo, powerGridLoad);
        homePageData.setPowerGrid(folder);
        //累计申报容量
        BigDecimal folderData = pvProjectInfoMapper.getHomePageDataLoad(pvProjectInfo, powerGridLoad);
        if (folderData != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setPowerGridLoad(folderData.divide(new BigDecimal(1000)));
            }else{
                homePageData.setPowerGridLoad(folderData);
            }

        }


        //今日累计申报户数
        Integer folderToday = pvProjectInfoMapper.getHomePageDatatToday(pvProjectInfo, powerGridLoad);
        homePageData.setPowerGridRToday(folderToday);
        //今日累计申报容量
        BigDecimal folderDataToday = pvProjectInfoMapper.getHomePageDataLoadToday(pvProjectInfo, powerGridLoad);
        if (folderDataToday != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setPowerGridLoadToday(folderDataToday.divide(new BigDecimal(1000)));
            }else{
                homePageData.setPowerGridLoadToday(folderDataToday);
            }
        }

        //申报完成数
        Integer powerComplete = pvProjectInfoMapper.getHomePageData(pvProjectInfo, powerCompleteBpmNodeId);
        homePageData.setPowerComplete(powerComplete);

        //申报完成容量
        BigDecimal powerCompleteLoad = pvProjectInfoMapper.getHomePageDataLoad(pvProjectInfo, powerCompleteBpmNodeId);
        if (powerCompleteLoad != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setPowerCompleteLoad(powerCompleteLoad.divide(new BigDecimal(1000)));
            }else {
                homePageData.setPowerCompleteLoad(powerCompleteLoad);
            }
        }


        //并网户数
        Integer onGrid = pvProjectInfoMapper.getHomePageData(pvProjectInfo, onGridBpmNodeId);
        homePageData.setOnGrid(onGrid);
        //并网完成容量
        BigDecimal onGridLoad = pvProjectInfoMapper.getHomePageDataLoadFact(pvProjectInfo, onGridBpmNodeId);
        if (onGridLoad != null) {
            if(!"-2103508991".equals(renterId)){
                homePageData.setOnGridLoad(onGridLoad.divide(new BigDecimal(1000)));
            }else {
                homePageData.setOnGridLoad(onGridLoad);
            }

        }


        //今日并网户数
        Integer onGridToday = pvProjectInfoMapper.getHomePageDatatToday(pvProjectInfo, onGridBpmNodeId);
        homePageData.setOnGridToday(onGridToday);

        //今日并网容量
        BigDecimal onGridLoadToday = pvProjectInfoMapper.getHomePageDataLoadTodayFact(pvProjectInfo, onGridBpmNodeId);
        if (onGridLoadToday != null) {
            if(!renterId.equals("-2103508991")){
                homePageData.setOnGridLoadToday(onGridLoadToday.divide(new BigDecimal(1000)));
            }
            homePageData.setOnGridLoadToday(onGridLoadToday);
        }


        // 签约合计完成(容量) - 资料录入及以下
        BigDecimal signTotal = pvProjectInfoMapper.getHomeDataLoad(pvProjectInfo, 10);
        if (signTotal != null) {
            if(!renterId.equals("-2103508991")){
                homePageData.setSignTotal(signTotal.divide(new BigDecimal(1000)));
            }else{
                homePageData.setSignTotal(signTotal);
            }
        }


        //施工进场合计完成（容量）
        BigDecimal workTotal = pvProjectInfoMapper.getHomeDataLoad(pvProjectInfo, 60);
        if (workTotal != null) {
            if(!renterId.equals("-2103508991")){
                homePageData.setWorkTotal(workTotal.divide(new BigDecimal(1000)));
            }else{
                homePageData.setWorkTotal(workTotal);
            }

        }


        //电网申报合计完成（容量）
        BigDecimal powerTotal = pvProjectInfoMapper.getHomeDataLoad(pvProjectInfo, powerGridLoad);
        if (powerTotal != null) {
            if(!renterId.equals("-2103508991")){
                homePageData.setPowerTotal(powerTotal.divide(new BigDecimal(1000)));
            }
            homePageData.setPowerTotal(powerTotal);
        }


        //并网合计完成（容量）
        BigDecimal onGridTotal = pvProjectInfoMapper.getHomeDataLoad(pvProjectInfo, onGridBpmNodeId);
        if (onGridTotal != null) {
            if(!renterId.equals("-2103508991")){
                homePageData.setOnGridTotal(onGridTotal.divide(new BigDecimal(1000)));
            }else{
                homePageData.setOnGridTotal(onGridTotal);
            }

        }


        //获取最近6个月
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        List<ChartVo> chartVos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            calendar.add(Calendar.MONTH, -1);
            int month = calendar.get(Calendar.MONTH) + 2;
            int weekYear = calendar.getWeekYear();
            ChartVo chart = pvProjectInfoMapper.getChart(weekYear + "-" + month);
            chart.setIndex(i);
            chart.setKey(month);
            chartVos.add(chart);
        }
        if (chartVos != null && chartVos.size() > 0) {
            List<ChartVo> collect = chartVos.stream().sorted(Comparator.comparing(ChartVo::getIndex).reversed()).collect(Collectors.toList());
            homePageData.setChartVos(collect);
        }
        return homePageData;

    }

    @Override
    public List<LocationVO> getHomeDataCount(PvProjectInfo pvProjectInfo) {
        List<PvProjectInfo> homeDataCount = pvProjectInfoMapper.getHomeDataCount(pvProjectInfo);
        List<LocationVO> locationVOS = new ArrayList<>();
        if (homeDataCount != null && !homeDataCount.isEmpty()) {
            for (int i = 0; i < homeDataCount.size(); i++) {
                LocationVO locationVO = new LocationVO();
                String address = homeDataCount.get(i).getAreaAddress();
                //经度
                String instLongitude = homeDataCount.get(i).getInstLongitude();
                //纬度
                String instLatitude = homeDataCount.get(i).getInstLatitude();
                if (StringUtils.isNotBlank(address)) {
                    String substring = address.substring(1, address.length() - 1);
                    String[] split = substring.replace("，", ",").split(",");
                    String last = split[split.length - 1]; // 获取最后一个元素
                    Regions model = iRegionsServiceOne.getModel(Integer.valueOf(last));
                    String localName = model.getLocalName();
                    if (StringUtils.isNotBlank(instLongitude) && StringUtils.isNotBlank(instLatitude)) {
                        locationVO.setLongitude(instLongitude);
                        locationVO.setLatitude(instLatitude);
                        locationVO.setLastAddress(localName);
                        locationVOS.add(locationVO);
                    }
                }
            }
            return locationVOS;
        }
        return locationVOS;
    }

    @Override
    public IPage<PvProjectInfo> getLedger(Page page, PvProjectInfo pvProjectInfo) {
        try {
            String isSeller = pvProjectInfo.getIsSeller();
            if (StringUtils.isNotBlank(isSeller)) {
                IPage<PvProjectInfo> ledger = pvProjectInfoMapper.getLedger(page, pvProjectInfo);
                return ledger;
            } else {
                IPage<PvProjectInfo> ledger = pvProjectInfoMapper.getLedgerNoSeller(page, pvProjectInfo);
                return ledger;
            }
        } catch (Exception e) {
            return page;
        }

    }

    @Override
    public IPage<PvProjectInfo> getRecycle(Page<PvProjectInfo> page, PvProjectInfo pvProjectInfo) {
        try {
            String isSeller = pvProjectInfo.getIsSeller();
            if (StringUtils.isNotBlank(isSeller)) {
                IPage<PvProjectInfo> ledger = pvProjectInfoMapper.getRecycle(page, pvProjectInfo);
                return ledger;
            } else {
                IPage<PvProjectInfo> ledger = pvProjectInfoMapper.getRecycleNoSeller(page, pvProjectInfo);
                return ledger;
            }
        } catch (Exception e) {
            return page;
        }

    }

    @Override
    public Result updateAreaAddress(PvProjectInfo pvProjectInfo) {
        Integer id = pvProjectInfo.getId();
        String instLocation = pvProjectInfo.getInstLocation();
        String instLongitude = pvProjectInfo.getInstLongitude();
        String instLatitude = pvProjectInfo.getInstLatitude();
        if (id != null && StringUtils.isNotBlank(instLocation) && StringUtils.isNotBlank(instLongitude) && StringUtils.isNotBlank(instLatitude)) {
            Boolean aBoolean = pvProjectInfoMapper.updateAreaAddress(instLocation, instLongitude, instLatitude, id);
            Result.ok(aBoolean);
        }
        return Result.ok();
    }

    /**
     * 获取租户项目
     *
     * @return
     */
    @Override
    public Result getRenterId() {
        List<SysTenant> renterId = pvProjectInfoMapper.getRenterId();
        if (renterId != null) {
            return Result.ok(renterId);
        }
        return Result.ok();
    }

    /**
     * 回收站恢复数据
     *
     * @param projectIds
     * @return
     */
    @Override
    public boolean updateByProjectId(List<Integer> projectIds) {
        try {
            for (int i = 0; i < projectIds.size(); i++) {
                pvProjectInfoMapper.updateByProjectId(projectIds.get(i), new Date());
                pvProjectBpmMapper.updateBpmByProjectId(projectIds.get(i), new Date());
                pvProjectFileMapper.updateFileByProjectId(projectIds.get(i));
                pvProjectRemarkMapper.updateRemarkByProjectId(projectIds.get(i));
            }
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public PvProjectInfo getByProjectId(Integer projectId) {
        return pvProjectInfoMapper.getByProjectId(projectId);
    }

    @Override
    public Result homePageCount(PvProjectInfo pvProjectInfo) {
        HomePageCountVo homePageCountVo = new HomePageCountVo();
        //户数
        HouseNum houseNum = new HouseNum();
        //容量
        Capacity capacity = new Capacity();
        //累计签约户数和容量
        ProjectDataVo signCountAndLoad = pvProjectInfoMapper.getLoad(pvProjectInfo, 10);
        houseNum.setSignCount(signCountAndLoad.getHouse());
        capacity.setSignLoad(signCountAndLoad.getVolume());
        //本周签约户数和容量
        ProjectDataVo signWeek = pvProjectInfoMapper.getWeek(pvProjectInfo, 10);
        houseNum.setSignWeek(signWeek.getHouse());
        capacity.setSignWeekLoad(signWeek.getVolume());
        //累计施工和容量
        ProjectDataVo buildCount = pvProjectInfoMapper.getLoad(pvProjectInfo, 50);
        houseNum.setBuildCount(buildCount.getHouse());
        capacity.setBuildLoad(buildCount.getVolume());
        //本周施工和容量
        ProjectDataVo buildWeek = pvProjectInfoMapper.getWeek(pvProjectInfo, 50);
        houseNum.setBuildWeek(buildWeek.getHouse());
        capacity.setBuildWeekLoad(buildWeek.getVolume());
        //累计申报和容量
        ProjectDataVo reportCount = pvProjectInfoMapper.getLoad(pvProjectInfo, 30);
        houseNum.setReportCount(reportCount.getHouse());
        capacity.setReportLoad(reportCount.getVolume());
        //本周申报和容量
        ProjectDataVo reportWeek = pvProjectInfoMapper.getWeek(pvProjectInfo, 30);
        houseNum.setReportWeek(reportWeek.getHouse());
        capacity.setReportWeekLoad(reportWeek.getVolume());
        //累计并网和容量
        ProjectDataVo onGridCount = pvProjectInfoMapper.getLoad(pvProjectInfo, 70);
        houseNum.setOnGridCount(onGridCount.getHouse());
        capacity.setOnGridLoad(onGridCount.getVolume());
        //本周并网和容量
        ProjectDataVo onGridWeek = pvProjectInfoMapper.getWeek(pvProjectInfo, 70);
        houseNum.setOnGridWeek(onGridWeek.getHouse());
        capacity.setOnGridWeekLoad(onGridWeek.getVolume());
        homePageCountVo.setCapacity(capacity);
        homePageCountVo.setHouseNum(houseNum);
        return Result.ok(homePageCountVo);
    }

    @Override
    public Result homePageCountNew(PvProjectInfo pvProjectInfo) {

        String addressId = pvProjectInfo.getAddressId();
        Integer bpmNodeId = pvProjectInfo.getBpmNodeId();
        String renterId = pvProjectInfo.getRenterId();

        //不同项目来一个默认值。查询出来二级区域下所有的三级区域集合。并且放入数据
        //表示并网 取
        if (StringUtils.isNotBlank(addressId)) {
            List<Integer> regionsIds = new ArrayList<>();
            // 徐州
            if ("118337537".equals(renterId) || "507514882".equals(renterId) || "-624947198".equals(renterId) || "1964826626".equals(renterId)) {
                regionsIds = iRegionsServiceOne.getRegionsIdsXZ(addressId);
            } else {
                // 天津北京廊坊
                regionsIds = iRegionsServiceOne.getRegionsIds(addressId);
            }
            List<CapacityNum> capacityNumList = new ArrayList<>();
            if (bpmNodeId != null && bpmNodeId == 70) {
                if (regionsIds.size() > 0 && !regionsIds.isEmpty()) {
                    for (int i = 0; i < regionsIds.size(); i++) {
                        CapacityNum capacityNum = new CapacityNum();
                        //把镇作为kye
                        String localName = iRegionsServiceOne.getModel(regionsIds.get(i)).getLocalName();
                        //查询当前这个镇下所有的本周签约户数，累计签约户数，本周签约容量，累计完成容量
                        //累计签约户数和容量
                        pvProjectInfo.setAddressId(regionsIds.get(i).toString());

                        ProjectDataVo signCountAndLoad = pvProjectInfoMapper.getLoadFact(pvProjectInfo, pvProjectInfo.getBpmNodeId());
                        ProjectDataVo signWeek = pvProjectInfoMapper.getWeekFact(pvProjectInfo, pvProjectInfo.getBpmNodeId());
                        //累计户数和本周户数
                        Integer house = signCountAndLoad.getHouse();
                        Integer weekHouse = signWeek.getHouse();
                        //累计签约容量和本周签约容量
                        BigDecimal volume = signCountAndLoad.getVolume();
                        BigDecimal weekVolume = signWeek.getVolume();

                        capacityNum.setTown(localName);
                        capacityNum.setHouseNum(house);
                        capacityNum.setWeekHouseNum(weekHouse);
                        capacityNum.setCapacity(volume);
                        capacityNum.setWeekCapacity(weekVolume);
                        capacityNumList.add(capacityNum);
                    }
                    return Result.ok(capacityNumList);
                }
            } else {
                if (regionsIds.size() > 0 && !regionsIds.isEmpty()) {
                    for (int i = 0; i < regionsIds.size(); i++) {
                        CapacityNum capacityNum = new CapacityNum();
                        //把镇作为kye
                        String localName = iRegionsServiceOne.getModel(regionsIds.get(i)).getLocalName();
                        //查询当前这个镇下所有的本周签约户数，累计签约户数，本周签约容量，累计完成容量
                        //累计签约户数和容量
                        pvProjectInfo.setAddressId(regionsIds.get(i).toString());

                        ProjectDataVo signCountAndLoad = pvProjectInfoMapper.getLoad(pvProjectInfo, pvProjectInfo.getBpmNodeId());
                        ProjectDataVo signWeek = pvProjectInfoMapper.getWeek(pvProjectInfo, pvProjectInfo.getBpmNodeId());
                        //累计户数和本周户数
                        Integer house = signCountAndLoad.getHouse();
                        Integer weekHouse = signWeek.getHouse();
                        //累计签约容量和本周签约容量
                        BigDecimal volume = signCountAndLoad.getVolume();
                        BigDecimal weekVolume = signWeek.getVolume();

                        capacityNum.setTown(localName);
                        capacityNum.setHouseNum(house);
                        capacityNum.setWeekHouseNum(weekHouse);
                        capacityNum.setCapacity(volume);
                        capacityNum.setWeekCapacity(weekVolume);
                        capacityNumList.add(capacityNum);
                    }
                    return Result.ok(capacityNumList);
                }
            }


        }
        return Result.ok();
    }

    @Override
    public void updateIsUpload(String isUpload, Integer projectId) {
        pvProjectInfoMapper.updateIsUpload(isUpload, projectId);
    }

    @Override
    public void updateBatchMain(List<Integer> ids, String typeId) {
        if (ids != null && ids.size() > 0) {
            pvProjectInfoMapper.updateBatchMain(ids, typeId);
        }
    }

    // 添加项目类型
    @Override
    public Result<String> addProjectType(Integer tenantId, String typeValue) {
        if (!StringUtils.isNotBlank(typeValue)) {
            return Result.error("项目类型不能为空! ");
        }
        LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTypeValue, typeValue);
        pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTenantId, tenantId);
        pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getDelFlag, 0);
        Long count = pvProjectTypeMapper.selectCount(pvProjectTypeLambdaQueryWrapper);
        if (count >= 1) {
            return Result.error("该项目类型已存在,请勿重复添加! ");
        }
        Long size = pvProjectTypeMapper.selectCount(new LambdaQueryWrapper<PvProjectType>().eq(PvProjectType::getDelFlag, 0));
        PvProjectType pvProjectType = new PvProjectType();
        pvProjectType.setTenantId(tenantId);
        pvProjectType.setTypeValue(typeValue);
        pvProjectType.setCreateTime(new Date());
        pvProjectType.setUpdateTime(new Date());
        pvProjectTypeMapper.insert(pvProjectType);
        return Result.ok("添加成功! ");
    }

    // 修改项目类型
    @Override
    public Result<String> updateProjectType(ProjectTypeVo projectTypeVo) {
        if (!StringUtils.isNotBlank(projectTypeVo.getTypeValue())) {
            return Result.error("项目类型不能为空! ");
        }
        LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        pvProjectTypeLambdaQueryWrapper1.eq(PvProjectType::getId, projectTypeVo.getId());
        pvProjectTypeLambdaQueryWrapper1.eq(PvProjectType::getTenantId, projectTypeVo.getTenantId());
        pvProjectTypeLambdaQueryWrapper1.eq(PvProjectType::getDelFlag, 0);
        PvProjectType pvProjectType = pvProjectTypeMapper.selectOne(pvProjectTypeLambdaQueryWrapper1);
        if (BeanUtil.isEmpty(pvProjectType)) {
            return Result.error("该项目类型不存在! ");
        }
        if (!pvProjectType.getTypeValue().equals(projectTypeVo.getTypeValue())) {
            LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTypeValue, projectTypeVo.getTypeValue());
            pvProjectTypeLambdaQueryWrapper1.eq(PvProjectType::getTenantId, projectTypeVo.getTenantId());
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getDelFlag, 0);
            Long countByValue = pvProjectTypeMapper.selectCount(pvProjectTypeLambdaQueryWrapper);
            if (countByValue == 1) {
                return Result.error("该项目类型已经存在! ");
            }
        }
        pvProjectTypeMapper.updateProjectType(projectTypeVo.getId(), projectTypeVo.getTypeValue(), projectTypeVo.getTenantId());
        return Result.ok("修改成功! ");
    }

    // 项目类型分页列表
    @Override
    public IPage<PvProjectType> pagePt(Page<PvProjectType> page, Integer tenantId) {
        LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pvProjectTypeLambdaQueryWrapper.orderByAsc(PvProjectType::getId);
        //天津
        if ("102".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTjType, 1);
        }
        //北京
        if ("-2103508991".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getBjType, 1);
        }
        //徐州
        if ("118337537".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getXzhType, 1);
        }
        //廊坊
        if ("-1759715327".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getLfType, 1);
        }
        //海南ApiTaskListController
        if ("-624947198".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTjType, 1);
        }
        //海南二期
        if ("1964826626".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getHain2Type, 1);
        }
        //河南
        if ("507514882".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getHenType, 1);
        }
        return pvProjectTypeMapper.selectPage(page, pvProjectTypeLambdaQueryWrapper);
    }

    // 禁用项目类型
    @Override
    public Result<String> delProjectType(List<String> ids, Integer tenantId) {
        List<Integer> collect = ids.stream().map(id -> {
            return Integer.parseInt(id);
        }).collect(Collectors.toList());
//        for (Integer id : collect) {
//            PvProjectInfo pvProjectInfo = pvProjectInfoMapper.selectById(id);
//            if (BeanUtil.isNotEmpty(pvProjectInfo)) {
//                return Result.error("该项目类型下存在数据,不能被禁用! ");
//            }
//        }
        pvProjectTypeMapper.delProjectType(collect, tenantId);
        return Result.ok("禁用成功! ");
    }

    // 根据类型id获取单个项目类型信息
    @Override
    public PvProjectType selectType(Integer typeId, Integer tenantId) {
        LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getId, typeId);
        //天津
        if ("102".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTjType, 1);
        }
        //北京
        if ("-2103508991".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getBjType, 1);
        }
        //徐州
        if ("118337537".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getXzhType, 1);
        }
        //廊坊
        if ("-1759715327".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getLfType, 1);
        }
        //海南ApiTaskListController
        if ("-624947198".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTjType, 1);
        }
        //海南二期
        if ("1964826626".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getHain2Type, 1);
        }
        //河南
        if ("507514882".equals(String.valueOf(tenantId))) {
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getHenType, 1);
        }
        return pvProjectTypeMapper.selectOne(pvProjectTypeLambdaQueryWrapper);
    }

    // 启用项目类型
    @Override
    public Result<String> enableProjectType(List<String> ids, Integer tenantId) {
        List<Integer> collect = ids.stream().map(id -> {
            return Integer.parseInt(id);
        }).collect(Collectors.toList());
        pvProjectTypeMapper.enableProjectType(collect, tenantId);
        return Result.ok("启用成功! ");
    }

    @Override
    public List<PvProjectType> selectProjectType(Integer tenantId) {
        //天津
        if ("102".equals(String.valueOf(tenantId))) {
            LambdaQueryWrapper<PvProjectType> pvProjectTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            pvProjectTypeLambdaQueryWrapper.eq(PvProjectType::getTjType, 1);
            return pvProjectTypeMapper.selectList(pvProjectTypeLambdaQueryWrapper);
        }
        //北京
        if ("-2103508991".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        }
        //徐州
        if ("118337537".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        }
        //廊坊
        if ("-1759715327".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        }
        //海南ApiTaskListController
        if ("-624947198".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        }
        //海南二期
        if ("1964826626".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        }
        //河南
        if ("507514882".equals(String.valueOf(tenantId))) {
            List<PvProjectType> pvProjectTypeList = pvProjectTypeMapper.selectList(null);
            List<PvProjectType> collect = pvProjectTypeList.stream().map(pvProjectType -> {
                pvProjectType.setDelFlag(0);
                return pvProjectType;
            }).collect(Collectors.toList());
            return collect;
        } else {
            return null;
        }
    }

    @Override
    public List<PvProjectInfo> selectProjectInfoList() {
        return pvProjectInfoMapper.selectProjectInfoList();
    }

    @Override
    public List<PvProjectInfo> getByRenterId(String renterId) {
        return pvProjectInfoMapper.getByRenterId(Integer.valueOf(renterId));
    }

    @Override
    public void addLogToOtherFile(PvProjectFileOther pvProjectFileOther, LoginUser loginUser, String type) { // type：0：上传 1：删除
        String fileType = pvProjectFileOther.getFileType(); // 文件类型
        String fileName = pvProjectFileOther.getFileName(); // 文件名
        // 操作名
        String operateName = "上传";
        if ("1".equals(type)) {
            operateName = "删除";
        }
        // 创建操作日志
        PvOperateLog pvOperateLog = new PvOperateLog();
        pvOperateLog.setProjectId(pvProjectFileOther.getProjectId());
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
        String fileTypeName = "";
        if ("0".equals(fileType)) {
            fileTypeName = "租赁合同";
        } else if ("1".equals(fileType)) {
            fileTypeName = "购售电合同";
        } else if ("2".equals(fileType)) {
            fileTypeName = "农户省份证";
        } else if ("3".equals(fileType)) {
            fileTypeName = "用电权属证明";
        } else if ("4".equals(fileType)) {
            fileTypeName = "蓝图";
        } else if ("5".equals(fileType)) {
            fileTypeName = "荷载与地勘报告";
        } else if ("6".equals(fileType)) {
            fileTypeName = "三级验收单";
        } else if ("7".equals(fileType)) {
            fileTypeName = "可研报告（村委会）";
        } else if ("8".equals(fileType)) {
            fileTypeName = "组织机构代码（村委会）";
        } else if ("9".equals(fileType)) {
            fileTypeName = "法人公户信息（村委会）";
        }
        pvOperateLog.setOperDescription(operateName + " " + fileTypeName + " " + fileName);
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        pvOperateLogMapper.insert(pvOperateLog);
    }

    @Override
    public void updateCustomerName(String decrypt, Integer projectInfoId) {
        pvProjectInfoMapper.updateCustomerName(decrypt,projectInfoId);
    }

    @Override
    public void updateByIdNew(PvProjectInfo pvProjectInfo) {
        pvProjectInfoMapper.updateByIdNew(pvProjectInfo);
    }

    @Override
    @Async("exportTaskExecutor")
    public List<ProjectExportVOHaiN2> getListHaiN2(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo) {
        List<Integer> ids = pvProjectInfo.getSelections();
        String renterId = pvProjectInfo.getRenterId();
        if (ids != null && !ids.isEmpty()) {
            Map<String, String[]> setParamGroupHashMap = new HashMap<>();
//            QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, setParamGroupHashMap);
            QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", ids);
            queryWrapper.orderByDesc("create_time");
            if (StringUtils.isNotBlank(renterId)) {
                queryWrapper.eq("tenant_id", renterId);
            }
            List<PvProjectInfo> projectList = this.list(queryWrapper);
            ArrayList<ProjectExportVOHaiN2> projectExportVOS = new ArrayList<>();
            projectList.forEach(pv -> {
                ProjectExportVOHaiN2 projectExportVO = new ProjectExportVOHaiN2();
                BeanUtils.copyProperties(pv, projectExportVO);
                //创建时间
                Date createTime = pv.getCreateTime();
                if (createTime != null) {
                    projectExportVO.setCreateTime(createTime);
                }
                //更新时间
                Date updateTime = pv.getUpdateTime();
                if (updateTime != null) {
                    projectExportVO.setUpdateTime(updateTime);
                }
                String projectType = pv.getProjectType();
                PvProjectType pvProjectType;
                if (StringUtils.isNotBlank(projectType)) {
                    pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                    String chineseDescription = pvProjectType.getTypeValue();
                    projectExportVO.setProjectType(chineseDescription);
                } else {
                    projectExportVO.setProjectType("");
                }
                projectExportVOS.add(projectExportVO);
            });
            List<ProjectExportVOHaiN2> data = this.getDataHaiN2(projectExportVOS);
            redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
            return data;
        }


//        QueryWrapper<PvProjectInfo> queryWrapper = QueryGenerator.initQueryWrapper(pvProjectInfo, request.getParameterMap());
        QueryWrapper<PvProjectInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        String addressId = pvProjectInfo.getAddressId();
        if (StringUtils.isNotBlank(addressId)) {
            queryWrapper.like("area_address", "," + addressId);
        }
        LocalDateTime beginDate = pvProjectInfo.getBeginDate();
        LocalDateTime endDate = pvProjectInfo.getEndDate();
        if (beginDate != null && endDate != null) {
            queryWrapper.ge("create_time", beginDate);
            queryWrapper.le("create_time", endDate);

        }
        List<Integer> statusIds = pvProjectInfo.getStatusIds();
        if (statusIds != null && !statusIds.isEmpty()) {
            queryWrapper.in("project_status", statusIds);
        }
        if (renterId != null) {
            queryWrapper.eq("tenant_id", renterId);
        }
        List<PvProjectInfo> projectList = this.list(queryWrapper);
        ArrayList<ProjectExportVOHaiN2> projectExportVOS = new ArrayList<>();
        projectList.forEach(pv -> {
            ProjectExportVOHaiN2 projectExportVO = new ProjectExportVOHaiN2();
            BeanUtils.copyProperties(pv, projectExportVO);
            //创建时间
            Date createTime = pv.getCreateTime();
            if (createTime != null) {
                projectExportVO.setCreateTime(createTime);
            }
            //更新时间
            Date updateTime = pv.getUpdateTime();
            if (updateTime != null) {
                projectExportVO.setUpdateTime(updateTime);
            }
            String projectType = pv.getProjectType();
            PvProjectType pvProjectType;
            if (StringUtils.isNotBlank(projectType)) {
                pvProjectType = this.selectType(Integer.valueOf(projectType), Integer.valueOf(renterId));
                String chineseDescription = pvProjectType.getTypeValue();
                projectExportVO.setProjectType(chineseDescription);
            } else {
                projectExportVO.setProjectType("");
            }
            projectExportVOS.add(projectExportVO);
        });
        List<ProjectExportVOHaiN2> data = this.getDataHaiN2(projectExportVOS);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, data.toArray());
        return data;
    }

    private List<ProjectExportVOHaiN2> getDataHaiN2(ArrayList<ProjectExportVOHaiN2> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 20);
                objectQueryWrapper.eq("param_itme_id", 78);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 21);
                objectQueryWrapper1.eq("param_itme_id", 89);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }
                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 22);
                objectQueryWrapper2.eq("param_itme_id", 91);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

//                //供电所备注
//                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
//                objectQueryWrapper3.eq("project_id",id);
//                objectQueryWrapper3.eq("param_group_id",15);
//                objectQueryWrapper3.eq("param_itme_id",75);
//                objectQueryWrapper3.orderByDesc("id").last("limit 1");
//                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
//                if(pvProjectRemark3 != null){
//                    String powerNotes = pvProjectRemark3.getRemark();
//                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
//                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 13);
                objectQueryWrapper4.eq("param_itme_id", 59);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 6);
                objectQueryWrapper5.eq("param_itme_id", 71);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 27);
                objectQueryWrapper6.eq("param_itme_id", 104);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 25);
                objectQueryWrapper7.eq("param_itme_id", 102);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    //封装数据
    private List<ProjectExportVO> getData(List<ProjectExportVO> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

//              安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 1);
                objectQueryWrapper.eq("param_itme_id", 21);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 2);
                objectQueryWrapper1.eq("param_itme_id", 70);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 3);
                objectQueryWrapper2.eq("param_itme_id", 25);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

                //供电所备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
                objectQueryWrapper3.eq("project_id", id);
                objectQueryWrapper3.eq("param_group_id", 15);
                objectQueryWrapper3.eq("param_itme_id", 75);
                objectQueryWrapper3.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
                if (pvProjectRemark3 != null) {
                    String powerNotes = pvProjectRemark3.getRemark();
                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 13);
                objectQueryWrapper4.eq("param_itme_id", 59);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 6);
                objectQueryWrapper5.eq("param_itme_id", 71);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 14);
                objectQueryWrapper6.eq("param_itme_id", 72);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 6);
                objectQueryWrapper7.eq("param_itme_id", 71);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    /*
    徐州
     */
    private List<ProjectExportVOXuZ> getDataXuZ(List<ProjectExportVOXuZ> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 91);
                objectQueryWrapper.eq("param_itme_id", 331);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 92);
                objectQueryWrapper1.eq("param_itme_id", 342);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 93);
                objectQueryWrapper2.eq("param_itme_id", 344);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

                //供电所备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
                objectQueryWrapper3.eq("project_id", id);
                objectQueryWrapper3.eq("param_group_id", 99);
                objectQueryWrapper3.eq("param_itme_id", 359);
                objectQueryWrapper3.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
                if (pvProjectRemark3 != null) {
                    String powerNotes = pvProjectRemark3.getRemark();
                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 97);
                objectQueryWrapper4.eq("param_itme_id", 356);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 95);
                objectQueryWrapper5.eq("param_itme_id", 352);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 98);
                objectQueryWrapper6.eq("param_itme_id", 357);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 96);
                objectQueryWrapper7.eq("param_itme_id", 355);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    private List<ProjectExportVOBeij> getDataBeiJ(List<ProjectExportVOBeij> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                DictProjectBeiJStatusEnum dictProjectBeiJStatusEnum = DictProjectBeiJStatusEnum.get(status);
                String name = "";
                if (dictProjectBeiJStatusEnum != null) {
                    name = dictProjectBeiJStatusEnum.getName();
                }
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 55);
                objectQueryWrapper.eq("param_itme_id", 198);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 56);
                objectQueryWrapper1.eq("param_itme_id", 209);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 60);
                objectQueryWrapper2.eq("param_itme_id", 211);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

                //供电所备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
                objectQueryWrapper3.eq("project_id", id);
                objectQueryWrapper3.eq("param_group_id", 67);
                objectQueryWrapper3.eq("param_itme_id", 236);
                objectQueryWrapper3.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
                if (pvProjectRemark3 != null) {
                    String powerNotes = pvProjectRemark3.getRemark();
                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 65);
                objectQueryWrapper4.eq("param_itme_id", 233);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 63);
                objectQueryWrapper5.eq("param_itme_id", 229);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 66);
                objectQueryWrapper6.eq("param_itme_id", 234);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 64);
                objectQueryWrapper7.eq("param_itme_id", 232);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    private List<ProjectExportVOHaiN> getDataHaiN(List<ProjectExportVOHaiN> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 20);
                objectQueryWrapper.eq("param_itme_id", 78);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 21);
                objectQueryWrapper1.eq("param_itme_id", 89);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 22);
                objectQueryWrapper2.eq("param_itme_id", 91);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

//                //供电所备注
//                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
//                objectQueryWrapper3.eq("project_id",id);
//                objectQueryWrapper3.eq("param_group_id",15);
//                objectQueryWrapper3.eq("param_itme_id",75);
//                objectQueryWrapper3.orderByDesc("id").last("limit 1");
//                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
//                if(pvProjectRemark3 != null){
//                    String powerNotes = pvProjectRemark3.getRemark();
//                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
//                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 13);
                objectQueryWrapper4.eq("param_itme_id", 59);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 6);
                objectQueryWrapper5.eq("param_itme_id", 71);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 27);
                objectQueryWrapper6.eq("param_itme_id", 104);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 25);
                objectQueryWrapper7.eq("param_itme_id", 102);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    private List<ProjectExportVOHeN> getDataHeN(List<ProjectExportVOHeN> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 29);
                objectQueryWrapper.eq("param_itme_id", 111);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 122);
                objectQueryWrapper1.eq("param_itme_id", 30);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 31);
                objectQueryWrapper2.eq("param_itme_id", 124);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

                //供电所备注
//                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
//                objectQueryWrapper3.eq("project_id",id);
//                objectQueryWrapper3.eq("param_group_id",15);
//                objectQueryWrapper3.eq("param_itme_id",75);
//                objectQueryWrapper3.orderByDesc("id").last("limit 1");
//                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
//                if(pvProjectRemark3 != null){
//                    String powerNotes = pvProjectRemark3.getRemark();
//                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
//                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 136);
                objectQueryWrapper4.eq("param_itme_id", 35);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 33);
                objectQueryWrapper5.eq("param_itme_id", 132);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 137);
                objectQueryWrapper6.eq("param_itme_id", 36);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 135);
                objectQueryWrapper7.eq("param_itme_id", 34);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    private List<ProjectExportVOLF> getDataLF(List<ProjectExportVOLF> list) {
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                Integer id = list.get(i).getId();
                //项目状态
                String status = list.get(i).getProjectStatus();
                String name = ProjectEnum.get(status).getName();
                list.get(i).setStatus(name);

                //安装地址
                String addressIds = list.get(i).getAreaAddress();
                if (StringUtils.isNotBlank(addressIds)) {
                    String fullAddress = list.get(i).getFullAddress();
                    String constrTeam = list.get(i).getConstrTeam();
                    if (StringUtils.isNotBlank(constrTeam)) {
                        String nameByCode = iRegionsServiceOne.getNameByCode(constrTeam);
                        if (StringUtils.isNotBlank(nameByCode)) {
                            list.get(i).setContrTeamName(nameByCode);
                        }
                    }
                    addressIds = addressIds.substring(1, addressIds.length());
                    String[] regionPathArray = addressIds.split(",");
                    //给地区赋值
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String path : regionPathArray) {
                        Regions region = iRegionsServiceOne.getModel(Integer.valueOf(path));
                        if (region == null) {
                            throw new IllegalArgumentException("地区不合法");
                        }
                        String localName = region.getLocalName();
                        stringBuilder.append(localName);
                    }
                    list.get(i).setAddress(stringBuilder + fullAddress);

                }

                //客户信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq("project_id", id);
                objectQueryWrapper.eq("param_group_id", 29);
                objectQueryWrapper.eq("param_itme_id", 111);
                objectQueryWrapper.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark = pvProjectRemarkMapper.selectOne(objectQueryWrapper);
                if (pvProjectRemark != null) {
                    String customerRemark = pvProjectRemark.getRemark();
                    list.get(i).setCustomerNotes(StringUtils.isNotBlank(customerRemark) ? customerRemark : "");
                }

                //房屋信息备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper1 = new QueryWrapper<>();
                objectQueryWrapper1.eq("project_id", id);
                objectQueryWrapper1.eq("param_group_id", 122);
                objectQueryWrapper1.eq("param_itme_id", 30);
                objectQueryWrapper1.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark1 = pvProjectRemarkMapper.selectOne(objectQueryWrapper1);
                if (pvProjectRemark1 != null) {
                    String houseNotes = pvProjectRemark1.getRemark();
                    list.get(i).setHouseNotes(StringUtils.isNotBlank(houseNotes) ? houseNotes : "");
                }

                //安装备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper2 = new QueryWrapper<>();
                objectQueryWrapper2.eq("project_id", id);
                objectQueryWrapper2.eq("param_group_id", 31);
                objectQueryWrapper2.eq("param_itme_id", 124);
                objectQueryWrapper2.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark2 = pvProjectRemarkMapper.selectOne(objectQueryWrapper2);
                if (pvProjectRemark2 != null) {
                    String installNotes = pvProjectRemark2.getRemark();
                    list.get(i).setInstallNotes(StringUtils.isNotBlank(installNotes) ? installNotes : "");
                }

                //资料员审核结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper = new QueryWrapper<>();
                projectBpmQueryWrapper.eq("project_id", id);
                projectBpmQueryWrapper.eq("bpm_node_id", 20);
                projectBpmQueryWrapper.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper);
                if (pvProjectBpm != null) {
                    String approvalResult = pvProjectBpm.getApprovalResult();
                    String approvalRemark = pvProjectBpm.getApprovalRemark();
                    if (approvalResult != null && approvalRemark != null) {
                        String name1 = DictApprovalResultEnum.get(approvalResult).getName();
                        list.get(i).setDocumenterEnd(StringUtils.isNotBlank(name1) ? name1 : "");
                        //审核意见
                        list.get(i).setDocumenterComments(StringUtils.isNotBlank(approvalRemark) ? approvalRemark : "");
                    }
                }

                //供电所备注
//                QueryWrapper<PvProjectRemark> objectQueryWrapper3 = new QueryWrapper<>();
//                objectQueryWrapper3.eq("project_id",id);
//                objectQueryWrapper3.eq("param_group_id",15);
//                objectQueryWrapper3.eq("param_itme_id",75);
//                objectQueryWrapper3.orderByDesc("id").last("limit 1");
//                PvProjectRemark pvProjectRemark3 = pvProjectRemarkMapper.selectOne(objectQueryWrapper3);
//                if(pvProjectRemark3 != null){
//                    String powerNotes = pvProjectRemark3.getRemark();
//                    list.get(i).setPowerNotes(StringUtils.isNotBlank(powerNotes) ? powerNotes : "");
//                }

                //施工管理员备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper4 = new QueryWrapper<>();
                objectQueryWrapper4.eq("project_id", id);
                objectQueryWrapper4.eq("param_group_id", 136);
                objectQueryWrapper4.eq("param_itme_id", 35);
                objectQueryWrapper4.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark4 = pvProjectRemarkMapper.selectOne(objectQueryWrapper4);
                if (pvProjectRemark4 != null) {
                    String contrTeamAdminNotes = pvProjectRemark4.getRemark();
                    list.get(i).setContrTeamAdminNotes(StringUtils.isNotBlank(contrTeamAdminNotes) ? contrTeamAdminNotes : "");
                }

                //施工队备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper5 = new QueryWrapper<>();
                objectQueryWrapper5.eq("project_id", id);
                objectQueryWrapper5.eq("param_group_id", 33);
                objectQueryWrapper5.eq("param_itme_id", 132);
                objectQueryWrapper5.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark5 = pvProjectRemarkMapper.selectOne(objectQueryWrapper5);
                if (pvProjectRemark5 != null) {
                    String contrTeamNotes = pvProjectRemark5.getRemark();
                    list.get(i).setContrTeamNotes(StringUtils.isNotBlank(contrTeamNotes) ? contrTeamNotes : "");
                }

                //施工巡检结果
                QueryWrapper<PvProjectBpm> projectBpmQueryWrapper6 = new QueryWrapper<>();
                projectBpmQueryWrapper6.eq("project_id", id);
                projectBpmQueryWrapper6.eq("bpm_node_id", "60");
                projectBpmQueryWrapper6.orderByDesc("update_time").last("limit 1");
                PvProjectBpm pvProjectBpm2 = pvProjectBpmMapper.selectOne(projectBpmQueryWrapper6);
                if (pvProjectBpm2 != null) {
                    String approvalResult2 = pvProjectBpm2.getApprovalResult();
                    String approvalRemark2 = pvProjectBpm2.getApprovalRemark();
                    if (approvalResult2 != null && approvalRemark2 != null) {
                        String name2 = DictApprovalResultEnum.get(approvalResult2).getName();
                        list.get(i).setContrTeamEnd(StringUtils.isNotBlank(name2) ? name2 : "");
                        //审核意见
                        list.get(i).setInspection(StringUtils.isNotBlank(approvalRemark2) ? approvalRemark2 : "");
                    }

                }

                //并网备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper6 = new QueryWrapper<>();
                objectQueryWrapper6.eq("project_id", id);
                objectQueryWrapper6.eq("param_group_id", 137);
                objectQueryWrapper6.eq("param_itme_id", 36);
                objectQueryWrapper6.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark6 = pvProjectRemarkMapper.selectOne(objectQueryWrapper6);
                if (pvProjectRemark6 != null) {
                    String notes = pvProjectRemark6.getRemark();
                    list.get(i).setOnGridNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }


                //结项备注
                QueryWrapper<PvProjectRemark> objectQueryWrapper7 = new QueryWrapper<>();
                objectQueryWrapper7.eq("project_id", id);
                objectQueryWrapper7.eq("param_group_id", 135);
                objectQueryWrapper7.eq("param_itme_id", 34);
                objectQueryWrapper7.orderByDesc("id").last("limit 1");
                PvProjectRemark pvProjectRemark7 = pvProjectRemarkMapper.selectOne(objectQueryWrapper7);
                if (pvProjectRemark7 != null) {
                    String notes = pvProjectRemark7.getRemark();
                    list.get(i).setEndNotes(StringUtils.isNotBlank(notes) ? notes : "");
                }

            }
            return list;
        }
        return new ArrayList<>();
    }

    private class SetParamGroup {
        private int groupId;
        private int paramId;

        public SetParamGroup(int groupId, int paramId) {
            this.groupId = groupId;
            this.paramId = paramId;
        }

        public int getGroupId() {
            return groupId;
        }

        public void setGroupId(int groupId) {
            this.groupId = groupId;
        }

        public int getParamId() {
            return paramId;
        }

        public void setParamId(int paramId) {
            this.paramId = paramId;
        }
    }
}
