package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.ParamGroup;
import org.jeecg.modules.system.vo.ParamVO;


/**
 * @Author gongzhou
 * @Date 2023/3/6 上午10:49
 * @Version 1.5
 */
public interface IParamGroupService extends IService<ParamGroup> {

    /**
     * 添加参数组
     *
     *            参数组
     * @return ParameterGroup 参数组
     */
    Result add(ParamGroup paramGroup);

    /**
     * 修改
     * @param paramGroup
     * @return
     */
    Result edit(ParamGroup paramGroup);


    /**
     * 删除
     *
     * @param id
     * @return
     */
    Result delete(Integer id);


    Result getParamGroup(Integer id);

    Result params(Integer projectId);

    Result groupSort(ParamVO paramVO);

    Result getParams(Integer groupId);


    Result getGroup(Integer id);
}
