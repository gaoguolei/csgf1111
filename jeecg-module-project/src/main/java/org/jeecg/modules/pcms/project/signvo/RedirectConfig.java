package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

@Data
public class RedirectConfig {

    private String redirectDelayTime;
    //签署完成后重定向跳转地址
    private String redirectUrl;

}
