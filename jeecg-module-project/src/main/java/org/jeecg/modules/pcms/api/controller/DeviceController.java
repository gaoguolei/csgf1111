package org.jeecg.modules.pcms.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.entity.*;
import org.jeecg.modules.pcms.energy.vo.DayStatistics;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;
import org.jeecg.modules.pcms.project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "电站接口")
@RestController
@RequestMapping(value = "project/device")
public class DeviceController {

    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationList}")
    private String stationSolarListUrl;

    @Value("${growatt.stationList}")
    private String stationGrowattListUrl;


    @Value("${growatt.stationData}")
    private String stationData;


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
    private IDeviceFaultService iDeviceFaultService;

    @Autowired
    private IDeviceDetailGrowattService iDeviceDetailGrowattService;


    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;


    @Autowired
    private IDeviceEquipmentPointService iDeviceEquipmentPointService;

    @Autowired
    private IDeviceEquipmentPointMinSixtyService iDeviceEquipmentPointMinSixtyService;

    @Autowired
    private IDevicePointMinFiveService iDevicePointMinFiveService;

    @Autowired
    private IDevicePointMinSixtyService iDevicePointMinSixtyService;

    @Autowired
    private IDevicePointDayService iDevicePointDayService;

    @Autowired
    private IDevicePointMonthService iDevicePointMonthService;

    @Autowired
    private IDevicePointYearService iDevicePointYearService;

    @Autowired
    private IDevicePointDayGrwService iDevicePointDayGrwService;

    @Autowired
    private IDevicePointMonthGrwService iDevicePointMonthGrwService;
    @Autowired
    private IDevicePointYearGrwService iDevicePointYearGrwService;



    @ApiOperation("电站分页")
    @RequestMapping(value = "/devicePageList", method = RequestMethod.GET)
    public Result devicePageList(DevicePageVo devicePageVo) throws Exception {
        QueryWrapper<Device> objectQueryWrapper = new QueryWrapper<>();
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();

        // 动态分页参数
        IPage<Device> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );

        //地区
        String address = devicePageVo.getAddress();
        if(StringUtils.isNotBlank(address)){
            objectQueryWrapper.like("device_name", address);
        }


        //厂商
        Integer supSource = devicePageVo.getSource();
        if(supSource != null){
            objectQueryWrapper.eq("source", supSource);
        }

        String renterId = devicePageVo.getRenterId();
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            objectQueryWrapper.eq("project_type", renterId);
        }

        // 修复 OR 条件
        if (StringUtils.isNotBlank(deviceNameOrSn)) {
            objectQueryWrapper.and(wrapper ->
                    wrapper.like("device_name", deviceNameOrSn)
            );
        }

        // 其他条件（保持AND关系）
        String deviceType = devicePageVo.getDeviceType();
        if (StringUtils.isNotBlank(deviceType)) {
            objectQueryWrapper.eq("ps_type", deviceType);
        }

        //等效小时最小
        String equivalentHourMin = devicePageVo.getEquivalentHourMin();
        if (StringUtils.isNotBlank(equivalentHourMin)) {
            objectQueryWrapper.ge("equivalent_hour", equivalentHourMin);
        }

        //等效小时最大
        String equivalentHourMax = devicePageVo.getEquivalentHourMax();
        if (StringUtils.isNotBlank(equivalentHourMax)) {
            objectQueryWrapper.le("equivalent_hour", equivalentHourMax);
        }

        String tabType = devicePageVo.getTabType();
        if (StringUtils.isNotBlank(tabType)) {
            //关注的
            if("1".equals(tabType)){
                objectQueryWrapper.eq("is_concern", "1");
            }
            //正常
            if("2".equals(tabType)){
                if (supSource != null ) {
                    if (supSource == 1) {
                        objectQueryWrapper.eq("ps_fault_status", "3")
                                .ne("ps_status", "0")
                                .eq("source", supSource);
                    } else if (supSource == 2) {
                        objectQueryWrapper.eq("ps_status", "1")
                                .eq("source", supSource);
                    }
                    if(StringUtils.isNotBlank(renterId)){
                        if(renterId.equals("1964826626")){
                            renterId="-624947198";
                        }
                        objectQueryWrapper.eq("project_type", renterId);
                    }
                    if (StringUtils.isNotBlank(deviceNameOrSn)) {
                        objectQueryWrapper.like("device_name", deviceNameOrSn);
                    }
                }
                // 如果没有传入source参数
                else {
                    // 使用OR条件组合两种情况的查询
                    objectQueryWrapper.and(qw -> qw
                            .eq("ps_fault_status", "3")
                            .ne("ps_status", "0")
                            .eq("source", "1")
                    ).or(qw -> qw
                            .eq("ps_status", "1")
                            .eq("source", "2")
                    );
                    // 添加去重
                    if(StringUtils.isNotBlank(renterId)){
                        if(renterId.equals("1964826626")){
                            renterId="-624947198";
                        }
                        objectQueryWrapper.eq("project_type", renterId);
                    }
                    objectQueryWrapper.select("DISTINCT *");
                }
                if (StringUtils.isNotBlank(deviceNameOrSn)) {
                    objectQueryWrapper.like("device_name", deviceNameOrSn);
                }
            }
            //异常
            if("3".equals(tabType)){
                if(supSource != null ){
                    //厂商
                    if (supSource == 1) {
                        // 非古瑞瓦特的异常设备条件
                        objectQueryWrapper.in("ps_fault_status", "1", "2")
                                .ne("ps_status", "0")
                                .eq("source", supSource);
                        if(StringUtils.isNotBlank(renterId)){
                            if(renterId.equals("1964826626")){
                                renterId="-624947198";
                            }
                            objectQueryWrapper.eq("project_type", renterId);
                        }
                    } else if (supSource == 2) {
                        // 古瑞瓦特的异常设备条件
                        objectQueryWrapper.eq("ps_status", "4")
                                .eq("source", supSource);
                        if(StringUtils.isNotBlank(renterId)){
                            if(renterId.equals("1964826626")){
                                renterId="-624947198";
                            }
                            objectQueryWrapper.eq("project_type", renterId);
                        }
                    }
                }
                // 如果没有传入source参数
                else {
                    // 使用OR条件组合两种异常情况的查询
                    objectQueryWrapper.and(qw -> qw
                            .in("ps_fault_status", "1", "2")
                            .ne("ps_status", "0")
                            .eq("source", "1")
                    ).or(qw -> qw
                            .eq("ps_status", "4")
                            .eq("source", "2")
                    );
                    if(renterId.equals("1964826626")){
                        renterId="-624947198";
                    }
                    objectQueryWrapper.eq("project_type", renterId);
                    if(StringUtils.isNotBlank(renterId)){
                        if(renterId.equals("1964826626")){
                            renterId="-624947198";
                        }
                        objectQueryWrapper.eq("project_type", renterId);
                    }

                }
                if (StringUtils.isNotBlank(deviceNameOrSn)) {
                    objectQueryWrapper.like("device_name", deviceNameOrSn);
                }
            }
            //离线
            if("4".equals(tabType)){
                if(supSource != null ){
                    if (1 == supSource ) {
                        // 非古瑞瓦特的离线设备条件
                        objectQueryWrapper.eq("ps_status", "0")
                                .eq("source", supSource);
                        if(StringUtils.isNotBlank(renterId)){
                            if(renterId.equals("1964826626")){
                                renterId="-624947198";
                            }
                            objectQueryWrapper.eq("project_type", renterId);
                        }
                    } else if (2 == supSource) {
                        // 古瑞瓦特的离线设备条件
                        objectQueryWrapper.eq("ps_status", "-1")
                                .eq("source", supSource);
                        if(StringUtils.isNotBlank(renterId)){
                            if(renterId.equals("1964826626")){
                                renterId="-624947198";
                            }
                            objectQueryWrapper.eq("project_type", renterId);
                        }
                    }
                }
                // 如果没有传入source参数
                else {
                    // 使用OR条件组合两种离线情况的查询
                    objectQueryWrapper.and(qw -> qw
                            .eq("ps_status", "0")
                            .eq("source", "1")
                    ).or(qw -> qw
                            .eq("ps_status", "-1")
                            .eq("source", "2")
                    );
                    if(StringUtils.isNotBlank(renterId)){
                        if(renterId.equals("1964826626")){
                            renterId="-624947198";
                        }
                        objectQueryWrapper.eq("project_type", renterId);
                    }
                }
                if (StringUtils.isNotBlank(deviceNameOrSn)) {
                    objectQueryWrapper.like("device_name", deviceNameOrSn);
                }
            }

            //接入中
            if("5".equals(tabType)){
                objectQueryWrapper.eq("valid_flag", "3");
                if (StringUtils.isNotBlank(deviceNameOrSn)) {
                    objectQueryWrapper.like("device_name", deviceNameOrSn);
                }
            }

        }

        // 执行查询
        IPage<Device> page = iDeviceService.page(pageData, objectQueryWrapper);
        //处理一下月发电量和年发电量数据
        List<Device> records = page.getRecords();
        if(records != null && records.size()>0){
            for (int i = 0; i < records.size(); i++) {
                Integer deviceId = records.get(i).getDeviceId();
                List<Integer> integers = new ArrayList<>();
                integers.add(deviceId);
                int source = records.get(i).getSource();
                //阳光云逻辑
                //通过电站id查询电站月报表，查询当月的月份的发电量
                if(source ==1){
                    String psKey1 = iDeviceDetailSolarService.plantPsKey(deviceId);
                    String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                    QueryWrapper<DevicePointMonth> devicePointMonthQueryWrapper = new QueryWrapper<>();
                    devicePointMonthQueryWrapper.eq("ps_key",psKey1);
                    devicePointMonthQueryWrapper.eq("time_stamp",currentMonth);
                    DevicePointMonth one = iDevicePointMonthService.getOne(devicePointMonthQueryWrapper);
                    if(one != null){
                        String p83022 = one.getP83022();
                        records.get(i).setMonthEnergy(new BigDecimal(p83022).divide(new BigDecimal(1000).setScale(2,RoundingMode.UP)).toString());
                    }

                    String currentYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
                    QueryWrapper<DevicePointYear> devicePointYearQueryWrapper = new QueryWrapper<>();
                    devicePointYearQueryWrapper.eq("ps_key",psKey1);
                    devicePointYearQueryWrapper.eq("time_stamp",currentYear);
                    DevicePointYear devicePointYear = iDevicePointYearService.getOne(devicePointYearQueryWrapper);
                    if(devicePointYear != null){
                        String p83022 = devicePointYear.getP83022();
                        records.get(i).setYearEnergy(new BigDecimal(p83022).divide(new BigDecimal(1000).setScale(2,RoundingMode.UP)).toString());
                    }
                    QueryWrapper<DeviceDetailSolar> detailSolarQueryWrapper = new QueryWrapper<>();
                    detailSolarQueryWrapper.eq("ps_id",deviceId);
                    DeviceDetailSolar deviceDetailSolar = iDeviceDetailSolarService.getOne(detailSolarQueryWrapper);
                    if (deviceDetailSolar != null) {
                        records.get(i).setPsKey(deviceDetailSolar.getPsKey());
                    }

//                    List<String> psKeyListByPlantId = iDeviceEquipmentService.getPsKeyListByPlantIdInteger(integers);
//                    if(psKeyListByPlantId != null && psKeyListByPlantId.size()>0){
//                        DeviceStatsVo pointDataInPsKey = iDeviceEquipmentPointService.getPointDataInPsKey(psKeyListByPlantId);
//                        if(pointDataInPsKey != null){






//                            String monthlyEnergy = pointDataInPsKey.getMonthlyEnergy();
//                            String yearlyEnergy = pointDataInPsKey.getYearlyEnergy();
//                            if(StringUtils.isNotBlank(monthlyEnergy)){
//                                BigDecimal monthNumber = new BigDecimal(monthlyEnergy);
//                                // 除以1000
//                                BigDecimal resultMonthly = monthNumber.divide(new BigDecimal("1000"));
//                                records.get(i).setMonthEnergy(resultMonthly.toString());
//                            }
//                            if(StringUtils.isNotBlank(yearlyEnergy)){
//                                BigDecimal yearNumber = new BigDecimal(yearlyEnergy);
//                                BigDecimal resultYearly = yearNumber.divide(new BigDecimal("1000"));
//                                records.get(i).setYearEnergy(resultYearly.toString());
//                            }
//                            //封装电站的psKey
//
//                        }
//                    }
                }
                //古瑞瓦特逻辑（需要一张表然后从表里面查询数据）
                if(source ==2){
                    DeviceStatsVo statsData = iDeviceService.getStatsData(deviceId);
                    if(statsData != null){
                        String monthlyEnergy = statsData.getMonthlyEnergy();
                        String yearEnergy = statsData.getYearlyEnergy();
                        String todayEnergy = statsData.getTodayEnergy();
                        records.get(i).setMonthEnergy(monthlyEnergy);
                        records.get(i).setYearEnergy(yearEnergy);
                        records.get(i).setTodayEnergy(todayEnergy);
                    }
                    String currPower = records.get(i).getCurrPower();
                    if(StringUtils.isNotBlank(currPower)){
                        String s = new BigDecimal(currPower).divide(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString();
                        records.get(i).setCurrPower(s);
                    }
                }
                //前端

                int returnSource = records.get(i).getSource();
                if(returnSource == 1){
                    Integer returnDeviceId = records.get(i).getDeviceId();
                    QueryWrapper<DeviceEquipment> baseWrapper = new QueryWrapper<>();
                    baseWrapper.eq("ps_id", returnDeviceId).eq("source", 1);

                    // 执行查询获取psKeys
                    List<String> psKeys = iDeviceEquipmentService.list(baseWrapper)
                            .stream()
                            .map(DeviceEquipment::getPsKey)
                            .collect(Collectors.toList());
                    QueryWrapper<DeviceFault> deviceFaultQueryWrapper = new QueryWrapper<>();
                    if(psKeys != null && psKeys.size()>0){
                        deviceFaultQueryWrapper.in("ps_key",psKeys);
                    }
                    //待处理的故障的
                    deviceFaultQueryWrapper.notIn("process_status",4,9);
                    List<DeviceFault> list = iDeviceFaultService.list(deviceFaultQueryWrapper);
                    //待处理的告警的
                    if(list != null){
                        for (int j = 0; j < list.size(); j++) {
                            String psKey = list.get(j).getPsKey();
                            DeviceEquipment deviceEquipmentByPsKey = iDeviceEquipmentService.getDeviceEquipmentByPsKey(psKey);
                            if(deviceEquipmentByPsKey != null){
                                list.get(j).setDeviceSn(deviceEquipmentByPsKey.getDeviceSn());
                            }
                        }
                    }
                    records.get(i).setDeviceFaults(list);
                }
                if(returnSource == 2){
                    Integer returnDeviceId = records.get(i).getDeviceId();
                    QueryWrapper<DeviceEquipment> baseWrapper = new QueryWrapper<>();
                    baseWrapper.eq("ps_id", returnDeviceId).eq("source", 2);

                    // 执行查询获取psKeys
                    List<String> deviceSns = iDeviceEquipmentService.list(baseWrapper)
                            .stream()
                            .map(DeviceEquipment::getDeviceSn)
                            .collect(Collectors.toList());
                    QueryWrapper<DeviceFault> deviceFaultQueryWrapper = new QueryWrapper<>();
                    if(deviceSns != null && deviceSns.size()>0){
                        deviceFaultQueryWrapper.in("device_sn",deviceSns);
                    }
                    //待处理的
                    List<DeviceFault> list = iDeviceFaultService.list(deviceFaultQueryWrapper);
                    records.get(i).setDeviceFaults(list);
                }
            }
        }
        page.setRecords(records);
        return Result.ok(page);
    }

    @ApiOperation("电量报表-电站信息")
    @RequestMapping(value = "/devicePowerReport", method = RequestMethod.POST)
    public Result devicePowerReport(@org.springframework.web.bind.annotation.RequestBody DevicePageVo devicePageVo) throws Exception {
        QueryWrapper<Device> objectQueryWrapper = new QueryWrapper<>();

        // 动态分页参数
        IPage<Device> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        String renterId = devicePageVo.getRenterId();
        //先查询出来各个项目下的电站id集合 在in查询一下
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            objectQueryWrapper.eq("project_type", renterId);
        }
        String queryDate = devicePageVo.getQueryDate();
        IPage<Device> page = iDeviceService.page(pageData, objectQueryWrapper);
        List<Device> records = page.getRecords();
        //电站名称  电站ID  装机功率(kWp)  并网日期 当日发电量(kWh) 总发电量(kWh) 等效小时(h) 今日收入  累计CO₂减排(kg)  电站地址  逆变器S/N集合
        if(records != null && records.size()>0){
            for (int i = 0; i < records.size(); i++) {
                Integer deviceId = records.get(i).getDeviceId();
                List<String> listPsSnByPlantId = iDeviceEquipmentService.getListEquipmentSnByPlantId(String.valueOf(deviceId));
                records.get(i).setEquipmentSnList(listPsSnByPlantId);
                if(records.get(i).getSource() == 1){
                    //如果 日期等于当前时间那就取当前表的数据
                    String deviceName = records.get(i).getDeviceName();
                    String totalCapcity = records.get(i).getTotalCapcity();
                    String gridConnectionTime = records.get(i).getGridConnectionTime();
                    String todayEnergy = records.get(i).getTodayEnergy();
                    String totalEnergy = records.get(i).getTotalEnergy();
                    String equivalentHour = records.get(i).getEquivalentHour();
                    String todayIncome = records.get(i).getTodayIncome();
                    String co2ReduceTotal = records.get(i).getCo2ReduceTotal();
                    String psLocation = records.get(i).getPsLocation();
                }
                if(records.get(i).getSource() == 2){
                    String totalCapcity = records.get(i).getTotalCapcity();
                    if(this.isPositiveNumber(totalCapcity)){
                        String s = new BigDecimal(totalCapcity).setScale(2, RoundingMode.UP).toString();
                        records.get(i).setTotalCapcity(s);
                    }
                }
            }
        }
        return Result.ok(page);
    }


    public  boolean isPositiveNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.trim().matches("\\d+(\\.\\d+)?");
    }

    @ApiOperation("电量报表-电站柱状图")
    @RequestMapping(value = "/deviceEquipmentListChartByPlantId", method = RequestMethod.GET)
    public Result deviceEquipmentListChartByPlantId(DevicePageVo devicePageVo) throws Exception {
        //查询类型 1：日  2：月  3：年 4:总  5：自定义
//        String queryType = devicePageVo.getQueryType();
        String minType = devicePageVo.getMinType();
        String queryDate = devicePageVo.getQueryDate();
        QueryWrapper<DevicePointDay> deviceEquipmentPointDayQueryWrapper = new QueryWrapper<>();
        //日数据
        if("day".equals(minType)){
            LocalDate query = LocalDate.parse(queryDate);
            String endTime = query.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            deviceEquipmentPointDayQueryWrapper.eq("time_stamp",endTime);
            List<DevicePointDay> deviceList = iDevicePointDayService.list(deviceEquipmentPointDayQueryWrapper);
            if(deviceList != null && deviceList.size()>0){
                Map<String, DayStatistics> collect = deviceList.stream()
                        .collect(Collectors.groupingBy(
                                DevicePointDay::getTimeStamp, // 按时间分组
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> calculateDayStatistics(list)
                                )
                        ));
                return Result.ok(collect);
            }
            //计算一下总值 和平均值

        }
        //月数据
        if("month".equals(minType)){
            LocalDate query = LocalDate.parse(queryDate);
            String endTime = query.format(DateTimeFormatter.ofPattern("yyyyMM"));
            deviceEquipmentPointDayQueryWrapper.like("time_stamp",endTime);
            List<DevicePointDay> deviceList = iDevicePointDayService.list(deviceEquipmentPointDayQueryWrapper);
            //计算一下总值 和平均值
            if(deviceList != null && deviceList.size()>0){
                Map<String, DayStatistics> collect = deviceList.stream()
                        .collect(Collectors.groupingBy(
                                DevicePointDay::getTimeStamp, // 按时间分组
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> calculateDayStatistics(list) // 使用分组后的数据计算
                                )
                        ));

                return Result.ok(collect);
            }
        }
        //年数据
        if("year".equals(minType)){
            QueryWrapper<DevicePointMonth> devicePointMonthQueryWrapper = new QueryWrapper<>();
            LocalDate query = LocalDate.parse(queryDate);
            String endTime = query.format(DateTimeFormatter.ofPattern("yyyy"));
            deviceEquipmentPointDayQueryWrapper.like("time_stamp",endTime);
            List<DevicePointMonth> deviceList = iDevicePointMonthService.list(devicePointMonthQueryWrapper);
            //计算一下总值 和平均值
            if(deviceList != null && deviceList.size()>0){
                Map<String, DayStatistics> collect = deviceList.stream()
                        .collect(Collectors.groupingBy(
                                DevicePointMonth::getTimeStamp, // 按时间分组
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> calculateMonthStatistics(list) // 使用分组后的数据计算
                                )
                        ));
                return Result.ok(collect);
            }
        }
        //总数据
        if("all".equals(minType)){
            QueryWrapper<DevicePointYear> devicePointYearQueryWrapper = new QueryWrapper<>();
            List<DevicePointYear> deviceList = iDevicePointYearService.list(devicePointYearQueryWrapper);
            if(deviceList != null && deviceList.size()>0){
                Map<String, DayStatistics> collect = deviceList.stream()
                        .collect(Collectors.groupingBy(
                                DevicePointYear::getTimeStamp, // 按时间分组
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> calculateYearStatistics(deviceList) // 使用分组后的数据计算
                                )
                        ));
                return Result.ok(collect);
            }
        }

        //自定义数据
        if("customize".equals(minType)){
            String startDate = devicePageVo.getStartDate();
            String endDate = devicePageVo.getEndDate();
            LocalDate start = LocalDate.parse(startDate);
            String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate end = LocalDate.parse(endDate);
            String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            deviceEquipmentPointDayQueryWrapper.ge("time_stamp",startTime);
            deviceEquipmentPointDayQueryWrapper.le("time_stamp",endTime);
            List<DevicePointDay> deviceList = iDevicePointDayService.list(deviceEquipmentPointDayQueryWrapper);
            if(deviceList != null && deviceList.size()>0){
                Map<String, DayStatistics> collect = deviceList.stream()
                        .collect(Collectors.groupingBy(
                                DevicePointDay::getTimeStamp, // 按时间分组
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> calculateDayStatistics(deviceList) // 使用分组后的数据计算
                                )
                        ));
                return Result.ok(collect);
            }

        }


        return Result.ok();
    }



    private DayStatistics calculateDayStatistics(List<DevicePointDay> dayList) {
        DoubleSummaryStatistics statsP83022 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83022()))
                .summaryStatistics();

        DoubleSummaryStatistics statsP83025 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83025()))
                .summaryStatistics();

        // 计算并格式化结果
        double sumP83022 = formatDecimal(statsP83022.getSum() / 1000.0);
        double averageP83025 = formatDecimal(statsP83025.getAverage());
        double revenue = formatDecimal(statsP83022.getSum() / 1000.0);

        return new DayStatistics(sumP83022, averageP83025, dayList.size(), revenue);
    }

    private DayStatistics calculateMonthStatistics(List<DevicePointMonth> dayList) {
        DoubleSummaryStatistics statsP83022 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83022()))
                .summaryStatistics();

        DoubleSummaryStatistics statsP83025 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83025()))
                .summaryStatistics();

        // 计算并格式化结果
        double sumP83022 = formatDecimal(statsP83022.getSum() / 1000.0);
        double averageP83025 = formatDecimal(statsP83025.getAverage());
        double revenue = formatDecimal(statsP83022.getSum() / 1000.0);

        return new DayStatistics(sumP83022, averageP83025, dayList.size(), revenue);
    }

    private DayStatistics calculateYearStatistics(List<DevicePointYear> dayList) {
        DoubleSummaryStatistics statsP83022 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83022()))
                .summaryStatistics();

        DoubleSummaryStatistics statsP83025 = dayList.stream()
                .mapToDouble(device -> parseDoubleSafely(device.getP83025()))
                .summaryStatistics();

        // 计算并格式化结果
        double sumP83022 = formatDecimal(statsP83022.getSum() / 1000.0);
        double averageP83025 = formatDecimal(statsP83025.getAverage());
        double revenue = formatDecimal(statsP83022.getSum() / 1000.0);

        return new DayStatistics(sumP83022, averageP83025, dayList.size(), revenue);
    }


    private double formatDecimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    private  double parseDoubleSafely(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }



    @ApiOperation("根据电站id获取设备列表")
    @RequestMapping(value = "/deviceEquipmentListByPlantId", method = RequestMethod.GET)
    public Result deviceEquipmentListByPlantId(DevicePageVo devicePageVo) throws Exception {
        String plantId = devicePageVo.getPlantId();
        if(StringUtils.isNotBlank(plantId)){
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.eq("ps_id",plantId);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if(list != null){
                return Result.ok(list);
            }
        }
        return Result.ok();
    }


    @ApiOperation("获取电站概览信息")
    @RequestMapping(value = "/deviceSummaryByPlantId", method = RequestMethod.GET)
    public Result deviceSummaryByPlantId(DevicePageVo devicePageVo) throws Exception {
        String plantId = devicePageVo.getPlantId();
        if(StringUtils.isNotBlank(plantId)){
            QueryWrapper<Device> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.eq("device_id",plantId);
            Device one = iDeviceService.getOne(deviceEquipmentQueryWrapper);
            if(one != null){
                int source = one.getSource();
                if(source == 2){
                    String currPower = one.getCurrPower();
                    if(StringUtils.isNotBlank(currPower)){
                        String s = new BigDecimal(currPower).divide(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString();
                        one.setCurrPower(s);
                    }
                }
            }
            return Result.ok(one);
        }
        return Result.ok();
    }

    @ApiOperation("根据电站id获取电站下所有设备的故障列表")
    @RequestMapping(value = "/deviceEquipmentFaultListByPlantId", method = RequestMethod.GET)
    public Result deviceEquipmentFaultListByPlantId(DevicePageVo devicePageVo) throws Exception {
        String plantId = devicePageVo.getPlantId();
        Integer source = devicePageVo.getPsSource();
        String tabType = devicePageVo.getTabType();
        if(source != null && StringUtils.isNotBlank(plantId)){
            if("1".equals(tabType)){
                List<DeviceFault> faultListByPlantId = iDeviceFaultService. getFaultListByPlantId(devicePageVo,plantId, source);
                return Result.ok(faultListByPlantId);
            }
            if("2".equals(tabType)){
                List<DeviceFaultHistory> faultListByPlantId = iDeviceFaultService. getFaultListByPlantIdHistory(devicePageVo,plantId, source);
                return Result.ok(faultListByPlantId);
            }
        }
        return Result.ok();
    }


    @ApiOperation("获取所有电站列表及电站下设备或者通讯模块数据")
    @RequestMapping(value = "/deviceEquipmentTreeList", method = RequestMethod.GET)
    public Result deviceEquipmentTreeList(DevicePageVo devicePageVo) throws Exception {
        List<Device> devices = iDeviceService.list();
        //电站id
        String plantId = devicePageVo.getPlantId();
        //
        if(StringUtils.isNotBlank(plantId)){
            ArrayList<Map<String, List<DeviceEquipment>>> maps = new ArrayList<>();
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.eq("ps_id",plantId);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            Map<Integer, List<DeviceEquipment>> collect = list.stream()
                    .collect(Collectors.groupingBy(
                            DeviceEquipment::getDeviceType,
                            Collectors.toList()
                    ));
            return Result.ok(collect);
        }

        return Result.ok(devices);
    }


    @ApiOperation("不同状态的电站数量")
    @RequestMapping(value = "/devicePageListStatusCount", method = RequestMethod.GET)
    public Result devicePageListStatusCount(DevicePageVo devicePageVo) throws Exception {
        HashMap<String, String> dataHashMap = new HashMap<>();

        // 获取查询参数
        Integer supSource = devicePageVo.getSource();
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();
        String deviceType = devicePageVo.getDeviceType();
        String equivalentHourMin = devicePageVo.getEquivalentHourMin();
        String equivalentHourMax = devicePageVo.getEquivalentHourMax();
        String address = devicePageVo.getAddress();
        String renterId = devicePageVo.getRenterId();

        // 构建基础查询条件（用于所有统计）
        QueryWrapper<Device> baseQueryWrapper = new QueryWrapper<>();

        // 设备名称或序列号筛选
        if (StringUtils.isNotBlank(deviceNameOrSn)) {
            baseQueryWrapper.and(wrapper -> wrapper.like("device_name", deviceNameOrSn));
        }

        // 设备类型筛选
        if (StringUtils.isNotBlank(deviceType)) {
            baseQueryWrapper.eq("ps_type", deviceType);
        }

        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            baseQueryWrapper.eq("project_type", renterId);
        }

        // 等效小时范围筛选
        if (StringUtils.isNotBlank(equivalentHourMin)) {
            baseQueryWrapper.ge("equivalent_hour", equivalentHourMin);
        }
        if (StringUtils.isNotBlank(equivalentHourMax)) {
            baseQueryWrapper.le("equivalent_hour", equivalentHourMax);
        }
        //按照地址查询
        if(StringUtils.isNotBlank(address)){
            baseQueryWrapper.like("device_name", address);
        }

        // 已关注的电站数量
        QueryWrapper<Device> concernQuery = baseQueryWrapper.clone();
        concernQuery.eq("is_concern", "1");
        if (supSource != null) {
            concernQuery.eq("source", supSource);
        } else {
            // 如果没有指定source，只统计source=1的关注设备（根据原始逻辑）
            concernQuery.eq("source", "1");
        }
        long concernCount = iDeviceService.count(concernQuery);
        dataHashMap.put("concern", String.valueOf(concernCount));

        // 正常电站数量
        long normalCount = 0;
        if(supSource == null){
            QueryWrapper<Device> normalQuery = baseQueryWrapper.clone();
            normalQuery.and(wrapper -> wrapper
                            .eq("ps_fault_status", "3")
                            .ne("ps_status", "0")
                            .eq("source", "1")
                    )
                    .or(wrapper -> wrapper
                            .eq("ps_status", "1")
                            .eq("source", "2")
                    );
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                normalQuery.eq("project_type", renterId);
            }
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                normalQuery.like("device_name", deviceNameOrSn);
            }
            normalCount = iDeviceService.count(normalQuery);
        }
        if(supSource != null && supSource == 1){
            QueryWrapper<Device> normalQuery1 = baseQueryWrapper.clone();
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                normalQuery1.eq("project_type", renterId);
            }

            normalQuery1.eq("ps_fault_status", "3")
                    .ne("ps_status", "0")
                    .eq("source", "1");
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                normalQuery1.like("device_name", deviceNameOrSn);
            }
            normalCount = iDeviceService.count(normalQuery1);
        }

        if(supSource != null && supSource == 2){
            QueryWrapper<Device> normalQuery2 = baseQueryWrapper.clone();
            normalQuery2.eq("ps_status", "1")
                    .eq("source", "2");
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                normalQuery2.eq("project_type", renterId);
            }

            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                normalQuery2.like("device_name", deviceNameOrSn);
            }
            normalCount = iDeviceService.count(normalQuery2);
        }


        dataHashMap.put("normalCount", String.valueOf(normalCount));

        // 异常电站数量
        long abnormalCount = 0;

        if(supSource == null){
            QueryWrapper<Device> abnormalQuery = baseQueryWrapper.clone();
            abnormalQuery.and(wrapper ->
                    wrapper.in("ps_fault_status", "1", "2")
                            .ne("ps_status", "0")
                            .eq("source", "1")
            ).or(wrapper ->
                    wrapper.eq("ps_status", "4")
                            .eq("source", "2")
            );
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                abnormalQuery.eq("project_type", renterId);
            }
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                abnormalQuery.like("device_name", deviceNameOrSn);
            }
            abnormalCount = iDeviceService.count(abnormalQuery);
        }
        if (supSource != null && supSource == 1) {
            QueryWrapper<Device> abnormalQuery1 = baseQueryWrapper.clone();
            abnormalQuery1.in("ps_fault_status", "1", "2")
                    .ne("ps_status", "0")
                    .eq("source", "1");
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                abnormalQuery1.eq("project_type", renterId);
            }
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                abnormalQuery1.like("device_name", deviceNameOrSn);
            }
            abnormalCount = iDeviceService.count(abnormalQuery1);
        }
        if (supSource != null &&  supSource == 2) {
            QueryWrapper<Device> abnormalQuery2 = baseQueryWrapper.clone();
            abnormalQuery2.eq("ps_status", "4")
                    .eq("source", "2");
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                abnormalQuery2.eq("project_type", renterId);
            }
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                abnormalQuery2.like("device_name", deviceNameOrSn);
            }
            abnormalCount = iDeviceService.count(abnormalQuery2);
        }
        dataHashMap.put("abnormalCount", String.valueOf(abnormalCount));

        // 离线电站数量
        long offlineCount = 0;
        if(supSource == null){
            QueryWrapper<Device> offlineQuery = baseQueryWrapper.clone();
            offlineQuery.nested(wrapper -> wrapper
                    .eq("ps_status", "0").eq("source", "1")
                    .or()
                    .eq("ps_status", "-1").eq("source", "2")
            );
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                offlineQuery.eq("project_type", renterId);
            }
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                offlineQuery.like("device_name", deviceNameOrSn);
            }
            offlineCount = iDeviceService.count(offlineQuery);
        }
        if (supSource != null && supSource == 1) {
            QueryWrapper<Device> offlineQuery1 = baseQueryWrapper.clone();
            offlineQuery1.eq("ps_status", "0")
                    .eq("source", "1");
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                offlineQuery1.like("device_name", deviceNameOrSn);
            }
            offlineCount = iDeviceService.count(offlineQuery1);
        }
        if (supSource != null && supSource == 2) {
            QueryWrapper<Device> offlineQuery2 = baseQueryWrapper.clone();
            offlineQuery2.eq("ps_status", "-1")
                    .eq("source", "2");
            if (StringUtils.isNotBlank(deviceNameOrSn)) {
                offlineQuery2.like("device_name", deviceNameOrSn);
            }
            offlineCount = iDeviceService.count(offlineQuery2);
        }
        dataHashMap.put("offlineCount", String.valueOf(offlineCount));

        // 接入中电站数量
        QueryWrapper<Device> accessQuery = baseQueryWrapper.clone();
        accessQuery.eq("valid_flag", "3");
        if (supSource != null) {
            accessQuery.eq("source", supSource);
        } else {
            // 如果没有指定source，只统计source=1的接入中设备（根据原始逻辑）
            accessQuery.eq("source", "1");
        }
        long accessCount = iDeviceService.count(accessQuery);
        dataHashMap.put("accessCount", String.valueOf(accessCount));
        return Result.ok(dataHashMap);
    }

    @ApiOperation("获取电站的逆变器集合")
    @RequestMapping(value = "/getEquipmentForPlant", method = RequestMethod.GET)
    public Result getEquipmentForPlant(DevicePageVo devicePageVo) throws Exception {
        //逆变器的pskKey
        String plantId = devicePageVo.getPlantId();
        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        deviceEquipmentQueryWrapper.eq("ps_id",plantId);
        List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
        return Result.ok(list);
    }

    @ApiOperation("电站内逆变器曲线图")
    @RequestMapping(value = "/deviceCurveGraph", method = RequestMethod.GET)
    public Result deviceCurveGraph(DevicePageVo devicePageVo) throws Exception {
        //逆变器的pskKey
        String psKeyOne = devicePageVo.getPsKeyOne();
        String queryDate = devicePageVo.getQueryDate();
        //   查询的分钟类型  5 15 30 60
        String minuteInterval = devicePageVo.getMinuteInterval();
        Integer pageNo = devicePageVo.getPageNo();
        Integer pageSize = devicePageVo.getPageSize();
        if(StringUtils.isNotBlank(minuteInterval)){
            if(minuteInterval.equals("5")){

                IPage<DeviceEquipmentPointMinSixty> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointMinSixtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinSixtyQueryWrapper.eq("ps_key",psKeyOne);
                String dbDatePrefix = queryDate.replace("-", ""); // 20251009
                deviceEquipmentPointMinSixtyQueryWrapper.likeRight("time_stamp", dbDatePrefix);
                iDeviceEquipmentPointMinSixtyService.page(pageData,deviceEquipmentPointMinSixtyQueryWrapper);

            }

            if(minuteInterval.equals("15")){

            }
            if(minuteInterval.equals("30")){

            }
            if(minuteInterval.equals("60")){
                IPage<DeviceEquipmentPointMinSixty> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DeviceEquipmentPointMinSixty> deviceEquipmentPointMinSixtyQueryWrapper = new QueryWrapper<>();
                deviceEquipmentPointMinSixtyQueryWrapper.eq("ps_key",psKeyOne);
                String dbDatePrefix = queryDate.replace("-", ""); // 20251009
                deviceEquipmentPointMinSixtyQueryWrapper.likeRight("time_stamp", dbDatePrefix);
                iDeviceEquipmentPointMinSixtyService.page(pageData,deviceEquipmentPointMinSixtyQueryWrapper);



            }
        }

        return Result.ok();


    }


    @ApiOperation("电站概览曲线图")
    @RequestMapping(value = "/deviceDetailCurveGraph", method = RequestMethod.GET)
    public Result deviceDetailCurveGraph(DevicePageVo devicePageVo) throws Exception {
        //电站pskey  //如果是古瑞瓦特的就传psId
        String psKeyOne = devicePageVo.getPsKeyOne();
        //查询日期
        String queryDate = devicePageVo.getQueryDate();
        //开始时间
        String startDate = devicePageVo.getStartDate();
        //结束时间
        String endDate = devicePageVo.getEndDate();
        //   查询的分钟类型  5 15 30 60
        String minuteInterval = devicePageVo.getMinuteInterval();
        Integer source = devicePageVo.getSource();
        //查询日
        if(minuteInterval != null && minuteInterval.equals("60")){
            if(source == 1){
                QueryWrapper<DevicePointMinSixty> devicePointDayQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    devicePointDayQueryWrapper.like("time_stamp",yyyyMMdd);

                }
                devicePointDayQueryWrapper.orderByAsc("time_stamp");
                devicePointDayQueryWrapper.eq("ps_key",psKeyOne);
                List<DevicePointMinSixty> list = iDevicePointMinSixtyService.list(devicePointDayQueryWrapper);
                if(list != null && list.size()>0){
                    for (int i = 0; i < list.size(); i++) {
                        String p83022 = list.get(i).getP83022();
                        String s = new BigDecimal(p83022).divide(new BigDecimal(1000).setScale(2, RoundingMode.UP)).toString();
                        list.get(i).setP83022(s);
                    }
                }
                return Result.ok(list);
            }
            if (source == 2) {
                //古瑞瓦特查询日的数据，就是查询的电站
                QueryWrapper<DevicePointMinFive> devicePointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate start = LocalDate.parse(queryDate);
                    LocalDate end = LocalDate.parse(queryDate);
                    // 开始时间设置为当天的 00:00:00
                    String startTime = start.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
                    // 结束时间设置为当天的 23:59:59
                    String endTime = end.format(DateTimeFormatter.BASIC_ISO_DATE) + "235959";
                    devicePointDayGrwQueryWrapper.between("time_stamp", startTime, endTime);
                }
                devicePointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                devicePointDayGrwQueryWrapper.orderByAsc("time_stamp");
                List<DevicePointMinFive> list = iDevicePointMinFiveService.list(devicePointDayGrwQueryWrapper);
                if(list != null){
                    for (int i = 0; i < list.size(); i++) {
                        String p83033 = list.get(i).getP83033();
                        String s = new BigDecimal(p83033).setScale(2, RoundingMode.UP).toString();
                        list.get(i).setP83033(s);
                    }
                }
                if(list != null){
                    for (int i = 0; i < list.size(); i++) {
                        String p83033 = list.get(i).getP83033();
                        String s = new BigDecimal(p83033).setScale(2, RoundingMode.UP).toString();
                        list.get(i).setP83033(s);
                    }
                }
                QueryWrapper<DevicePointDayGrw> deviceDetailGrowattQueryWrapper = new QueryWrapper<>();
                deviceDetailGrowattQueryWrapper.eq("ps_key",psKeyOne);
                deviceDetailGrowattQueryWrapper.eq("time_stamp",queryDate);
                DevicePointDayGrw one = iDevicePointDayGrwService.getOne(deviceDetailGrowattQueryWrapper);
                if(one != null){
                    if(list!= null && list.size()>0){
                        list.get(0).setP83022(one.getP83022());
                    }

                }
                return Result.ok(list);
            }


