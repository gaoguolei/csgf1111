package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.system.model.ParamItem;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/3/6 下午4:18
 * @Version 1.5
 */
public interface ParamItemMapper extends BaseMapper<ParamItem> {

    @Select("select * from set_param_item where param_group_id = #{paramGroupId} order by sort_no desc limit 0,1")
    ParamItem getParamGroupByGroupId(@Param("paramGroupId") Integer paramGroupId);

    @Select("select * from set_param_item where sort_no<#{sortNo} and param_group_id=#{paramGroupId} order by sort_no desc limit 0,1")
    ParamItem getParamItemBySortNoUp(@Param("sortNo") Integer sortNo, @Param("paramGroupId") Integer paramGroupId);

    @Select("select * from set_param_item where sort_no>#{sortNo} and param_group_id=#{paramGroupId} order by sort_no desc limit 0,1")
    ParamItem getParamItemBySortNoDown(@Param("sortNo") Integer sortNo, @Param("paramGroupId") Integer paramGroupId);


    @Update("update set_param_item set sort_no = #{sortNo} where id = #{id}")
    void updateSort(@Param("sortNo") Integer sortNo, @Param("id") Integer id);

    @Delete("delete from set_param_item where param_group_id=#{groupId}")
    void deleteByGroupId(@Param("groupId") Integer groupId);


}
