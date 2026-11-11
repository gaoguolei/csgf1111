package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.util.List;

@Data
public class updateBatchVo {

    private List<Integer> ids;

    private String typeId;

}
