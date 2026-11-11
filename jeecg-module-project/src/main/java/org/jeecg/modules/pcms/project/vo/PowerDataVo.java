package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/10/9 上午10:21
 * @Version 1.5
 */
@Data
public class PowerDataVo {

    private PowerVo data;

    private Integer errorCode;

    private String errorMsg;
}
