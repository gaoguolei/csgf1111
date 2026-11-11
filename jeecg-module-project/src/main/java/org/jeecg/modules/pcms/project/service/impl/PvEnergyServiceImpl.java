package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvDayPower;
import org.jeecg.modules.pcms.project.entity.PvEnergy;
import org.jeecg.modules.pcms.project.mapper.PvEnergyMapper;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;
import org.jeecg.modules.pcms.project.stationvo.PvEnergyResultVo;
import org.jeecg.modules.pcms.project.stationvo.StationChartVo;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;
import org.jeecg.modules.pcms.project.vo.HistoryPowerResult;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;
import org.jeecg.modules.pcms.project.vo.QueryDateParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:16
 * @Version 1.5
 */
@Service
public class PvEnergyServiceImpl extends ServiceImpl<PvEnergyMapper, PvEnergy> implements IPvEnergyService {
    @Autowired
    private IPvStationsService iPvStationsService;

    @Autowired
    private IPvStationsViewService iPvStationsViewService;

    @Autowired
    private IPvDayPowerService iPvDayPowerService;

    @Autowired
    private StationConfig stationConfig;

    @Autowired
    private PvEnergyMapper pvEnergyMapper;
    public void insert(){
        List<PvStations> list = iPvStationsService.list();
        for (int i = 0; i < list.size(); i++) {
            try {
                Integer plantId = list.get(i).getPlantId();
                LocalDate over = LocalDate.of(2023, 01, 01);
                List<QueryDateParam> queryDates = getQueryDates(over);
                for (int j = 0; j < queryDates.size(); j++) {
                    try {
                        LocalDate start = queryDates.get(j).getStart();
                        LocalDate end = queryDates.get(j).getEnd();
                        RestTemplate restTemplate = new RestTemplate();
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.add("token", stationConfig.getToken());
                        String url = stationConfig.getHistoryEnergy()+"?plant_id={0}&start_date={1}&end_date={2}&time_unit={3}";
                        ResponseEntity<HistoryPowerResult> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), HistoryPowerResult.class, plantId, start, end, "day");
                        if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                            HistoryPowerResult body = response.getBody();
                            List<HistoryPowerResult.PageData.Energys> energys = body.getData().getEnergys();
                            //拿到数据之后插入到数据库
                            for (int k = 0; k <energys.size(); k++) {
                                BigDecimal energy = energys.get(k).getEnergy();
                                LocalDate date = energys.get(k).getDate();
                                PvEnergy pvEnergy = new PvEnergy();
                                pvEnergy.setYear(date.getYear());
                                pvEnergy.setMonth(date.getMonthValue());
                                pvEnergy.setDay( date.getDayOfMonth());
                                pvEnergy.setPlantId(plantId);
                                pvEnergy.setEnergy(energy);
                                this.save(pvEnergy);
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    /**
     * 插入今天的数据
     */
    @Override
    public void updatePvEnergy() {
        List<PvStations> list = iPvStationsService.list();
        for (int i = 0; i < list.size(); i++) {
            try {
                Integer plantId = list.get(i).getPlantId();
                LocalDate time = LocalDate.now();
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add("token", stationConfig.getToken());
                String url = stationConfig.getHistoryEnergy()+"?plant_id={0}&start_date={1}&end_date={2}&time_unit={3}";
                ResponseEntity<HistoryPowerResult> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), HistoryPowerResult.class, plantId, time, time, "day");
                if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                    HistoryPowerResult body = response.getBody();
                    List<HistoryPowerResult.PageData.Energys> energys = body.getData().getEnergys();
                    //拿到数据之后插入到数据库
                    for (int k = 0; k < energys.size(); k++) {
                        BigDecimal energy = energys.get(k).getEnergy();
                        LocalDate date = energys.get(k).getDate();
                        PvEnergy pvEnergy = new PvEnergy();
                        pvEnergy.setYear(date.getYear());
                        pvEnergy.setMonth(date.getMonthValue());
                        pvEnergy.setDay(date.getDayOfMonth());
                        pvEnergy.setPlantId(plantId);
                        pvEnergy.setEnergy(energy);
                        this.save(pvEnergy);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    @Override
    public Result getStationChart(PageVo pageVo) {
        Integer plantId = pageVo.getPlantId();
        QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
        pvStationsViewQueryWrapper.eq("plant_id",plantId);
        PvStationsView pvStationsView = iPvStationsViewService.getOne(pvStationsViewQueryWrapper);
        String dayType = pageVo.getDayType();
        Date queryTime = pageVo.getQueryTime();
        LocalDate time = queryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int dayOfMonth = time.getDayOfMonth();
        int monthValue = time.getMonthValue();
        int year = time.getYear();
        //如果按照天查询就是另外一套逻辑
        if(dayType.equals("0")){
            QueryWrapper<PvDayPower> pvDayPowerQueryWrapper = new QueryWrapper<>();
            pvDayPowerQueryWrapper.eq("plant_id",plantId);
            pvDayPowerQueryWrapper.eq("year",year);
            pvDayPowerQueryWrapper.eq("month",monthValue);
            pvDayPowerQueryWrapper.eq("day",dayOfMonth);
            List<PvDayPower> list = iPvDayPowerService.list(pvDayPowerQueryWrapper);
            List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
            StationChartVo stationChartVo = new StationChartVo();
            for (int i = 0; i < list.size(); i++) {
                Integer hour = list.get(i).getHour();
                BigDecimal energy = list.get(i).getPower();
                if(hour != null && energy!= null){
                    pvEnergyVos.add(new PvEnergyVo(hour,energy,new BigDecimal(0.00),energy.multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))));
                }
            }
            List<PvEnergyVo> collect = pvEnergyVos.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
            String peakPower = pvStationsView.getPeakPowerActual();
//            String todayEnergy = pvStationsView.getTodayEnergy();

            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            pvEnergyQueryWrapper.eq("plant_id",plantId);
            pvEnergyQueryWrapper.eq("year",year);
            pvEnergyQueryWrapper.eq("month",monthValue);
            pvEnergyQueryWrapper.eq("day",dayOfMonth);
            if((StringUtils.isNotBlank(peakPower) && !peakPower.equals("0"))){
                PvEnergy pvEnergy = pvEnergyMapper.selectOne(pvEnergyQueryWrapper);
                if(pvEnergy != null){
                BigDecimal energy = pvEnergy.getEnergy();
                StationCountVo stationCountVo = new StationCountVo();
                    stationCountVo.setPower(energy.setScale(2,BigDecimal.ROUND_HALF_UP));
                    stationCountVo.setHour(energy.divide(new BigDecimal(peakPower),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                    stationCountVo.setIncome(energy.multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP)));
                    stationChartVo.setStationCountVo(stationCountVo);
                }
                stationChartVo.setPvEnergyVos(collect);
            }
            return Result.ok(stationChartVo);
        }
        //月
        if(dayType.equals("1")){
            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            pvEnergyQueryWrapper.eq("plant_id",plantId);
            pvEnergyQueryWrapper.eq("year",year);
            pvEnergyQueryWrapper.eq("month",monthValue);
            List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
            List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
            StationChartVo stationChartVo = new StationChartVo();
            for (int i = 0; i < pvEnergies.size(); i++) {
                Integer day = pvEnergies.get(i).getDay();
                BigDecimal energy = pvEnergies.get(i).getEnergy();
                if(day!= null && energy != null){
                    pvEnergyVos.add(new PvEnergyVo(day,energy,new BigDecimal(0.00),energy.multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))));
                }
            }
            List<PvEnergyVo> collect = pvEnergyVos.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
            String peakPower = pvStationsView.getPeakPowerActual();
            BigDecimal todayEnergy = new BigDecimal(0.00);
            for (PvEnergy pvEnergy : pvEnergies) {
                BigDecimal energy = pvEnergy.getEnergy();
                todayEnergy=todayEnergy.add(energy);
            }
            StationCountVo stationCountVo = new StationCountVo();
            if((StringUtils.isNotBlank(peakPower) && !peakPower.equals("0"))&& (todayEnergy != null)){
                stationCountVo.setPower(todayEnergy.setScale(2,BigDecimal.ROUND_HALF_UP));
                stationCountVo.setHour(todayEnergy.divide(new BigDecimal(peakPower),BigDecimal.ROUND_CEILING).setScale(2,BigDecimal.ROUND_HALF_UP));
                stationCountVo.setIncome(todayEnergy.multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP)));
            }
            stationChartVo.setPvEnergyVos(collect);
            stationChartVo.setStationCountVo(stationCountVo);
            return Result.ok(stationChartVo);
        }
        //年
        if(dayType.equals("2")){
            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            pvEnergyQueryWrapper.eq("plant_id",plantId);
            pvEnergyQueryWrapper.eq("year",year);
            List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
            Map<Integer, BigDecimal> ageGroup = pvEnergies.stream().collect(Collectors.groupingBy(PvEnergy::getMonth
                    , Collectors.mapping(PvEnergy::getEnergy, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

            List<PvEnergyVo> groupList = ageGroup.entrySet().stream()
                    .map(e -> new PvEnergyVo(e.getKey(), e.getValue(),new BigDecimal(0.00),e.getValue().multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))))
                    .collect(Collectors.toList());
            groupList.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
            StationChartVo stationChartVo = new StationChartVo();
            String peakPower = pvStationsView.getPeakPowerActual();
            String todayEnergy = pvStationsView.getYearlyEnergy();
            StationCountVo stationCountVo = new StationCountVo();
            if((StringUtils.isNotBlank(peakPower) && !peakPower.equals("0"))&& (StringUtils.isNotBlank(todayEnergy) && !peakPower.equals("0"))){
                stationCountVo.setPower(new BigDecimal(todayEnergy).setScale(2,BigDecimal.ROUND_HALF_UP));
                stationCountVo.setHour(new BigDecimal(todayEnergy).divide(new BigDecimal(peakPower),BigDecimal.ROUND_CEILING));
                stationCountVo.setIncome(new BigDecimal(todayEnergy).multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP)));
            }
            stationChartVo.setPvEnergyVos(groupList);
            stationChartVo.setStationCountVo(stationCountVo);
            return Result.ok(stationChartVo);
        }
        //总
        if(dayType.equals("3")){
            QueryWrapper<PvEnergy> pvEnergyQueryWrapper = new QueryWrapper<>();
            pvEnergyQueryWrapper.eq("plant_id",plantId);
            List<PvEnergy> pvEnergies = pvEnergyMapper.selectList(pvEnergyQueryWrapper);
            Map<Integer, BigDecimal> ageGroup = pvEnergies.stream().collect(Collectors.groupingBy(PvEnergy::getYear
                    , Collectors.mapping(PvEnergy::getEnergy, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

            List<PvEnergyVo> groupList = ageGroup.entrySet().stream()
                    .map(e -> new PvEnergyVo(e.getKey(), e.getValue(),new BigDecimal(0.00),e.getValue().multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP))))
                    .collect(Collectors.toList());
            groupList.stream().sorted(Comparator.comparing(PvEnergyVo::getKey)).collect(Collectors.toList());
            StationChartVo stationChartVo = new StationChartVo();
            String peakPower = pvStationsView.getPeakPowerActual();
            String todayEnergy = pvStationsView.getTotalEnergy();
            StationCountVo stationCountVo = new StationCountVo();
            if((StringUtils.isNotBlank(peakPower) && !peakPower.equals("0"))&& (StringUtils.isNotBlank(todayEnergy) && !peakPower.equals("0"))){
                stationCountVo.setPower(new BigDecimal(todayEnergy).setScale(2,BigDecimal.ROUND_HALF_UP));
                stationCountVo.setHour(new BigDecimal(todayEnergy).divide(new BigDecimal(peakPower),BigDecimal.ROUND_CEILING));
                stationCountVo.setIncome(new BigDecimal(todayEnergy).multiply(new BigDecimal(0.9).setScale(2,BigDecimal.ROUND_HALF_UP)));
            }
            stationChartVo.setStationCountVo(stationCountVo);
            stationChartVo.setPvEnergyVos(groupList);
            return Result.ok(stationChartVo);
        }
        return Result.ok();
    }

