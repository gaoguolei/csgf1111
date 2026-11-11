package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/9/19 上午8:47
 * @Version 1.5
 */

@Data
public class HouseTypeVo implements Serializable {

    //类型
    private String houseType;

    //数量
    private Integer num;

    //尺寸（长×宽）
    private BigDecimal length;

    private BigDecimal width;
}
