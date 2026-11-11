package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;

public interface PvStationsViewMapper extends BaseMapper<PvStationsView> {

    @Delete("delete from pv_stations_view")
    public void deleteAll();
}