    @Override
    public List<StationCountVo> weeks() {
        LocalDate currentDate = LocalDate.now();
        // 创建日期格式化对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 创建日期列表
        ArrayList<StationCountVo> stationCountVos = new ArrayList<>();
        // 循环获取最近7天的日期
        for (int i = 0; i < 7; i++) {
            StationCountVo stationCountVo = new StationCountVo();
            // 将日期格式化为字符串并添加到日期列表中
            String format = currentDate.minusDays(i).format(formatter);
            LocalDate date = LocalDate.parse(format);

            int year = date.getYear();
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();
            BigDecimal week = pvEnergyMapper.getWeek(year, month, day);
            stationCountVo.setKey(day);
            stationCountVo.setPower(week);
            stationCountVo.setIndex(i);
            if(week !=null){
                stationCountVo.setIncome(week.multiply(new BigDecimal(1.1).setScale(2, RoundingMode.UP)));
            }
            stationCountVos.add(stationCountVo);
        }
        // 输出日期列表
        return stationCountVos;
    }

    @Override
    public List<StationCountVo> month() {
        ArrayList<StationCountVo> stationCountVos = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        for (int i = 0; i < 6; i++) {
            StationCountVo stationCountVo = new StationCountVo();
            calendar.add(Calendar.MONTH, -1);
            int month = calendar.get(Calendar.MONTH)+2;
            int weekYear = calendar.getWeekYear();
            BigDecimal bigDecimal = pvEnergyMapper.getMonth(weekYear, month);
            stationCountVo.setKey(month);
            stationCountVo.setIndex(i);
            if(bigDecimal != null){
                stationCountVo.setPower(bigDecimal);
                stationCountVo.setIncome(bigDecimal.multiply(new BigDecimal(1.1).setScale(2, RoundingMode.UP)));
            }
            stationCountVos.add(stationCountVo);
        }
        return stationCountVos;
    }

