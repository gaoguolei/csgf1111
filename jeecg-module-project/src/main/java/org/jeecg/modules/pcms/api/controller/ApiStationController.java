package org.jeecg.modules.pcms.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.station.StationSignConfig;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.project.entity.*;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.stationentity.*;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceLast;
import org.jeecg.modules.pcms.project.stationvo.SignVo;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;
import org.jeecg.modules.pcms.project.stationvo.StatisticsVo;
import org.jeecg.modules.pcms.project.vo.*;
import org.jeecg.modules.pcms.util.FileUploadUtils;
import org.jeecg.modules.pcms.util.ReplaceWord;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gongzhou
 * @Date 2023/10/27 上午7:57
 * @Version 1.5
 */
@Api(tags = "光伏-电站")
@RestController
@RequestMapping("/project/pvStationList")
//stationList
@Slf4j
public class ApiStationController {


    @Autowired
    private IPvStationsService iPvStationsService;
    @Autowired
    private IPvStationsDetailsService iPvStationsDetailsService;

    @Resource
    private IPvDeviceService iPvDeviceService;

    @Autowired
    private IPvStationsViewService iPvStationsViewService;

    @Autowired
    private IPvEnergyService iPvEnergyService;

    @Autowired
    private IPvDeviceAlarmService iPvDeviceAlarmService;

    @Autowired
    private IPvDeviceLastService iPvDeviceLastService;

    @Autowired
    private IPvDeviceHistoryDataService iPvDeviceHistoryDataService;

    @Autowired
    private IPvDataloggerService dataloggerService;

    @Autowired
    private StationSignConfig stationSignConfig;

    @Autowired
    private FileUploadUtils fileUploadUtils;

    @Autowired
    private IPvProjectInfoService iPvProjectInfoService;

    @Autowired
    private IRegionsServiceOne iRegionsService;

    @Autowired
    private IPvCallbackService iPvCallbackService;

    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private IDevicePointDayService iDevicePointDayService;

    /**
     * 电站概览-统计数据
     * @return
     */
    @GetMapping(value = "/stationListCount")
    public Result stationListCount(PageVo pageVo) {
        String plantName = pageVo.getPlantName();
        Integer searchType = pageVo.getSearchType();
        if(StringUtils.isNotBlank(plantName)){
            String regex = "\\*";
            String result = plantName.replaceAll(regex, "");
            pageVo.setPlantName(result);
        }
        Page<PvStations> page = new Page<>(pageVo.getPageNo(), pageVo.getPageSize());
        IPage<PvStations> pvStationsIPage = iPvDeviceLastService.pageList(page, pageVo);


        List<PvStations> list = pvStationsIPage.getRecords();
        //思路：先按照条件查询把 概览的列表数据查询出来。然后再拼凑对象返回给前端
        for (int i = 0; i < list.size(); i++) {
            Integer plantId = list.get(i).getPlantId();
            QueryWrapper<PvStationsDetails> pvStationsDetailsQueryWrapper = new QueryWrapper<>();
            pvStationsDetailsQueryWrapper.eq("plant_id",plantId);
            PvStationsDetails one = iPvStationsDetailsService.getOne(pvStationsDetailsQueryWrapper);
            if(one != null){
                list.get(i).setPlantId(plantId);
                list.get(i).setStatus(one.getStatus());
            }
            //逆变器数量
            QueryWrapper<PvDevice> pvDeviceQueryWrapper = new QueryWrapper<>();
            pvDeviceQueryWrapper.eq("plant_id", plantId);
            List<PvDevice> pvDevices = iPvDeviceService.list(pvDeviceQueryWrapper);
            if(pvDevices != null && pvDevices.size() >0){
                list.get(i).setDeviceNum(String.valueOf(pvDevices.size()));
            }
            QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
            pvStationsViewQueryWrapper.eq("plant_id", plantId);
            PvStationsView view = iPvStationsViewService.getOne(pvStationsViewQueryWrapper);
            //当日电量
            if(view != null){
                String todayEnergy = view.getTodayEnergy();
                list.get(i).setTotalTodayPower(StringUtils.isNotBlank(todayEnergy) ? view.getTodayEnergy():"");

                //累计电量
                list.get(i).setTotalEfficiency(view.getTotalEnergy());

                //日发满小时数（计算得来） 当日电量/峰值功率
                if(!view.getPeakPowerActual().equals("0")){
                    list.get(i).setDayHour(new BigDecimal(view.getTodayEnergy()).divide(new BigDecimal(view.getPeakPowerActual()),BigDecimal.ROUND_CEILING).toString());
                }
                //实时功率
                list.get(i).setNowPower(new BigDecimal(view.getCurrentPower()).setScale(2,BigDecimal.ROUND_CEILING).toString());
                //组件容量
                list.get(i).setIncomeVolume(view.getPeakPowerActual());
                list.get(i).setUpdateTime(view.getUpdateTime());

                //取电站的采集器的数据
                String status = iPvStationsService.getStatus(plantId);
                list.get(i).setStatus(status);
            }
            pvStationsIPage.setRecords(list);
        }

        return Result.ok(pvStationsIPage);
    }

