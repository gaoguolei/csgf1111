package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.entity.PvProjectKey;
import org.jeecg.modules.pcms.project.entity.PvProjectRemark;
import org.jeecg.modules.pcms.project.mapper.PvProjectInfoMapper;
import org.jeecg.modules.pcms.project.mapper.PvProjectRemarkMapper;
import org.jeecg.modules.pcms.project.service.IPvProjectRemarkService;
import org.jeecg.modules.pcms.project.service.PvProjectKeyService;
import org.jeecg.modules.pcms.util.AESUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * @Description: pv_project_remark
 * @Author: jun
 * @Date: 2023-03-07
 * @Version: V1.0
 */
@Service
public class PvProjectRemarkServiceImpl extends ServiceImpl<PvProjectRemarkMapper, PvProjectRemark> implements IPvProjectRemarkService {

    @Autowired
    private PvProjectRemarkMapper pvProjectRemarkMapper;

    @Autowired
    private PvProjectKeyService pvProjectKeyService;

    @Autowired
    private PvProjectInfoMapper pvProjectInfoMapper;


    @Override
    public List<PvProjectRemark> selectByMainId(Integer mainId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoMapper.selectById(mainId);
        if (pvProjectInfo != null) {
            Integer tenantId = pvProjectInfo.getTenantId();
            List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkMapper.selectByMainId(mainId);
            if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
                if (pvProjectKey != null) {
                    // 秘钥
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                            Integer groupId = pvProjectRemark.getParamGroupId();
                            Integer paramItmeId = pvProjectRemark.getParamItmeId();
                            // 北京项目
                            if (tenantId != null && tenantId == -2103508991) {
                                if (paramItmeId == 238 && groupId == 57) {
                                    //户主姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 242 && groupId == 58) {
                                    //电卡姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 363 && groupId == 61) {
                                    // 电费收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 367 && groupId == 61) {
                                    // 租金收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 366 && groupId == 61) {
                                    // 租金收益卡持有人
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pvProjectRemarks;
        }
        return null;
    }

    @Override
    public List<PvProjectRemark> selectByMainIdNew(Integer projectInfoId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoMapper.getById(projectInfoId);
        Integer tenantId = pvProjectInfo.getTenantId();
        List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkMapper.selectByMainId(projectInfoId);
        if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
            PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectId(projectInfoId, tenantId);
            if (pvProjectKey != null) {
                // 秘钥
                String passwordKey = pvProjectKey.getPasswordKey();
                if (StringUtils.isNotBlank(passwordKey)) {
                    SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                    for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                        Integer groupId = pvProjectRemark.getParamGroupId();
                        Integer paramItmeId = pvProjectRemark.getParamItmeId();
                        // 北京项目
                        if (tenantId != null && tenantId == -2103508991) {
                            if (paramItmeId == 238 && groupId == 57) {
                                //户主姓名
                                String remark = pvProjectRemark.getRemark();
                                String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                if (StringUtils.isNotBlank(encryptRemark)) {
                                    pvProjectRemark.setRemark(encryptRemark);
                                }
                            }
                            if (paramItmeId == 242 && groupId == 58) {
                                //电卡姓名
                                String remark = pvProjectRemark.getRemark();
                                String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                if (StringUtils.isNotBlank(encryptRemark)) {
                                    pvProjectRemark.setRemark(encryptRemark);
                                }
                            }
                            if (paramItmeId == 363 && groupId == 61) {
                                // 电费收益卡号
                                String remark = pvProjectRemark.getRemark();
                                String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                if (StringUtils.isNotBlank(encryptRemark)) {
                                    pvProjectRemark.setRemark(encryptRemark);
                                }
                            }
                            if (paramItmeId == 367 && groupId == 61) {
                                // 租金收益卡号
                                String remark = pvProjectRemark.getRemark();
                                String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                if (StringUtils.isNotBlank(encryptRemark)) {
                                    pvProjectRemark.setRemark(encryptRemark);
                                }
                            }
                            if (paramItmeId == 366 && groupId == 61) {
                                // 租金收益卡持有人
                                String remark = pvProjectRemark.getRemark();
                                String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                if (StringUtils.isNotBlank(encryptRemark)) {
                                    pvProjectRemark.setRemark(encryptRemark);
                                }
                            }
                        }
                    }
                }
            }
        }
        return pvProjectRemarks;
    }

    @Override
    public void updateByIdNew(PvProjectRemark pvProjectRemark) {
        pvProjectRemarkMapper.updateByIdNew(pvProjectRemark);
    }

    @Override
    public List<PvProjectRemark> selectByMainIdNewNew(Integer mainId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoMapper.selectById(mainId);
        if (pvProjectInfo != null) {
            Integer tenantId = pvProjectInfo.getTenantId();
            List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkMapper.selectByMainId(mainId);
            if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectId(pvProjectInfo.getId(),tenantId);
                if (pvProjectKey != null) {
                    // 秘钥
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                            Integer groupId = pvProjectRemark.getParamGroupId();
                            Integer paramItmeId = pvProjectRemark.getParamItmeId();
                            // 北京项目
                            if (tenantId != null && tenantId == -2103508991) {
                                if (paramItmeId == 238 && groupId == 57) {
                                    //户主姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 242 && groupId == 58) {
                                    //电卡姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 363 && groupId == 61) {
                                    // 电费收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 367 && groupId == 61) {
                                    // 租金收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 366 && groupId == 61) {
                                    // 租金收益卡持有人
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pvProjectRemarks;
        }
        return null;
    }

    @Override
    public List<PvProjectRemark> selectByMainIdNewNewNewNewNew(Integer projectInfoId) {
        return pvProjectRemarkMapper.selectByMainId(projectInfoId);
    }

    @Override
    public List<PvProjectRemark> selectByMainIdNewNewNew(Integer mainId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoMapper.selectById(mainId);
        if (pvProjectInfo != null) {
            Integer tenantId = pvProjectInfo.getTenantId();
            List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkMapper.selectByMainId(mainId);
            if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNew(tenantId);
                if (pvProjectKey != null) {
                    // 秘钥
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                            Integer groupId = pvProjectRemark.getParamGroupId();
                            Integer paramItmeId = pvProjectRemark.getParamItmeId();
                            // 北京项目
                            if (tenantId != null && tenantId == -2103508991) {
                                if (paramItmeId == 238 && groupId == 57) {
                                    //户主姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 242 && groupId == 58) {
                                    //电卡姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 363 && groupId == 61) {
                                    // 电费收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 367 && groupId == 61) {
                                    // 租金收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 366 && groupId == 61) {
                                    // 租金收益卡持有人
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pvProjectRemarks;
        }
        return null;
    }

    @Override
    public List<PvProjectRemark> selectByMainIdNewNewNewNew(Integer mainId) {
        PvProjectInfo pvProjectInfo = pvProjectInfoMapper.selectById(mainId);
        if (pvProjectInfo != null) {
            Integer tenantId = pvProjectInfo.getTenantId();
            List<PvProjectRemark> pvProjectRemarks = pvProjectRemarkMapper.selectByMainId(mainId);
            if (pvProjectRemarks != null && pvProjectRemarks.size() > 0) {
                PvProjectKey pvProjectKey = pvProjectKeyService.getByProjectIdNewNew();
                if (pvProjectKey != null) {
                    // 秘钥
                    String passwordKey = pvProjectKey.getPasswordKey();
                    if (StringUtils.isNotBlank(passwordKey)) {
                        SecretKey secretKey = AESUtils.decodeKeyFromBase64(passwordKey);
                        for (PvProjectRemark pvProjectRemark : pvProjectRemarks) {
                            Integer groupId = pvProjectRemark.getParamGroupId();
                            Integer paramItmeId = pvProjectRemark.getParamItmeId();
                            // 北京项目
                            if (tenantId != null && tenantId == -2103508991) {
                                if (paramItmeId == 238 && groupId == 57) {
                                    //户主姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 242 && groupId == 58) {
                                    //电卡姓名
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 363 && groupId == 61) {
                                    // 电费收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 367 && groupId == 61) {
                                    // 租金收益卡号
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                                if (paramItmeId == 366 && groupId == 61) {
                                    // 租金收益卡持有人
                                    String remark = pvProjectRemark.getRemark();
                                    if (StringUtils.isNotBlank(remark)) {
                                        String encryptRemark = AESUtils.decrypt(remark, secretKey);
                                        if (StringUtils.isNotBlank(encryptRemark)) {
                                            pvProjectRemark.setRemark(encryptRemark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pvProjectRemarks;
        }
        return null;
    }
}
