package org.jeecg.modules.system.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/3/5 上午9:37
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
