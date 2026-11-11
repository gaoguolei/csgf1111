package org.jeecg.modules.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.ParamItem;
import org.jeecg.modules.system.vo.ParamVO;

/**
 * @Author gongzhou
 * @Date 2023/3/6 下午4:15
 * @Version 1.5
 */
public interface IParamItemService extends IService<ParamItem> {

    Result add(ParamItem paramItem);


    Result edit(ParamItem paramItem);

    Result delete(Integer id);


    Result getParamItem(Integer id);


    Result paramSort(ParamVO paramVO);
}
