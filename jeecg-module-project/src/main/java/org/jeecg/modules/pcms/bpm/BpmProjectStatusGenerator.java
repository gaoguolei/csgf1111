package org.jeecg.modules.pcms.bpm;

import org.jeecg.modules.pcms.common.DictApprovalResultEnum;
import org.jeecg.modules.pcms.common.DictProjectBeiJStatusEnum;
import org.jeecg.modules.pcms.common.DictProjectStatusEnum;
import org.springframework.stereotype.Component;

/**
 * 项目状态生成器
 *
 * @author jun
 */
@Component
public class BpmProjectStatusGenerator {

    public String getProjectStatus(String curStatus, String approResult) {
        DictProjectStatusEnum currentStatus = DictProjectStatusEnum.get(curStatus);
        DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(approResult);
        DictProjectStatusEnum nextStatus = null;
        switch (currentStatus) {
            case DATA_APPROVAL:
                if (approvalResult == DictApprovalResultEnum.APPROVED) {
                    nextStatus = DictProjectStatusEnum.POWER_STATION_APPROVAL;
                } else {
                    nextStatus = DictProjectStatusEnum.DATA_APPROVAL_REJECTED;
                }
                break;
            case CONSTR_INSPECTION:
                if (approvalResult == DictApprovalResultEnum.APPROVED) {
                    nextStatus = DictProjectStatusEnum.PROJECT_CONNECTED;
                } else {
                    nextStatus = DictProjectStatusEnum.CONSTR_INSPECTION_REJECTED;
                }
                break;
            case DATA_APPROVAL_REJECTED:
                nextStatus = DictProjectStatusEnum.DATA_APPROVAL;
                break;
            case CONSTR_INSPECTION_REJECTED:
                nextStatus = DictProjectStatusEnum.CONSTR_INSPECTION;
                break;
            default:
                nextStatus = currentStatus.next();
        }
        return nextStatus.getCode();
    }


    public String getBeiJProjectStatus(String curStatus, String approResult) {
        DictProjectBeiJStatusEnum currentStatus = DictProjectBeiJStatusEnum.get(curStatus);
        DictApprovalResultEnum approvalResult = DictApprovalResultEnum.get(approResult);
        DictProjectBeiJStatusEnum nextStatus = null;
        switch (currentStatus) {
            //管理员一审
//            case ADMINWORK_APPROVAL:
//                if (approvalResult == DictApprovalResultEnum.APPROVED) {
//                    nextStatus = DictProjectBeiJStatusEnum.DATA_APPROVAL;
//                } else {
//                    nextStatus = DictProjectBeiJStatusEnum.ADMINWORK_RETURN;
//                }
//                break;

                //资料员审核
            case DATA_APPROVAL:
                if (approvalResult == DictApprovalResultEnum.APPROVED) {
                    nextStatus = DictProjectBeiJStatusEnum.ASSIGN_CONSTR_TEAMS;
                } else {
                    nextStatus = DictProjectBeiJStatusEnum.DATA_APPROVAL_REJECTED;
                }
                break;
//
//            case CONSTR_INSPECTION:
//                if (approvalResult == DictApprovalResultEnum.APPROVED) {
//                    nextStatus = DictProjectBeiJStatusEnum.PROJECT_POWER;
//                } else {
//                    nextStatus = DictProjectBeiJStatusEnum.CONSTR_INSPECTION_REJECTED;
//                }
//                break;
            //巡检员
//            case  ADMINWORK_APPROVAL_SECOND:
//                if (approvalResult == DictApprovalResultEnum.APPROVED) {
//                    nextStatus = DictProjectBeiJStatusEnum.CONSTR_INSPECTION;
//                } else {
//                    nextStatus = DictProjectBeiJStatusEnum.CONSTR_INSPECTION_REJECTED;
//                }
//                break;

            case  CONSTR_INSPECTION:
                if (approvalResult == DictApprovalResultEnum.APPROVED) {
                    nextStatus = DictProjectBeiJStatusEnum.PROJECT_INSPECTION;
                } else {
                    nextStatus = DictProjectBeiJStatusEnum.CONSTR_NO;
                }
                break;

                //
            case DATA_APPROVAL_REJECTED:
                nextStatus = DictProjectBeiJStatusEnum.DATA_APPROVAL;
                break;
//            case CONSTR_INSPECTION_REJECTED:
//                nextStatus = DictProjectBeiJStatusEnum.ADMINWORK_APPROVAL_SECOND;
//                break;

//            case ADMINWORK_RETURN:
//                nextStatus = DictProjectBeiJStatusEnum.ADMINWORK_APPROVAL;
//                break;

            case CONSTR_NO:
                nextStatus = DictProjectBeiJStatusEnum.CONSTR_INSPECTION;
                break;
            default:
                nextStatus = currentStatus.next();
        }
        return nextStatus.getCode();
    }
}
