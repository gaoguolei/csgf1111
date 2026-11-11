package org.jeecg.modules.pcms.common.set.service;

import org.jeecg.modules.pcms.common.set.entity.SetRegions;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: set_regions
 * @Author: jeecg-boot
 * @Date:   2023-03-12
 * @Version: V1.0
 */
public interface ISetRegionsService extends IService<SetRegions> {

    /**
     * 根据ID值，查询LocalName
     * @param id
     * @return
     */
    public String getLocalName(Integer id);

}
