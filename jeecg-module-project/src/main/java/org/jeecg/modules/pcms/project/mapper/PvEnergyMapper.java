package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.entity.PvEnergy;

import java.math.BigDecimal;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:15
 * @Version 1.5
 */
public interface PvEnergyMapper extends BaseMapper<PvEnergy> {


    @Select("select sum(energy) from pv_energy where year=#{year} and month=#{month} and day=#{day}")
   public BigDecimal getWeek(@Param("year")Integer year,@Param("month")Integer month,@Param("day")Integer day);

    @Select("select sum(energy) from pv_energy where year=#{year} and month=#{month}")
    public BigDecimal getMonth(@Param("year")Integer year,@Param("month")Integer month);
}
