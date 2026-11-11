package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.SysModel;
import org.jeecg.modules.pcms.project.mapper.SysModelMapper;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/3 上午9:40
 * @Version 1.5
 */
public interface ISysModelService extends IService<SysModel> {

    List<SysModel> getModels(Integer id);

}
