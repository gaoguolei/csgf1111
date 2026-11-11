package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/14 下午2:50
 * @Version 1.5
 */
@Data
public class PvEnergyResultVo {


    private String Time;
    private BigDecimal power;
    private BigDecimal income;
    private BigDecimal hour;

    private List<PvEnergyVo>  pvEnergyVos;
}
