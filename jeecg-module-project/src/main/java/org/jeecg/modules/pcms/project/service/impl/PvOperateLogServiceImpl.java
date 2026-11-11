package org.jeecg.modules.pcms.project.service.impl;

import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.common.*;
import org.jeecg.modules.pcms.project.entity.PvOperateLog;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.mapper.PvOperateLogMapper;
import org.jeecg.modules.pcms.project.service.IPvOperateLogService;
import org.jeecg.modules.pcms.project.service.IPvProjectInfoService;
import org.jeecg.modules.pcms.project.vo.PvProjectInfoPage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Date;

/**
 * @Description: pv_operate_log
 * @Author: jun
 * @Date: 2023-03-11
 * @Version: V1.0
 */
@Service
public class PvOperateLogServiceImpl extends ServiceImpl<PvOperateLogMapper, PvOperateLog> implements IPvOperateLogService {

    @Autowired
    IPvProjectInfoService pvProjectInfoService;

    @Override
    public <T> int addOperateLog(T object, DictOperateTypeEnum operateTypeEnum) {
        PvProjectInfo pvProjectInfo = new PvProjectInfo();
        PvProjectInfoPage pvProjectInfoPage = new PvProjectInfoPage();
        if (object instanceof Integer) {
            pvProjectInfo = pvProjectInfoService.getLogicalDeletedById((Integer) object);
        } else if (object instanceof PvProjectInfo) {
            pvProjectInfo = (PvProjectInfo) object;
        } else if (object instanceof PvProjectInfoPage) {
            pvProjectInfoPage = (PvProjectInfoPage) object;
            BeanUtils.copyProperties((PvProjectInfoPage) object, pvProjectInfo);
        } else {
            return 0;
        }

        PvOperateLog pvOperateLog = new PvOperateLog();
        String desc = null;
        switch (operateTypeEnum) {
            case ADD:
                desc = DictProjectStatusEnum.DATA_ENTRY.getName();
                break;
            case EDIT:
                desc = getEditDesc(pvProjectInfoPage);
                break;
            case DELETE:
                desc = "删除";
                break;
            default:
                break;
        }

        pvOperateLog.setProjectId(pvProjectInfo.getId());
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvOperateLog.setOperDescription(desc);
        pvOperateLog.setOperName(loginUser.getUsername());
        pvOperateLog.setDeptName(loginUser.getOrgCode());
        pvOperateLog.setOperTime(new Date());
        baseMapper.insert(pvOperateLog);
        return 0;
    }

    private String getEditDesc(PvProjectInfoPage pvProjectInfoPage) {
        String desc = null;
        DictProjectStatusEnum projectStatus = DictProjectStatusEnum.get(pvProjectInfoPage.getProjectStatus());

        desc = projectStatus.getName();
        if (pvProjectInfoPage.getPvProjectBpmList() == null || pvProjectInfoPage.getPvProjectBpmList().size() == 0) {
            return desc;
        }

        DictYesNoEnum isCommitted = DictYesNoEnum.get(pvProjectInfoPage.getPvProjectBpmList().get(0).getIsCommitted());
        DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(pvProjectInfoPage.getPvProjectBpmList().get(0).getApprovalResult());

        if (projectStatus == DictProjectStatusEnum.DATA_APPROVAL || projectStatus == DictProjectStatusEnum.POWER_STATION_APPROVAL || projectStatus == DictProjectStatusEnum.CONSTR_INSPECTION) {
            switch (approvalResult) {
                case APPROVED:
                    desc += " " + DictApprovalResultEnum.APPROVED.getName();
                    break;
                case REJECTED:
                    desc +=  " " + DictApprovalResultEnum.REJECTED.getName();
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
