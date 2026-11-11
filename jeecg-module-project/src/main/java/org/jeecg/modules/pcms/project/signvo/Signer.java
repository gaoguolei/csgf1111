package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

import java.util.List;

@Data
public class Signer {

    private SignConfigOfSign signConfig;

    private NoticeConfig noticeConfig;

    private Integer signerType;

    private PsnSignerInfo psnSignerInfo;

    private OrgSignerInfo orgSignerInfo;

    private List<SignField> signFields;



}
