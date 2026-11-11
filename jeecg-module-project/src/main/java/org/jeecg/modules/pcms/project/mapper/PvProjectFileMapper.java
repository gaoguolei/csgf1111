package org.jeecg.modules.pcms.project.mapper;

import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: pv_project_file
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface PvProjectFileMapper extends BaseMapper<PvProjectFile> {

	/**
	 * 通过主表id删除子表数据
	 *
	 * @param mainId 主表id
	 * @return boolean
	 */
	public boolean deleteByMainId(@Param("mainId") Integer mainId);

	/**
	 * 物理删除
	 * @param mainId
	 * @return
	 */
	public boolean physicalDeleteByMainId(@Param("mainId") Integer mainId);

  /**
   * 通过主表id查询子表数据
   *
   * @param mainId 主表id
   * @return List<PvProjectFile>
   */
	public List<PvProjectFile> selectByMainId(@Param("mainId") Integer mainId);

	/**
	 * 获取需要删除原图的文件列表
	 * @param mainId
	 * @return
	 */
	@InterceptorIgnore(tenantLine = "true")
	public List<PvProjectFile> getExpirationList(@Param("mainId") Integer mainId);


	@Select("select file_url from pv_project_file where project_id = #{mainId}")
	public List<String> selectFilesId(@Param("mainId") Integer mainId);



	@Update("update pv_project_file set del_flag=0 where project_id=#{projectId}")
	Boolean updateFileByProjectId(@Param("projectId")Integer projectId);


	@Delete("delete from  pv_project_file  where project_id=#{projectId}")
	Boolean deleteByProjectId(@Param("projectId")Integer projectId);

}
