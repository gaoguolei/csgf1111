package org.jeecg.modules.pcms.api.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import okhttp3.*;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.dto.solar.DeviceResultDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.entity.*;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentDayExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentYearExportVO;
import org.jeecg.modules.pcms.energy.vo.*;
import org.jeecg.modules.pcms.energyjob.DeviceGatherGrwJob;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.util.DateFormatterUtils;
import org.jeecg.modules.pcms.project.util.DecimalFormatterUtils;
import org.jeecg.modules.pcms.util.StringMultiplier;
import org.jeecg.modules.pcms.util.StringUnitUtils;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "电站下设备录入")
@RestController
@RequestMapping(value = "project/deviceEquipment")
public class DeviceEquipmentController {


    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationEquipments}")
    private String stationEquipmentsUrl;

    @Value("${growatt.deviceList}")
    private String deviceGrwListUrl;

    @Value("${growatt.token}")
    private String growattToken;

    @Value("${solar.username}")
    private String username;
    @Value("${solar.password}")
    private String password;
    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Autowired
    private IDeviceService iDeviceService;


    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;

    @Autowired
    private IDevicePointDayService iDevicePointDayService;


    @Autowired
    private IDeviceEquipmentPointService iDeviceEquipmentPointService;


    @Autowired
    private IDeviceFaultService iDeviceFaultService;

    @Autowired
    private IDeviceFaultHistoryService iDeviceFaultHistoryService;


    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;

    @Autowired
    private IDeviceEquipmentPointMinFiveService iDeviceEquipmentPointMinFiveService;
    @Autowired
    private IDeviceEquipmentPointMinFiveGrwService iDeviceEquipmentPointMinFiveGrwService;


    @Autowired
    private IDeviceEquipmentPointMinFifteenService iDeviceEquipmentPointMinFifteenService;
    @Autowired
    private IDeviceEquipmentPointMinThirtyService iDeviceEquipmentPointMinThirtyService;
    @Autowired
    private IDeviceEquipmentPointMinSixtyService iDeviceEquipmentPointMinSixtyService;

    @Autowired
    private IDeviceEquipmentPointDayService iDeviceEquipmentPointDayService;

    @Autowired
    private IDeviceEquipmentPointDayGrwService iDeviceEquipmentPointDayGrwService;

    @Autowired
    private IDeviceEquipmentPointMonthService iDeviceEquipmentPointMonthService;

    @Autowired
    private IDeviceEquipmentPointYearService iDeviceEquipmentPointYearService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IDeviceGatherGrwService iDeviceGatherGrwService;




    @ApiOperation("条件查询设备列表分页")
    @RequestMapping(value = "/deviceEquipmentPageList", method = RequestMethod.GET)
    public Result deviceEquipmentPageList(DevicePageVo devicePageVo) throws Exception {

        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        // 动态分页参数
        IPage<DeviceEquipment> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        //查询条件 根据电站id查询
        String plantId = devicePageVo.getPlantId();
        if(StringUtils.isNotBlank(plantId)){
            deviceEquipmentQueryWrapper.eq("ps_id",plantId);
        }
        String renterId = devicePageVo.getRenterId();
        //先查询出来各个项目下的电站id集合 在in查询一下
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            List<Long> psIdsByProjectType = iDeviceService.getPsIdsByProjectType(renterId);
            if(psIdsByProjectType != null && psIdsByProjectType.size() > 0){
                deviceEquipmentQueryWrapper.in("ps_id",psIdsByProjectType);
            }
        }

        //查询条件 根据设备类型查询
        String deviceType = devicePageVo.getDeviceType();
        if(StringUtils.isNotBlank(deviceType)){
            deviceEquipmentQueryWrapper.eq("device_type",deviceType);
        }
        //厂商
        Integer source = devicePageVo.getSource();
        if(source != null){
            deviceEquipmentQueryWrapper.eq("source",source);
        }
        //查询条件 设备sn或者设备名
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();
        if (StringUtils.isNotBlank(deviceNameOrSn)) {
            deviceEquipmentQueryWrapper.and(wrapper ->
                    wrapper.like("device_name", deviceNameOrSn)
                            .or()
                            .like("device_sn", deviceNameOrSn)
            );
        }
        //查询条件 设备型号
        String equipmentModelName = devicePageVo.getEquipmentModelName();
        if(StringUtils.isNotBlank(equipmentModelName)){
            deviceEquipmentQueryWrapper.eq("device_model_code",equipmentModelName);
        }
        IPage<DeviceEquipment> page = iDeviceEquipmentService.page(pageData, deviceEquipmentQueryWrapper);
        if(page != null){
            List<DeviceEquipment> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    Long psId = records.get(i).getPsId();
                    QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
                    deviceQueryWrapper.eq("device_id",psId);
                    Device plant = iDeviceService.getOne(deviceQueryWrapper);
                    if(plant != null ){
                        records.get(i).setPlantName(StringUtils.isNotBlank(plant.getDeviceName()) ? plant.getDeviceName(): "");
                    }
                    //赋值设备当日发电和总发电
                    String psKey = records.get(i).getPsKey();
                    DeviceEquipmentPoint deviceEquipmentByPsKey = iDeviceEquipmentPointService.getDeviceEquipmentByPsKey(psKey);
                    if(deviceEquipmentByPsKey != null){
                        String p1 = deviceEquipmentByPsKey.getP1();
                        if(StringUtils.isNotBlank(p1)){
                            String s = DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p1);
                            records.get(i).setDayPower(s);
                        }
                        String p24 = deviceEquipmentByPsKey.getP24();
                        if(StringUtils.isNotBlank(p24)){
                            String s = StringUnitUtils.divideByThousand(p24);
                            records.get(i).setTotalActivePower(s);
                        }

                    }
                }
            }
            page.setRecords(records);
            return Result.ok(page);
        }
        return Result.ok(page);
    }

    @ApiOperation("条件查询从采集器列表分页")
    @RequestMapping(value = "deviceGathePageList", method = RequestMethod.GET)
    public Result deviceGathePageList(DevicePageVo devicePageVo) throws Exception {

        QueryWrapper<DeviceGatherGrw> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        // 动态分页参数
        IPage<DeviceGatherGrw> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        //查询条件 根据电站id查询
        String plantId = devicePageVo.getPlantId();
        if(StringUtils.isNotBlank(plantId)){
            deviceEquipmentQueryWrapper.eq("ps_id",plantId);
        }
        String renterId = devicePageVo.getRenterId();
        //先查询出来各个项目下的电站id集合 在in查询一下
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            List<Long> psIdsByProjectType = iDeviceService.getPsIdsByProjectType(renterId);
            if(psIdsByProjectType != null && psIdsByProjectType.size() > 0){
                deviceEquipmentQueryWrapper.in("ps_id",psIdsByProjectType);
            }
        }

        //查询条件 根据设备类型查询
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();
        if (StringUtils.isNotBlank(deviceNameOrSn)) {
            deviceEquipmentQueryWrapper.and(wrapper ->
                    wrapper.like("device_name", deviceNameOrSn)
                            .or()
                            .like("device_sn", deviceNameOrSn)
            );
        }
        //查询条件 设备型号
        String equipmentModelName = devicePageVo.getEquipmentModelName();
        if(StringUtils.isNotBlank(equipmentModelName)){
            deviceEquipmentQueryWrapper.eq("model",equipmentModelName);
        }
        IPage<DeviceGatherGrw> page = iDeviceGatherGrwService.page(pageData, deviceEquipmentQueryWrapper);
        if(page != null){
            List<DeviceGatherGrw> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    Integer psId = records.get(i).getPsId();
                    QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
                    deviceQueryWrapper.eq("device_id",psId);
                    Device plant = iDeviceService.getOne(deviceQueryWrapper);
                    if(plant != null ){
                        records.get(i).setPlantName(StringUtils.isNotBlank(plant.getDeviceName()) ? plant.getDeviceName(): "");
                    }
                    records.get(i).setEquipmentName("采集器");
                    records.get(i).setSource("2");
                    //赋值设备当日发电和总发电
                }
            }
            page.setRecords(records);
            return Result.ok(page);
        }
        return Result.ok(page);
    }


    @ApiOperation("获取全部电站或者设备型号")
    @RequestMapping(value = "/deviceEquipmentOrModelName", method = RequestMethod.GET)
    public Result deviceEquipmentOrModelName(DevicePageVo devicePageVo) throws Exception {
        Map<String, Object> objectHashMap = new HashMap<>();
        String queryType = devicePageVo.getQueryType();
        if(StringUtils.isNotBlank(queryType)){
            //获取的是电站的数据
            String renterId = devicePageVo.getRenterId();
            List<DeviceResultDto> resultList = iDeviceService.getDeviceNameGrw(renterId);
            if(resultList != null && resultList.size()>0) {
                objectHashMap.put("deviceList", resultList);
            }
            //获取的设备型号的数据
//            QueryWrapper<DeviceGatherGrw> queryWrapper = new QueryWrapper<>();
//            //逆变器数据
//            queryWrapper.eq("device_type","1");
//            // 选择需要分组的字段
//            queryWrapper.select("DISTINCT device_model_code");
//            // 执行查询，使用selectObjs只获取device_model_code字段的值
//            List<DeviceGatherGrw> objects = iDeviceGatherGrwService.list(queryWrapper);
//            ArrayList<String> strings = new ArrayList<>();
//            objects.stream().forEach(i ->{
//                strings.add(i.getModel());
//            });
//            objectHashMap.put("deviceInverter",strings);

            //获取的设备型号的数据
            QueryWrapper<DeviceGatherGrw> queryWrapperTwo = new QueryWrapper<>();
            // 选择需要分组的字段
            queryWrapperTwo.select("DISTINCT model");
            // 执行查询，使用selectObjs只获取device_model_code字段的值
            List<DeviceGatherGrw> objectsTwo = iDeviceGatherGrwService.list(queryWrapperTwo);
            ArrayList<String> stringsTwo = new ArrayList<>();
            objectsTwo.stream().forEach(i ->{
                stringsTwo.add(i.getModel());
            });
            objectHashMap.put("deviceEquipment",stringsTwo);
            return Result.ok(objectHashMap);
        }
        //获取的是电站的数据
        String renterId = devicePageVo.getRenterId();
        List<DeviceResultDto> resultList = iDeviceService.getDeviceName(renterId);
        if(resultList != null && resultList.size()>0) {
            objectHashMap.put("deviceList", resultList);
        }
        //获取的设备型号的数据
        QueryWrapper<DeviceEquipment> queryWrapper = new QueryWrapper<>();
        //逆变器数据
        queryWrapper.eq("device_type","1");
        // 选择需要分组的字段
        queryWrapper.select("DISTINCT device_model_code");
        // 执行查询，使用selectObjs只获取device_model_code字段的值
        List<DeviceEquipment> objects = iDeviceEquipmentService.list(queryWrapper);
        ArrayList<String> strings = new ArrayList<>();
        objects.stream().forEach(i ->{
            strings.add(i.getDeviceModelCode());
        });
        objectHashMap.put("deviceInverter",strings);

        //获取的设备型号的数据
        QueryWrapper<DeviceEquipment> queryWrapperTwo = new QueryWrapper<>();
        //逆变器数据
        queryWrapperTwo.eq("device_type","22");
        // 选择需要分组的字段
        queryWrapperTwo.select("DISTINCT device_model_code");
        // 执行查询，使用selectObjs只获取device_model_code字段的值
        List<DeviceEquipment> objectsTwo = iDeviceEquipmentService.list(queryWrapperTwo);
        ArrayList<String> stringsTwo = new ArrayList<>();
        objectsTwo.stream().forEach(i ->{
            stringsTwo.add(i.getDeviceModelCode());
        });
        objectHashMap.put("deviceEquipment",stringsTwo);
        return Result.ok(objectHashMap);
    }


    @ApiOperation("根据电站设备的sn码或者psKey获取故障列表")
    @RequestMapping(value = "/deviceEquipmentFaultListBySn", method = RequestMethod.GET)
    public Result deviceEquipmentFaultListBySn(DevicePageVo devicePageVo) throws Exception {
        String devicePsKye = devicePageVo.getDevicePsKey();
        String deviceEquipmentSn = devicePageVo.getDeviceEquipmentSn();
        String tabType = devicePageVo.getTabType();
        Integer psSource = devicePageVo.getPsSource();
        String faultReason = devicePageVo.getFaultReason();
        String faultName = devicePageVo.getFaultName();
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();

        //实时数据
        if("1".equals(tabType)){
            QueryWrapper<DeviceFault> deviceFaultQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(startDate)){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
                deviceFaultQueryWrapper.ge("create_time",startTime);
            }
            if(StringUtils.isNotBlank(endDate)){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime endTime = LocalDateTime.parse(endDate, formatter);
                deviceFaultQueryWrapper.le("create_time",endTime);
            }
            if(StringUtils.isNotBlank(faultReason)){
                deviceFaultQueryWrapper.like("fault_reason",faultReason);
            }
            if(StringUtils.isNotBlank(faultName)){
                deviceFaultQueryWrapper.like("fault_name",faultName);
            }

            if(psSource == 1){
                deviceFaultQueryWrapper.eq("ps_key",devicePsKye);
            }
            if(psSource == 2){
                deviceFaultQueryWrapper.eq("device_sn",deviceEquipmentSn);
            }
            List<DeviceFault> list = iDeviceFaultService.list(deviceFaultQueryWrapper);
            if(list != null && list.size()>0){
                return Result.ok(list);
            }
        }
        //查询历史数据
        if("2".equals(tabType)){
            QueryWrapper<DeviceFaultHistory> deviceFaultQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(startDate)){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
                deviceFaultQueryWrapper.ge("create_time",startTime);
            }
            if(StringUtils.isNotBlank(endDate)){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime endTime = LocalDateTime.parse(endDate, formatter);
                deviceFaultQueryWrapper.le("create_time",endTime);
            }
            if(StringUtils.isNotBlank(faultReason)){
                deviceFaultQueryWrapper.like("fault_reason",faultReason);
            }
            if(StringUtils.isNotBlank(faultName)){
                deviceFaultQueryWrapper.like("fault_name",faultName);
            }

            if(psSource == 1){
                deviceFaultQueryWrapper.eq("ps_key",devicePsKye);
            }
            if(psSource == 2){
                deviceFaultQueryWrapper.eq("device_sn",deviceEquipmentSn);
            }
            List<DeviceFaultHistory> list = iDeviceFaultHistoryService.list(deviceFaultQueryWrapper);
            if(list != null && list.size()>0){
                return Result.ok(list);
            }
        }

        return Result.ok();
    }


    @ApiOperation("设备报表(包括天里面 5分钟，15分钟，60分钟的数据；月里面每天的数据，年里面每月的数据，总计里面每年的数据)")
    @RequestMapping(value = "/getDeviceEquipmentReport", method = RequestMethod.POST)
    public Result getDeviceEquipmentReport(@org.springframework.web.bind.annotation.RequestBody DevicePageVo devicePageVo) throws Exception {
        //电站的pskey 根据电站的psk获取设备的pskey
        //厂商来源 1：阳光云，2：古瑞瓦特
        Integer source = devicePageVo.getSource();
        List<String> psKey = devicePageVo.getPsKey();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        String renterId = devicePageVo.getRenterId();
        String isAll = devicePageVo.getIsAll();
        List<String> equipmentPsKeyList =new ArrayList<>();
        List<String> equipmentNotPsKeyList =new ArrayList<>();
        // 阳光云
        if(source == 1){
            if( psKey != null && psKey.size()>0){
                QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
                deviceEquipmentQueryWrapper.in("ps_id",psKey);
                List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
                if(list != null && list.size()>0){
                    equipmentPsKeyList=list.stream()
                            .map(DeviceEquipment::getPsKey)
                            .collect(Collectors.toList());
                }

            }
            if(StringUtils.isNotBlank(isAll)){
                List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
                deviceEquipmentQueryWrapper.in("ps_id",psKeyList);
                if( notInPsKey != null && notInPsKey.size()>0){
                    deviceEquipmentQueryWrapper.notIn("ps_id",notInPsKey);
                }
                deviceEquipmentQueryWrapper.eq("device_type","1");
                deviceEquipmentQueryWrapper.eq("source","1");
                List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
                if(list != null && list.size()>0){
                    equipmentNotPsKeyList=list.stream()
                            .map(DeviceEquipment::getPsKey)
                            .collect(Collectors.toList());
                }
            }

        }
        if(source == 2){
            if( psKey != null && psKey.size()>0){
                QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
                deviceEquipmentQueryWrapper.in("ps_id",psKey);
                List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);

                if(list != null && list.size()>0){
                    equipmentPsKeyList=list.stream()
                            .map(DeviceEquipment::getDeviceSn)
                            .collect(Collectors.toList());
                }

            }
            if(StringUtils.isNotBlank(isAll)){
                QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
                List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
                deviceEquipmentQueryWrapper.in("ps_id",psKeyListGrw);
                if( notInPsKey != null && notInPsKey.size()>0){
                    deviceEquipmentQueryWrapper.notIn("ps_id",notInPsKey);

                }
                List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);

                if(list != null && list.size()>0){
                    equipmentNotPsKeyList=list.stream()
                            .map(DeviceEquipment::getDeviceSn)
                            .collect(Collectors.toList());
                }
            }

        }

        //全选中之后的取消部分选中.
        String minuteInterval = devicePageVo.getMinuteInterval();
        String minType = devicePageVo.getMinType();
        String queryDate = devicePageVo.getQueryDate();
        //是否全选
        //日
        if(notInPsKey == null && psKey == null && StringUtils.isBlank(isAll)){
            return Result.ok();
        }
        //某天内的数据
        if("day".equals(minType)){
            //5分钟数据
            if("5".equals(minuteInterval)){
                // 动态分页参数
                IPage<DeviceEquipmentPoint> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DeviceEquipmentPoint> deviceEquipmentPointQueryWrapper = new QueryWrapper<>();

                //电站全选中
                if(StringUtils.isNotBlank(isAll)){
                    //电站全选中，但取消选中了一部分
                    if(notInPsKey != null && notInPsKey.size()>0){
                        //拿到电站所属的设备psk集合
                        List<Long> integers = iDeviceDetailSolarService.notInPsKey(notInPsKey);
                        deviceEquipmentPointQueryWrapper.in("ps_id",integers);
                    }
                }
                //多选一部分
                if(psKey != null && psKey.size()>0){
                    List<Long> integers = iDeviceDetailSolarService.inPsKey(psKey);
                    if(integers != null && integers.size()>0){
                        deviceEquipmentPointQueryWrapper.in("ps_id",integers);
                    }
                }


                IPage<DeviceEquipmentPoint> page = iDeviceEquipmentPointService.page(pageData, deviceEquipmentPointQueryWrapper);
                if(page != null){
                    List<DeviceEquipmentPoint> records = page.getRecords();
                    String replace = queryDate.replace("-", "");
                    String dbDateStart = convertToDbDateFormat(queryDate, "start");
                    String dbDateEnd = convertToDbDateFormat(queryDate, "end");
                    for (int i = 0; i < records.size(); i++) {

                        String equipmentPsKey = records.get(i).getPsKey();
                        Integer psId = records.get(i).getPsId();
                        DeviceReturnVo psNameByPsId = iDeviceService.getPsNameByPsId(psId);
                        String plantPsKey = psNameByPsId.getPsKey();
                        QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
                        devicePointDayQueryWrapper.eq("ps_key",plantPsKey);
                        devicePointDayQueryWrapper.eq("time_stamp",replace);
                        DevicePointDay one = iDevicePointDayService.getOne(devicePointDayQueryWrapper);
                        if(one != null){
                            String p83022 = one.getP83022();
                            if(StringUtils.isNotBlank(p83022)){
                                records.get(i).setDayTotal(DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022));
                            }
                        }

                        QueryWrapper<DeviceEquipmentPointMinFive> deviceEquipmentPointMinFiveQueryWrapper = new QueryWrapper<>();
                        deviceEquipmentPointMinFiveQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                        deviceEquipmentPointMinFiveQueryWrapper.eq("ps_key",equipmentPsKey);
                        List<DeviceEquipmentPointMinFive> list = iDeviceEquipmentPointMinFiveService.list(deviceEquipmentPointMinFiveQueryWrapper);
                        if(list != null && list.size()>0){
                            for (int j = 0; j < list.size(); j++) {
                                String timeStamp = list.get(j).getTimeStamp();
                                String s = DateFormatterUtils.formatTimeString(timeStamp);
                                list.get(j).setFormatTime(s);
                            }
                            records.get(i).setDeviceEquipmentPointMinFive(list);
                        }
                        DeviceEquipment deviceEquipmentByPsKey = iDeviceEquipmentService.getDeviceEquipmentByPsKey(equipmentPsKey);
                        if(deviceEquipmentByPsKey != null){
                            records.get(i).setDeviceModelCode(StringUtils.isNotBlank(deviceEquipmentByPsKey.getDeviceModelCode()) ? deviceEquipmentByPsKey.getDeviceModelCode() : "");
                            records.get(i).setEquipmentStatus(StringUtils.isNotBlank(deviceEquipmentByPsKey.getDevStatus()) ? deviceEquipmentByPsKey.getDevStatus() : "");
                        }
                        if(psNameByPsId != null){
                            records.get(i).setAddress(StringUtils.isNotBlank(psNameByPsId.getPsLocation()) ? psNameByPsId.getPsLocation() : "");
                            records.get(i).setPlantName(StringUtils.isNotBlank(psNameByPsId.getDeviceName()) ? psNameByPsId.getDeviceName() : "");
                        }
                    }

                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            //15分钟数据

            //30分钟数据

            //60分钟数据
            if("60".equals(minuteInterval)){
                // 动态分页参数
                if(source == 1){
//                    QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointMinSixtyQueryWrapper = new QueryWrapper<>();
//                    if(StringUtils.isNotBlank(isAll)){
//                        if(notInPsKey != null && notInPsKey.size()>0){
//                            deviceEquipmentPointMinSixtyQueryWrapper.notIn("ps_key",equipmentNotPsKeyList);
//                        }
//                    }
//
//                    if(psKey != null && psKey.size()>0){
//                        deviceEquipmentPointMinSixtyQueryWrapper.in("ps_key",equipmentPsKeyList);
//                    }
//
//                    if(StringUtils.isNotBlank(queryDate)){
//                        LocalDate date = LocalDate.parse(queryDate);
//                        String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//                        deviceEquipmentPointMinSixtyQueryWrapper.like("time_stamp",yyyyMMdd);
//                    }
//                    List<DeviceEquipmentPointMinSixty> list = iDeviceEquipmentPointMinSixtyService.list(deviceEquipmentPointMinSixtyQueryWrapper);
//                    if(list != null && list.size()>0){
//                        List<DeviceEquipmentPointMinSixtyFlat> flattenedList = flattenDataMinSixTy(list);
//                        // 创建一个新的分页对象，使用扁平化后的列表
//                        Page<DeviceEquipmentPointMinSixtyFlat> flattenedPage = new Page<>();
//                        // 这里假设你的Page类有相应的方法，我们设置分页信息
//                        flattenedPage.setRecords(flattenedList);
//                        flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
//                        flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
//                        flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
//                        return Result.ok(flattenedPage);
//                    }
                    // 动态分页参数
                    QueryWrapper<DeviceEquipmentPointDay> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(isAll)){
                        deviceEquipmentPointDayQueryWrapper.in("ps_key",equipmentNotPsKeyList);
                    }

                    if(psKey != null && psKey.size()>0){
                        deviceEquipmentPointDayQueryWrapper.in("ps_key",equipmentPsKeyList);
                    }

                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        deviceEquipmentPointDayQueryWrapper.eq("time_stamp",yyyyMM);
                    }
                    List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(deviceEquipmentPointDayQueryWrapper);
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {

                        }
                        List<DeviceEquipmentPointFlat> flattenedList = flattenData(list);
                        // 创建一个新的分页对象，使用扁平化后的列表
                        Page<DeviceEquipmentPointFlat> flattenedPage = new Page<>();
                        // 这里假设你的Page类有相应的方法，我们设置分页信息
                        flattenedPage.setRecords(flattenedList);
                        flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                        flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                        flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                        return Result.ok(flattenedPage);
                    }
                }
                //古瑞瓦特
                if(source == 2){
                    // 动态分页参数
                    QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(isAll)){
                        deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
                    }

                    if(psKey != null && psKey.size()>0){
                        deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
                    }

                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        deviceEquipmentPointDayGrwQueryWrapper.eq("time_stamp",yyyyMM);
                    }
                    List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {
                            String p1 = list.get(i).getP1();
                            if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                                String s = StringMultiplier.multiplyAndFormat(p1);
                                list.get(i).setP1(s);
                            }
                        }
                        List<DeviceEquipmentPointGrwFlat> flattenedList = flattenDataGrw(list);
                        // 创建一个新的分页对象，使用扁平化后的列表
                        Page<DeviceEquipmentPointGrwFlat> flattenedPage = new Page<>();
                        // 这里假设你的Page类有相应的方法，我们设置分页信息
                        flattenedPage.setRecords(flattenedList);
                        flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                        flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                        flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                        return Result.ok(flattenedPage);
                    }
                }
            }

        }
        //月数据（查询某年某月的每一天的数据）
        if("month".equals(minType)){
            if(source == 1){
                // 动态分页参数
                QueryWrapper<DeviceEquipmentPointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    devicePointDayQueryWrapper.in("ps_key",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    devicePointDayQueryWrapper.in("ps_key",equipmentPsKeyList);
                }

                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
                    devicePointDayQueryWrapper.like("time_stamp",yyyyMM);
                }
                List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(devicePointDayQueryWrapper);
                if(list != null && list.size()>0){
                    List<DeviceEquipmentPointFlat> flattenedList = this.flattenData(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
            //古瑞瓦特
            if(source == 2){
                // 动态分页参数
                QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
                }

                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
                }
                List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                if(list != null && list.size()>0){
                    for (int i = 0; i < list.size(); i++) {
                        String p1 = list.get(i).getP1();
                        if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                            String s = StringMultiplier.multiplyAndFormat(p1);
                            list.get(i).setP1(s);
                        }
                    }
                    List<DeviceEquipmentPointGrwFlat> flattenedList = flattenDataGrw(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointGrwFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
        }


        //年数据（查询某年的月份数据）
        if("year".equals(minType)){
            if(source == 1){
                // 动态分页参数
                QueryWrapper<DeviceEquipmentPointMonth> deviceEquipmentPointMonthQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointMonthQueryWrapper.in("ps_key",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointMonthQueryWrapper.in("ps_key",equipmentPsKeyList);
                }

                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                    deviceEquipmentPointMonthQueryWrapper.like("time_stamp",yyyy);
                }

                List<DeviceEquipmentPointMonth> list = iDeviceEquipmentPointMonthService.list(deviceEquipmentPointMonthQueryWrapper);

                if(list != null && list.size()>0){
                    List<DeviceEquipmentPointFlatMonth> flattenedList = flattenDataMonth(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointFlatMonth> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }

            //古瑞瓦特年数据
            if(source == 2 ){
                QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
                }

                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy"));
                    deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
                }
                List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                //处理一下逻辑，把这一年里面的数据按照字段time_stamp分组,分组逻辑是把类似“2025-11-11” 按照月份来分组，然后在计算一下这个组里面的字段p1的值计算和，再返回list集合
                List<DeviceEquipmentPointDayGrw> list = deviceEquipmentPointDayGrws.stream()
                        .collect(Collectors.groupingBy(
                                item -> {
                                    // 创建复合键：月份 + device_sn
                                    String month = item.getTimeStamp().substring(0, 7); // 提取年月 "2025-11"
                                    String deviceSn = item.getDeviceSn();
                                    return month + "|" + deviceSn; // 使用分隔符避免冲突
                                },
                                Collectors.summingDouble(item -> {
                                    try {
                                        return item.getP1() != null ? Double.parseDouble(item.getP1()) : 0.0;
                                    } catch (NumberFormatException e) {
                                        return 0.0;
                                    }
                                })
                        ))
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            // 解析复合键
                            String[] keys = entry.getKey().split("\\|");
                            String month = keys[0];
                            String deviceSn = keys[1];

                            // 创建新的汇总对象
                            DeviceEquipmentPointDayGrw summary = new DeviceEquipmentPointDayGrw();
                            summary.setTimeStamp(month);        // 设置月份 "2025-11"
                            summary.setDeviceSn(deviceSn);      // 设置设备序列号
                            summary.setP1(String.format("%.2f", entry.getValue())); // 设置求和结果，保留2位小数

                            return summary;
                        })
                        .collect(Collectors.toList());
                    if(list != null && list.size()>0){
                    for (int i = 0; i < list.size(); i++) {
                        String p1 = list.get(i).getP1();
                        if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                            String s = StringMultiplier.multiplyAndFormat(p1);
                            list.get(i).setP1(s);
                        }
                    }
                    List<DeviceEquipmentPointGrwFlat> flattenedList = flattenDataGrw(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointGrwFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
        }

        //总数据（查询总的数据）
        if("all".equals(minType)){
            // 动态分页参数阳光云
            if(source == 1){
                QueryWrapper<DeviceEquipmentPointYear> deviceEquipmentPointYearQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointYearQueryWrapper.in("ps_key",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointYearQueryWrapper.in("ps_key",equipmentPsKeyList);
                }

                List<DeviceEquipmentPointYear> list = iDeviceEquipmentPointYearService.list(deviceEquipmentPointYearQueryWrapper);
                if(list != null && list.size()>0){
                    List<DeviceEquipmentPointFlatYear> flattenedList = flattenDataYear(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointFlatYear> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
            if(source == 2){
                QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
                }

                List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                //处理一下逻辑，把这一年里面的数据按照字段time_stamp分组,分组逻辑是把类似“2025-11-11” 按照月份来分组，然后在计算一下这个组里面的字段p1的值计算和，再返回list集合
                // 按年份和device_sn分组，计算p1字段的和
                List<DeviceEquipmentPointDayGrw> list = deviceEquipmentPointDayGrws.stream()
                        .collect(Collectors.groupingBy(
                                item -> {
                                    // 创建复合键：年份 + device_sn
                                    String year = item.getTimeStamp().substring(0, 4); // 提取年份 "2025"
                                    String deviceSn = item.getDeviceSn();
                                    return year + "|" + deviceSn; // 使用分隔符避免冲突
                                },
                                Collectors.summingDouble(item -> {
                                    try {
                                        return item.getP1() != null ? Double.parseDouble(item.getP1()) : 0.0;
                                    } catch (NumberFormatException e) {
                                        return 0.0;
                                    }
                                })
                        ))
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            // 解析复合键
                            String[] keys = entry.getKey().split("\\|");
                            String year = keys[0];
                            String deviceSn = keys[1];

                            // 创建新的汇总对象
                            DeviceEquipmentPointDayGrw summary = new DeviceEquipmentPointDayGrw();
                            summary.setTimeStamp(year);         // 设置年份 "2025"
                            summary.setDeviceSn(deviceSn);      // 设置设备序列号
                            summary.setP1(String.format("%.2f", entry.getValue())); // 设置年度求和结果，保留2位小数

                            return summary;
                        })
                        .collect(Collectors.toList());
                if(list != null && list.size()>0){
                    for (int i = 0; i < list.size(); i++) {
                        String p1 = list.get(i).getP1();
                        if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                            String s = StringMultiplier.multiplyAndFormat(p1);
                            list.get(i).setP1(s);
                        }
                    }
                    List<DeviceEquipmentPointGrwFlat> flattenedList = flattenDataGrw(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointGrwFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }

        }

        //自定义数据
        if("customize".equals(minType)){
            // 动态分页参数
            if(source == 1){
                QueryWrapper<DeviceEquipmentPointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    devicePointDayQueryWrapper.in("ps_key",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    devicePointDayQueryWrapper.in("ps_key",equipmentPsKeyList);
                }
                String startDate = devicePageVo.getStartDate();
                String endDate = devicePageVo.getEndDate();
                LocalDate start = LocalDate.parse(startDate);
                String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalDate end = LocalDate.parse(endDate);
                String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                devicePointDayQueryWrapper.ge("time_stamp",startTime);
                devicePointDayQueryWrapper.le("time_stamp",endTime);

                List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(devicePointDayQueryWrapper);
                if(list != null && list.size()>0){
                    List<DeviceEquipmentPointFlat> flattenedList = flattenData(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
            //古瑞瓦特
            if(source == 2){
                QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
                }

                if(psKey != null && psKey.size()>0){
                    deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
                }

                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
                }
                String startDate = devicePageVo.getStartDate();
                String endDate = devicePageVo.getEndDate();
                LocalDate start = LocalDate.parse(startDate);
                String startTime = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate end = LocalDate.parse(endDate);
                String endTime = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                deviceEquipmentPointDayGrwQueryWrapper.ge("time_stamp",startTime);
                deviceEquipmentPointDayGrwQueryWrapper.le("time_stamp",endTime);
                List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                if(list != null && list.size()>0){
                    for (int i = 0; i < list.size(); i++) {
                        String p1 = list.get(i).getP1();
                        if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                            String s = StringMultiplier.multiplyAndFormat(p1);
                            list.get(i).setP1(s);
                        }
                    }
                    List<DeviceEquipmentPointGrwFlat> flattenedList = flattenDataGrw(list);
                    // 创建一个新的分页对象，使用扁平化后的列表
                    Page<DeviceEquipmentPointGrwFlat> flattenedPage = new Page<>();
                    // 这里假设你的Page类有相应的方法，我们设置分页信息
                    flattenedPage.setRecords(flattenedList);
                    flattenedPage.setTotal(flattenedList.size()); // 总记录数设置为当前页扁平化后的记录数，这不正确，但是没有办法
                    flattenedPage.setCurrent(devicePageVo.getPageNo()); // 当前页设为1
                    flattenedPage.setSize(flattenedList.size()); // 每页记录数设为当前页的记录数
                    return Result.ok(flattenedPage);
                }
            }
        }
        return Result.ok();
    }



    //2.设备报表的导出
    @PostMapping("/start")
    public Result startExport(HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody DevicePageVo devicePageVo) {
        String taskId = "TASK_" + System.currentTimeMillis();
        //处理数据到redis返回任务id然后再导出
        List<String> psKey = devicePageVo.getPsKey();
        String minType = devicePageVo.getMinType();
        //是谁家的电站 1 阳光云  2 古瑞瓦特
        Integer source = devicePageVo.getSource();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();

        //是否全选
        String isAll = devicePageVo.getIsAll();
        //日
        if((notInPsKey== null  && psKey == null) && StringUtils.isBlank(isAll)){
            return Result.ok();
        }
        //日数据(查询日表)
        if("day".equals(minType) || "customize".equals(minType) || "month".equals(minType)){
            if(source == 1){
                iDeviceEquipmentPointDayService.getExportExcelData(taskId,request,devicePageVo);
            }
            if(source == 2){
                iDeviceEquipmentPointDayGrwService.getExportExcelData(taskId,request,devicePageVo);
            }
        }
        //月数据(等于是月报表）
        if("year".equals(minType)) {
            if (source == 1) {
                iDeviceEquipmentPointMonthService.getExportExcelData(taskId, request, devicePageVo);
            }
            if (source == 2) {
                iDeviceEquipmentPointDayGrwService.getExportExcelDataMonth(taskId, request, devicePageVo);
            }
        }
        //年报表
        if ("all".equals(minType)) {
            if (source == 1) {
                iDeviceEquipmentPointYearService.getExportExcelData(taskId, request, devicePageVo);
            }
            if (source == 2) {
                iDeviceEquipmentPointDayGrwService.getExportExcelDataYear(taskId, request, devicePageVo);
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("taskId",taskId);
        resultMap.put("message","操作成功");
        return Result.ok(resultMap);
    }

    @GetMapping("/status/{taskId}")
    public Result getStatus(@PathVariable String taskId) {
        List<Object> taskStatus = iDeviceService.getTaskStatus(taskId);
        return Result.ok(taskStatus);
    }

    /**
     * 导出导出报表数据
     *
//     * @param request
//     * @param devicePageVo
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls( HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody DevicePageVo devicePageVo) {
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String minType = devicePageVo.getMinType();
        Integer source = devicePageVo.getSource();
        String taskId = devicePageVo.getTaskId();
        //日数据(查询日表)
        if("day".equals(minType) || "customize".equals(minType) || "month".equals(minType)){
            if(source == 1){
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentDayExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentDayExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentDayExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
            if(source == 2){
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentDayExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentDayExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentDayExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }
        //数据(等于是月报表）
        if("year".equals(minType)) {
            if (source == 1) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentMonthExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentMonthExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentMonthExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;

            }
            if (source == 2) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentMonthExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentMonthExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentMonthExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }
        //年报表
        if ("all".equals(minType)) {
            if (source == 1) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentYearExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentYearExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentYearExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
            if (source == 2) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointEquipmentYearExportVO> collect = range.stream()
                        .map(obj -> (DevicePointEquipmentYearExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "设备日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointEquipmentYearExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }

        return null;
    }









    public List<DeviceEquipmentPointMinSixtyFlat> flattenDataMinSixTy(List<DeviceEquipmentPointMinSixty> records) {
        Map<String, DeviceEquipmentPointMinSixtyFlat> resultMap = new HashMap<>();


        for (DeviceEquipmentPointMinSixty record : records) {
            String psKey = record.getPsKey();
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.eq("ps_key",psKey);
            DeviceEquipment one = iDeviceEquipmentService.getOne(deviceEquipmentQueryWrapper);

            // 如果还没有这个psKey的记录，创建新的
            if (!resultMap.containsKey(psKey)) {
                DeviceEquipmentPointMinSixtyFlat flat = new DeviceEquipmentPointMinSixtyFlat();
                flat.setPsKey(psKey);
                flat.setType(record.getType());
                flat.setDayData(new HashMap<>());
                if(one != null){
                    flat.setPsId(one.getPsId());
                    String psName = iDeviceService.getPsName(one.getPsId());
                    if(StringUtils.isNotBlank(psName)){
                        flat.setPsName(psName);
                    }
                    flat.setDeviceEquipmentCode(one.getDeviceSn());
                    flat.setDeviceEquipmentStatus(one.getDevStatus());
                    //
                    flat.setDeviceModelName(one.getDeviceModelCode());
                    DeviceEquipmentPoint deviceEquipmentByPsKey = iDeviceEquipmentPointService.getDeviceEquipmentByPsKey(psKey);
                    if(deviceEquipmentByPsKey != null){
                        flat.setDeviceEquipmentName(deviceEquipmentByPsKey.getDeviceName());

                    }
                }
                resultMap.put(psKey, flat);
            }

            // 添加日期数据
            resultMap.get(psKey).getDayData().put(record.getTimeStamp(), record);

        }
        resultMap.values();
        return new ArrayList<>(resultMap.values());
    }


//    public List<DeviceEquipmentPointFlat> flattenData(List<DeviceEquipmentPointDay> records) {
//        Map<String, DeviceEquipmentPointFlat> resultMap = new HashMap<>();
//
//        for (DeviceEquipmentPointDay record : records) {
//            String psKey = record.getPsKey();
//            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
//            deviceEquipmentQueryWrapper.eq("ps_key",psKey);
//            DeviceEquipment one = iDeviceEquipmentService.getOne(deviceEquipmentQueryWrapper);
//
//            // 如果还没有这个psKey的记录，创建新的
//            if (!resultMap.containsKey(psKey)) {
//                DeviceEquipmentPointFlat flat = new DeviceEquipmentPointFlat();
//                flat.setPsKey(psKey);
//                flat.setType(record.getType());
//                flat.setDayData(new HashMap<>());
//                if(one != null){
//                    flat.setPsId(one.getPsId());
//                    String psName = iDeviceService.getPsName(one.getPsId());
//                    if(StringUtils.isNotBlank(psName)){
//                        flat.setPsName(psName);
//                    }
//                    flat.setDeviceEquipmentCode(one.getDeviceSn());
//                    flat.setDeviceEquipmentStatus(one.getDevStatus());
//                    //
//                    flat.setDeviceModelName(one.getDeviceModelCode());
//                    DeviceEquipmentPoint deviceEquipmentByPsKey = iDeviceEquipmentPointService.getDeviceEquipmentByPsKey(psKey);
//                    if(deviceEquipmentByPsKey != null){
//                        flat.setDeviceEquipmentName(deviceEquipmentByPsKey.getDeviceName());
//                    }
//                    PsInfoVo deviceInfo = iDeviceEquipmentService.getDeviceInfo(psKey);
//                    if(deviceInfo != null){
//                        flat.setMessageSN(deviceInfo.getCommunicationDevSn());
//                    }
//
//                }
//                resultMap.put(psKey, flat);
//            }
//
//
//
//            // 添加日期数据
//            resultMap.get(psKey).getDayData().put(record.getTimeStamp(), record);
//        }
//        return new ArrayList<>(resultMap.values());
//    }

    public List<DeviceEquipmentPointFlat> flattenData(List<DeviceEquipmentPointDay> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有唯一的 psKeys
        Set<String> psKeys = records.stream()
                .map(DeviceEquipmentPointDay::getPsKey)
                .collect(Collectors.toSet());

        // 批量查询 DeviceEquipment
        QueryWrapper<DeviceEquipment> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ps_key", psKeys);
        List<DeviceEquipment> deviceEquipments = iDeviceEquipmentService.list(queryWrapper);
        Map<String, DeviceEquipment> deviceEquipmentMap = deviceEquipments.stream()
                .collect(Collectors.toMap(DeviceEquipment::getPsKey, de -> de));

        // 批量查询 PsName (假设可以通过批量接口获取，否则需自定义 SQL)
        Set<Long> psIds = deviceEquipments.stream()
                .map(DeviceEquipment::getPsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> psIdToNameMap = new HashMap<>();
        if (!psIds.isEmpty()) {
            // 假设 iDeviceService 提供了批量查询方法 getPsNameBatch(Set<Long>)
            // 如果没有，需要你自己实现一个批量查询的方法（如通过 mapper 查询）
            psIdToNameMap = iDeviceService.getPsNameBatch(psIds); // 自定义批量接口
        }

        // 批量查询 DeviceEquipmentPoint 的 deviceName（按 psKey）
        List<DeviceEquipmentPoint> deviceEquipmentPoints = iDeviceEquipmentPointService.getDeviceEquipmentByPsKeys(psKeys);
        Map<String, String> psKeyToDeviceNameMap = deviceEquipmentPoints.stream()
                .collect(Collectors.toMap(
                        DeviceEquipmentPoint::getPsKey,
                        DeviceEquipmentPoint::getDeviceName,
                        (a, b) -> a // 若有重复取第一个
                ));

        // 批量查询 DeviceInfo (messageSN)，也应支持批量
        Map<String, String> psKeyToMessageSNMap = new HashMap<>();
        if (!psKeys.isEmpty()) {
            // 假设该服务支持批量查询
            List<PsInfoVo> deviceInfos = iDeviceEquipmentService.getDeviceInfos(psKeys); // 需要你实现此批量方法
            psKeyToMessageSNMap = deviceInfos.stream()
                    .collect(Collectors.toMap(
                            PsInfoVo::getPsKey,
                            PsInfoVo::getCommunicationDevSn,
                            (a, b) -> a
                    ));
        }

        // 构建最终结果
        Map<String, DeviceEquipmentPointFlat> resultMap = new LinkedHashMap<>(); // 保持插入顺序

        for (DeviceEquipmentPointDay record : records) {
            String psKey = record.getPsKey();
            DeviceEquipmentPointFlat flat = resultMap.get(psKey);

            if (flat == null) {
                flat = new DeviceEquipmentPointFlat();
                flat.setPsKey(psKey);
                flat.setType(record.getType());
                flat.setDayData(new HashMap<>());

                DeviceEquipment equipment = deviceEquipmentMap.get(psKey);
                if (equipment != null) {
                    flat.setPsId(equipment.getPsId());
                    String psId = equipment.getPsId().toString();
                    if (psId != null && psIdToNameMap.containsKey(psId)) {
                        flat.setPsName(psIdToNameMap.get(psId));
                    }
                    flat.setDeviceEquipmentCode(equipment.getDeviceSn());
                    flat.setDeviceEquipmentStatus(equipment.getDevStatus());
                    flat.setDeviceModelName(equipment.getDeviceModelCode());
                }

                // 设置 deviceName
                if (psKeyToDeviceNameMap.containsKey(psKey)) {
                    flat.setDeviceEquipmentName(psKeyToDeviceNameMap.get(psKey));
                }

                // 设置 messageSN
                if (psKeyToMessageSNMap.containsKey(psKey)) {
                    flat.setMessageSN(psKeyToMessageSNMap.get(psKey));
                }

                resultMap.put(psKey, flat);
            }

            // 添加时间序列数据
            flat.getDayData().put(record.getTimeStamp(), record);
        }

        return new ArrayList<>(resultMap.values());
    }

    public List<DeviceEquipmentPointFlatMonth> flattenDataMonth(List<DeviceEquipmentPointMonth> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有唯一的 psKeys
        Set<String> psKeys = records.stream()
                .map(DeviceEquipmentPointMonth::getPsKey)
                .collect(Collectors.toSet());

        // 批量查询 DeviceEquipment
        QueryWrapper<DeviceEquipment> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ps_key", psKeys);
        List<DeviceEquipment> deviceEquipments = iDeviceEquipmentService.list(queryWrapper);
        Map<String, DeviceEquipment> deviceEquipmentMap = deviceEquipments.stream()
                .collect(Collectors.toMap(DeviceEquipment::getPsKey, de -> de));

        // 批量查询 PsName (假设可以通过批量接口获取，否则需自定义 SQL)
        Set<Long> psIds = deviceEquipments.stream()
                .map(DeviceEquipment::getPsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> psIdToNameMap = new HashMap<>();
        if (!psIds.isEmpty()) {
            // 假设 iDeviceService 提供了批量查询方法 getPsNameBatch(Set<Long>)
            // 如果没有，需要你自己实现一个批量查询的方法（如通过 mapper 查询）
            psIdToNameMap = iDeviceService.getPsNameBatch(psIds); // 自定义批量接口
        }

        // 批量查询 DeviceEquipmentPoint 的 deviceName（按 psKey）
        List<DeviceEquipmentPoint> deviceEquipmentPoints = iDeviceEquipmentPointService.getDeviceEquipmentByPsKeys(psKeys);
        Map<String, String> psKeyToDeviceNameMap = deviceEquipmentPoints.stream()
                .collect(Collectors.toMap(
                        DeviceEquipmentPoint::getPsKey,
                        DeviceEquipmentPoint::getDeviceName,
                        (a, b) -> a // 若有重复取第一个
                ));

        // 批量查询 DeviceInfo (messageSN)，也应支持批量
        Map<String, String> psKeyToMessageSNMap = new HashMap<>();
        if (!psKeys.isEmpty()) {
            // 假设该服务支持批量查询
            List<PsInfoVo> deviceInfos = iDeviceEquipmentService.getDeviceInfos(psKeys); // 需要你实现此批量方法
            psKeyToMessageSNMap = deviceInfos.stream()
                    .collect(Collectors.toMap(
                            PsInfoVo::getPsKey,
                            PsInfoVo::getCommunicationDevSn,
                            (a, b) -> a
                    ));
        }

        // 构建最终结果
        Map<String, DeviceEquipmentPointFlatMonth> resultMap = new LinkedHashMap<>(); // 保持插入顺序

        for (DeviceEquipmentPointMonth record : records) {
            String psKey = record.getPsKey();
            DeviceEquipmentPointFlatMonth flat = resultMap.get(psKey);

            if (flat == null) {
                flat = new DeviceEquipmentPointFlatMonth();
                flat.setPsKey(psKey);
                flat.setType(record.getType());
                flat.setDayData(new HashMap<>());

                DeviceEquipment equipment = deviceEquipmentMap.get(psKey);
                if (equipment != null) {
                    flat.setPsId(equipment.getPsId());
                    String psId = equipment.getPsId().toString();
                    if (psId != null && psIdToNameMap.containsKey(psId)) {
                        flat.setPsName(psIdToNameMap.get(psId));
                    }
                    flat.setDeviceEquipmentCode(equipment.getDeviceSn());
                    flat.setDeviceEquipmentStatus(equipment.getDevStatus());
                    flat.setDeviceModelName(equipment.getDeviceModelCode());
                }

                // 设置 deviceName
                if (psKeyToDeviceNameMap.containsKey(psKey)) {
                    flat.setDeviceEquipmentName(psKeyToDeviceNameMap.get(psKey));
                }

                // 设置 messageSN
                if (psKeyToMessageSNMap.containsKey(psKey)) {
                    flat.setMessageSN(psKeyToMessageSNMap.get(psKey));
                }

                resultMap.put(psKey, flat);
            }

            // 添加时间序列数据
            flat.getDayData().put(record.getTimeStamp(), record);
        }

        return new ArrayList<>(resultMap.values());
    }

    public List<DeviceEquipmentPointFlatYear> flattenDataYear(List<DeviceEquipmentPointYear> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有唯一的 psKeys
        Set<String> psKeys = records.stream()
                .map(DeviceEquipmentPointYear::getPsKey)
                .collect(Collectors.toSet());

        // 批量查询 DeviceEquipment
        QueryWrapper<DeviceEquipment> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ps_key", psKeys);
        List<DeviceEquipment> deviceEquipments = iDeviceEquipmentService.list(queryWrapper);
        Map<String, DeviceEquipment> deviceEquipmentMap = deviceEquipments.stream()
                .collect(Collectors.toMap(DeviceEquipment::getPsKey, de -> de));

        // 批量查询 PsName (假设可以通过批量接口获取，否则需自定义 SQL)
        Set<Long> psIds = deviceEquipments.stream()
                .map(DeviceEquipment::getPsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> psIdToNameMap = new HashMap<>();
        if (!psIds.isEmpty()) {
            // 假设 iDeviceService 提供了批量查询方法 getPsNameBatch(Set<Long>)
            // 如果没有，需要你自己实现一个批量查询的方法（如通过 mapper 查询）
            psIdToNameMap = iDeviceService.getPsNameBatch(psIds); // 自定义批量接口
        }

        // 批量查询 DeviceEquipmentPoint 的 deviceName（按 psKey）
        List<DeviceEquipmentPoint> deviceEquipmentPoints = iDeviceEquipmentPointService.getDeviceEquipmentByPsKeys(psKeys);
        Map<String, String> psKeyToDeviceNameMap = deviceEquipmentPoints.stream()
                .collect(Collectors.toMap(
                        DeviceEquipmentPoint::getPsKey,
                        DeviceEquipmentPoint::getDeviceName,
                        (a, b) -> a // 若有重复取第一个
                ));

        // 批量查询 DeviceInfo (messageSN)，也应支持批量
        Map<String, String> psKeyToMessageSNMap = new HashMap<>();
        if (!psKeys.isEmpty()) {
            // 假设该服务支持批量查询
            List<PsInfoVo> deviceInfos = iDeviceEquipmentService.getDeviceInfos(psKeys); // 需要你实现此批量方法
            psKeyToMessageSNMap = deviceInfos.stream()
                    .collect(Collectors.toMap(
                            PsInfoVo::getPsKey,
                            PsInfoVo::getCommunicationDevSn,
                            (a, b) -> a
                    ));
        }

        // 构建最终结果
        Map<String, DeviceEquipmentPointFlatYear> resultMap = new LinkedHashMap<>(); // 保持插入顺序

        for (DeviceEquipmentPointYear record : records) {
            String psKey = record.getPsKey();
            DeviceEquipmentPointFlatYear flat = resultMap.get(psKey);

            if (flat == null) {
                flat = new DeviceEquipmentPointFlatYear();
                flat.setPsKey(psKey);
                flat.setType(record.getType());
                flat.setDayData(new HashMap<>());

                DeviceEquipment equipment = deviceEquipmentMap.get(psKey);
                if (equipment != null) {
                    flat.setPsId(equipment.getPsId());
                    String psId = equipment.getPsId().toString();
                    if (psId != null && psIdToNameMap.containsKey(psId)) {
                        flat.setPsName(psIdToNameMap.get(psId));
                    }
                    flat.setDeviceEquipmentCode(equipment.getDeviceSn());
                    flat.setDeviceEquipmentStatus(equipment.getDevStatus());
                    flat.setDeviceModelName(equipment.getDeviceModelCode());
                }

                // 设置 deviceName
                if (psKeyToDeviceNameMap.containsKey(psKey)) {
                    flat.setDeviceEquipmentName(psKeyToDeviceNameMap.get(psKey));
                }

                // 设置 messageSN
                if (psKeyToMessageSNMap.containsKey(psKey)) {
                    flat.setMessageSN(psKeyToMessageSNMap.get(psKey));
                }

                resultMap.put(psKey, flat);
            }

            // 添加时间序列数据
            flat.getDayData().put(record.getTimeStamp(), record);
        }

        return new ArrayList<>(resultMap.values());
    }


    public List<DeviceEquipmentPointGrwFlat> flattenDataGrw(List<DeviceEquipmentPointDayGrw> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: 提取所有唯一的 deviceSn (即 psKey)
        Set<String> deviceSns = records.stream()
                .map(DeviceEquipmentPointDayGrw::getDeviceSn)
                .collect(Collectors.toSet());

        // Step 2: 批量查询 DeviceEquipment（通过 device_sn）
        QueryWrapper<DeviceEquipment> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("device_sn", deviceSns);
        List<DeviceEquipment> deviceEquipments = iDeviceEquipmentService.list(queryWrapper);

        // 构建 deviceSn -> DeviceEquipment 映射
        Map<String, DeviceEquipment> deviceEquipmentMap = deviceEquipments.stream()
                .collect(Collectors.toMap(
                        DeviceEquipment::getDeviceSn,
                        de -> de,
                        (a, b) -> a // 重复取第一个
                ));

        // Step 3: 提取所有 psId，批量查询 psName
        Set<Long> psIds = deviceEquipments.stream()
                .map(DeviceEquipment::getPsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> psIdToNameMap = new HashMap<>();
        if (!psIds.isEmpty()) {
            // 假设你实现了批量方法：getPsNameBatch(Set<Long>) 返回 Map<psId, psName>
            psIdToNameMap = iDeviceService.getPsNameBatchGrw(psIds);
        }

        // Step 4: 构建最终结果（避免重复创建）
        Map<String, DeviceEquipmentPointGrwFlat> resultMap = new LinkedHashMap<>();

        for (DeviceEquipmentPointDayGrw record : records) {
            String deviceSn = record.getDeviceSn();
            DeviceEquipmentPointGrwFlat flat = resultMap.get(deviceSn);

            if (flat == null) {
                flat = new DeviceEquipmentPointGrwFlat();
                flat.setPsKey(deviceSn);
                flat.setDayData(new HashMap<>());

                DeviceEquipment equipment = deviceEquipmentMap.get(deviceSn);
                if (equipment != null) {
                    flat.setPsId(equipment.getPsId());
                    String psId = equipment.getPsId().toString();
                    if (psId != null && psIdToNameMap.containsKey(psId)) {
                        flat.setPsName(psIdToNameMap.get(psId));
                    }
                    flat.setDeviceEquipmentCode(equipment.getDeviceSn());
                    flat.setDeviceEquipmentStatus(equipment.getDevStatus());
                    flat.setDeviceModelName(equipment.getDeviceModelCode());
                    flat.setDeviceEquipmentName("逆变器"); // 固定值
                    //采集器
                    List<String> strings = iDeviceGatherGrwService.listMessageSn(psId);
                    if(strings != null && strings.size()>0){
                        String join = String.join(",", strings);
                        flat.setMessageSN(join);
                    }
                }
                resultMap.put(deviceSn, flat);
            }

            // 添加时间序列数据
            flat.getDayData().put(record.getTimeStamp(), record);
        }

        return new ArrayList<>(resultMap.values());
    }







    @ApiOperation("根据电站id获取电站下设备列表")
    @RequestMapping(value = "/deviceEquipmentListById", method = RequestMethod.GET)
    public Result deviceEquipmentListById(DevicePageVo devicePageVo) throws Exception {
        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        deviceEquipmentQueryWrapper.eq("ps_id",devicePageVo.getPlantId());
        String equipmentType = devicePageVo.getEquipmentType();
        String equipmentStatus = devicePageVo.getEquipmentStatus();
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();
        //设备类型  1:逆变器 22：通讯设备
        if(StringUtils.isNotBlank(equipmentType)){
            deviceEquipmentQueryWrapper.eq("device_type",equipmentType);
        }
        //设备状态 1：故障，2：告警，4：正常
        if(StringUtils.isNotBlank(equipmentStatus)){
            deviceEquipmentQueryWrapper.eq("dev_fault_status",equipmentStatus);
        }
        //设备名称或者sn号码
        // 修复 OR 条件deviceEquipmentCurve
        if (StringUtils.isNotBlank(deviceNameOrSn)) {
            deviceEquipmentQueryWrapper.and(wrapper ->
                    wrapper.like("device_name", deviceNameOrSn)
                            .or()
                            .like("device_sn", deviceNameOrSn)
            );
        }

        String plantId = devicePageVo.getPlantId();

//        PsInfoVo psInfoByPsKeyForGrw = iDeviceService.getPsInfoByPsKeyForGrw(plantId);
//        //不等于空就说明是古瑞瓦特的电站，就还有采集器的数据需要查询
//        List<DeviceEquipment> deviceEquipments = new ArrayList<>();
//
//        if(psInfoByPsKeyForGrw != null){
//            QueryWrapper<DeviceGatherGrw> deviceGatherGrwQueryWrapper = new QueryWrapper<>();
//            deviceGatherGrwQueryWrapper.eq("ps_id",plantId);
//            List<DeviceGatherGrw> list = iDeviceGatherGrwService.list(deviceGatherGrwQueryWrapper);
//            if(list != null && list.size()>0){
//                for (int i = 0; i < list.size(); i++) {
//                    DeviceEquipment deviceEquipment = new DeviceEquipment();
//                    DeviceGatherGrw deviceGatherGrw = list.get(i);
//                    String model = deviceGatherGrw.getModel();
//                    String sn = deviceGatherGrw.getSn();
//                    deviceEquipment.setDeviceName(psInfoByPsKeyForGrw.getPsName());
//                    deviceEquipment.setDeviceSn(sn);
//                    deviceEquipment.setDeviceModelCode(model);
//                    deviceEquipment.setSource("2");
//                    deviceEquipment.setDeviceName("采集器");
//                    deviceEquipments.add(deviceEquipment);
//                }
//            }
//        }

        List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
        if(list != null && list.size()>0){
            for (int i = 0; i < list.size(); i++) {
                String psKey = list.get(i).getPsKey();
                DeviceEquipmentPoint deviceEquipmentByPsKey = iDeviceEquipmentPointService.getDeviceEquipmentByPsKey(psKey);
                if(deviceEquipmentByPsKey != null){
                    list.get(i).setDeviceName(StringUtils.isNotBlank(deviceEquipmentByPsKey.getDeviceName()) ? deviceEquipmentByPsKey.getDeviceName():"");
                }
            }
        }
//        list.addAll(deviceEquipments);
        return Result.ok(list);
    }




    @ApiOperation("根据设备的psKey获取曲线数据")
    @RequestMapping(value = "/deviceEquipmentCurve", method = RequestMethod.GET)
    public Result  deviceEquipmentCurve(DeviceEquipmentVo deviceEquipmentVo) throws Exception {
        //逆变器的psKey
        String psKeyOne = deviceEquipmentVo.getPsKey();
        //获取的分钟类型deviceEquipmentVo
        String minuteInterval = deviceEquipmentVo.getMinuteInterval();
        String minType = deviceEquipmentVo.getMinType();
        //查询的日期
        String queryDate = deviceEquipmentVo.getQueryDate();
        //开始时间
        String startDate = deviceEquipmentVo.getStartDate();
        //结束时间
        String endDate = deviceEquipmentVo.getEndDate();
        //数据来源
        Integer source = deviceEquipmentVo.getSource();

        //日数据
        if("day".equals(minType)){
            if("5".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinFive> deviceEquipmentPointMinFiveQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinFiveQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    deviceEquipmentPointMinFiveQueryWrapper.like("time_stamp",yyyyMMdd);
                }
                List<DeviceEquipmentPointMinFive> list = iDeviceEquipmentPointMinFiveService.list(deviceEquipmentPointMinFiveQueryWrapper);
                return Result.ok(list);
            }

            //15分钟数据
            if("15".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinFifteen> deviceEquipmentPointMinFifteenQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinFifteenQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    deviceEquipmentPointMinFifteenQueryWrapper.like("time_stamp",yyyyMMdd);
                }
                List<DeviceEquipmentPointMinFifteen> list = iDeviceEquipmentPointMinFifteenService.list(deviceEquipmentPointMinFifteenQueryWrapper);
                return Result.ok(list);
            }

            //30分钟数据
            if("30".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinThirty> deviceEquipmentPointMinThirtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinThirtyQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    deviceEquipmentPointMinThirtyQueryWrapper.like("time_stamp",yyyyMMdd);
                }
                List<DeviceEquipmentPointMinThirty> list = iDeviceEquipmentPointMinThirtyService.list(deviceEquipmentPointMinThirtyQueryWrapper);
                return Result.ok(list);
            }

            //60分钟数据
            if("60".equals(minuteInterval)){
               if(source == 1){
                   QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                   deviceEquipmentPointDayQueryWrapper.eq("ps_key",psKeyOne);
                   LocalDate start = LocalDate.parse(startDate);
                   LocalDate end = LocalDate.parse(endDate);
                   // 开始时间设置为当天的 00:00:00
                   String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                   // 结束时间设置为当天的 23:59:59
                   String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                   deviceEquipmentPointDayQueryWrapper.between("time_stamp", startTime, endTime);
                   List<DeviceEquipmentPointMinSixty> list = iDeviceEquipmentPointMinSixtyService.list(deviceEquipmentPointDayQueryWrapper);
                   return Result.ok(list);
               }
               if(source == 2){
                   QueryWrapper<DeviceEquipmentPointMinFiveGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                   deviceEquipmentPointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                   // 开始时间设置为当天的 00:00:00
                   if(StringUtils.isNotBlank(startDate)  && StringUtils.isNotBlank(endDate)){
                       LocalDate start = LocalDate.parse(startDate);
                       // 开始时间设置为当天的 00:00:00
                       // 结束时间设置为当天的 23:59:59
                       deviceEquipmentPointDayGrwQueryWrapper.between("time_stamp",
                               start.atStartOfDay(),          // 2025-12-19 00:00:00
                               start.atTime(LocalTime.MAX));
                       deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                   }
                   List<DeviceEquipmentPointMinFiveGrw> list = iDeviceEquipmentPointMinFiveGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                   if(list != null && list.size()>0){
                       for (int i = 0; i < list.size(); i++) {
                           String p1 = list.get(i).getP1();
                           if(StringUtils.isNotBlank(p1)){
                               list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                           }

                       }
                   }
                   return Result.ok(list);
               }
            }
        }

        //周数据
        if("week".equals(minType)){
            //15分钟数据
            if("15".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinFifteen> deviceEquipmentPointMinFifteenQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinFifteenQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinFifteenQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinFifteen> list = iDeviceEquipmentPointMinFifteenService.list(deviceEquipmentPointMinFifteenQueryWrapper);
                return Result.ok(list);
            }

            //30分钟数据
            if("30".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinThirty> deviceEquipmentPointMinThirtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinThirtyQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinThirtyQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinThirty> list = iDeviceEquipmentPointMinThirtyService.list(deviceEquipmentPointMinThirtyQueryWrapper);
                return Result.ok(list);
            }
            //60分钟数据
            if("60".equals(minuteInterval)){
                if(source == 1){
                    QueryWrapper<DeviceEquipmentPointDay> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                        LocalDate start = LocalDate.parse(startDate);
                        String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        LocalDate end = LocalDate.parse(endDate);
                        String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        deviceEquipmentPointDayQueryWrapper.ge("time_stamp",startTime);
                        deviceEquipmentPointDayQueryWrapper.le("time_stamp",endTime);
                    }
                    deviceEquipmentPointDayQueryWrapper.orderByAsc("time_stamp");
                    deviceEquipmentPointDayQueryWrapper.eq("ps_key",psKeyOne);
                    List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(deviceEquipmentPointDayQueryWrapper);
                    return Result.ok(list);
                }
                //古瑞瓦特
                if(source == 2){
                    QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                        deviceEquipmentPointDayGrwQueryWrapper.ge("time_stamp",startDate);
                        deviceEquipmentPointDayGrwQueryWrapper.le("time_stamp",endDate);
                    }
                    deviceEquipmentPointDayGrwQueryWrapper.eq("device_sn",psKeyOne);
                    deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {
                            String p1 = list.get(i).getP1();
                            if(StringUtils.isNotBlank(p1)){
                                list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                            }

                        }
                    }
                    return Result.ok(list);
                }
            }
            if("day".equals(minuteInterval)){
                if(source == 1){
                    QueryWrapper<DeviceEquipmentPointDay> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                        LocalDate start = LocalDate.parse(startDate);
                        String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        LocalDate end = LocalDate.parse(endDate);
                        String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        deviceEquipmentPointDayQueryWrapper.ge("time_stamp",startTime);
                        deviceEquipmentPointDayQueryWrapper.le("time_stamp",endTime);
                    }
                    deviceEquipmentPointDayQueryWrapper.orderByAsc("time_stamp");
                    deviceEquipmentPointDayQueryWrapper.eq("ps_key",psKeyOne);
                    List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(deviceEquipmentPointDayQueryWrapper);
                    return Result.ok(list);
                }
                //古瑞瓦特
               if(source == 2){
                   QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                   if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                       deviceEquipmentPointDayGrwQueryWrapper.ge("time_stamp",startDate);
                       deviceEquipmentPointDayGrwQueryWrapper.le("time_stamp",endDate);
                   }
                   deviceEquipmentPointDayGrwQueryWrapper.eq("device_sn",psKeyOne);
                   deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                   List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                   if(list != null && list.size()>0){
                       for (int i = 0; i < list.size(); i++) {
                           String p1 = list.get(i).getP1();
                           if(StringUtils.isNotBlank(p1)){
                               list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                           }

                       }
                   }
                   return Result.ok(list);
               }
            }
        }

        //月数据
        if("month".equals(minType)){
            //30分钟数据
            if("30".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinThirty> deviceEquipmentPointMinThirtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinThirtyQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinThirtyQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinThirty> list = iDeviceEquipmentPointMinThirtyService.list(deviceEquipmentPointMinThirtyQueryWrapper);
                return Result.ok(list);
            }
            //60分钟数据
            if("60".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointMinSixtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinSixtyQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinSixtyQueryWrapper.between("time_stamp", startTime, endTime);
                }
                deviceEquipmentPointMinSixtyQueryWrapper.orderByAsc("time_stamp");
                List<DeviceEquipmentPointMinSixty> list = iDeviceEquipmentPointMinSixtyService.list(deviceEquipmentPointMinSixtyQueryWrapper);
                return Result.ok(list);
            }
            if("day".equals(minuteInterval)){
                if(source == 1){
                    QueryWrapper<DeviceEquipmentPointDay> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                        LocalDate start = LocalDate.parse(startDate);
                        String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        LocalDate end = LocalDate.parse(endDate);
                        String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        deviceEquipmentPointDayQueryWrapper.ge("time_stamp",startTime);
                        deviceEquipmentPointDayQueryWrapper.le("time_stamp",endTime);
                    }

                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
                        deviceEquipmentPointDayQueryWrapper.like("time_stamp",yyyyMM);
                    }
                    deviceEquipmentPointDayQueryWrapper.eq("ps_key",psKeyOne);
                    deviceEquipmentPointDayQueryWrapper.orderByAsc("time_stamp");

                    List<DeviceEquipmentPointDay> list = iDeviceEquipmentPointDayService.list(deviceEquipmentPointDayQueryWrapper);
                    return Result.ok(list);
                }

                if(source == 2){
                    QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
                    }
                    deviceEquipmentPointDayGrwQueryWrapper.eq("device_sn",psKeyOne);
                    deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointDayGrw> list = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {
                            String p1 = list.get(i).getP1();
                            if(StringUtils.isNotBlank(p1)){
                                list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                            }

                        }
                    }
                    return Result.ok(list);
                }
            }
        }

        //年数据
        if("year".equals(minType)){
            if("month".equals(minuteInterval)){
                if(source == 1){
                    QueryWrapper<DeviceEquipmentPointMonth> deviceEquipmentPointMonthQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                        deviceEquipmentPointMonthQueryWrapper.like("time_stamp",yyyy);
                    }
                    deviceEquipmentPointMonthQueryWrapper.eq("ps_key",psKeyOne);
                    deviceEquipmentPointMonthQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointMonth> list = iDeviceEquipmentPointMonthService.list(deviceEquipmentPointMonthQueryWrapper);
                    return Result.ok(list);
                }
                if(source == 2){
                    QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(queryDate)){
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                        deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyy);
                    }
                    deviceEquipmentPointDayGrwQueryWrapper.eq("device_sn",psKeyOne);
                    deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = iDeviceEquipmentPointDayGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);

                    List<DeviceEquipmentPointDayGrw> list = deviceEquipmentPointDayGrws.stream()
                            .collect(Collectors.groupingBy(
                                    item -> {
                                        // 创建复合键：月份 + device_sn
                                        String month = item.getTimeStamp().substring(0, 7); // 提取年月 "2025-11"
                                        String deviceSn = item.getDeviceSn();
                                        return month + "|" + deviceSn; // 使用分隔符避免冲突
                                    },
                                    Collectors.summingDouble(item -> {
                                        try {
                                            return item.getP1() != null ? Double.parseDouble(item.getP1()) : 0.0;
                                        } catch (NumberFormatException e) {
                                            return 0.0;
                                        }
                                    })
                            ))
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                // 解析复合键
                                String[] keys = entry.getKey().split("\\|");
                                String month = keys[0];
                                String deviceSn = keys[1];

                                // 创建新的汇总对象
                                DeviceEquipmentPointDayGrw summary = new DeviceEquipmentPointDayGrw();
                                summary.setTimeStamp(month);        // 设置月份 "2025-11"
                                summary.setDeviceSn(deviceSn);      // 设置设备序列号
                                summary.setP1(String.format("%.2f", entry.getValue())); // 设置求和结果，保留2位小数

                                return summary;
                            })
                            .collect(Collectors.toList());
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {
                            String p1 = list.get(i).getP1();
                            if(StringUtils.isNotBlank(p1)){
                                list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                            }

                        }
                    }
                    return Result.ok(list);
                }
            }
        }

        //自定义
        if("customize".equals(minType)){
            //5分钟数据
            if("5".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinFive> deviceEquipmentPointMinFiveQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinFiveQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinFiveQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinFive> list = iDeviceEquipmentPointMinFiveService.list(deviceEquipmentPointMinFiveQueryWrapper);
                return Result.ok(list);
            }
            //15分钟数据
            if("15".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinFifteen> deviceEquipmentPointMinFifteenQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinFifteenQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinFifteenQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinFifteen> list = iDeviceEquipmentPointMinFifteenService.list(deviceEquipmentPointMinFifteenQueryWrapper);
                return Result.ok(list);
            }

            //30分钟数据
            if("30".equals(minuteInterval)){
                QueryWrapper<DeviceEquipmentPointMinThirty> deviceEquipmentPointMinThirtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinThirtyQueryWrapper.eq("ps_key",psKeyOne);
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    deviceEquipmentPointMinThirtyQueryWrapper.between("time_stamp", startTime, endTime);
                }
                List<DeviceEquipmentPointMinThirty> list = iDeviceEquipmentPointMinThirtyService.list(deviceEquipmentPointMinThirtyQueryWrapper);
                return Result.ok(list);
            }
            //60分钟数据
            if("60".equals(minuteInterval)){
                if(source == 1){
                    QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                        LocalDate start = LocalDate.parse(startDate);
                        LocalDate end = LocalDate.parse(endDate);
                        // 开始时间设置为当天的 00:00:00
                        String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                        // 结束时间设置为当天的 23:59:59
                        String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                        deviceEquipmentPointDayQueryWrapper.between("time_stamp", startTime, endTime);
                    }
                    deviceEquipmentPointDayQueryWrapper.eq("ps_key",psKeyOne);
                    deviceEquipmentPointDayQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointMinSixty> list = iDeviceEquipmentPointMinSixtyService.list(deviceEquipmentPointDayQueryWrapper);
                    return Result.ok(list);
                }
                //古瑞瓦特
                if(source == 2){
                    QueryWrapper<DeviceEquipmentPointMinFiveGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
                    deviceEquipmentPointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                    // 开始时间设置为当天的 00:00:00
                    if(StringUtils.isNotBlank(startDate)  && StringUtils.isNotBlank(endDate)){
                        LocalDate start = LocalDate.parse(startDate);
                        // 开始时间设置为当天的 00:00:00
                        // 结束时间设置为当天的 23:59:59
                        deviceEquipmentPointDayGrwQueryWrapper.between("time_stamp",
                                start.atStartOfDay(),          // 2025-12-19 00:00:00
                                start.atTime(LocalTime.MAX));
                    }
                    deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
                    List<DeviceEquipmentPointMinFiveGrw> list = iDeviceEquipmentPointMinFiveGrwService.list(deviceEquipmentPointDayGrwQueryWrapper);
                    if(list != null && list.size()>0){
                        for (int i = 0; i < list.size(); i++) {
                            String p1 = list.get(i).getP1();
                            if(StringUtils.isNotBlank(p1)){
                                list.get(i).setP1(new BigDecimal(p1).multiply(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString());
                            }
                        }
                    }
                    return Result.ok(list);
                }
            }
        }
        return Result.ok();
    }



    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     * @param stationFault
     * @param token
     * @param appKey
     * @param appSecret
     * @param pageSize
     * @param pageNum
     * @return
     * @throws IOException
     */
    public SolarDeviceEquipmentDto sendDeviceListSolarRequest(String stationFault, String token,
                                                              String appKey, String appSecret, String pageSize, String pageNum) throws IOException {
        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        Map<String, String> params = new HashMap<>();
        params.put("appkey", appKey);
        params.put("size", pageSize);
        params.put("curPage", pageNum);
        params.put("token", token);
        // 3. 转换为JSON字符串（使用Gson库，需引入依赖）
        Gson gson = new Gson();
        String jsonBody = gson.toJson(params);

        // 4. 创建JSON类型的请求体
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody  // 明确指定JSON类型和编码
        );

        // 5. 构建请求
        Request request = new Request.Builder()
                .url(stationFault)
                .addHeader("x-access-key", appSecret)  // 添加请求头
                .post(requestBody)                     // 设置JSON请求体
                .build();

        // 6. 执行请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 优化点5：处理非200状态码的情况
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }
            //反解析成java对象
            ResponseBody body = response.body();
            return JSONObject.parseObject(body.string(), SolarDeviceEquipmentDto.class);
        }
    }

    /***
     *  获取阳光云的电站设备全部数据
     * @param token
     * @return
     * @throws IOException
     */
    public List<SolarDeviceEquipmentDto.PageItem> fetchSolarDeviceEquipmentList(String token) throws IOException {

        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceEquipmentDto.PageItem> allData = new ArrayList<>();
        SolarDeviceEquipmentDto firstData = this.sendDeviceListSolarRequest(stationEquipmentsUrl, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
        // 添加第一页数据
        allData.addAll(firstData.getResultData().getPageList());
        //总数据条数
        int rowCount = firstData.getResultData().getRowCount();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) rowCount / 100);

        // 循环获取剩余页的数据
        for (currentPage = 2; currentPage <= totalPages; currentPage++) {
            SolarDeviceEquipmentDto pageData = this.sendDeviceListSolarRequest(stationEquipmentsUrl, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
            if (pageData != null && pageData.getResultData().getPageList() != null) {
                allData.addAll(pageData.getResultData().getPageList());
            }

            // 可以添加适当的延迟，避免请求过于频繁
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return allData;
    }

    /**
     * 古瑞瓦特获取电站列表数据
     *
     * @return
     */
    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum) {
        if(listId != null && listId.size() > 0){
            for (int i = 0; i < listId.size(); i++) {
                // 1. 创建OkHttpClient实例，并配置超时参数
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                        .build();

                // 2. 构建请求参数Map
                // 1. 创建参数映射
                Map<String, String> params = new HashMap<>();
                int random = (int) ((Math.random() * 10) + 1);
                params.put("search_type", String.valueOf(random));
                params.put("page", String.valueOf(pageNum));  // 转换为字符串类型
                params.put("perpage", String.valueOf(pageSize));
                params.put("plant_id",String.valueOf(listId.get(i)));

                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(deviceGrwListUrl);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + deviceGrwListUrl);
                }

                HttpUrl.Builder urlBuilder = baseUrl.newBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                HttpUrl urlWithParams = urlBuilder.build();

                // 3. 构建GET请求
                Request request = new Request.Builder()
                        .url(urlWithParams)
                        .addHeader("token", growattToken)
                        .get()  // GET方法（OkHttp默认就是GET，可省略）
                        .build();


                // 6. 执行请求并处理响应
                try (Response response = client.newCall(request).execute()) {

                    if (!response.isSuccessful()) {
                        // 优化点5：处理非200状态码的情况
                        throw new IOException("请求失败: " + response.code() + " " + response.message());
                    }
                    //反解析成java对象并返回token
                    ResponseBody body = response.body();
                    GrowattDeviceEquipmentDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceEquipmentDto.class);
                    if(growattDeviceDto != null){
                        if(growattDeviceDto.getData() != null){
                            if(growattDeviceDto.getData().getDevices() != null ){
                                List<GrowattDeviceEquipmentDto.DeviceEquipmentDto> deviceEquipmentDtos = growattDeviceDto.getData().getDevices();
                                if(deviceEquipmentDtos != null){
                                    for (int j = 0; j < deviceEquipmentDtos.size(); j++) {
                                        GrowattDeviceEquipmentDto.DeviceEquipmentDto deviceEquipmentDto = deviceEquipmentDtos.get(j);
                                        if(deviceEquipmentDto.getType()==1){
                                            DeviceEquipment deviceEquipment = new DeviceEquipment();
                                            deviceEquipment.setDeviceSn(deviceEquipmentDto.getDeviceSn());
                                            deviceEquipment.setFactoryName(deviceEquipmentDto.getManufacturer());
                                            deviceEquipment.setDeviceModelCode(deviceEquipmentDto.getModel());
                                            deviceEquipment.setDeviceType(deviceEquipmentDto.getType());
                                            deviceEquipment.setPsId(Long.valueOf(listId.get(i)));
                                            deviceEquipment.setDevStatus(deviceEquipmentDto.getLost());
                                            deviceEquipment.setGrwStatus(String.valueOf(deviceEquipmentDto.getStatus()));
                                            deviceEquipment.setSource("2");
                                            iDeviceEquipmentService.save(deviceEquipment);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private String convertToDbDateFormat(String frontendDate, String type) {
        try {
            // 解析前端日期
            LocalDate date = LocalDate.parse(frontendDate, DateTimeFormatter.ISO_DATE);

            if ("start".equals(type)) {
                // 转换为当天的开始时间 20240921000000
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "000000";
            } else {
                // 转换为当天的结束时间 20240921235959
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "235959";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误: " + frontendDate);
        }
    }

    private String convertToDbDateFormatMonth(String frontendDate, String type) {
        try {
            // 解析前端日期
            LocalDate date = LocalDate.parse(frontendDate, DateTimeFormatter.ISO_DATE);

            if ("start".equals(type)) {
                // 转换为当天的开始时间 20240921000000
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) ;
            } else {
                // 转换为当天的结束时间 20240921235959
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误: " + frontendDate);
        }
    }


}
