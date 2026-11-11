package org.jeecg.modules.pcms.energy.vo;


import lombok.Data;

@Data
public class ResultLoginApiVo {

    private String reqSerialNum;
    private String resultCode;
    private String resultMsg;
    private ResultLoginVo resultData;
}
