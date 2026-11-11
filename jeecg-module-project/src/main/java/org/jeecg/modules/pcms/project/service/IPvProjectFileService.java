package org.jeecg.modules.pcms.project.service;

import org.jeecg.modules.pcms.project.entity.PvProjectFile;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: pv_project_file
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface IPvProjectFileService extends IService<PvProjectFile> {

	/**
	 * 通过主表id查询子表数据
	 *
	 * @param mainId 主表id
	 * @return List<PvProjectFile>
	 */
	public List<PvProjectFile> selectByMainId(Integer mainId);

	public List<String> selectFilesId(Integer mainId);
}
