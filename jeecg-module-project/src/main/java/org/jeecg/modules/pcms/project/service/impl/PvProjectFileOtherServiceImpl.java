package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.project.entity.PvProjectFileOther;
import org.jeecg.modules.pcms.project.mapper.PvProjectFileOtherMapper;
import org.jeecg.modules.pcms.project.service.IPvProjectFileOtherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PvProjectFileOtherServiceImpl extends ServiceImpl<PvProjectFileOtherMapper, PvProjectFileOther> implements IPvProjectFileOtherService {

    @Autowired
    private PvProjectFileOtherMapper pvProjectFileOtherMapper;

    @Override
    public void delProjectInfoOtherFile(Integer id) {
        pvProjectFileOtherMapper.delProjectInfoOtherFile(id);
    }

}
