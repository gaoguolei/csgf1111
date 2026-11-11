package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

@Data
public class NormalSignFieldConfig {
    private Boolean autoSign;

    private Boolean freeMode;
    private Boolean movableSignField;
    private String psnSealStyles;
    private String signFieldSize;
    private Integer signFieldStyle;
    private SignFieldPosition signFieldPosition;
    private SignDateConfig signDateConfig;

}
