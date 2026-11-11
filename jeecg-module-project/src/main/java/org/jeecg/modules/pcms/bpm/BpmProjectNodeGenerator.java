package org.jeecg.modules.pcms.bpm;

import org.jeecg.modules.pcms.common.DictProjectBpmEnum;
import org.jeecg.modules.pcms.common.DictProjectStatusEnum;

/**
 * 根据项目状态获取当前业务流程环节
 *
 * @author jun
 */
public class BpmProjectNodeGenerator {

    public static DictProjectBpmEnum getProjectBpmNode(String projectStatus){
        DictProjectBpmEnum projectBpmEnum = null;
        DictProjectStatusEnum dictProjectStatusEnum = DictProjectStatusEnum.get(projectStatus);
        switch (dictProjectStatusEnum){
            case DATA_ENTRY:
            case DATA_APPROVAL_REJECTED:
                projectBpmEnum=DictProjectBpmEnum.DATA_ENTRY;
                break;
            case DATA_APPROVAL:
                projectBpmEnum=DictProjectBpmEnum.DATA_APPROVAL;
                break;

            case POWER_STATION_APPROVAL:
                projectBpmEnum=DictProjectBpmEnum.POWER_STATION_APPROVAL;
                break;
            case ASSIGN_CONSTR_TEAMS:
                projectBpmEnum=DictProjectBpmEnum.ASSIGN_CONSTR_TEAMS;
                break;
            case PROJECT_CONSTR:
            case CONSTR_INSPECTION_REJECTED:
                projectBpmEnum=DictProjectBpmEnum.PROJECT_CONSTR;
                break;
            case CONSTR_INSPECTION:
                projectBpmEnum=DictProjectBpmEnum.CONSTR_INSPECTION;
                break;
            case PROJECT_CONNECTED:
                projectBpmEnum=DictProjectBpmEnum.PROJECT_CONNECTED;
                break;
            case PROJECT_CONCLUSION:
                projectBpmEnum=DictProjectBpmEnum.PROJECT_CONCLUSION;
                break;
            case PROJECT_COMPLETED:
                projectBpmEnum=DictProjectBpmEnum.PROJECT_COMPLETED;
                break;
            default:
                break;

        }
        return projectBpmEnum;

    }
}
