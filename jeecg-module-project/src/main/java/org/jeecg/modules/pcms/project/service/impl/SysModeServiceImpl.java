package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.entity.SysModel;
import org.jeecg.modules.pcms.project.mapper.SysModelMapper;
import org.jeecg.modules.pcms.project.service.ISysModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/3 上午9:45
 * @Version 1.5
 */
@Service
public class SysModeServiceImpl extends ServiceImpl<SysModelMapper, SysModel> implements ISysModelService {

    @Autowired
    private SysModelMapper sysModelMapper;
    @Override
    public List<SysModel> getModels(Integer id) {
        List<SysModel> modes = sysModelMapper.getModes(id);
        if(modes  != null || modes.size() > 0){
            return modes;
        }else {
            //如果没有查询到就给通用的
           return sysModelMapper.getModes(51148);
        }
    }
}
