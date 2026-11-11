package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.system.model.Regions;

/**
 * @Author gongzhou
 * @Date 2023/3/5 上午9:41
 * @Version 1.5
 */
public interface RegionsMapper extends BaseMapper<Regions> {

    @Select("select max(id) from set_regions")
    Integer getLastId();

    Integer deleteAndPath(@Param("id") Integer id);

    Integer selectUseRegions(@Param("id") Integer id);

    Integer selectUseRegionsTenant(@Param("id") Integer id);
}
