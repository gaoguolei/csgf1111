package org.jeecg.modules.system.controller;


import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.model.Regions;
import org.jeecg.modules.system.service.IRegionsService;
import org.jeecg.modules.system.vo.ParamVO;
import org.jeecg.modules.system.vo.RegionsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 区域Controller
 */

@RestController
@RequestMapping("/sys/regions")
@Api(tags = "区域Api")
@Slf4j
public class RegionsBaseController {


    @Autowired
    private IRegionsService iRegionsService;

    @PostMapping("/add")
    public Result add(@RequestBody RegionsVO regionsVO) {
        return iRegionsService.add(regionsVO);

    }


    @PutMapping(value = "/{id}")
    @ApiOperation(value = "修改地区", response = Regions.class)
    public Result edit(@RequestBody RegionsVO regionsVO, @PathVariable Integer id) {
        Regions regions = iRegionsService.getModel(id);
        String localName = regionsVO.getLocalName();
        if (regions == null) {
            return Result.error(500, "当前地区不存在");
        }
        regions.setLocalName(localName);
        Result edit = iRegionsService.edit(regions, id);

        return edit;

    }


    @DeleteMapping(value = "/{id}")
    @ApiOperation(value = "删除地区")
    public Result delete(@PathVariable Integer id) {
        return iRegionsService.delete(id);
    }


    @GetMapping(value = "/{id}/children")
    @ApiOperation(value = "获取某地区的子地区")
    public Result getChildrenById(ParamVO paramVO,@PathVariable Integer id) {
        return iRegionsService.getRegionsChildren(paramVO,id);
    }

    @GetMapping(value = "/getRegions")
    @ApiOperation(value = "获取子地区")
    public Result getRegions() {
        return iRegionsService.getRegions();
    }


    @DeleteMapping(value = "/delete/{id}")
    @ApiOperation(value = "删除地区")
    public Result deleteTest(@PathVariable Integer id) {
        return iRegionsService.deleteTest(id);
    }

    @GetMapping(value = "/{id}/getRegionsByTenantId")
    @ApiOperation(value = "获取某地区的子地区")
    public Result getRegionsByTenantId(@PathVariable Integer id) {
        return iRegionsService.getRegionsByTenantId(id);
    }

    @GetMapping(value = "/getRegionsByRenterId")
    @ApiOperation(value = "获取某地区的子地区")
    public Result getRegionsByRenterId(@RequestParam(name = "renterId", required = false) String renterId) {
        return iRegionsService.getRegionsByRenterId(renterId);
    }



}
