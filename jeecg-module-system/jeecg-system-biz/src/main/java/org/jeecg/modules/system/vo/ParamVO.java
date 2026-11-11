package org.jeecg.modules.system.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/3/7 上午11:50
 * @Version 1.5
 */
@Data
public class ParamVO {
    private Integer id;
    private Integer areaProjectId;
    private Integer paramGroupId;
    private String sortType;
    private String renterId;
}
