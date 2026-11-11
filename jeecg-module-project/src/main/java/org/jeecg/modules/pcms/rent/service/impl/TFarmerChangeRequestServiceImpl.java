package org.jeecg.modules.pcms.rent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.pcms.rent.entity.TFarmerChangeRequest;
import org.jeecg.modules.pcms.rent.mapper.TFarmerChangeRequestMapper;
import org.jeecg.modules.pcms.rent.service.ITFarmerChangeRequestService;
import org.springframework.stereotype.Service;

/**
 * @Description: 农户信息变更审核申请
 * @Version: V1.0
 */
@Service
public class TFarmerChangeRequestServiceImpl
        extends ServiceImpl<TFarmerChangeRequestMapper, TFarmerChangeRequest>
        implements ITFarmerChangeRequestService {
}
