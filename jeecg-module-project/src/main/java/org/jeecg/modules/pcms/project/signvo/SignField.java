package org.jeecg.modules.pcms.project.signvo;

import lombok.Data;

@Data
public class SignField {

    private String fileId;

    private String customBizNum;

    private Integer signFieldType;

    private NormalSignFieldConfig normalSignFieldConfig;


}
