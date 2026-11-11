package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PdfVO implements Serializable {

    private List<Long> imgIds;

    private List<String> names;

    private String projectCode;

    private List<Integer> projectId;
}
