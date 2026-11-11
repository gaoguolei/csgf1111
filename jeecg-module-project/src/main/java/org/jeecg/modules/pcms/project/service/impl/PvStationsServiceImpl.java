package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvEnergy;
import org.jeecg.modules.pcms.project.mapper.*;
import org.jeecg.modules.pcms.project.service.IPvStationsService;
import org.jeecg.modules.pcms.project.stationentity.*;
import org.jeecg.modules.pcms.project.stationvo.*;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PvStationsServiceImpl extends ServiceImpl<PvStationsMapper, PvStations> implements IPvStationsService {



    @Resource
    private PvStationsMapper pvStationsMapper;
    @Resource
    private PvStationsDetailsMapper pvStationsDetailsMapper;

    @Resource
    private PvStationsViewMapper pvStationsViewMapper;

    @Resource
    private PvDeviceMapper pvDeviceMapper;

    @Resource
    private PvDeviceLastMapper pvDeviceLastMapper;

    @Resource
    private PvEnergyMapper pvEnergyMapper;

    @Autowired
    private StationConfig stationConfig;


    @Override
    public void insert() {
        //思路  拿到总数count 拿到每页数量，用总数除以每页数量得到循环次数。
        HashMap<String, Object> paramMap = new HashMap<>();
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
        String result2 = HttpRequest.get(stationConfig.getStationList())
                .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();

        if(StringUtils.isNotBlank(result2)){
            //封装一个对象用来转换数据
            PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
            if(plantResultVo != null) {
                //拿到总数  在利用总数里面的去计算然后给前端返回
                Integer count = plantResultVo.getData().getCount();
                List<PlantResultVo> data = this.getData(1, count);
                ArrayList<StationListVo> plants = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    List<StationListVo> stationListVos = data.get(i).getData().getPlants();
                    plants.addAll(stationListVos);
                }
                //所有数据在这里面，存储到本地
                List<PvStations> pvStations = new ArrayList<>();
                for (StationListVo plant : plants) {
                    PvStations stations = new PvStations();
                    BeanUtils.copyProperties(plant,stations);
                    stations.setUpdateTime(new Date());
                    pvStations.add(stations);
                }
                //更新所有电站
                this.updateStationAndDetails(pvStations);
                //更新电站详情
                this.updateStationDetails(pvStations);
                //更新逆变器数据，循环拿到的电站列表，通过电站id获取逆变器sn码，通过sn码查询逆变器基本信息，然后插入到逆变器表
                List<Device> deviceSnList = this.getDeviceSnList();
                this.updateDevice(deviceSnList);
            }
        }
    }

    @Override
    public void updateStationAndDetails(List<PvStations> plantVos) {
        //把数据存储到本地
        //先删除本地的数据库。再重新插入
        pvStationsMapper.deleteAll();
        if(plantVos != null && plantVos.size()>0){
            for (int i = 0; i < plantVos.size(); i++) {
                PvStations stations = plantVos.get(i);
                pvStationsMapper.insert(stations);
            }
        }

    }

    /**
     * 插入详情
     * @param plantVos
     */
    @Override
    public void updateStationDetails(List<PvStations> plantVos) {
        if(plantVos != null && plantVos.size()>0){
            pvStationsDetailsMapper.deleteAll();
            for (int i = 0; i < plantVos.size(); i++) {
                //先删除表数据 然后再插入
                Integer plantId = plantVos.get(i).getPlantId();
                HashMap<String, Object> paramMap = new HashMap<>();
                paramMap.put("plant_id", plantId);
                String result2 = HttpRequest.get(stationConfig.getStationDetails())
                        .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                        .form(paramMap)//表单内容
                        .timeout(20000)//超时，毫秒
                        .execute()
                        .body();
                StationsVo stationsVo = JSON.parseObject(result2, StationsVo.class);
                PvStationsDetails data = stationsVo.getData();
                data.setPlantId(plantId);
                pvStationsDetailsMapper.insert(data);
            }
        }
    }

    /**
     * 插入逆变器
     * @param devices
     */
    @Override
    public void updateDevice(List<Device> devices) {
        if(devices != null && devices.size()>0){
            pvDeviceMapper.deleteAll();
            for (int i = 0; i < devices.size(); i++) {
                //通过电站id获取逆变器列表
                String plantId = devices.get(i).getPlantId();
                String deviceSn = devices.get(i).getDeviceSn();
                HashMap<String, Object> paramMap = new HashMap<>();
                paramMap.put("device_sn", deviceSn);
                String result2 = HttpRequest.get(stationConfig.getInverterInfo())
                        .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                        .form(paramMap)//表单内容
                        .timeout(20000)//超时，毫秒
                        .execute()
                        .body();

                DeviceInfoVo deviceInfoVo = JSON.parseObject(result2, DeviceInfoVo.class);
                if(deviceInfoVo != null){
                    DeviceInfo data = deviceInfoVo.getData();
                    LastUpdateTimeVo lastUpdateTimeVo = data.getLastUpdateTime();
                    PvDevice device = new PvDevice();
                    BeanUtils.copyProperties(data,device);
                    if(lastUpdateTimeVo != null){
                        String string = JSON.toJSONString(lastUpdateTimeVo);
                        device.setLastUpdateTime(string);
                    }
                    device.setPlantId(plantId);
                    device.setUpdateTime(new Date());
                    device.setDeviceSn(deviceSn);
                    pvDeviceMapper.insert(device);
                }
            }
        }
    }

    @Override
    public Result stationReport(PageVo pageVo) {
        List<Integer> plantIds = pageVo.getPlantIds();
        String status = pageVo.getStatus();
        QueryWrapper<PvStations> objectQueryWrapper = new QueryWrapper<>();
        if(plantIds != null && plantIds.size()>0){
            objectQueryWrapper.in("plant_id",plantIds);
        }
        if(StringUtils.isNotBlank(status)){
            objectQueryWrapper.eq("status",status);
        }
        Date queryTime = pageVo.getQueryTime();
        LocalDate time = queryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int dayOfMonth = time.getDayOfMonth();
        int monthValue = time.getMonthValue();
        int year = time.getYear();
        Page<PvStations> page = new Page<>(pageVo.getPageNo(), pageVo.getPageSize());
        Page<PvStations> pvStationsPage = this.page(page, objectQueryWrapper);
        List<PvStations> records = pvStationsPage.getRecords();
        if(records != null && records.size()>0){
            for (int i = 0; i < records.size(); i++) {
                Integer plantId = records.get(i).getPlantId();
                String name = records.get(i).getName();
                String stationStatus = records.get(i).getStatus();
                //根据传入的时间类型来返回不同的数据
                String dayType = pageVo.getDayType();
                //查询某一天的数据
                if(dayType.equals("0")){
                    QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
                    pvStationsViewQueryWrapper.eq("plant_id",plantId);
                    PvStationsView pvStationsView = pvStationsViewMapper.selectOne(pvStationsViewQueryWrapper);
                    if(pvStationsView != null){
                        String totalEnergy = pvStationsView.getTotalEnergy();
                        //查询当天的电量
                        QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
                        pvEnergyQueryWrapper.eq("plant_id",plantId);
                        pvEnergyQueryWrapper.eq("year",year);
                        pvEnergyQueryWrapper.eq("month",monthValue);
                        pvEnergyQueryWrapper.eq("day",dayOfMonth);
                        List<PvEnergy> list = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
                        if(list != null && list.size()>0){
                            PvEnergy pvEnergy = list.get(0);
                            if(pvEnergy != null ){
                                BigDecimal energy = pvEnergy.getEnergy();
                                //峰值功率
                                String peakPowerActual = pvStationsView.getPeakPowerActual();
                                //累计
                                if(pvEnergy != null){
                                    if((energy != null&& !pvEnergy.equals("0")) && ((StringUtils.isNotBlank(peakPowerActual) && !peakPowerActual.equals("0")))){
                                        records.get(i).setDayNum(energy.divide(new BigDecimal(peakPowerActual),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                                    }
                                    records.get(i).setDayPower(energy);
                                    records.get(i).setName(StringUtils.isNotBlank(name) ? name : "");
                                    records.get(i).setStatus(StringUtils.isNotBlank(stationStatus) ? stationStatus:"");
                                    records.get(i).setIncome(new BigDecimal(0.9));
                                    records.get(i).setPeakPower(StringUtils.isNotBlank(peakPowerActual) ? peakPowerActual:"");
                                    records.get(i).setPlantId(plantId);
                                }
                                records.get(i).setTotalPower(StringUtils.isNotBlank(totalEnergy) ? new BigDecimal(totalEnergy) : new BigDecimal(0.00));
                            }
                        }
                    }
                }
                //月
                if(dayType.equals("1")){
                    QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
                    pvStationsViewQueryWrapper.eq("plant_id",plantId);
                    PvStationsView pvStationsView = pvStationsViewMapper.selectOne(pvStationsViewQueryWrapper);
                    if(pvStationsView != null){
                        String peakPowerActual = pvStationsView.getPeakPowerActual();
                        //计算每个月的数据
                        //查询当天的电量
                        QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
                        pvEnergyQueryWrapper.eq("plant_id",plantId);
                        pvEnergyQueryWrapper.eq("year",year);
                        pvEnergyQueryWrapper.eq("month",monthValue);
                        List<PvEnergy> list = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
                        BigDecimal bigDecimal = new BigDecimal(0.00);
                        for (int k = 0; k < list.size(); k++) {
                            BigDecimal energy = list.get(k).getEnergy();
                            bigDecimal=bigDecimal.add(energy);
                        }
                        if((bigDecimal!= null) && ((StringUtils.isNotBlank(peakPowerActual) && !peakPowerActual.equals("0")))){
                            records.get(i).setMonthNum(bigDecimal.divide(new BigDecimal(peakPowerActual),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                        }
                        records.get(i).setMonthPower(bigDecimal);
                        String totalEnergy = pvStationsView.getTotalEnergy();

                        records.get(i).setName(StringUtils.isNotBlank(name) ? name:"");
                        records.get(i).setStatus(StringUtils.isNotBlank(stationStatus) ? stationStatus:"");
                        records.get(i).setIncome(new BigDecimal(0.9));
                        records.get(i).setPeakPower(StringUtils.isNotBlank(peakPowerActual) ? peakPowerActual:"");
                        records.get(i).setTotalPower(StringUtils.isNotBlank(totalEnergy) ? new BigDecimal(totalEnergy):new BigDecimal(0.00));
                        records.get(i).setPlantId(plantId);
                        //图标数据统计
                        QueryWrapper<PvEnergy> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("plant_id",plantId);
                        queryWrapper.eq("year",year);
                        queryWrapper.eq("month",monthValue);
                        List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(queryWrapper);
                        List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
                        for (int j = 0; j < pvEnergies.size(); j++) {
                            Integer day = pvEnergies.get(j).getDay();
                            BigDecimal energy = pvEnergies.get(j).getEnergy();
                            if(day!= null && energy != null){
                                pvEnergyVos.add(new PvEnergyVo(day,energy,new BigDecimal(0.00),energy.multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))));
                            }
                        }
                        pvEnergyVos.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
                        records.get(i).setEnergyVos(pvEnergyVos);
                    }

                }
                //年
                if(dayType.equals("2")){
                    QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
                    pvStationsViewQueryWrapper.eq("plant_id",plantId);
                    PvStationsView pvStationsView = pvStationsViewMapper.selectOne(pvStationsViewQueryWrapper);
                    if(pvStationsView != null){
                        String peakPowerActual = pvStationsView.getPeakPowerActual();
                        //累计
                        QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
                        pvEnergyQueryWrapper.eq("plant_id",plantId);
                        pvEnergyQueryWrapper.eq("year",year);
                        List<PvEnergy> list = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
                        BigDecimal bigDecimal = new BigDecimal(0.00);
                        for (int k = 0; k < list.size(); k++) {
                            BigDecimal energy = list.get(k).getEnergy();
                            bigDecimal=bigDecimal.add(energy);
                        }
                        String totalEnergy = pvStationsView.getTotalEnergy();
                        if((bigDecimal!= null) && ((StringUtils.isNotBlank(peakPowerActual) && !peakPowerActual.equals("0")))){
                            records.get(i).setYearNum(bigDecimal.divide(new BigDecimal(peakPowerActual),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                        }
                        records.get(i).setYearPower(bigDecimal.setScale(2,BigDecimal.ROUND_HALF_UP));
                        records.get(i).setName(StringUtils.isNotBlank(name) ? name:"");
                        records.get(i).setStatus(StringUtils.isNotBlank(stationStatus) ? stationStatus:"");
                        records.get(i).setIncome(new BigDecimal(0.9));
                        records.get(i).setPeakPower(StringUtils.isNotBlank(peakPowerActual) ? peakPowerActual:"");
                        records.get(i).setTotalPower(StringUtils.isNotBlank(totalEnergy) ? new BigDecimal(totalEnergy):new BigDecimal(0.00));
                        records.get(i).setPlantId(plantId);

                        //统计年
                        QueryWrapper<PvEnergy> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("plant_id",plantId);
                        queryWrapper.eq("year",year);
                        List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(queryWrapper);
                        Map<Integer, BigDecimal> ageGroup = pvEnergies.stream().collect(Collectors.groupingBy(PvEnergy::getMonth
                                , Collectors.mapping(PvEnergy::getEnergy, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

                        List<PvEnergyVo> groupList = ageGroup.entrySet().stream()
                                .map(e -> new PvEnergyVo(e.getKey(), e.getValue(),new BigDecimal(0.00),e.getValue().multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))))
                                .collect(Collectors.toList());
                        groupList.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());

                        records.get(i).setYearIncome(new BigDecimal(0.9).multiply(bigDecimal).setScale(2,BigDecimal.ROUND_HALF_UP));
                        records.get(i).setEnergyVos(groupList);
                    }
                }
                //统计总数居
                if(dayType.equals("3")){
                    QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
                    pvStationsViewQueryWrapper.eq("plant_id",plantId);
                    PvStationsView pvStationsView = pvStationsViewMapper.selectOne(pvStationsViewQueryWrapper);
                    if(pvStationsView != null){
                        //累计
                        String totalEnergy = pvStationsView.getTotalEnergy();
                        String peakPowerActual = pvStationsView.getPeakPowerActual();
                        if((StringUtils.isNotBlank(totalEnergy) && !totalEnergy.equals("0")) && ((StringUtils.isNotBlank(peakPowerActual) && !peakPowerActual.equals("0")))){
                            records.get(i).setTotalPowerNum(new BigDecimal(totalEnergy).divide(new BigDecimal(peakPowerActual),BigDecimal.ROUND_CEILING));
                        }
                        records.get(i).setName(StringUtils.isNotBlank(name) ? name:"");
                        records.get(i).setStatus(StringUtils.isNotBlank(stationStatus) ? stationStatus:"");
                        records.get(i).setIncome(new BigDecimal(0.9));
                        records.get(i).setPeakPower(StringUtils.isNotBlank(peakPowerActual) ? peakPowerActual: "");
                        records.get(i).setTotalPower(StringUtils.isNotBlank(totalEnergy) ? new BigDecimal(totalEnergy):new BigDecimal(0.00));
                        records.get(i).setPlantId(plantId);

                        QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
                        pvEnergyQueryWrapper.eq("plant_id",plantId);
                        List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
                        Map<Integer, BigDecimal> ageGroup = pvEnergies.stream().collect(Collectors.groupingBy(PvEnergy::getYear
                                , Collectors.mapping(PvEnergy::getEnergy, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
                        List<PvEnergyVo> groupList = ageGroup.entrySet().stream()
                                .map(e -> new PvEnergyVo(e.getKey(), e.getValue(),new BigDecimal(0.00),e.getValue().multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))))
                                .collect(Collectors.toList());
                        groupList.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
                        records.get(i).setEnergyVos(groupList);
                    }
                }

            }
            pvStationsPage.setRecords(records);
            return Result.ok(pvStationsPage);
        }
        return Result.ok();
    }

    /**
     * 电量报表
     * @param pageVo
     * @return
     */
    @Override
    public Result powerReport(PageVo pageVo) {
        Date queryTime = pageVo.getQueryTime();
        String dayType = pageVo.getDayType();
        LocalDate time = queryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int monthValue = time.getMonthValue();
        int year = time.getYear();
        List<PvStations> list = this.list();
        BigDecimal bigDecimal1 = new BigDecimal(0.00);
        for (int i = 0; i < list.size(); i++) {
            String peakPower = list.get(i).getPeakPower();
            bigDecimal1=bigDecimal1.add(new BigDecimal(peakPower));
        }
        //月
        if(dayType.equals("1")){
            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            pvEnergyQueryWrapper.eq("year",year);
            pvEnergyQueryWrapper.eq("month",monthValue);
            List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
            List<PvEnergy> collect = pvEnergies.stream().sorted(Comparator.comparing(PvEnergy::getDay)).collect(Collectors.toList());
            Map<Integer, List<PvEnergy>> collect1 = collect.stream().collect(Collectors.groupingBy(PvEnergy::getDay));
            Set<Map.Entry<Integer, List<PvEnergy>>> entries = collect1.entrySet();
            PvEnergyResultVo pvEnergyResultVo = new PvEnergyResultVo();
            List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
            for (Map.Entry<Integer, List<PvEnergy>> entry : entries) {
                Integer key = entry.getKey();
                List<PvEnergy> value = entry.getValue();
                BigDecimal bigDecimal = new BigDecimal(0.00);
                for (int i = 0; i < value.size(); i++) {
                    bigDecimal= bigDecimal.add(value.get(i).getEnergy());
                }
                PvEnergyVo pvEnergyVo = new PvEnergyVo();
                pvEnergyVo.setKey(key);
                pvEnergyVo.setIncome(bigDecimal);
                pvEnergyVo.setValue(bigDecimal);
                if(!bigDecimal.equals(BigDecimal.ZERO)){
                    pvEnergyVo.setHour(bigDecimal.divide(bigDecimal1,BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                }
                pvEnergyVos.add(pvEnergyVo);
            }
            BigDecimal bigDecimal = new BigDecimal(0.00);
            for (int i = 0; i < pvEnergies.size(); i++) {
                bigDecimal=bigDecimal.add(pvEnergies.get(i).getEnergy());
            }
            pvEnergyResultVo.setPower(bigDecimal);
            pvEnergyResultVo.setIncome(bigDecimal.multiply(new BigDecimal(1.1)));
            pvEnergyResultVo.setTime(String.valueOf(monthValue));
            pvEnergyVos.stream().filter(t -> t.getIncome()==new BigDecimal(0)).sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
            pvEnergyResultVo.setPvEnergyVos(pvEnergyVos);
            return Result.ok(pvEnergyResultVo);
        }
        //年
        if(dayType.equals("2")){
            ArrayList<PvEnergyResultVo> pvEnergyResultVos = new ArrayList<>();
            for (int i = 1; i < monthValue; i++) {
                QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
                pvEnergyQueryWrapper.eq("year",year);
                pvEnergyQueryWrapper.eq("month",i);
                List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
                List<PvEnergy> collect = pvEnergies.stream().sorted(Comparator.comparing(PvEnergy::getDay)).collect(Collectors.toList());
                Map<Integer, List<PvEnergy>> collect1 = collect.stream().collect(Collectors.groupingBy(PvEnergy::getDay));
                Set<Map.Entry<Integer, List<PvEnergy>>> entries = collect1.entrySet();
                PvEnergyResultVo pvEnergyResultVo = new PvEnergyResultVo();
                List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
                for (Map.Entry<Integer, List<PvEnergy>> entry : entries) {
                    Integer key = entry.getKey();
                    List<PvEnergy> value = entry.getValue();
                    BigDecimal bigDecimal = new BigDecimal(0.00);
                    for (int k = 0; k < value.size(); k++) {
                        bigDecimal= bigDecimal.add(value.get(k).getEnergy());
                    }
                    PvEnergyVo pvEnergyVo = new PvEnergyVo();
                    pvEnergyVo.setKey(key);
                    pvEnergyVo.setIncome(bigDecimal);
                    pvEnergyVo.setValue(bigDecimal);
                    pvEnergyVo.setHour(bigDecimal.divide(bigDecimal1,BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                    pvEnergyVos.add(pvEnergyVo);
                }

                BigDecimal bigDecimal = new BigDecimal(0.00);
                for (int j = 0; j < pvEnergies.size(); j++) {
                    bigDecimal=bigDecimal.add(pvEnergies.get(j).getEnergy());
                }
                pvEnergyResultVo.setPower(bigDecimal);
                pvEnergyResultVo.setHour(bigDecimal.divide(bigDecimal1,BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                pvEnergyResultVo.setIncome(bigDecimal.multiply(new BigDecimal(1.1)));
                pvEnergyResultVo.setTime(String.valueOf(i));
                pvEnergyVos.stream().filter(t -> t.getIncome()==new BigDecimal(0)).sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
                pvEnergyResultVo.setPvEnergyVos(pvEnergyVos);
                pvEnergyResultVos.add(pvEnergyResultVo);
            }
            return Result.ok(pvEnergyResultVos);
        }
        //累计
        if(dayType.equals("3")){
            //先按照年分组，分组之后得到每年的list。再把这个list按照月分组，然后再进行计算
            ArrayList<PvEnergyResultVo> pvEnergyResultVos = new ArrayList<>();
            PvEnergyResultVo pvEnergyResultVo = new PvEnergyResultVo();
            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);

            Map<Integer, List<PvEnergy>> collect = pvEnergies.stream().collect(Collectors.groupingBy(PvEnergy::getYear));
            Set<Map.Entry<Integer, List<PvEnergy>>> entries = collect.entrySet();
            for (Map.Entry<Integer, List<PvEnergy>> entry : entries) {
                Integer key = entry.getKey();
                List<PvEnergy> value = entry.getValue();
                BigDecimal bigDecimal = new BigDecimal(0.00);
                for (int i = 0; i < value.size(); i++) {
                    BigDecimal energy = value.get(i).getEnergy();
                    bigDecimal=bigDecimal.add(energy);
                }
                pvEnergyResultVo.setTime(String.valueOf(key));
                pvEnergyResultVo.setPower(bigDecimal);
                pvEnergyResultVo.setIncome(bigDecimal.multiply(new BigDecimal(1.1)));
                ArrayList<PvEnergyVo> pvEnergyVos = new ArrayList<>();
                Map<Integer, List<PvEnergy>> collect1 = value.stream().collect(Collectors.groupingBy(PvEnergy::getMonth));
                Set<Map.Entry<Integer, List<PvEnergy>>> entries1 = collect1.entrySet();
                for (Map.Entry<Integer, List<PvEnergy>> entry1 : entries1) {
                    PvEnergyVo pvEnergyVo = new PvEnergyVo();
                    Integer key1 = entry1.getKey();
                    List<PvEnergy> value1 = entry1.getValue();
                    BigDecimal big = new BigDecimal(0.00);
                    for (int i = 0; i < value1.size(); i++) {
                        BigDecimal energy = value1.get(i).getEnergy();
                        big=big.add(energy);
                    }
                    pvEnergyVo.setKey(key1);
                    pvEnergyVo.setValue(big);
                    pvEnergyVo.setIncome(big.multiply(new BigDecimal(1.1)));
                    pvEnergyVo.setHour(bigDecimal.divide(bigDecimal1,BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                    pvEnergyVos.add(pvEnergyVo);
                }
                pvEnergyResultVo.setPvEnergyVos(pvEnergyVos);
            }
            pvEnergyResultVos.add(pvEnergyResultVo);
            return Result.ok(pvEnergyResultVos);
        }
        return Result.ok();
    }

    @Override
    public String getStatus(Integer plantId) {
        return pvStationsMapper.getStatus(plantId);
    }

    @Override
    public List<String> getUserIds(String userId) {
        return pvStationsMapper.getUserIds(userId);
    }



    public List<PlantResultVo> getData(Integer page, Integer perpage){
        if(perpage >= 100){
            int i = perpage / 100;
            ArrayList<PlantResultVo> plantResultVos = new ArrayList<>();
            for (int j = 1; j <= i+1; j++) {
                HashMap<String, Object> paramMap = new HashMap<>();
                int random=(int) ((Math.random() * 10) + 1);
                paramMap.put("search_type", random);
                paramMap.put("page", j);
                paramMap.put("perpage", 100);
                String result2 = HttpRequest.get(stationConfig.getStationList())
                        .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                        .form(paramMap)//表单内容
                        .timeout(20000)//超时，毫秒
                        .execute()
                        .body();
                PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
                plantResultVos.add(plantResultVo);

            }
            return plantResultVos;
        }
        else {
            ArrayList<PlantResultVo> plantResultVos = new ArrayList<>();
            HashMap<String, Object> paramMap = new HashMap<>();
            int random=(int) ((Math.random() * 10) + 1);
            paramMap.put("search_type", random);
            paramMap.put("page", page);
            paramMap.put("perpage", perpage);
            String result2 = HttpRequest.get(stationConfig.getStationList())
                    .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                    .form(paramMap)//表单内容
                    .timeout(20000)//超时，毫秒
                    .execute()
                    .body();
            PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
            plantResultVos.add(plantResultVo);
            return plantResultVos;
        }
    }

    public Integer getCount(){
        HashMap<String, Object> paramMap = new HashMap<>();
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
        String result2 = HttpRequest.get(stationConfig.getStationList())
                .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
        Integer count = plantResultVo.getData().getCount();
        return count;
    }


    /**
     * 获取逆变器sn码集合
     */
    public List<Device> getDeviceSnList(){
        Integer count = this.getCount();
        //如果总数小于100
        if(count < 100 ){
            List<PlantResultVo> data = this.getData(1, 100);
            ArrayList<StationListVo> plants = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                List<StationListVo> stationListVos = data.get(i).getData().getPlants();
                stationListVos.addAll(plants);
            }
            if(plants != null && plants.size()>0){
                List<Device> devices = new ArrayList<>();
                for (int i = 0; i < plants.size(); i++) {
                    Integer plantId = plants.get(i).getPlantId();
                    HashMap<String, Object> paramMap = new HashMap<>();
                    paramMap.put("plant_id", plantId);
                    String result2 = HttpRequest.get(stationConfig.getDeviceList())
                            .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                            .form(paramMap)//表单内容
                            .timeout(20000)//超时，毫秒
                            .execute()
                            .body();

                    DeviceVo deviceVo = JSON.parseObject(result2, DeviceVo.class);
                    if(deviceVo != null){
                        List<Device> deviceList = deviceVo.getData().getDevices();
                        List<Device> collect = deviceList.stream().filter(j -> j.getType().equals("1")).collect(Collectors.toList());
                        collect.stream().forEach(j->{
                            j.setPlantId(plantId.toString());
                        });
                        devices.addAll(collect);
                    }
                }
                return devices;
            }
        }
        if(count > 100){
            int i = count / 100;
            List<PlantResultVo> data = this.getData(1, count);
            ArrayList<StationListVo> plants = new ArrayList<>();
            for (int k = 0; k < data.size(); k++) {
                List<StationListVo> stationListVos = data.get(k).getData().getPlants();
                plants.addAll(stationListVos);
            }
            if(plants != null && plants.size()>0){
                List<Device> devices = new ArrayList<>();
                for (int k = 0; k < plants.size(); k++) {
                    Integer plantId = plants.get(k).getPlantId();
                    HashMap<String, Object> paramMap = new HashMap<>();
                    paramMap.put("plant_id", plantId);
                    String result2 = HttpRequest.get(stationConfig.getDeviceList())
                            .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                            .form(paramMap)//表单内容
                            .timeout(20000)//超时，毫秒
                            .execute()
                            .body();
                    DeviceVo deviceVo = JSON.parseObject(result2, DeviceVo.class);
                    List<Device> deviceList = deviceVo.getData().getDevices();
                    deviceList.stream().filter(m->m.getType().equals("1")).forEach(m->{
                        m.setPlantId(plantId.toString());
                    });
                    devices.addAll(deviceList);
                }
                return devices;
            }
        }
        return new ArrayList<>();
    }
}
