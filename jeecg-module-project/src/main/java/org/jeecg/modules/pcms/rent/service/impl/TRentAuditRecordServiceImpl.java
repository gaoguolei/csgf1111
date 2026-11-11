package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TRentAuditRecord;
import org.jeecg.modules.pcms.rent.mapper.TRentAuditRecordMapper;
import org.jeecg.modules.pcms.rent.service.ITRentAuditRecordService;
import org.springframework.stereotype.Service;

/**
 * @Description: TRentAuditRecord
 * @Version: V1.0
 */
@Service
public class TRentAuditRecordServiceImpl extends ServiceImpl<TRentAuditRecordMapper, TRentAuditRecord>
        implements ITRentAuditRecordService {
}
