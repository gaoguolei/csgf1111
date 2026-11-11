package org.jeecg.modules.pcms.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentDispatch;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentDispatchService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Api(tags = "文件操作接口")
@RestController
@RequestMapping(value = "project/deviceEquipmentDispatch")
public class DeviceEquipmentDispatchController {

    @Autowired
    private IDeviceEquipmentDispatchService iDeviceEquipmentDispatchService;

    @Autowired
    private ISysUserService iSysUserService;

    @Autowired
    private IDeviceService iDeviceService;

    @GetMapping(value = "/pageList")
    public Result<?> queryPageList(DevicePageVo devicePageVo) {
        QueryWrapper<DeviceEquipmentDispatch> dispatchQueryWrapper = new QueryWrapper<>();

        String dispatchNo = devicePageVo.getDispatchNo();
        String faultType = devicePageVo.getFaultType();
        String dispatchStatus = devicePageVo.getDispatchStatus();
        String dispatchType = devicePageVo.getDispatchType();

        String plantId = devicePageVo.getDeviceId();

        String renterId = devicePageVo.getRenterId();

        //派单号
        if(StringUtils.isNotBlank(dispatchNo)){
            dispatchQueryWrapper.like("dispatch_no",dispatchNo);
        }
        if(StringUtils.isNotBlank(plantId)){
            dispatchQueryWrapper.eq("device_id",plantId);
        }
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            List<Long> psIdsByProjectType = iDeviceService.getPsIdsByProjectType(renterId);
            if(psIdsByProjectType != null && psIdsByProjectType.size() > 0){
                dispatchQueryWrapper
                        .in("device_id",psIdsByProjectType)
                        .or()
                        .eq("renter_id",renterId);
            }
        }
        //故障类型
        if(StringUtils.isNotBlank(faultType)){
            dispatchQueryWrapper.like("fault_type",faultType);
        }
        //工单状态0：待分配 ；1：已分配；3：已完成
        if(StringUtils.isNotBlank(dispatchStatus)){
            dispatchQueryWrapper.eq("dispatch_status",dispatchStatus);
        }
        //生成类型1：自动生成（异常工单）；1：手动添加
        if(StringUtils.isNotBlank(dispatchType)){
            dispatchQueryWrapper.eq("dispatch_type",dispatchType);
        }
        //按照创建时间倒序
        dispatchQueryWrapper.orderByDesc("create_time");
        // 动态分页参数
        IPage<DeviceEquipmentDispatch> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );


        IPage<DeviceEquipmentDispatch> page = iDeviceEquipmentDispatchService.page(pageData, dispatchQueryWrapper);

        if(page != null ){
            List<DeviceEquipmentDispatch> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    String userId = records.get(i).getUserId();
                    String deviceId = records.get(i).getDeviceId();
                    if(StringUtils.isNotBlank(deviceId)){
                        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
                        deviceQueryWrapper.eq("device_id",deviceId);
                        Device one = iDeviceService.getOne(deviceQueryWrapper);
                        if(records.get(i).getDispatchType().equals("2")){
                            if(one != null){
                                records.get(i).setAddress(StringUtils.isNotBlank(one.getPsLocation())? one.getPsLocation() : "");
                                records.get(i).setPsName(StringUtils.isNotBlank(one.getDeviceName())? one.getDeviceName() : "");
                            }
                        }
                        if(records.get(i).getDispatchType().equals("1")){
                            if(one != null){
                                records.get(i).setAddress(StringUtils.isNotBlank(one.getPsLocation())? one.getPsLocation() : "");
                            }
                        }

                    }
                    SysUser byId = iSysUserService.getById(userId);
                    if(byId != null){
                        records.get(i).setUserName(byId.getRealname());
                    }
                }
            }
            page.setRecords(records);
        }

        return Result.ok(page);
    }

    /**
     * 手动添加工单
     * @param deviceEquipmentDispatch
     * @return
     */
    @PostMapping(value = "/addDispatch")
    public Result<String> addDispatch(@RequestBody DeviceEquipmentDispatch deviceEquipmentDispatch) {
        deviceEquipmentDispatch.setCreateTime(new Date());
        Instant instant = Instant.now();
        long timestamp = instant.toEpochMilli();
        String userId = deviceEquipmentDispatch.getUserId();
        if(StringUtils.isNotBlank(userId)){
            deviceEquipmentDispatch.setDispatchStatus("1");
        }
        deviceEquipmentDispatch.setDispatchNo(String.valueOf(timestamp));
        iDeviceEquipmentDispatchService.save(deviceEquipmentDispatch);
        return Result.ok("新增成功");
    }


    /**
     * 修改工单
     * @param deviceEquipmentDispatch
     * @return
     */
    @PostMapping(value = "/updateDispatch")
    public Result<String> updateDispatch(@RequestBody DeviceEquipmentDispatch deviceEquipmentDispatch) {
        deviceEquipmentDispatch.setUpdateTime(new Date());
        iDeviceEquipmentDispatchService.updateById(deviceEquipmentDispatch);
        return Result.ok();
    }






















}
