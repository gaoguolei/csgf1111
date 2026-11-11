package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/11/29 下午7:50
 * @Version 1.5
 */
@Data
public class ChartVo {

    private Integer index;
    private Integer key;

    private Integer signCount;

    private BigDecimal capacity;
}
