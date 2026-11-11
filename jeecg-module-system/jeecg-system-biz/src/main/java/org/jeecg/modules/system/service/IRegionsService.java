package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.Regions;
import org.jeecg.modules.system.vo.ParamVO;
import org.jeecg.modules.system.vo.RegionsVO;

/**
 * 地区service
 * @Author gongzhou
 * @Date 2023/3/5 上午9:30
 * @Version 1.5
 */
public interface IRegionsService extends IService<Regions> {

    /**
     * 添加地区
     *
     * @param regionsVO 地区
     * @return Regions 地区
     */
    Result add(RegionsVO regionsVO);

    /**
     * 修改地区
     *
     * @param regions 地区
     * @param id      地区主键
     * @return Regions 地区
     */
    Result edit(Regions regions, Integer id);


    /**
     * 删除地区
     *
     * @param id 地区主键
     */
    Result delete(Integer id);


    /**
     * 获取地区
     *
     * @param id 地区主键
     * @return Regions  地区
     */
    Regions getModel(Integer id);



    /**
     * 根据地区id获取其子地区
     *
     * @return 地区集合
     */
    Result getRegionsChildren(ParamVO paramVO,Integer id);


    Result getRegions();

    Result deleteTest(Integer id);

    Result getRegionsByTenantId(Integer id);

    Result getRegionsByRenterId(String renterId);
}
