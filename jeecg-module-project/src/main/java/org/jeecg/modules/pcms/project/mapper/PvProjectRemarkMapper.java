package org.jeecg.modules.pcms.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectRemark;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: pv_project_remark
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface PvProjectRemarkMapper extends BaseMapper<PvProjectRemark> {

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
   * @return List<PvProjectRemark>
   */
	public List<PvProjectRemark> selectByMainId(@Param("mainId") Integer mainId);


	@Update("update pv_project_remark set del_flag=0 where project_id=#{projectId}")
	Boolean updateRemarkByProjectId(@Param("projectId")Integer projectId);


	@Delete("delete from  pv_project_remark  where project_id=#{projectId}")
	Boolean deleteByProjectId(@Param("projectId")Integer projectId);

	List<PvProjectRemark> selectByProjectInfoId(@Param("projectInfoId") Integer projectInfoId);

	void updateByIdNew(@Param("p") PvProjectRemark pvProjectRemark);

}
