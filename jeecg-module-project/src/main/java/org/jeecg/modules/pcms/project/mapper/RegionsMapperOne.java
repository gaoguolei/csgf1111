package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.pcms.project.entity.Regions;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/3/16 下午5:05
 * @Version 1.5
 */
public interface RegionsMapperOne extends BaseMapper<Regions> {
    @Select("select max(id) from set_regions")
    Integer getLastId();

    Integer deleteAndPath(@Param("id") Integer id);

    @Select("select depart_name from  sys_depart where org_code  = #{orgCode}")
    String getNameByCode(@Param("orgCode") String orgCode);

    @Select("select dict_code from sys_dict where dict_name = #{projectName}")
    String getProjectName(@Param("projectName") String projectName);

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<String> getRoleIds(@Param("userId")String userId);

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    String getRoleId(@Param("userId")String userId);

    @Select("select file_url from pv_project_file where id =#{imgId} ")
    String getImgUrl(@Param("imgId")Long imgId);

    @Select("select * from set_regions where region_grade=3 and region_path like CONCAT(CONCAT('%',#{regionsId}),'%')")
    List<Integer> getRegionsIds(String regionsId);

    @Select("select * from set_regions where region_grade=2 and region_path like CONCAT(CONCAT('%',#{regionsId}),'%')")
    List<Integer> getRegionsIdsXZ(String id);

}
