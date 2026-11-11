package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.system.model.ParamGroup;
import org.jeecg.modules.system.model.ParamItem;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/3/6 上午10:47
 * @Version 1.5
 */
public interface ParamGroupMapper extends BaseMapper<ParamGroup> {



    @Select("select * from set_param_group where area_project_id = #{areaProjectId} order by sort_no desc limit 0,1")
    ParamGroup getParamGroupByProjectId(@Param("areaProjectId") Integer areaProjectId);

    @Select("select * from set_param_group where tenantId = #{tenantId} order by sort_no desc limit 0,1")
    ParamGroup getParamGroupByTenantId(@Param("tenantId") Integer tenantId);

    @Select("select * from set_param_group where id = #{paramGroupId}")
    ParamGroup getParamGroupById(@Param("paramGroupId") Integer paramGroupId);

    @Select("select si.* from set_param_item si left join set_param_group sp on si.param_group_id=sp.id order by si.sort_no asc")
    List<ParamItem> getItemList();



    @Select("select * from set_param_group where sort_no<#{sortNo}  and area_project_id=#{areaProjectId}  order by sort_no desc limit 0,1")
    ParamGroup getParamItemBySortNoUp(@Param("sortNo") Integer sortNo, @Param("areaProjectId") Integer areaProjectId);

    @Select("select * from set_param_group where sort_no>#{sortNo}  and area_project_id=#{areaProjectId}  order by sort_no desc limit 0,1")
    ParamGroup getParamItemBySortNoDown(@Param("sortNo") Integer sortNo, @Param("areaProjectId") Integer areaProjectId);

    @Update("update set_param_group set sort_no = #{sortNo} where id = #{id}")
    void updateSort(@Param("sortNo") Integer sortNo, @Param("id") Integer id);


    @Select("select * from set_param_group where area_project_id= #{id} order  by id asc")
    List<ParamGroup> getGroup(@Param("id")Integer id);
}
