package org.jeecg.modules.pcms.project.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午3:37
 * @Version 1.5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PvEnergyVo {

    private Integer key;
    private BigDecimal value;
    private BigDecimal hour;
    private BigDecimal income;

}
