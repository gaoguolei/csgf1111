package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author gongzhou
 * @Date 2023/7/31 上午10:06
 * @Version 1.5
 */
@Data
public class HomePageCountVo implements Serializable {

    /**
     * 户数
     */
    private HouseNum houseNum;

    /**
     * 容量
     */
    private Capacity capacity;

}
