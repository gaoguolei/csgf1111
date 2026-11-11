package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvProjectFileOther;

public interface IPvProjectFileOtherService extends IService<PvProjectFileOther> {

    void delProjectInfoOtherFile(Integer id);

}
