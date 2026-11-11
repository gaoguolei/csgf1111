package org.jeecg.modules.pcms.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.energy.entity.*;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.util.DateFormatterUtils;
import org.jeecg.modules.pcms.project.util.DecimalFormatterUtils;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "电站测点分钟级数据")
@RestController
@RequestMapping(value = "project/devicePointMin")
public class DevicePointMinController {



    @Autowired
    private IDeviceService iDeviceService;
    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;

    @Autowired
    private IDevicePointDayService iDevicePointDayService;

    @Autowired
    private IDevicePointDayGrwService iDevicePointDayGrwService;
    @Autowired
    private IDevicePointMonthService iDevicePointMonthService;

    @Autowired
    private IDevicePointMonthGrwService iDevicePointMonthGrwService;

    @Autowired
    private IDevicePointYearService iDevicePointYearService;

    @Autowired
    private IDevicePointYearGrwService iDevicePointYearGrwService;

    @Autowired
    private IDevicePointMinFiveService iDevicePointMinFiveService;

    @Autowired
    private IDevicePointMinFifteenService iDevicePointMinFifteenService;

    @Autowired
    private IDevicePointMinThirtyService iDevicePointMinThirtyService;

    @Autowired
    private IDevicePointMinSixtyService iDevicePointMinSixtyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @ApiOperation("获取所有电站")
    @RequestMapping(value = "/devicePageList", method = RequestMethod.GET)
    public Result devicePageList(DevicePageVo devicePageVo) throws Exception {

        Integer sourceRequest = devicePageVo.getSource();
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        // 动态分页参数
        IPage<Device> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        //电站名称
        String deviceNameOrSn = devicePageVo.getDeviceNameOrSn();
        if(StringUtils.isNotBlank(deviceNameOrSn)){
            deviceQueryWrapper.like("device_name",deviceNameOrSn);
        }
        if(sourceRequest != null){
            deviceQueryWrapper.eq("source",sourceRequest);

        }
        String renterId = devicePageVo.getRenterId();
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            deviceQueryWrapper.eq("project_type", renterId);
        }
        IPage<Device> page = iDeviceService.page(pageData, deviceQueryWrapper);
        if(page != null){
            List<Device> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    int source = records.get(i).getSource();
                    if(source == 1){
                        Integer deviceId = records.get(i).getDeviceId();
                        QueryWrapper<DeviceDetailSolar> deviceDetailSolarQueryWrapper = new QueryWrapper<>();
                        deviceDetailSolarQueryWrapper.eq("ps_id",deviceId);
                        DeviceDetailSolar one = iDeviceDetailSolarService.getOne(deviceDetailSolarQueryWrapper);
                        if(one != null){
                            String psKey = one.getPsKey();
                            records.get(i).setPsKey(psKey);
                        }
                    }
                }
                page.setRecords(records);
            }
          return   Result.ok(page);
        }
        return Result.ok();

    }

    @ApiOperation("电站分钟级报表")
    @RequestMapping(value = "/deviceMinReportPageList", method = RequestMethod.POST)
    public Result deviceEquipmentPageList(@RequestBody DevicePageVo devicePageVo) throws Exception {
        List<String> psKey = devicePageVo.getPsKey();
        String minuteInterval = devicePageVo.getMinuteInterval();
        String queryDate = devicePageVo.getQueryDate();
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String minType = devicePageVo.getMinType();
        String renterId = devicePageVo.getRenterId();

        //是谁家的电站 1 阳光云  2 古瑞瓦特
        Integer source = devicePageVo.getSource();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();

        //是否全选
        String isAll = devicePageVo.getIsAll();
        //日
        if((notInPsKey== null  && psKey == null) && StringUtils.isBlank(isAll)){
            return Result.ok();
        }
        //日数据
        if("day".equals(minType)){
            //5分钟数据
            if("5".equals(minuteInterval)){
                // 动态分页参数
                IPage<DevicePointMinFive> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointMinFive> devicePointMinFiveQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointMinFiveQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointMinFiveQueryWrapper.in("ps_key",psKey);
                }
                String dbDateStart = convertToDbDateFormat(queryDate, "start");
                String dbDateEnd = convertToDbDateFormat(queryDate, "end");

                if(StringUtils.isNotBlank(queryDate)){
                    devicePointMinFiveQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                }
                IPage<DevicePointMinFive> page = iDevicePointMinFiveService.page(pageData, devicePointMinFiveQueryWrapper);
                if(page != null){
                    List<DevicePointMinFive> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeString(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(psInfoByPsKey.getDesignCapacity()) : "");
                        }

                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            if("15".equals(minuteInterval)){
                // 动态分页参数
                IPage<DevicePointMinFifteen> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointMinFifteen> devicePointMinFifteenQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointMinFifteenQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointMinFifteenQueryWrapper.in("ps_key",psKey);
                }
                String dbDateStart = convertToDbDateFormat(queryDate, "start");
                String dbDateEnd = convertToDbDateFormat(queryDate, "end");

                if(StringUtils.isNotBlank(queryDate)){
                    devicePointMinFifteenQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                }
                IPage<DevicePointMinFifteen> page = iDevicePointMinFifteenService.page(pageData, devicePointMinFifteenQueryWrapper);
                if(page != null){
                    List<DevicePointMinFifteen> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeString(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(psInfoByPsKey.getDesignCapacity()) : "");
                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            if("30".equals(minuteInterval)){
                // 动态分页参数
                IPage<DevicePointMinThirty> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointMinThirty> devicePointMinThirtyQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointMinThirtyQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointMinThirtyQueryWrapper.in("ps_key",psKey);
                }
                String dbDateStart = convertToDbDateFormat(queryDate, "start");
                String dbDateEnd = convertToDbDateFormat(queryDate, "end");

                if(StringUtils.isNotBlank(queryDate)){
                    devicePointMinThirtyQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                }
                IPage<DevicePointMinThirty> page = iDevicePointMinThirtyService.page(pageData, devicePointMinThirtyQueryWrapper);
                if(page != null){
                    List<DevicePointMinThirty> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeString(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(psInfoByPsKey.getDesignCapacity()) : "");
                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            if("60".equals(minuteInterval)){
                // 动态分页参数
                if(source == 1){
                    IPage<DevicePointDay> pageData = new Page<>(
                            devicePageVo.getPageNo(),
                            devicePageVo.getPageSize()
                    );
                    QueryWrapper<DevicePointDay> devicePointMinSixtyQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(isAll)){
                        List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                        devicePointMinSixtyQueryWrapper.in("ps_key",psKeyList);
                        if(notInPsKey != null && notInPsKey.size()>0){
                            devicePointMinSixtyQueryWrapper.notIn("ps_key",notInPsKey);
                        }
                    }
                    if(psKey != null && psKey.size()>0){
                        devicePointMinSixtyQueryWrapper.in("ps_key",psKey);
                    }

                    if(StringUtils.isNotBlank(queryDate)){
//                    devicePointMinSixtyQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                        LocalDate date = LocalDate.parse(queryDate);
                        String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        devicePointMinSixtyQueryWrapper.eq("time_stamp",yyyyMM);
                    }
                    IPage<DevicePointDay> page = iDevicePointDayService.page(pageData, devicePointMinSixtyQueryWrapper);
                    if(page != null){
                        List<DevicePointDay> records = page.getRecords();
                        for (int i = 0; i < records.size(); i++) {
                            String psKeyParam = records.get(i).getPsKey();
                            String timeStamp = records.get(i).getTimeStamp();
                            if(StringUtils.isNotBlank(timeStamp)){
                                String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                                records.get(i).setFormatTime(s);
                            }
                            PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                            if(psInfoByPsKey != null){
                                records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                                records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                                records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                                records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                                records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                            }
                            //日发电量
                            String p83022 = records.get(i).getP83022();
                            records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                            //累计发电量
                            String p83024 = records.get(i).getP83024();
                            records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                            //等效时长
                            String p83025 = records.get(i).getP83025();
                            records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                            //电站功率
                            String p83033 = records.get(i).getP83033();
                            records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                        }
                        page.setRecords(records);
                        return Result.ok(page);
                    }
                }
                if(source == 2){
                    IPage<DevicePointDayGrw> pageData = new Page<>(
                            devicePageVo.getPageNo(),
                            devicePageVo.getPageSize()
                    );
                    QueryWrapper<DevicePointDayGrw> devicePointDayGrwQueryWrapper = new QueryWrapper<>();
                    if(StringUtils.isNotBlank(isAll)){
                        List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
                        devicePointDayGrwQueryWrapper.in("ps_key",psKeyListGrw);
                        if(notInPsKey != null && notInPsKey.size()>0){
                            devicePointDayGrwQueryWrapper.notIn("ps_key",notInPsKey);
                        }
                    }
                    if(psKey != null && psKey.size()>0){
                        devicePointDayGrwQueryWrapper.in("ps_key",psKey);
                    }

                    if(StringUtils.isNotBlank(queryDate)){
//                    devicePointMinSixtyQueryWrapper.between("time_stamp",dbDateStart,dbDateEnd);
                        devicePointDayGrwQueryWrapper.eq("time_stamp",queryDate);
                    }
                    IPage<DevicePointDayGrw> page = iDevicePointDayGrwService.page(pageData, devicePointDayGrwQueryWrapper);
                    if(page != null){
                        List<DevicePointDayGrw> records = page.getRecords();
                        for (int i = 0; i < records.size(); i++) {
                            String psKeyParam = records.get(i).getPsKey();
                            String timeStamp = records.get(i).getTimeStamp();
                            if(StringUtils.isNotBlank(timeStamp)){
                                String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                                records.get(i).setFormatTime(s);
                            }
                            PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                            if(psInfoByPsKey != null){
                                records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                                records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                                records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                                records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                                records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                            }
                            //日发电量
                            String p83022 = records.get(i).getP83022();
                            records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimals(p83022) : "");
                            //累计发电量
                            String p83024 = records.get(i).getP83024();
                            records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimals(p83024) : "");
                            //等效时长
                            String p83025 = records.get(i).getP83025();
                            records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                            //电站功率
                            String p83033 = records.get(i).getP83033();
                            records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                        }
                        page.setRecords(records);
                        return Result.ok(page);
                    }
                }
            }
        }
        //月数据
        if("month".equals(minType)){
            //是古瑞瓦特的数据
            if(source == 2){
                // 动态分页参数
                IPage<DevicePointDayGrw> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointDayGrw> devicePointDayQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
                    devicePointDayQueryWrapper.in("ps_key",psKeyListGrw);
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointDayQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointDayQueryWrapper.in("ps_key",psKey);
                }
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    devicePointDayQueryWrapper.like("time_stamp",yyyyMM);
                }
                devicePointDayQueryWrapper.orderByAsc("time_stamp");
                IPage<DevicePointDayGrw> page = iDevicePointDayGrwService.page(pageData, devicePointDayQueryWrapper);
                if(page != null) {
                    List<DevicePointDayGrw> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if (StringUtils.isNotBlank(timeStamp)) {
                            String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                        if (psInfoByPsKey != null) {
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ? psInfoByPsKey.getCreateTime() : "");
                            if(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity())){
                                BigDecimal bd = new BigDecimal(psInfoByPsKey.getDesignCapacity());
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                records.get(i).setDesignCapacity(bd.toString());
                            }                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? p83022: "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimals(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            // 动态分页参数
            IPage<DevicePointDay> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(isAll)){
                List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                devicePointDayQueryWrapper.in("ps_key",psKeyList);
                if(notInPsKey != null && notInPsKey.size()>0){
                    devicePointDayQueryWrapper.notIn("ps_key",notInPsKey);
                }
            }
            if(psKey != null && psKey.size()>0){
                devicePointDayQueryWrapper.in("ps_key",psKey);
            }
            if(StringUtils.isNotBlank(queryDate)){
                LocalDate date = LocalDate.parse(queryDate);
                String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
                devicePointDayQueryWrapper.like("time_stamp",yyyyMM);
            }
            devicePointDayQueryWrapper.orderByAsc("time_stamp");
            IPage<DevicePointDay> page = iDevicePointDayService.page(pageData, devicePointDayQueryWrapper);
            if(page != null){
                List<DevicePointDay> records = page.getRecords();
                for (int i = 0; i < records.size(); i++) {
                    String psKeyParam = records.get(i).getPsKey();
                    String timeStamp = records.get(i).getTimeStamp();
                    if(StringUtils.isNotBlank(timeStamp)){
                        String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                        records.get(i).setFormatTime(s);
                    }
                    PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                    if(psInfoByPsKey != null){
                        records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                        records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                        records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                        records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                        records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                    }
                    //日发电量
                    String p83022 = records.get(i).getP83022();
                    records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                    //累计发电量
                    String p83024 = records.get(i).getP83024();
                    records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                    //等效时长
                    String p83025 = records.get(i).getP83025();
                    records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                    //电站功率
                    String p83033 = records.get(i).getP83033();
                    records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                }
                page.setRecords(records);
                return Result.ok(page);
            }
        }
        //年数据
        if("year".equals(minType)){
            if(source == 2){
                // 动态分页参数
                IPage<DevicePointMonthGrw> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointMonthGrw> devicePointMonthQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    List<String> psKeyList = iDeviceService.getPsKeyListGrw(renterId);
                    devicePointMonthQueryWrapper.in("ps_key",psKeyList);
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointMonthQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointMonthQueryWrapper.in("ps_key",psKey);
                }
                if(StringUtils.isNotBlank(queryDate)){
                    LocalDate date = LocalDate.parse(queryDate);
                    String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                    devicePointMonthQueryWrapper.like("time_stamp",yyyy);
                }
                devicePointMonthQueryWrapper.orderByAsc("time_stamp");
                IPage<DevicePointMonthGrw> page = iDevicePointMonthGrwService.page(pageData, devicePointMonthQueryWrapper);
                if(page != null){
                    List<DevicePointMonthGrw> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeStringYear(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            if(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity())){
                                BigDecimal bd = new BigDecimal(psInfoByPsKey.getDesignCapacity());
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                records.get(i).setDesignCapacity(bd.toString());
                            }
                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? p83022: "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimals(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            // 动态分页参数
            IPage<DevicePointMonth> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DevicePointMonth> devicePointMonthQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(isAll)){
                List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                devicePointMonthQueryWrapper.in("ps_key",psKeyList);
                if(notInPsKey != null && notInPsKey.size()>0){
                    devicePointMonthQueryWrapper.notIn("ps_key",notInPsKey);
                }
            }
            if(psKey != null && psKey.size()>0){
                devicePointMonthQueryWrapper.in("ps_key",psKey);
            }
            if(StringUtils.isNotBlank(queryDate)){
                LocalDate date = LocalDate.parse(queryDate);
                String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
                devicePointMonthQueryWrapper.like("time_stamp",yyyy);
            }
            devicePointMonthQueryWrapper.orderByAsc("time_stamp");
            IPage<DevicePointMonth> page = iDevicePointMonthService.page(pageData, devicePointMonthQueryWrapper);
            if(page != null){
                List<DevicePointMonth> records = page.getRecords();
                for (int i = 0; i < records.size(); i++) {
                    String psKeyParam = records.get(i).getPsKey();
                    String timeStamp = records.get(i).getTimeStamp();
                    if(StringUtils.isNotBlank(timeStamp)){
                        String s = DateFormatterUtils.formatTimeStringYear(timeStamp);
                        records.get(i).setFormatTime(s);
                    }
                    PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                    if(psInfoByPsKey != null){
                        records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                        records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                        records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                        records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                        records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                    }
                    //日发电量
                    String p83022 = records.get(i).getP83022();
                    records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                    //累计发电量
                    String p83024 = records.get(i).getP83024();
                    records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                    //等效时长
                    String p83025 = records.get(i).getP83025();
                    records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                    //电站功率
                    String p83033 = records.get(i).getP83033();
                    records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                }
                page.setRecords(records);
                return Result.ok(page);
            }
        }
        //总数据
        if("all".equals(minType)){
            // 动态分页参数
            if(source == 2){
                IPage<DevicePointYearGrw> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointYearGrw> devicePointYearQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    List<String> psKeyList = iDeviceService.getPsKeyListGrw(renterId);
                    devicePointYearQueryWrapper.in("ps_key",psKeyList);
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointYearQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointYearQueryWrapper.in("ps_key",psKey);
                }
                devicePointYearQueryWrapper.orderByAsc("time_stamp");

                IPage<DevicePointYearGrw> page = iDevicePointYearGrwService.page(pageData, devicePointYearQueryWrapper);
                if(page != null){
                    List<DevicePointYearGrw> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeStringYear(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            if(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity())){
                                BigDecimal bd = new BigDecimal(psInfoByPsKey.getDesignCapacity());
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                records.get(i).setDesignCapacity(bd.toString());
                            }
                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? p83022: "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            IPage<DevicePointYear> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DevicePointYear> devicePointYearQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(isAll)){
                List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                devicePointYearQueryWrapper.in("ps_key",psKeyList);
                if(notInPsKey != null && notInPsKey.size()>0){
                    devicePointYearQueryWrapper.notIn("ps_key",notInPsKey);
                }
            }
            if(psKey != null && psKey.size()>0){
                devicePointYearQueryWrapper.in("ps_key",psKey);
            }
            devicePointYearQueryWrapper.orderByAsc("time_stamp");

            IPage<DevicePointYear> page = iDevicePointYearService.page(pageData, devicePointYearQueryWrapper);
            if(page != null){
                List<DevicePointYear> records = page.getRecords();
                for (int i = 0; i < records.size(); i++) {
                    String psKeyParam = records.get(i).getPsKey();
                    String timeStamp = records.get(i).getTimeStamp();
                    if(StringUtils.isNotBlank(timeStamp)){
                        String s = DateFormatterUtils.formatTimeStringYear(timeStamp);
                        records.get(i).setFormatTime(s);
                    }
                    PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                    if(psInfoByPsKey != null){
                        records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                        records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                        records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                        records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                        records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                    }
                    //日发电量
                    String p83022 = records.get(i).getP83022();
                    records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                    //累计发电量
                    String p83024 = records.get(i).getP83024();
                    records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                    //等效时长
                    String p83025 = records.get(i).getP83025();
                    records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                    //电站功率
                    String p83033 = records.get(i).getP83033();
                    records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                }
                page.setRecords(records);
                return Result.ok(page);
            }
        }
        //自定义数据
        if("customize".equals(minType)){
            if(source == 2){
                IPage<DevicePointDayGrw> pageData = new Page<>(
                        devicePageVo.getPageNo(),
                        devicePageVo.getPageSize()
                );
                QueryWrapper<DevicePointDayGrw> devicePointDayQueryWrapper = new QueryWrapper<>();
                if(StringUtils.isNotBlank(isAll)){
                    if(notInPsKey != null && notInPsKey.size()>0){
                        devicePointDayQueryWrapper.notIn("ps_key",notInPsKey);
                    }
                }
                if(psKey != null && psKey.size()>0){
                    devicePointDayQueryWrapper.in("ps_key",psKey);
                }

                if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                    devicePointDayQueryWrapper.ge("time_stamp",startDate);
                    devicePointDayQueryWrapper.le("time_stamp",endDate);
                }
                devicePointDayQueryWrapper.orderByAsc("time_stamp");
                IPage<DevicePointDayGrw> page = iDevicePointDayGrwService.page(pageData, devicePointDayQueryWrapper);
                if(page != null){
                    List<DevicePointDayGrw> records = page.getRecords();
                    for (int i = 0; i < records.size(); i++) {
                        String psKeyParam = records.get(i).getPsKey();
                        String timeStamp = records.get(i).getTimeStamp();
                        if(StringUtils.isNotBlank(timeStamp)){
                            String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                            records.get(i).setFormatTime(s);
                        }
                        PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                        if(psInfoByPsKey != null){
                            records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                            records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                            records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                            records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                            if(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity())){
                                BigDecimal bd = new BigDecimal(psInfoByPsKey.getDesignCapacity());
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                records.get(i).setDesignCapacity(bd.toString());
                            }
                        }
                        //日发电量
                        String p83022 = records.get(i).getP83022();
                        records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? p83022: "");
                        //累计发电量
                        String p83024 = records.get(i).getP83024();
                        records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                        //等效时长
                        String p83025 = records.get(i).getP83025();
                        records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                        //电站功率
                        String p83033 = records.get(i).getP83033();
                        records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                    }
                    page.setRecords(records);
                    return Result.ok(page);
                }
            }
            IPage<DevicePointDay> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
            if(StringUtils.isNotBlank(isAll)){
                List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
                devicePointDayQueryWrapper.in("ps_key",psKeyList);
                if(notInPsKey != null && notInPsKey.size()>0){
                    devicePointDayQueryWrapper.notIn("ps_key",notInPsKey);
                }
            }
            if(psKey != null && psKey.size()>0){
                devicePointDayQueryWrapper.in("ps_key",psKey);
            }

            if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                LocalDate start = LocalDate.parse(startDate);
                String startTime = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalDate end = LocalDate.parse(endDate);
                String endTime = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                devicePointDayQueryWrapper.ge("time_stamp",startTime);
                devicePointDayQueryWrapper.le("time_stamp",endTime);
            }
            devicePointDayQueryWrapper.orderByAsc("time_stamp");
            IPage<DevicePointDay> page = iDevicePointDayService.page(pageData, devicePointDayQueryWrapper);
            if(page != null){
                List<DevicePointDay> records = page.getRecords();
                for (int i = 0; i < records.size(); i++) {
                    String psKeyParam = records.get(i).getPsKey();
                    String timeStamp = records.get(i).getTimeStamp();
                    if(StringUtils.isNotBlank(timeStamp)){
                        String s = DateFormatterUtils.formatTimeStringMonth(timeStamp);
                        records.get(i).setFormatTime(s);
                    }
                    PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKeyParam);
                    if(psInfoByPsKey != null){
                        records.get(i).setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                        records.get(i).setPsAddress(StringUtils.isNotBlank(psInfoByPsKey.getAddress()) ? psInfoByPsKey.getAddress() : "");
                        records.get(i).setGridTime(StringUtils.isNotBlank(psInfoByPsKey.getGridTime()) ? psInfoByPsKey.getGridTime() : "");
                        records.get(i).setCreateTime(StringUtils.isNotBlank(psInfoByPsKey.getCreateTime()) ?  psInfoByPsKey.getCreateTime() : "");
                        records.get(i).setDesignCapacity(StringUtils.isNotBlank(psInfoByPsKey.getDesignCapacity()) ? psInfoByPsKey.getDesignCapacity() : "");
                    }
                    //日发电量
                    String p83022 = records.get(i).getP83022();
                    records.get(i).setP83022(StringUtils.isNotBlank(p83022) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83022) : "");
                    //累计发电量
                    String p83024 = records.get(i).getP83024();
                    records.get(i).setP83024(StringUtils.isNotBlank(p83024) ? DecimalFormatterUtils.formatPositiveToTwoDecimalsDivide(p83024) : "");
                    //等效时长
                    String p83025 = records.get(i).getP83025();
                    records.get(i).setP83025(StringUtils.isNotBlank(p83025) ? p83025 : "");
                    //电站功率
                    String p83033 = records.get(i).getP83033();
                    records.get(i).setP83033(StringUtils.isNotBlank(p83033) ? p83033 : "");
                }
                page.setRecords(records);
                return Result.ok(page);
            }
        }
        return Result.ok();
    }


    @PostMapping("/start")
    public Result startExport(HttpServletRequest request, @RequestBody DevicePageVo devicePageVo) {
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
            if(source.equals("1") || source == 1){
                iDevicePointDayService.getExportExcelData(taskId,request,devicePageVo);
            }
            if(source.equals("2") || source==2){
                iDevicePointDayGrwService.getExportExcelData(taskId,request,devicePageVo);
            }
        }
        //数据(等于是月报表）
        if("year".equals(minType)) {
            if (source.equals("1") || source == 1) {
                iDevicePointMonthService.getExportExcelData(taskId, request, devicePageVo);

            }
            if (source.equals("2") || source==2) {
                iDevicePointMonthGrwService.getExportGrwExcelData(taskId, request, devicePageVo);
            }
        }
        //年报表
        if ("all".equals(minType)) {
            if (source.equals("1") || source == 1) {
                iDevicePointYearService.getExportExcelData(taskId, request, devicePageVo);
            }
            if (source.equals("2") ||  source==2) {
                iDevicePointYearGrwService.getExportExcelData(taskId, request, devicePageVo);
            }
        }



        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("taskId",taskId);
        resultMap.put("message","操作成功");
        return Result.ok(resultMap);
    }

    // 2. 状态查询接口
    @GetMapping("/status/{taskId}")
    public Result getStatus(@PathVariable String taskId) {
        List<Object> taskStatus = iDeviceService.getTaskStatus(taskId);
        return Result.ok(taskStatus);
    }

    /**
     * 导出导出报表数据
     *
     * @param request
     * @param devicePageVo
     */
    //@RequiresPermissions("project:pv_project_info:exportXls")
    @PostMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, @RequestBody DevicePageVo devicePageVo) {
        String taskId = devicePageVo.getTaskId();
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String minType = devicePageVo.getMinType();
        Integer source = devicePageVo.getSource();
        //日数据(查询日表)
        if("day".equals(minType) || "customize".equals(minType) || "month".equals(minType)){
            if(source == 1){
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointDayExportVO> collect = range.stream()
                        .map(obj -> (DevicePointDayExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointDayExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
            if(source == 2){
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointDayExportVO> collect = range.stream()
                        .map(obj -> (DevicePointDayExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站日报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointDayExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站日报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }
        //数据(等于是月报表）
        if("year".equals(minType)) {
            if (source == 1) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointMonthExportVO> collect = range.stream()
                        .map(obj -> (DevicePointMonthExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站月报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointMonthExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站月报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;

            }
            if (source == 2) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointMonthExportVO> collect = range.stream()
                        .map(obj -> (DevicePointMonthExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站月报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointMonthExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站月报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }
        //年报表
        if ("all".equals(minType)) {
            if (source == 1) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointYearExportVO> collect = range.stream()
                        .map(obj -> (DevicePointYearExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站年报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointYearExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站年报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
            if (source == 2) {
                List<Object> range = redisTemplate.opsForList().range("Task:" + taskId, 0, -1);
                redisTemplate.delete("Task:" + taskId);
                List<DevicePointYearExportVO> collect = range.stream()
                        .map(obj -> (DevicePointYearExportVO) obj)  // 强制类型转换
                        .collect(Collectors.toList());
//            System.err.println(collect);
                // Step.4 AutoPoi 导出Excel
                ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
                mv.addObject(NormalExcelConstants.FILE_NAME, "电站年报表");
                mv.addObject(NormalExcelConstants.CLASS, DevicePointYearExportVO.class);
                mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("电站年报表数据", "导出人:" + sysUser.getRealname(), "报表"));
                mv.addObject(NormalExcelConstants.DATA_LIST, collect);
                return mv;
            }
        }

        return null;
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
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                // 转换为当天的结束时间 20240921235959
                return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误: " + frontendDate);
        }
    }

}