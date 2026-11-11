package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.entity.SysModel;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/3 上午9:36
 * @Version 1.5
 */
public interface SysModelMapper extends BaseMapper<SysModel> {

    @Select("select * from sys_model m left join sys_model_town mt on mt.model_id= m.id where mt.town_id =#{id} order by mt.sort asc")
    List<SysModel> getModes(@Param("id") Integer id);
}
