package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvDatalogger;
import org.springframework.stereotype.Service;

/**
 * @Author gongzhou
 * @Date 2023/11/16 上午8:06
 * @Version 1.5
 */


public interface IPvDataloggerService extends IService<PvDatalogger> {
    public void insert();
}
