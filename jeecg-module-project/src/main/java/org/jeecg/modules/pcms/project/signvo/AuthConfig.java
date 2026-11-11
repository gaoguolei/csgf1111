package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

@Data
public class AuthConfig {


    private String[] psnAvailableAuthModes;
    private String[] willingnessAuthModes;
    private String[] orgAvailableAuthModes;

}
