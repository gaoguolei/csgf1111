package org.jeecg.modules.pcms.project.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: pv_project_bpm
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface PvProjectBpmMapper extends BaseMapper<PvProjectBpm> {

	/**
	 * 通过主表id删除子表数据
	 *
	 * @param mainId 主表id
	 * @return boolean
	 */
	public boolean deleteByMainId(@Param("mainId") Integer mainId);

	/**
	 * 物理删除
	 * @param pvProjectBpm
	 * @return
	 */
	public boolean physicalDeleteByMainId(PvProjectBpm pvProjectBpm);

  /**
   * 通过主表id查询子表数据
   *
   * @param mainId 主表id
   * @return List<PvProjectBpm>
   */
	public List<PvProjectBpm> selectByMainId(@Param("mainId") Integer mainId);

	/**
	 * 查询某一环节数据
	 * @param pvProjectBpm
	 * @return
	 */
	public PvProjectBpm selectByProjectBpm(PvProjectBpm pvProjectBpm);


	public PvProjectBpm queryStatusById(@Param("id")Integer id);

	@Update("update pv_project_bpm set del_flag=0,update_time=#{updateTime} where project_id=#{projectId}")
	Boolean updateBpmByProjectId(@Param("projectId")Integer projectId,@Param("updateTime") Date updateTime);

}
