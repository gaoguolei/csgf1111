package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.mapper.TOperLogMapper;
import org.jeecg.modules.pcms.rent.service.ITOperLogService;
import org.springframework.stereotype.Service;

/**
 * @Description: TOperLog
 * @Version: V1.0
 */
@Service
public class TOperLogServiceImpl extends ServiceImpl<TOperLogMapper, TOperLog> implements ITOperLogService {
}
