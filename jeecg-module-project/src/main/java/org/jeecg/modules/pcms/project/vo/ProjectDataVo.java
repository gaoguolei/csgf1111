package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/7/31 上午11:55
 * @Version 1.5
 */
@Data
public class ProjectDataVo {

    //户数
    private Integer house;

    //容量
    private BigDecimal volume;
}
