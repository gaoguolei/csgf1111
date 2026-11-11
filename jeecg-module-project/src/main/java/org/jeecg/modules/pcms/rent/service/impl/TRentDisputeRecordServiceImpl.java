package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TRentDisputeRecord;
import org.jeecg.modules.pcms.rent.mapper.TRentDisputeRecordMapper;
import org.jeecg.modules.pcms.rent.service.ITRentDisputeRecordService;
import org.springframework.stereotype.Service;

/**
 * @Description: TRentDisputeRecord
 * @Version: V1.0
 */
@Service
public class TRentDisputeRecordServiceImpl
        extends ServiceImpl<TRentDisputeRecordMapper, TRentDisputeRecord>
        implements ITRentDisputeRecordService {
}
