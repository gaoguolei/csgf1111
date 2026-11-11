package org.jeecg.modules.pcms.bpm;

import org.jeecg.modules.pcms.common.DicProjectBeijBpmEnum;
import org.jeecg.modules.pcms.common.DictProjectBeiJStatusEnum;

/**
 * @Author gongzhou
 * @Date 2023/9/20 上午8:48
 * @Version 1.5
 */
public class BpmProjectBeiJNodeGenerator {
    public static DicProjectBeijBpmEnum getProjectBpmNode(String projectStatus){
        DicProjectBeijBpmEnum projectBpmEnum = null;
        DictProjectBeiJStatusEnum dictProjectStatusEnum = DictProjectBeiJStatusEnum.get(projectStatus);
        switch (dictProjectStatusEnum){
            case DATA_ENTRY:
                projectBpmEnum=DicProjectBeijBpmEnum.DATA_ENTRY;
                break;
            case DATA_APPROVAL:
                projectBpmEnum=DicProjectBeijBpmEnum.DATA_APPROVAL;
                break;
            case DATA_APPROVAL_REJECTED:
                projectBpmEnum=DicProjectBeijBpmEnum.DATA_ENTRY;
                break;
//            case POWER_STATION_APPROVAL:
//                projectBpmEnum=DicProjectBeijBpmEnum.POWER_STATION_APPROVAL;
//                break;
            case ASSIGN_CONSTR_TEAMS:
                projectBpmEnum=DicProjectBeijBpmEnum.ASSIGN_CONSTR_TEAMS;
                break;
            case PROJECT_CONSTR:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_CONSTR;
                break;
            case CONSTR_INSPECTION:
                projectBpmEnum=DicProjectBeijBpmEnum.CONSTR_INSPECTION;
                break;
            case PROJECT_INSPECTION:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_CONCLUSION;
                break;
            case CONSTR_NO:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_CONSTR;
                break;
//            case PROJECT_POWER:
//                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_POWER_CONNECTED;
//                break;
            case PROJECT_CONNECTED:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_CONNECTED;
                break;
            case PROJECT_CONCLUSION:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_OVER;
                break;
            case PROJECT_COMPLETED:
                projectBpmEnum=DicProjectBeijBpmEnum.PROJECT_COMPLETED;
                break;
            default:
                break;
        }
        System.err.println(projectBpmEnum.getCode());
        return projectBpmEnum;

    }
}
