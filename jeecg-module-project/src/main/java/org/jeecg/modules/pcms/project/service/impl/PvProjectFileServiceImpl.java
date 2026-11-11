package org.jeecg.modules.pcms.project.service.impl;

import org.jeecg.modules.pcms.project.entity.PvProjectFile;
import org.jeecg.modules.pcms.project.mapper.PvProjectFileMapper;
import org.jeecg.modules.pcms.project.service.IPvProjectFileService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: pv_project_file
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
@Service
public class PvProjectFileServiceImpl extends ServiceImpl<PvProjectFileMapper, PvProjectFile> implements IPvProjectFileService {
	
	@Autowired
	private PvProjectFileMapper pvProjectFileMapper;
	
	@Override
	public List<PvProjectFile> selectByMainId(Integer mainId) {
		return pvProjectFileMapper.selectByMainId(mainId);
	}

	@Override
	public List<String> selectFilesId(Integer mainId) {
		return pvProjectFileMapper.selectFilesId(mainId);
	}
}
