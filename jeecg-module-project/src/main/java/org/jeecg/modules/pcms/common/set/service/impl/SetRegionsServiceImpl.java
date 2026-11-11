package org.jeecg.modules.pcms.common.set.service.impl;

import org.jeecg.modules.pcms.common.set.entity.SetRegions;
import org.jeecg.modules.pcms.common.set.mapper.SetRegionsMapper;
import org.jeecg.modules.pcms.common.set.service.ISetRegionsService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: set_regions
 * @Author: jeecg-boot
 * @Date:   2023-03-12
 * @Version: V1.0
 */
@Service
public class SetRegionsServiceImpl extends ServiceImpl<SetRegionsMapper, SetRegions> implements ISetRegionsService {

    @Override
    public String getLocalName(Integer id) {
        SetRegions regions = getById(id);
        return regions.getLocalName();
    }
}
