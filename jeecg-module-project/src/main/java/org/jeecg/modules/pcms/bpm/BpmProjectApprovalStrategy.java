package org.jeecg.modules.pcms.bpm;

import org.jeecg.modules.pcms.common.*;
import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.service.IPvProjectBpmService;
import org.jeecg.modules.pcms.project.service.IPvProjectInfoService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户项目业务流程
 *
 * @author jun
 */
@Component
public class BpmProjectApprovalStrategy implements BpmApprovalStrategy, InitializingBean {
    private String processCategory = BpmBusinessCategoryEnum.PROJECT.getCode();

    @Autowired
    private IPvProjectInfoService pvProjectInfoService;

    @Autowired
    private IPvProjectBpmService pvProjectBpmService;

    @Autowired
    private BpmProjectStatusGenerator bpmProjectStatusGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approval(PvProjectInfo pvProjectInfo, PvProjectBpm pvProjectBpm) {

        // 未提交不进行流程处理
        if (pvProjectBpm.getIsCommitted() == null || pvProjectBpm.getIsCommitted().equals("") || pvProjectBpm.getIsCommitted().equals(DictYesNoEnum.NO.getCode())) {
            return 0;
        }

        PvProjectInfo projectInfo = pvProjectInfoService.getById(pvProjectBpm.getProjectId());
        Integer tenantId = pvProjectInfo.getTenantId();
        // 北京项目
        if (tenantId == -2103508991) {
            DicProjectBeijBpmEnum current = DicProjectBeijBpmEnum.get(pvProjectBpm.getBpmNodeId());
            DicProjectBeijBpmEnum next = null;

            DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(pvProjectBpm.getApprovalResult());
            switch (approvalResult) {
                case APPROVED:
                    next = current.next();
                    break;
                case REJECTED:
                    next = current.previous();
                    break;
                default:
                    break;
            }
            String nextStatus = bpmProjectStatusGenerator.getBeiJProjectStatus(pvProjectInfo.getProjectStatus(), pvProjectBpm.getApprovalResult());
            pvProjectInfoService.setProjectBeiJStatus(pvProjectBpm.getProjectId(), nextStatus);
            pvProjectBpmService.commitBeiJ(pvProjectBpm.getProjectId(), current);
            pvProjectBpmService.insertProjectBpmBeiJ(pvProjectBpm.getProjectId(), next);
            if (next == next.last()) {
                pvProjectBpmService.commitBeiJ(pvProjectBpm.getProjectId(), next);
            }

        } else {
            DictProjectBpmEnum current = DictProjectBpmEnum.get(pvProjectBpm.getBpmNodeId());
            DictProjectBpmEnum next = null;

            DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(pvProjectBpm.getApprovalResult());
            switch (approvalResult) {
                case APPROVED:
                    next = current.next();
                    // 如果为供电所审核,判断是否为候补施工
                    if (current == DictProjectBpmEnum.POWER_STATION_APPROVAL) {
                        // 选择候补施工 判断项目原来是否候补施工
                        if ("1".equals(pvProjectInfo.getBuildAfter())) {
//                            // 项目原来是后补施工
//                            if ("1".equals(projectInfo.getBuildAfter())) {
//                                next = DictProjectBpmEnum.PROJECT_CONNECTED;
//                            } else if ("0".equals(projectInfo.getBuildAfter()) {
//                                // 项目原来是后补施工
//                            }
                            // 候补施工
                            next = DictProjectBpmEnum.PROJECT_CONNECTED;
                        }
                    }
                    break;
                case REJECTED:
                    next = current.previous();
                    break;
                default:
                    break;
            }
            if ("1".equals(pvProjectInfo.getBuildAfter()) && current == DictProjectBpmEnum.POWER_STATION_APPROVAL) {
                pvProjectInfoService.setProjectStatus(pvProjectBpm.getProjectId(), DictProjectStatusEnum.PROJECT_CONNECTED.getCode());
            } else {
                String nextStatus = bpmProjectStatusGenerator.getProjectStatus(pvProjectInfo.getProjectStatus(), pvProjectBpm.getApprovalResult());
                pvProjectInfoService.setProjectStatus(pvProjectBpm.getProjectId(), nextStatus);
            }
            pvProjectBpmService.commit(pvProjectBpm.getProjectId(), current);
            pvProjectBpmService.insertProjectBpm(pvProjectBpm.getProjectId(), next);
            if (next == next.last()) {
                pvProjectBpmService.commit(pvProjectBpm.getProjectId(), next);
            }
        }
        return 0;
    }

    @Override
    public int processComplete(Integer id) {
        pvProjectInfoService.setProjectStatus(id, DictProjectStatusEnum.PROJECT_COMPLETED.getCode());
        return 0;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        BpmApprovalStrategyFactory.register(processCategory, this);
    }
}
