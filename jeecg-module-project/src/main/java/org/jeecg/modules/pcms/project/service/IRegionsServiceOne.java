package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.entity.Regions;
import org.jeecg.modules.pcms.project.vo.RegionsVO;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/3/16 下午5:11
 * @Version 1.5
 */
public interface IRegionsServiceOne extends IService<Regions> {
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
     * @param regionId 地区id
     * @return 地区集合
     */
    Result getRegionsChildren(Integer regionId);


    Result getRegions();

    Result deleteTest(Integer id);

    String getNameByCode(String orgCode);

    String getProjectName(String ProjectName);

    List<String> getRoleIds(String userId);

    String getRoleId(String userId);

    String getImgUrl(Long imgId);

    List<Integer> getRegionsIds(String id);

    List<Integer> getRegionsIdsXZ(String addressId);

}
