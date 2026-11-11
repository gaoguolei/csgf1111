package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TRentReconcileTask;
import org.jeecg.modules.pcms.rent.mapper.TRentReconcileTaskMapper;
import org.jeecg.modules.pcms.rent.service.ITRentReconcileTaskService;
import org.springframework.stereotype.Service;

/**
 * @Description: 回单对账异步任务
 * @Version: V1.0
 */
@Service
public class TRentReconcileTaskServiceImpl extends ServiceImpl<TRentReconcileTaskMapper, TRentReconcileTask>
        implements ITRentReconcileTaskService {
}