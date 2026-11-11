package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

@Data
public class ProjectTypeVo {

    // 类型id
    private Integer Id;

    // 所属租户
    private Integer tenantId;

    // 项目类型value
    private String typeValue;

}
