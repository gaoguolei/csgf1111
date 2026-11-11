package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectType;
import org.jeecg.modules.pcms.project.vo.ProjectTypeVo;

import java.util.List;

public interface PvProjectTypeMapper extends BaseMapper<PvProjectType> {

//    @Update("update pv_project_type set type_value = #{typeValue},update_time = #{} where id = #{typeId} and tenant_id = #{tenantId} and del_flag = 0")
    void updateProjectType(@Param("typeId") Integer typeId, @Param("typeValue") String typeValue, @Param("tenantId")Integer tenantId);

    @Update({
            "<script>",
            "UPDATE pv_project_type",
            "SET del_flag = 1",
            "WHERE id IN",
            "<foreach item='item' index='index' collection='keys' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "AND del_flag = 0",
            "AND tenant_id = #{tenantId}",
            "</script>"
    })
    void delProjectType(@Param("keys") List<Integer> keys,@Param("tenantId") Integer tenantId);


    @Update({
            "<script>",
            "UPDATE pv_project_type",
            "SET del_flag = 0",
            "WHERE id IN",
            "<foreach item='item' index='index' collection='keys' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "AND del_flag = 1",
            "AND tenant_id = #{tenantId}",
            "</script>"
    })
    void enableProjectType(@Param("keys") List<Integer> keys,@Param("tenantId") Integer tenantId);

    @Select("select * from pv_project_type where tenant_id = #{tenantId}")
    List<PvProjectType> findByPage(Integer tenantId);
}
