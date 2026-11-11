package org.jeecg.modules.pcms.project.service;

import org.jeecg.modules.pcms.common.DicProjectBeijBpmEnum;
import org.jeecg.modules.pcms.common.DictProjectBpmEnum;
import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;

import java.util.List;

/**
 * @Description: pv_project_bpm
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface IPvProjectBpmService extends IService<PvProjectBpm> {

	/**
	 * 通过主表id查询子表数据
	 *
	 * @param mainId 主表id
	 * @return List<PvProjectBpm>
	 */
	public List<PvProjectBpm> selectByMainId(Integer mainId);

	/**
	 * 通过主表id查询子表数据
	 * @param
	 * @param projectStatus
	 * @return
	 */
	public List<PvProjectBpm> selectByProjectId(Integer projectId,String projectStatus);

	public List<PvProjectBpm> selectByProjectIdBeiJ(Integer projectId,String projectStatus);

	/**
	 * 查询某一环节数据
	 * @param pvProjectBpm
	 * @return
	 */
	public PvProjectBpm selectByProjectBpm(PvProjectBpm pvProjectBpm);


	/**
	 * 启动一个新业务
	 * @param projectId
	 * @return
	 */
	public PvProjectBpm startProjectBpm(Integer projectId);


	public PvProjectBpm startProjectBpmBeiJ(Integer projectId);

	/**
	 * 新增业务环节
	 * @param projectId
	 * @param dictProjectBpmEnum
	 * @return
	 */
	public int insertProjectBpm(Integer projectId, DictProjectBpmEnum dictProjectBpmEnum);

	/**
	 * 新增业务环节(北京项目)
	 * @param projectId
	 * @param dictProjectBpmEnum
	 * @return
	 */
	public int insertProjectBpmBeiJ(Integer projectId, DicProjectBeijBpmEnum dictProjectBpmEnum);

	/**
	 * 将指定环节设置为已提交
	 * @param projectId
	 * @param dictProjectBpmEnum
	 * @return
	 */
	public int commit(Integer projectId,DictProjectBpmEnum dictProjectBpmEnum);



	/**
	 * 将指定环节设置为已提交（北京项目）
	 * @param projectId
	 * @param dictProjectBpmEnum
	 * @return
	 */
	public int commitBeiJ(Integer projectId, DicProjectBeijBpmEnum dictProjectBpmEnum);
}