    @GetMapping(value = "/stationListCountInfo")
    public Result stationListCountInfo(PageVo pageVo) {
        StatisticsVo statisticsVo = new StatisticsVo();
        List<PvStationsView> views = iPvStationsViewService.list();
        BigDecimal currentPower = new BigDecimal(0.00);
        BigDecimal todayEnergy = new BigDecimal(0.00);
        BigDecimal monthlyEnergy = new BigDecimal(0.00);
        BigDecimal totalEnergy = new BigDecimal(0.00);
        BigDecimal peakPowerActual = new BigDecimal(0.00);
        for (int i = 0; i < views.size(); i++) {
            PvStationsView view = views.get(i);
            //实时功率
            currentPower=  currentPower.add(new BigDecimal(view.getCurrentPower()));
            //当日电量
            todayEnergy=  todayEnergy.add(new BigDecimal(view.getTodayEnergy()));
            //当月电量
            monthlyEnergy =  monthlyEnergy.add(new BigDecimal(view.getMonthlyEnergy()));
            //累计电量
            totalEnergy=  totalEnergy.add(new BigDecimal(view.getTotalEnergy()));
            //组件总容量
            peakPowerActual=  peakPowerActual.add(new BigDecimal( view.getPeakPowerActual()));
        }
        statisticsVo.setTotalPower(currentPower.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
        statisticsVo.setTotalTodayPower(todayEnergy.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
        statisticsVo.setTotalMonthPower(monthlyEnergy.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
        statisticsVo.setTotalEfficiency(totalEnergy.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
        statisticsVo.setTotalPeakPower(peakPowerActual.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
        statisticsVo.setCount(views.size());
        //在线数量
        Integer integer = iPvDeviceLastService.selectStationCount("1").size();
        statisticsVo.setStatus1(integer);
        //离线数量
        Integer status0 = iPvDeviceLastService.selectStationCount("0").size();
        statisticsVo.setStatus0(status0);
        return  Result.ok(statisticsVo);
    }

    /**
     * 逆变器概览
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/deviceList")
    public Result deviceList(PageVo pageVo) {
        String sn = pageVo.getSn();
        String minPower = pageVo.getMinPower();
        String maxPower = pageVo.getMaxPower();
        String status = pageVo.getStatus();

        QueryWrapper<PvDevice> objectQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(sn)){
            String regex = "\\*";
            String result = sn.replaceAll(regex, "");
            objectQueryWrapper.like("device_sn",result);
        }
        if(StringUtils.isNotBlank(minPower)){
            objectQueryWrapper.ge("nominal_power",new BigDecimal(minPower).multiply(new BigDecimal(1000)));
        }
        if(StringUtils.isNotBlank(status)){
            objectQueryWrapper.eq("status",status);
        }
        if(StringUtils.isNotBlank(maxPower)){
            objectQueryWrapper.le("nominal_power",new BigDecimal(maxPower).multiply(new BigDecimal(1000)));
        }
        Page<PvDevice> page = new Page<>(pageVo.getPageNo(), pageVo.getPageSize());

        IPage<PvDevice> pvDevicePage = iPvDeviceService.page(page, objectQueryWrapper);
        List<PvDevice> records = pvDevicePage.getRecords();
        if(records != null && records.size()>1){
            for (int i = 0; i < records.size(); i++) {
                String plantId = records.get(i).getPlantId();
                Integer status1 = records.get(i).getStatus();
                String deviceSn = records.get(i).getDeviceSn();
                String nominalPower = records.get(i).getNominalPower();
                if(StringUtils.isNotBlank(nominalPower)){
                    BigDecimal divide = new BigDecimal(nominalPower).divide(new BigDecimal(1000), BigDecimal.ROUND_CEILING);
                    records.get(i).setRatedPower(divide);
                }
                Date updateTime = records.get(i).getUpdateTime();
                records.get(i).setUpdateTime(updateTime);
                QueryWrapper<PvStations> pvStationsQueryWrapper = new QueryWrapper<>();
                PvStations pvStations = iPvStationsService.getOne(pvStationsQueryWrapper.eq("plant_id", plantId));
                String name = pvStations.getName();
                records.get(i).setPlantName(name);
                QueryWrapper<PvDeviceLast> pvDeviceLastQueryWrapper = new QueryWrapper<>();
                PvDeviceLast pvDeviceLast = iPvDeviceLastService.getOne(pvDeviceLastQueryWrapper.eq("inverter_id", deviceSn));
                if(pvDeviceLast != null){
                    String powerToday = pvDeviceLast.getPowerToday();
                    String powerTotal = pvDeviceLast.getPowerTotal();
                    String pac = pvDeviceLast.getPac();
                    if(StringUtils.isNotBlank(powerToday)){
                        records.get(i).setDayPower( new BigDecimal(powerToday).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                    if(StringUtils.isNotBlank(powerTotal)){
                        records.get(i).setTotalPower( new BigDecimal(powerTotal).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                    if(StringUtils.isNotBlank(pac)){
                        records.get(i).setRatedPower( new BigDecimal(powerTotal).divide(new BigDecimal(1000),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                    records.get(i).setStatus(status1);
                    records.get(i).setSn(deviceSn);

                }
                pvDevicePage.setRecords(records);
            }
            return Result.ok(pvDevicePage);
        }
        return Result.ok(pvDevicePage);

    }
    @GetMapping(value = "/deviceListInfo")
    public Result deviceListInfo(PageVo pageVo) {
        String renterId = pageVo.getRenterId();
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("project_type",renterId);
        List<Device> list = iDeviceService.list(deviceQueryWrapper);
        int size = list.size();
        StationPageData stationPageData = new StationPageData();
        StationPageVo stationPageVo = new StationPageVo();
        stationPageVo.setStationCount(size);
        BigDecimal totalEnergy = new BigDecimal("0.00");
        //节能减排
        BigDecimal emission = new BigDecimal(0.00);
        //实时功率
        BigDecimal totalCurrentPowerSol= new BigDecimal(0.00);
        BigDecimal totalCurrentPowerGrw = new BigDecimal(0.00);
        BigDecimal totalCurrentPower=new BigDecimal(0.00);
        //组件总容量
        BigDecimal totalPeakPower = new BigDecimal(0.00);
        for (int i = 0; i < list.size(); i++) {
            int source = list.get(i).getSource();
            String peakPower = list.get(i).getTotalCapcity();
            String currentPower = list.get(i).getCurrPower();

            String carbonOffset = list.get(i).getCo2Reduce();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(carbonOffset)) {
                emission = emission.add(new BigDecimal(carbonOffset));
            }
            totalEnergy = totalEnergy.add(new BigDecimal(list.get(i).getTotalEnergy()));
            if(this.isPositiveNumber(currentPower)){
                if(source == 2){
                    totalCurrentPowerGrw = totalCurrentPowerGrw.add(new BigDecimal(currentPower).divide(new BigDecimal(1000)));
                }
                if(source == 1){
                    BigDecimal bigDecimal = this.convertToWatt(currentPower);
                    totalCurrentPowerSol = totalCurrentPowerSol.add(bigDecimal);
                }
            }

            if(this.isPositiveNumber(peakPower)){
                totalPeakPower = totalPeakPower.add(new BigDecimal(peakPower));
            }

        }
        BigDecimal divide = totalCurrentPowerSol.divide(new BigDecimal(1000));
        totalCurrentPower = totalCurrentPowerGrw.add(divide);
        stationPageVo.setTotalPower(totalEnergy.setScale(2, RoundingMode.HALF_UP).toString());
        stationPageVo.setIncome(totalEnergy.multiply(new BigDecimal(1.1)).setScale(2, RoundingMode.HALF_UP).toString());
        //离线数量
        int size1 = iDeviceService.getUnLineDeviceNumSolar(renterId,0);
        stationPageVo.setStatus1(size1);
        //节能减排
        stationPageVo.setSection(emission.setScale(2, RoundingMode.HALF_UP));
        //在线数量
        int size0 = iDeviceService.getUnLineDeviceNumGrw(renterId,1);
        stationPageVo.setStatus0(size0);
        //组件总数
        stationPageVo.setEmission(emission.divide(new BigDecimal(7), BigDecimal.ROUND_CEILING).setScale(0, RoundingMode.HALF_UP));
        //实时功率
        stationPageVo.setTotalCurrentPower(totalCurrentPower.setScale(2, RoundingMode.HALF_UP).toString());
        //总容量
        stationPageVo.setTotalPeakPower(totalPeakPower.setScale(2, RoundingMode.HALF_UP).toString());
        String s = iDeviceService.countTotalPower(renterId);
        stationPageVo.setDeviceTotalPower(s);
        //在线数量
        stationPageData.setStationPageVo(stationPageVo);
        List<StationCountVo> weeks = iDevicePointDayService.weeks(renterId);
        if (weeks != null && weeks.size() > 0) {
            List<StationCountVo> collect = weeks.stream().sorted(Comparator.comparing(StationCountVo::getIndex).reversed()).collect(Collectors.toList());
            stationPageData.setWeekList(collect);
        }
        stationPageData.setWeekList(weeks);
        return Result.ok(stationPageData);
    }

    public  boolean isPositiveNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.trim().matches("\\d+(\\.\\d+)?");
    }

    public BigDecimal convertToWatt(String rawPower) {
        if (rawPower == null || rawPower.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String cleaned = rawPower.trim();

        // 尝试解析为数值
        try {
            // 判断是否包含小数点
            if (cleaned.contains(".")) {
                BigDecimal value = new BigDecimal(cleaned);
                // 小数 → 当作 kW，转为 W
                return value.multiply(BigDecimal.valueOf(1000));
            } else {
                // 整数 → 当作 W
                return new BigDecimal(cleaned);
            }
        } catch (NumberFormatException e) {
            System.err.println("无效功率值: " + cleaned);
            return BigDecimal.ZERO;
        }
    }


    /**
     * 电站详情
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationInfo")
    public Result stationInfo(PageVo pageVo) {
        Integer plantId = pageVo.getPlantId();
        QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
        pvStationsViewQueryWrapper.eq("plant_id",plantId);
        QueryWrapper<PvStationsDetails> pvStationsDetailsQueryWrapper = new QueryWrapper<>();
        pvStationsDetailsQueryWrapper.eq("plant_id",plantId);
        PvStationsDetails pvStationsDetails = iPvStationsDetailsService.getOne(pvStationsDetailsQueryWrapper);
        PvStationsView one = iPvStationsViewService.getOne(pvStationsViewQueryWrapper);
        if(one != null && pvStationsDetails != null){
            List<String> objects = new ArrayList<>();
            String imageUrl = pvStationsDetails.getImageUrl();
            objects.add(StringUtils.isNotBlank(imageUrl) ? imageUrl:"");
            one.setImgUrl(objects);
            one.setLatitude(StringUtils.isNotBlank(pvStationsDetails.getLatitude()) ? pvStationsDetails.getLatitude(): "");
            one.setLongitude(StringUtils.isNotBlank(pvStationsDetails.getLongitude()) ? pvStationsDetails.getLongitude() : "");
            one.setAddress(StringUtils.isNotBlank(pvStationsDetails.getAddress1()) ? pvStationsDetails.getAddress1() : "");
            one.setName(StringUtils.isNotBlank(pvStationsDetails.getName()) ? pvStationsDetails.getName() : "");
        }
        String currentPower = one.getCurrentPower();
        BigDecimal bigDecimal = new BigDecimal(currentPower).setScale(2, BigDecimal.ROUND_CEILING);
        one.setCurrentPower(bigDecimal.toString());
        if(one != null){
            return Result.ok(one);
        }
        return Result.ok();
    }


    /**
     *电站地图
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationMap")
    public Result stationMap(PageVo pageVo) {
        List<PvStations> pvStations = iPvStationsService.list();
        List<LocationVO> locationVOList = new ArrayList<>();
        //电站地图
        for (PvStations stations : pvStations) {
            LocationVO locationVO = new LocationVO();
            String longitude = stations.getLongitude();
            String latitude = stations.getLatitude();
            locationVO.setLongitude(StringUtils.isNotBlank(longitude) ? longitude:"");
            locationVO.setLatitude(StringUtils.isNotBlank(latitude) ? latitude:"");
            locationVOList.add(locationVO);
        }
        return Result.ok(locationVOList);
    }
    /**
     * 获取详情里面的图表数据
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationChart")
    public Result stationChart(PageVo pageVo) {
        //图表的数据
        return iPvEnergyService.getStationChart(pageVo);
    }

    /**
     * 报警信息
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationAlarm")
    @ApiOperation(value = "报警信息", notes = "报警信息")
    public Result<IPage<PvDeviceAlarm>> stationAlarm(PageVo pageVo) {
        IPage<PvDeviceAlarm> pvDeviceAlarmIPage = iPvDeviceAlarmService.stationAlarm(pageVo);
        return Result.OK(pvDeviceAlarmIPage);
    }

    @PostMapping(value = "/deleteAlam")
    @ApiOperation(value = "电站报警删除", notes = "电站报警删除")
    public Result deleteAlam(@RequestBody PageVo pageVo) {
        iPvDeviceAlarmService.delete(pageVo.getId());
        return Result.OK("操作成功！");
    }

    @PostMapping(value = "/updateAlam")
    @ApiOperation(value = "电站报警更新", notes = "电站报警更新")
    public Result updateAlam(@RequestBody PageVo pageVo) {
        iPvDeviceAlarmService.update(pageVo.getId());
        return Result.OK("操作成功！");
    }

    /**
     * 获取电站报表
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationReport")
    public Result stationReport(PageVo pageVo) {
        return iPvStationsService.stationReport(pageVo);
    }

    /**
     * 获取逆变器报表
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/deviceReport")
    public Result deviceReport(PageVo pageVo) {
        String sn = pageVo.getSn();
        if(StringUtils.isNotBlank(sn)){
         return  iPvDeviceHistoryDataService.deviceReport(pageVo);
        }
        return Result.ok();
    }

    /**
     * 搜索设备
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/devices")
    public Result devices(PageVo pageVo) {
        String sn = pageVo.getSn();
        QueryWrapper<PvDevice> pvDeviceQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(sn)){
            pvDeviceQueryWrapper.eq("device_sn",sn);
        }
        List<PvDevice> list = iPvDeviceService.list(pvDeviceQueryWrapper);
        return Result.ok(list);
    }

    /**
     * 电量报表
     */
    @GetMapping(value = "/powerReport")
    public Result powerReport(PageVo pageVo) {
        return iPvStationsService.powerReport(pageVo);
    }




    /**
     * 获取全部的电站
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/stationList")
    public Result stationList(PageVo pageVo) {
        QueryWrapper<PvStations> pvStationsQueryWrapper = new QueryWrapper<>();
        String plantName = pageVo.getPlantName();
        if(StringUtils.isNotBlank(plantName)){
            pvStationsQueryWrapper.like("name",plantName);
        }
        List<PvStations> list = iPvStationsService.list(pvStationsQueryWrapper);
        if(list != null && list.size()>0){
            Result.ok(list);
        }
        return Result.ok(list);
    }


    /**
     * 逆变器详情
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/pvDeviceInfo")
    public Result pvDeviceInfo(PageVo pageVo) {

        QueryWrapper<PvDeviceLast> pvDeviceLastQueryWrapper = new QueryWrapper<>();
        String sn = pageVo.getSn();
        if(StringUtils.isNotBlank(sn)){
            pvDeviceLastQueryWrapper.eq("inverter_id",sn);
        }
        QueryWrapper<PvDevice> pvDeviceQueryWrapper = new QueryWrapper<PvDevice>();
        if(StringUtils.isNotBlank(sn)){
            pvDeviceQueryWrapper.eq("device_sn",sn);
        }
        PvDevice pvDevice = iPvDeviceService.getOne(pvDeviceQueryWrapper);
        PvDeviceLast one = iPvDeviceLastService.getOne(pvDeviceLastQueryWrapper);
        if(pvDevice != null){

            one.setPvDevice(pvDevice);
        }
        if(one !=null){
            return Result.ok(one);
        }
        return Result.ok();
    }

    /**
     * 逆变器详情图表
     * @param pageVo
     * @return
     */
    @GetMapping(value = "/pvDeviceInfoChart")
    public Result pvDeviceInfoChart(PageVo pageVo) {

        return Result.ok();
    }


    /**
     *签订合同
     * @param signVo
     * @return
     */
    @PostMapping(value = "/sign")
    public Result sign(@RequestBody SignVo signVo) throws Exception {
        PvProjectInfo projectInfo = iPvProjectInfoService.getById(signVo.getId());
        String projectCode = projectInfo.getProjectCode();
        //1，拿到数据之后调用填充文件的方法，填充文件生成文件
        //2，拿到生成的文件调用发起签署文件接口。
        //3，在回调函数里面更新此合同的状态
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> picData = new HashMap<>();
        data.put("${artyA}", StringUtils.isNotBlank(signVo.getArtyA()) ? signVo.getArtyA():"");
        data.put("${artyB}", "新能源（河北）有限公司");
        data.put("${signAddress}",StringUtils.isNotBlank(signVo.getAddress()) ? signVo.getAddress() : "");
        data.put("${address}",StringUtils.isNotBlank(signVo.getAddress()) ? signVo.getAddress() : "");
        Calendar cal = Calendar.getInstance();
        Date date=new Date();//现在的日期
        cal.setTime(date);
        Integer year=cal.get(Calendar.YEAR);//获取年
        Integer month = cal.get(Calendar.MONTH)+1;//获取月（月份从0开始，如果按照中国的习惯，需要加一）
        Integer day=cal.get(Calendar.DAY_OF_MONTH);//获取日（月中的某一天）
        data.put("${dete}", year+"年"+month+"月"+day+"日");
        data.put("${IDNum}", StringUtils.isNotBlank(signVo.getIdnum()) ? signVo.getIdnum() : "");
        data.put("${phone}",StringUtils.isNotBlank(signVo.getPhone()) ? signVo.getPhone() : "");
        //直系亲属
        data.put("${personPhone}", StringUtils.isNotBlank(signVo.getPersonPhone()) ? signVo.getPersonPhone():"");
        data.put("${personB}", "郑卜天");
        data.put("${phoneB}", "010-61419053");
        data.put("${projectName}", StringUtils.isNotBlank(projectInfo.getCustomerName()) ? projectInfo.getCustomerName()+"-光伏电站":""+"-光伏电站");
        data.put("${peakPower}", projectInfo.getInstCmptCapacity() != null ? projectInfo.getInstCmptCapacity()+"kw":new BigDecimal(0.00)+"kw");
        //电站总价（暂时为空）
        data.put("${totalPrice}", "");
        data.put("${owner}", StringUtils.isNotBlank(projectInfo.getCustomerName()) ? projectInfo.getCustomerName():"");
        data.put("${number}", "");
        data.put("${area}", "");
        data.put("${realityArea}", "");
        data.put("${range}", "");
        data.put("${year}", year+20);
        data.put("${month}", month);
        data.put("${day}", day);
        data.put("${amountToFront}", "");
        data.put("${amountToAfter}", "");
        data.put("${bank}", StringUtils.isNotBlank(projectInfo.getReturnFeeDebitCardOwner()) ? projectInfo.getReturnFeeDebitCardOwner():"");
        data.put("${account}", StringUtils.isNotBlank(projectInfo.getReturnFeeDebitCardBank()) ? projectInfo.getReturnFeeDebitCardBank():"");
        data.put("${length}", StringUtils.isNotBlank(projectInfo.getReturnFeeDebitCardNo()) ? projectInfo.getReturnFeeDebitCardNo():"");
        data.put("${length}", "");
        data.put("${width}", "");
        data.put("${roofArea}", "");
        data.put("${structure}", "");
        data.put("${assembly}", projectInfo.getInstCmptNumber() != null ? projectInfo.getInstCmptNumber():"");
        data.put("${priceOfPower}","");
        data.put("${capacity}",projectInfo.getInstCmptCapacity() != null ? projectInfo.getInstCmptCapacity().multiply(new BigDecimal(1000)):new BigDecimal(0.00));
        data.put("${subtotal}","");
        data.put("${priceTotal}","");
        data.put("${priceTotalBig}","");
        operateWord(data, picData,projectCode);
        Path path = Paths.get(stationSignConfig.getOutPath(),projectCode + ".docx");
        if(Files.isRegularFile(path) && Files.exists(path)){
            //去签署
            Result integer = fileUploadUtils.sendUploadFile(stationSignConfig.getOutPath() + projectCode + ".docx", projectCode + ".docx", projectInfo,signVo);
            //签署完成即可删除文件
            if(integer.getCode() == 200 ){
                Files.deleteIfExists(path);
            }
            return Result.ok(integer);
        }

        return Result.error("文件不存在 !");
    }


    /**
     * 签署文件的下载
     * @return
     */
    @GetMapping(value = "/signFileUpload")
    public Result signFileUpload(PvProjectInfo projectInfo) throws Exception {
        String signFlowId = iPvCallbackService.getSignFlowId(projectInfo.getId());
        String s = fileUploadUtils.signFileUpload(signFlowId);
        JSONObject jsonObject = JSONObject.parseObject(s);
        Object data = jsonObject.get("data");
        String string = JSONObject.toJSONString(data);
        JSONObject jsonObject1 = JSONObject.parseObject(string);
        Object files = jsonObject1.get("files");
        return Result.ok(files);
    }


    /**
     * 获取地址
     */

    private String getAddress(String path,String address){
        if (StringUtils.isNotBlank(path)) {
            path = path.substring(1, path.length());
            String[] regionPathArray = path.split(",");
            //给地区赋值
            StringBuilder stringBuilder = new StringBuilder();
            for (String string : regionPathArray) {
                Regions region = iRegionsService.getModel(Integer.valueOf(path));
                if (region == null) {
                    throw new IllegalArgumentException("地区不合法");
                }
                String localName = region.getLocalName();
                stringBuilder.append(localName);
            }
            return stringBuilder + address;
        }
        return "";
    }
    /**
     * 填充文件生成文件
     * @param data
     * @param picData
     */

    public  void operateWord(Map<String, Object> data, Map<String, Object> picData, String projectName) {
        try {
            ClassPathResource classPathResource = new ClassPathResource("/template/ContractModel.docx");
            XWPFDocument document = new XWPFDocument(classPathResource.getInputStream());
            if (data.size() > 0) {
                // 替换掉表格之外的文本(仅限文本)
                ReplaceWord.changeText(document, data);
                // 替换表格内的文本对象
                ReplaceWord.changeTableText(document, data);
            }
            if (picData.size() > 0) {
                // 替换内容图片
                ReplaceWord.changePic(document, picData);
                // 替换表格内的图片对象
                ReplaceWord.changeTablePic(document, picData);
            }
            Path path = Paths.get(stationSignConfig.getOutPath(),projectName + ".docx");
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            document.write(outputStream);
            document.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @PostMapping(value = "/callback")
    public Result callback(@RequestBody CallbackVo pageVo) {
        System.err.println("/callback+66666666666666666666666666666666666666666666666666");
        //直接存起来
        if(pageVo !=null){
            PvCallback pvCallback = new PvCallback();
            String signFlowId = pageVo.getSignFlowId();
            if(StringUtils.isNotBlank(signFlowId)){
                UpdateWrapper<PvCallback> pvCallbackUpdateWrapper = new UpdateWrapper<>();
                BeanUtils.copyProperties(pageVo,pvCallback);
                pvCallbackUpdateWrapper.eq("sign_flow_id",signFlowId);
                iPvCallbackService.saveOrUpdate(pvCallback);
            }
            //回调函数到签署完成更新项目可可下载的状态
            String customBizNum = pageVo.getCustomBizNum();
            String action = pageVo.getAction();
            String signOrder = pageVo.getSignOrder();
            String signResult = pageVo.getSignResult();
            if(StringUtils.isNotBlank(customBizNum) && StringUtils.isNotBlank(action) && StringUtils.isNotBlank(signOrder) && StringUtils.isNotBlank(signResult)){
                iPvProjectInfoService.updateIsUpload("2",Integer.valueOf(customBizNum));
            }
        }
        return Result.ok();
    }

    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    @ResponseBody
    public void uploadPicture(@RequestParam("file")MultipartFile file) {
        //1.判断用户是否选择了文件
        if(file.isEmpty()){
        }
        //2.获取上传文件的原始名称
        String filename = file.getOriginalFilename();
        //3.获取上传文件的大小
        long size = file.getSize();
        //4.定义上传文件的保存路径
        String path = "/home/coder/project/renergy/jeecg-module-system/jeecg-system-start/src/main/resources/template";
        //5.在上传文件的保存路径下创建一个File对象
        File dest = new File(path+"/"+filename);
        //6、找到上传文件的保存路径的父目录，判断上传文件的目录和文件名是否存在，若不存在则创建
        if(!dest.getParentFile().exists()){
            dest.getParentFile().mkdir();
        }
        //7、将上传文件拷贝到上传路径中
        try {
            Files.write(Paths.get(path, filename), file.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/download")
    @ResponseBody
    public String downLoad (HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 读取图片123.png
        String realPath = "/home/coder/project/renergy/jeecg-module-system/jeecg-system-start/src/main/resources/template/ContractModel.docx";
        File file = new File(realPath);
        String filename = file.getName();
        // 2. 设置浏览器以下载方式打开， 此处不设置，会直接在浏览器预览图片
        response.setHeader("content-disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));

        // 3. 读取文件，写到输出流
        InputStream in = new FileInputStream(file);
        byte[] buf = new byte[1024];
        while (in.read(buf) > 0) {
            response.getOutputStream().write(buf, 0, buf.length);
        }
        in.close();
        return "下载完成";
    }


    @GetMapping("/getIp")
    @ResponseBody
    public String getIp () throws IOException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);

        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }


}