    @Override
    public Result getStationInfoStatistics(PageVo pageVo) {
        Integer plantId = pageVo.getPlantId();
        StationChartVo stationChartVo = new StationChartVo();
        QueryWrapper<PvStationsView> pvStationsViewQueryWrapper = new QueryWrapper<>();
        pvStationsViewQueryWrapper.eq("plant_id",plantId);
        PvStationsView pvStationsView = iPvStationsViewService.getOne(pvStationsViewQueryWrapper);
        Date queryTime = pageVo.getTime();
        LocalDate time = queryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int dayOfMonth = time.getDayOfMonth();
        int monthValue = time.getMonthValue();
        int year = time.getYear();
        //如果按照天查询就是另外一套逻辑
        QueryWrapper<PvDayPower> pvDayPowerQueryWrapper = new QueryWrapper<>();
        pvDayPowerQueryWrapper.eq("plant_id",plantId);
        pvDayPowerQueryWrapper.eq("year",year);
        pvDayPowerQueryWrapper.eq("month",monthValue);
        pvDayPowerQueryWrapper.eq("day",dayOfMonth);
        List<PvDayPower> list = iPvDayPowerService.list(pvDayPowerQueryWrapper);
        if(list != null){
            Map<Integer, List<PvDayPower>> collect = list.stream().filter(a -> a.getPower() != null).sorted(Comparator.comparing(PvDayPower::getHour)).collect(Collectors.groupingBy(PvDayPower::getHour));
            Set<Map.Entry<Integer, List<PvDayPower>>> entries = collect.entrySet();
            List<PvEnergyVo> pvEnergyVos = new ArrayList<>();
            for (Map.Entry<Integer, List<PvDayPower>> entry : entries) {
                List<PvDayPower> value = entry.getValue();
                BigDecimal bigDecimal = new BigDecimal(0.00);
                for (int i = 0; i < value.size(); i++) {
                    bigDecimal=bigDecimal.add(value.get(i).getPower()).divide(new BigDecimal(1000).setScale(2,RoundingMode.UP));
                }
                PvEnergyVo pvEnergyVo = new PvEnergyVo();
                Integer key = entry.getKey();
                pvEnergyVo.setKey(key);
                pvEnergyVo.setValue(bigDecimal.setScale(2,RoundingMode.UP));
                pvEnergyVos.add(pvEnergyVo);
            }
            stationChartVo.setPvEnergyVos(pvEnergyVos);
        }
        StationCountVo stationCountVo = new StationCountVo();
        String peakPower = pvStationsView.getPeakPowerActual();
        String todayEnergy = pvStationsView.getTodayEnergy();
        String totalEnergy = pvStationsView.getTotalEnergy();
        String currentPower = pvStationsView.getCurrentPower();
        if(StringUtils.isNotBlank(peakPower)){
            stationCountVo.setCurrentPower(new BigDecimal(currentPower));
        }
        if(StringUtils.isNotBlank(totalEnergy)){
            stationCountVo.setTotalPower(new BigDecimal(totalEnergy));
        }
        if(StringUtils.isNotBlank(todayEnergy)){
            stationCountVo.setPower(new BigDecimal(todayEnergy));
        }
        if(StringUtils.isNotBlank(peakPower)){
            stationCountVo.setPeakPower(new BigDecimal(peakPower));
        }


        stationChartVo.setStationCountVo(stationCountVo);
        return Result.ok(stationChartVo);
    }


    public List<QueryDateParam> getQueryDates(LocalDate over) {
        LocalDate now = LocalDate.now();
        if (over.compareTo(now) > 0) {
            return Collections.emptyList();
        }
        if (over.isEqual(now.plusDays(0))) {
            return Collections.singletonList(new QueryDateParam(over, over));
        }

        List<QueryDateParam> result = new ArrayList<>();
        QueryDateParam temp = new QueryDateParam(now.plusDays(-6), now.plusDays(0));

        while (true) {
            result.add(temp);
            if (temp.getStart().isBefore(over) || temp.getStart().isEqual(over)) {
                temp.setStart(over);
                break;
            }
            temp = new QueryDateParam(temp.getStart().plusDays(-6), temp.getStart().plusDays(0));
        }
        return result;
    }

}
