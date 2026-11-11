package org.jeecg.modules.pcms.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMinFive;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMinSixty;
import org.jeecg.modules.pcms.energy.vo.DevicePointVo;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinFiveService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinSixtyService;
import org.jeecg.modules.pcms.project.util.DateFormatterUtils;
import org.jeecg.modules.pcms.project.util.DecimalFormatterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Api(tags = "测点历史数据分钟级别")
@RestController
@RequestMapping(value = "project/deviceEquipmentPointMin")
public class DeviceEquipmentPointMinController {


    @Autowired
    private IDeviceEquipmentPointMinFiveService iDeviceEquipmentPointMinFiveService;

    @Autowired
    private IDeviceEquipmentPointMinSixtyService iDeviceEquipmentPointMinSixtyService;



    @ApiOperation("根据设备key及条件查询分钟级别的测点值")
    @RequestMapping(value = "/deviceEquipmentPointMinList", method = RequestMethod.GET)
    public Result deviceEquipmentPointMinList(DevicePointVo devicePointVo) throws Exception {
        //调用登录接口获取token，获取下一步的电站列表
        String pointValue = devicePointVo.getPointValue();

        String pointTimeType = devicePointVo.getPointTimeType();

        String psKey = devicePointVo.getPsKey();
        String startDate = devicePointVo.getStartDate();
        String endDate = devicePointVo.getEndDate();
        String startDateFormat = DateFormatterUtils.convertToDatabaseFormat(startDate);
        String endDateFormat = DateFormatterUtils.convertToDatabaseFormat(endDate);

        //60分钟数据
        if("4".equals(pointTimeType)){
            QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointMinSixtyQueryWrapper = new QueryWrapper<>();
            deviceEquipmentPointMinSixtyQueryWrapper.select("time_stamp",pointValue);
            deviceEquipmentPointMinSixtyQueryWrapper.eq("ps_key",psKey);
            deviceEquipmentPointMinSixtyQueryWrapper.le("time_stamp",endDateFormat);
            deviceEquipmentPointMinSixtyQueryWrapper.ge("time_stamp",startDateFormat);
            List<DeviceEquipmentPointMinSixty> list = iDeviceEquipmentPointMinSixtyService.list(deviceEquipmentPointMinSixtyQueryWrapper);
            if(list != null){
                for (int i = 0; i < list.size(); i++) {
                    String timeStamp = list.get(i).getTimeStamp();
                    String p1 = list.get(i).getP1();
                    String p24 = list.get(i).getP24();
                    String p14 = list.get(i).getP14();
                    if(StringUtils.isNotBlank(p1)){
                        String s = DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p1);
                        list.get(i).setP1(s);
                    }
                    if(StringUtils.isNotBlank(p24)){
                        String s = DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p24);
                        list.get(i).setP24(s);
                    }
                    if(StringUtils.isNotBlank(p14)){
                        String s = DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p14);
                        list.get(i).setP14(s);
                    }
                    if(StringUtils.isNotBlank(timeStamp)){
                        String s = DateFormatterUtils.convertToFrontendFormat(timeStamp);
                        list.get(i).setTimeStamp(s);
                    }

                }
            }
            return Result.ok(list);
        }

        return Result.ok();

    }









}
