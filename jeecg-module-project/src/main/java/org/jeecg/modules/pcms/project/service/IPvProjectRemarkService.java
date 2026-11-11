package org.jeecg.modules.pcms.project.service;

import org.jeecg.modules.pcms.project.entity.PvProjectRemark;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: pv_project_remark
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface IPvProjectRemarkService extends IService<PvProjectRemark> {

	/**
	 * 通过主表id查询子表数据
	 *
	 * @param mainId 主表id
	 * @return List<PvProjectRemark>
	 */
	public List<PvProjectRemark> selectByMainId(Integer mainId);

	List<PvProjectRemark> selectByMainIdNew(Integer projectInfoId);

	public List<PvProjectRemark> selectByMainIdNewNew(Integer mainId);

	public List<PvProjectRemark> selectByMainIdNewNewNew(Integer mainId);

	List<PvProjectRemark> selectByMainIdNewNewNewNew(Integer projectInfoId);

	void updateByIdNew(PvProjectRemark pvProjectRemark);

	List<PvProjectRemark> selectByMainIdNewNewNewNewNew(Integer projectInfoId);

}
