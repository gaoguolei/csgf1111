package org.jeecg.modules.pcms.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.entity.DeviceInventory;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.service.IDeviceInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api(tags = "库存模块")
@RestController
@RequestMapping(value = "project/deviceInventory")
public class DeviceInventoryController {


    @Autowired
    private IDeviceInventoryService iDeviceInventoryService;



    @GetMapping(value = "/pageList")
    public Result<?> queryPageList(DevicePageVo devicePageVo) {
        QueryWrapper<DeviceInventory> dispatchQueryWrapper = new QueryWrapper<>();

        //按照创建时间倒序
        dispatchQueryWrapper.orderByDesc("create_time");
        // 动态分页参数
        IPage<DeviceInventory> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        IPage<DeviceInventory> page = iDeviceInventoryService.page(pageData, dispatchQueryWrapper);
        return Result.ok(page);

    }

    /**
     * 手动添加库存
     * @param deviceInventory
     * @return
     */
    @PostMapping(value = "/addDeviceInventory")
    public Result<String> addDeviceInventory(@RequestBody DeviceInventory deviceInventory) {
        deviceInventory.setCreateTime(new Date());
        Instant instant = Instant.now();
        long timestamp = instant.toEpochMilli();
        deviceInventory.setInventoryNo("NO-"+timestamp);
        iDeviceInventoryService.save(deviceInventory);
        return Result.ok("新增成功");
    }

    /**
     * 修改
     * @param deviceInventory
     * @return
     */
    @PostMapping(value = "/updateDeviceInventory")
    public Result<String> updateDeviceInventory(@RequestBody DeviceInventory deviceInventory) {
        deviceInventory.setUpdateTime(new Date());
        iDeviceInventoryService.updateById(deviceInventory);
        return Result.ok("修改成功");
    }

    /**
     * 删除库存
     * @param deviceInventory
     * @return
     */
    @PostMapping(value = "/deleteDeviceInventory")
    public Result<String> deleteDeviceInventory(@RequestBody DeviceInventory deviceInventory) {
        Integer id = deviceInventory.getId();
        iDeviceInventoryService.removeById(id);
        return Result.ok("删除成功");
    }


    /**
     * 地址
     * @param devicePageVo
     * @return
     */
    @GetMapping(value = "/address")
    public Result<?> address(DevicePageVo devicePageVo) {
        List<String> strings = new ArrayList<>();
        strings.add("天津市蓟州区马伸桥镇");
        return Result.ok(strings);

    }



    /**
     * 资产型号
     * @param devicePageVo
     * @return
     */
    @GetMapping(value = "/assetModel")
    public Result<?> assetModel(DevicePageVo devicePageVo) {

        List<String> strings = new ArrayList<>();
        strings.add("SG36CX-P3-CN");
        strings.add("SG110CX-P2-CN");
        strings.add("SG30T-P2-CN");
        strings.add("SG50CX-P2-CN");
        return Result.ok(strings);

    }


    /**
     * 供应商
     * @param devicePageVo
     * @return
     */
    @GetMapping(value = "/supplier")
    public Result<?> supplier(DevicePageVo devicePageVo) {
        List<String> strings = new ArrayList<>();
        strings.add("华为数字能源");
        strings.add("阳关能源");
        strings.add("古瑞瓦特新能源");
        return Result.ok(strings);

    }



}
