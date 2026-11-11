package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.project.stationentity.PvStationsDetails;

public interface PvStationsDetailsMapper extends BaseMapper<PvStationsDetails> {

    @Delete("delete from  pv_stations_details")
    public void deleteAll();
}
