package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.jeecg.modules.pcms.project.entity.PvDatalogger;

/**
 * @Author gongzhou
 * @Date 2023/11/16 上午8:04
 * @Version 1.5
 */
public interface PvDataloggerMapper extends BaseMapper<PvDatalogger> {



    @Delete("delete from pv_datalogger")
    public void deleteAll();
}
