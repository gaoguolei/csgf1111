package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

@Data
public class SignFlowConfig {

    private String signFlowTitle;
    private Boolean autoStart;
    private Boolean autoFinish;
    private Boolean identityVerify;
    private ChargeConfig chargeConfig;
    private SignConfig signConfig;

    //回调地址
    private String notifyUrl;
    private RedirectConfig redirectConfig;
    private AuthConfig authConfig;


}
