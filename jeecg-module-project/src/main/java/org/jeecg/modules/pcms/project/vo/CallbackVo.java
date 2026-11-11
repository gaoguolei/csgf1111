package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/16 下午3:48
 * @Version 1.5
 */

@Data
public class CallbackVo {

    private String action;
    private String timestamp;
    private String signFlowId;
    private String customBizNum;
    private String signOrder;
    private String operateTime;
    private String signResult;
    private String resultDescription;
    private OperatorVo operator;
}
