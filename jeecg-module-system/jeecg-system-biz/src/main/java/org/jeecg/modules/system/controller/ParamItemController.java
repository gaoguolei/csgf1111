package org.jeecg.modules.system.controller;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.ParamItem;
import org.jeecg.modules.system.service.IParamItemService;
import org.jeecg.modules.system.vo.ParamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author gongzhou
 * @Date 2023/3/6 下午3:11
 * @Version 1.5
 */


@RestController
@RequestMapping("/sys/param-item")
@Slf4j
public class ParamItemController {
    @Autowired
    private IParamItemService iParamItemService;


    /**
     * 添加参数项
     *
     * @param paramItem
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody ParamItem paramItem) {
        return iParamItemService.add(paramItem);
    }


    /**
     * 修改
     *
     * @param paramItem
     * @return
     */
    @PutMapping("/edit")
    public Result edit(@RequestBody ParamItem paramItem) {
        return iParamItemService.edit(paramItem);
    }


    /**
     * 获取一个参数项
     * @param id
     * @return
     */
    @GetMapping(value = "/{id}")
    public Result get(@PathVariable Integer id) {
        return iParamItemService.getParamItem(id);
    }


    @DeleteMapping(value = "delete/{id}")
    public Result delete(@PathVariable Integer id) {
        return iParamItemService.delete(id);
    }


    /**
     * 参数项上移或者下移
     *
     * @param paramVO
     * @return
     */
    @PostMapping("/sort")
    public Result paramSort(@RequestBody ParamVO paramVO) {
        return iParamItemService.paramSort(paramVO);
    }
}
