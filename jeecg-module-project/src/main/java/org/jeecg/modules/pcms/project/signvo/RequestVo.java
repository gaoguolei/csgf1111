package org.jeecg.modules.pcms.project.signvo;


import lombok.Data;

import java.util.List;

@Data
public class RequestVo {

    private List<DocVo> docs;

    private SignFlowConfig signFlowConfig;

    private List<Signer> signers;


}
