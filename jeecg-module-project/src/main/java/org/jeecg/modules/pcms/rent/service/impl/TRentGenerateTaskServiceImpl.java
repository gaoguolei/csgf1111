package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TRentGenerateTask;
import org.jeecg.modules.pcms.rent.mapper.TRentGenerateTaskMapper;
import org.jeecg.modules.pcms.rent.service.ITRentGenerateTaskService;
import org.springframework.stereotype.Service;

/**
 * @Description: 年度租金台账生成任务
 * @Version: V1.0
 */
@Service
public class TRentGenerateTaskServiceImpl
        extends ServiceImpl<TRentGenerateTaskMapper, TRentGenerateTask>
        implements ITRentGenerateTaskService {
}
