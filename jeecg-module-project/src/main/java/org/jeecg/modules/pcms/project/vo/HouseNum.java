package org.jeecg.modules.pcms.project.vo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/7/31 上午10:53
 * @Version 1.5
 */

/**
 * 户数
 */
@Data
public class HouseNum {

    //累计签约完成户数
    private Integer signCount;

    //本周签约户数
    private Integer signWeek;


    //累计施工户数
    private Integer buildCount;

    //本周施工户数
    private Integer buildWeek;


    //累计申报户数
    private Integer reportCount;

    //本周申报户数
    private Integer reportWeek;


    //累计并网户数
    private Integer onGridCount;

    //本周并网户数
    private Integer onGridWeek;


}
