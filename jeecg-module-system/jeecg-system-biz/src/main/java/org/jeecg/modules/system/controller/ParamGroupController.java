package org.jeecg.modules.system.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.ParamGroup;
import org.jeecg.modules.system.service.IParamGroupService;
import org.jeecg.modules.system.vo.ParamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @Author gongzhou
 * @Date 2023/3/6 上午10:55
 * @Version 1.5
 */

@RestController
@RequestMapping("/sys/param-group")
@Slf4j
public class ParamGroupController {

    @Autowired
    private IParamGroupService iParamGroupService;

    /**
     * 添加
     *
     * @param paramGroup
     * @return
     */
    @PostMapping("add")
    public Result add(@RequestBody ParamGroup paramGroup) {
        return iParamGroupService.add(paramGroup);

    }


    /**
     * 修改
     *
     * @param paramGroup
     * @return
     */
    @PutMapping("/edit")
    public Result edit(@RequestBody ParamGroup paramGroup) {
        return iParamGroupService.edit(paramGroup);
    }


    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "delete/{id}")
    public Result delete(@PathVariable Integer id) {
        return iParamGroupService.delete(id);
    }


    /**
     * 查询一个参数组
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/{id}")
    public Result getParamGroup(@PathVariable Integer id) {
        return iParamGroupService.getParamGroup(id);
    }


    /**
     * 查询参数组及参数组内的数据
     *
     * @return
     */
    @GetMapping(value = "/param/{areaProjectId}")
    public Result getParams(@PathVariable Integer areaProjectId) {
        return iParamGroupService.params(areaProjectId);
    }



    @ApiOperation(value = "参数组上移或者下移", notes = "分类绑定参数时，上移下移参数组时使用")
    @PostMapping(value = "/sort")
    public Result groupSort(@RequestBody ParamVO paramVO) {
        Result result = this.iParamGroupService.groupSort(paramVO);
        return result;
    }

    @GetMapping(value = "/params/{id}")
    public Result params(@PathVariable Integer id) {
        return iParamGroupService.getParams(id);
    }


}