//            QueryWrapper<DevicePointMinFive> devicePointMinFiveQueryWrapper = new QueryWrapper<>();
//            if(StringUtils.isNotBlank(queryDate)){
//                LocalDate date = LocalDate.parse(queryDate);
//                String yyyyMMdd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//                devicePointMinFiveQueryWrapper.like("time_stamp",yyyyMMdd);
//
//            }
//            devicePointMinFiveQueryWrapper.eq("ps_key",psKeyOne);
//            List<DevicePointMinFive> list = iDevicePointMinFiveService.list(devicePointMinFiveQueryWrapper);
//            return Result.ok(list);
        }
        //查询周
        if(minuteInterval != null && minuteInterval.equals("7")){
            //如果是古瑞瓦特的电站
            if(source == 2){
                QueryWrapper<DevicePointDayGrw> devicePointDayGrwQueryWrapper = new QueryWrapper<>();
                devicePointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                devicePointDayGrwQueryWrapper.ge("time_stamp",startDate);
                devicePointDayGrwQueryWrapper.le("time_stamp",endDate);
                devicePointDayGrwQueryWrapper.orderByAsc("time_stamp");
                List<DevicePointDayGrw> list = iDevicePointDayGrwService.list(devicePointDayGrwQueryWrapper);
                return Result.ok(list);
            }

            QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                LocalDate start = LocalDate.parse(startDate);
                String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalDate end = LocalDate.parse(endDate);
                String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                devicePointDayQueryWrapper.ge("time_stamp",startTime);
                devicePointDayQueryWrapper.le("time_stamp",endTime);
            }
            devicePointDayQueryWrapper.eq("ps_key",psKeyOne);
            devicePointDayQueryWrapper.orderByAsc("time_stamp");

            List<DevicePointDay> list = iDevicePointDayService.list(devicePointDayQueryWrapper)
                    .stream()
                    .peek(devicePointDay -> {
                        if (devicePointDay.getP83022() != null) {
                            try {
                                double value = Double.parseDouble(devicePointDay.getP83022()) / 1000.0;
                                devicePointDay.setP83022(String.valueOf(value));
                            } catch (NumberFormatException e) {
                                // 处理转换异常
                            }
                        }
                    })
                    .collect(Collectors.toList());
            return Result.ok(list);
        }

        //查询月
        if(minuteInterval != null && minuteInterval.equals("30")){

            //如果是古瑞瓦特的电站
            if(source == 2){
                QueryWrapper<DevicePointDayGrw> devicePointDayGrwQueryWrapper = new QueryWrapper<>();
                devicePointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                LocalDate date = LocalDate.parse(queryDate);
                String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                devicePointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
                devicePointDayGrwQueryWrapper.orderByAsc("time_stamp");
                List<DevicePointDayGrw> list = iDevicePointDayGrwService.list(devicePointDayGrwQueryWrapper);
                return Result.ok(list);
            }

            QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(queryDate)){
                LocalDate date = LocalDate.parse(queryDate);
                String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
                devicePointDayQueryWrapper.like("time_stamp",yyyyMM);
            }
            devicePointDayQueryWrapper.eq("ps_key",psKeyOne);
            List<DevicePointDay> list = iDevicePointDayService.list(devicePointDayQueryWrapper)
                    .stream()
                    .peek(devicePointDay -> {
                        if (devicePointDay.getP83022() != null) {
                            try {
                                double value = Double.parseDouble(devicePointDay.getP83022()) / 1000.0;
                                devicePointDay.setP83022(String.valueOf(value));
                            } catch (NumberFormatException e) {
                                // 处理转换异常
                            }
                        }
                    })
                    .collect(Collectors.toList());
            return Result.ok(list);
        }

        //查年
        if(minuteInterval != null && minuteInterval.equals("365")){
            if(source == 2){
                QueryWrapper<DevicePointMonthGrw> devicePointMonthGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                    devicePointMonthGrwQueryWrapper.like("time_stamp",yyyy);
                    devicePointMonthGrwQueryWrapper.eq("ps_key",psKeyOne);
                }
                List<DevicePointMonthGrw> list = iDevicePointMonthGrwService.list(devicePointMonthGrwQueryWrapper);
                return Result.ok(list);
            }

            QueryWrapper<DevicePointMonth> devicePointMonthQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(queryDate)){
                LocalDate date = LocalDate.parse(queryDate);
                String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                devicePointMonthQueryWrapper.eq("ps_key",psKeyOne);
                devicePointMonthQueryWrapper.like("time_stamp",yyyy);
            }
            devicePointMonthQueryWrapper.eq("ps_key",psKeyOne);
            devicePointMonthQueryWrapper.orderByAsc("time_stamp");
            List<DevicePointMonth> list = iDevicePointMonthService.list(devicePointMonthQueryWrapper)
                    .stream()
                    .peek(devicePointDay -> {
                        if (devicePointDay.getP83022() != null) {
                            try {
                                double value = Double.parseDouble(devicePointDay.getP83022()) / 1000.0;
                                devicePointDay.setP83022(String.valueOf(value));
                            } catch (NumberFormatException e) {
                                // 处理转换异常
                            }
                        }
                    })
                    .collect(Collectors.toList());
            return Result.ok(list);
        }

        //查询总计
        if(minuteInterval != null && minuteInterval.equals("999")){
            if(source == 2){
                QueryWrapper<DevicePointYearGrw> devicePointYearGrwQueryWrapper = new QueryWrapper<>();
                devicePointYearGrwQueryWrapper.eq("ps_key",psKeyOne);
                List<DevicePointYearGrw> list = iDevicePointYearGrwService.list(devicePointYearGrwQueryWrapper);
                return Result.ok(list);
            }

            QueryWrapper<DevicePointYear> devicePointYearQueryWrapper = new QueryWrapper<>();
            devicePointYearQueryWrapper.eq("ps_key",psKeyOne);
            List<DevicePointYear> list = iDevicePointYearService.list(devicePointYearQueryWrapper).stream()
                    .peek(devicePointDay -> {
                        if (devicePointDay.getP83022() != null) {
                            try {
                                double value = Double.parseDouble(devicePointDay.getP83022()) / 1000.0;
                                devicePointDay.setP83022(String.valueOf(value));
                            } catch (NumberFormatException e) {
                                // 处理转换异常
                            }
                        }
                    })
                    .collect(Collectors.toList());
            return Result.ok(list);
        }

        //自定义
        if(minuteInterval.equals("customize")){
            if (source == 2) {
                QueryWrapper<DevicePointDayGrw> devicePointDayGrwQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    devicePointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                    devicePointDayGrwQueryWrapper.ge("time_stamp",startDate);
                    devicePointDayGrwQueryWrapper.le("time_stamp",endDate);
                    devicePointDayGrwQueryWrapper.orderByAsc("time_stamp");
                }
                devicePointDayGrwQueryWrapper.eq("ps_key",psKeyOne);
                devicePointDayGrwQueryWrapper.orderByAsc("time_stamp");
                List<DevicePointDayGrw> list = iDevicePointDayGrwService.list(devicePointDayGrwQueryWrapper);
                return Result.ok(list);
            }
            QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                LocalDate start = LocalDate.parse(startDate);
                String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalDate end = LocalDate.parse(endDate);
                String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                devicePointDayQueryWrapper.ge("time_stamp",startTime);
                devicePointDayQueryWrapper.le("time_stamp",endTime);
            }
            devicePointDayQueryWrapper.eq("ps_key",psKeyOne);
            devicePointDayQueryWrapper.orderByAsc("time_stamp");
            List<DevicePointDay> list = iDevicePointDayService.list(devicePointDayQueryWrapper).stream()
                    .peek(devicePointDay -> {
                        if (devicePointDay.getP83022() != null) {
                            try {
                                double value = Double.parseDouble(devicePointDay.getP83022()) / 1000.0;
                                devicePointDay.setP83022(String.valueOf(value));
                            } catch (NumberFormatException e) {
                                // 处理转换异常
                            }
                        }
                    })
                    .collect(Collectors.toList());
            return Result.ok(list);
        }
        return Result.ok();
    }




}
