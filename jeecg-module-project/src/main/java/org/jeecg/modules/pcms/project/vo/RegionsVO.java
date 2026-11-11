package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/3/16 下午5:14
 * @Version 1.5
 */
@Data

public class RegionsVO {
    private Integer parentId;
    /**
     * 名称
     */
    private String localName;
    /**
     * 邮编
     */
    private String zipcode;
}
