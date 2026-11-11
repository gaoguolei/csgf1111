package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/20 下午5:41
 * @Version 1.5
 */

@Data
public class SignResultVo {

    private Integer code;

    private String message;

    private SignFlowId data;
}
