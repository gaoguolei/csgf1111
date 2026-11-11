package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.mapper.PvDeviceHistoryDataMapper;
import org.jeecg.modules.pcms.project.service.IPvDeviceHistoryDataService;
import org.jeecg.modules.pcms.project.service.IPvDeviceService;
import org.jeecg.modules.pcms.project.stationentity.PvDevice;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceHistoryData;
import org.jeecg.modules.pcms.project.stationvo.DeviceHistoryVo;
import org.jeecg.modules.pcms.project.stationvo.ResultPvDeviceVO;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.jeecg.modules.pcms.project.vo.PvEnergyVo;
import org.jeecg.modules.pcms.project.vo.QueryDateParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jeecg.modules.pcms.project.stationvo.DeviceHistoryPowerResult;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author gongzhou
 * @Date 2023/11/11 下午4:31
 * @Version 1.5
 */

@Service
public class PvDeviceHistoryDataServiceImpl extends ServiceImpl<PvDeviceHistoryDataMapper, PvDeviceHistoryData> implements IPvDeviceHistoryDataService {

    @Autowired
    private IPvDeviceService iPvDeviceService;
    @Autowired
    private PvDeviceHistoryDataMapper pvDeviceHistoryDataMapper;

    @Override
    public void insert()  {
        List<PvDevice> list = iPvDeviceService.list();
        for (int i = 0; i < list.size(); i++) {
            String deviceSn = list.get(i).getDeviceSn();
            String plantId = list.get(i).getPlantId();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date endDate = new Date(new Date().getTime());
            List<String> everyDay = null;
            try {
                everyDay = this.findEveryDay("2023-01-01", format.format(endDate));
                for (int j = 0; j < everyDay.size(); j++) {
                    String start = everyDay.get(j);
                    List<DeviceHistoryPowerResult.PageData.HistoryDevice> historyDevices = null;
                    try {
                        historyDevices = this.getList(start, deviceSn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if(historyDevices != null && historyDevices.size()>0){
                        for (DeviceHistoryPowerResult.PageData.HistoryDevice historyDevice : historyDevices) {
                            PvDeviceHistoryData pvDeviceHistoryData = new PvDeviceHistoryData();
                            String time = historyDevice.getTime();
                            String powerFactor = historyDevice.getPowerFactor();
                            if(StringUtils.isNotBlank(powerFactor)){
                                pvDeviceHistoryData.setPowerFactor(powerFactor);
                            }
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            if(StringUtils.isNotBlank(time)){
                                Date date = null;
                                try {
                                    date = simpleDateFormat.parse(time);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                                LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                int year = localDate.getYear();
                                int monthValue = localDate.getMonthValue();
                                int dayOfMonth = localDate.getDayOfMonth();
                                BeanUtils.copyProperties(historyDevice,pvDeviceHistoryData);
                                pvDeviceHistoryData.setSn(deviceSn);
                                pvDeviceHistoryData.setPlantId(plantId);
                                pvDeviceHistoryData.setYear(year);
                                pvDeviceHistoryData.setMonth(monthValue);
                                pvDeviceHistoryData.setDay(dayOfMonth);
                                pvDeviceHistoryData.setTime(date);
                            }
                            this.save(pvDeviceHistoryData);
                        }
                    }
                  }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateData() {
        List<PvDevice> list = iPvDeviceService.list();
        for (int i = 0; i < list.size(); i++) {
            String deviceSn = list.get(i).getDeviceSn();
            String plantId = list.get(i).getPlantId();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date endDate = new Date();
            try {
                List<DeviceHistoryPowerResult.PageData.HistoryDevice> historyDevices = this.getList(format.format(endDate), deviceSn);
                for (DeviceHistoryPowerResult.PageData.HistoryDevice historyDevice : historyDevices) {
                    try {
                        PvDeviceHistoryData pvDeviceHistoryData = new PvDeviceHistoryData();
                        String time = historyDevice.getTime();
                        String powerFactor = historyDevice.getPowerFactor();
                        if(StringUtils.isNotBlank(powerFactor)){
                            pvDeviceHistoryData.setPowerFactor(powerFactor);
                        }
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        if(StringUtils.isNotBlank(time)){
                            Date date = null;
                            try {
                                date = simpleDateFormat.parse(time);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            int year = localDate.getYear();
                            int monthValue = localDate.getMonthValue();
                            int dayOfMonth = localDate.getDayOfMonth();
                            BeanUtils.copyProperties(historyDevice,pvDeviceHistoryData);
                            pvDeviceHistoryData.setSn(deviceSn);
                            pvDeviceHistoryData.setPlantId(plantId);
                            pvDeviceHistoryData.setYear(year);
                            pvDeviceHistoryData.setMonth(monthValue);
                            pvDeviceHistoryData.setDay(dayOfMonth);
                            pvDeviceHistoryData.setTime(date);
                        }
                        this.save(pvDeviceHistoryData);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public Result deviceReport(PageVo pageVo) {
        String dayType = pageVo.getDayType();
        String sn = pageVo.getSn();
        Date queryTime = pageVo.getQueryTime();
        LocalDate time = queryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int dayOfMonth = time.getDayOfMonth();
        int monthValue = time.getMonthValue();
        int year = time.getYear();
        //日
        if(dayType.equals("0")){
            QueryWrapper<PvDeviceHistoryData> pvDeviceHistoryDataQueryWrapper = new QueryWrapper<>();
            pvDeviceHistoryDataQueryWrapper.eq("year",year);
            pvDeviceHistoryDataQueryWrapper.eq("month",monthValue);
            pvDeviceHistoryDataQueryWrapper.eq("day",dayOfMonth);
            if(StringUtils.isNotBlank(sn)){
                pvDeviceHistoryDataQueryWrapper.eq("sn",sn);
            }
            List<PvDeviceHistoryData> list = this.list(pvDeviceHistoryDataQueryWrapper);
            return Result.ok(list);
        }
        //月
        if(dayType.equals("1")){
            ArrayList<DeviceHistoryVo> deviceHistoryVos = new ArrayList<>();
            QueryWrapper<PvDevice> pvDeviceQueryWrapper = new QueryWrapper<>();
            PvDevice deviceSn = iPvDeviceService.getOne(pvDeviceQueryWrapper.eq("device_sn", sn));
            String nominalPower = deviceSn.getNominalPower();
            Calendar c = Calendar.getInstance();
            c.set(year,monthValue-1,1);
            int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            //这个是月份
            for (int i = 1; i < maxDay; i++) {
                ResultPvDeviceVO resultPvDeviceVO = pvDeviceHistoryDataMapper.getList(sn, year, monthValue, i);
                if(resultPvDeviceVO != null){
                    Date time1 = resultPvDeviceVO.getTime();
                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                    String format = sdf1.format(time1);
                    String todayEnergy = resultPvDeviceVO.getTodayEnergy();
                    DeviceHistoryVo deviceHistoryVo = new DeviceHistoryVo();
                    deviceHistoryVo.setDayPower(new BigDecimal(todayEnergy).setScale(2, RoundingMode.HALF_UP));
                    //获取的是第一天的数据
                    deviceHistoryVo.setSn(sn);
                    deviceHistoryVo.setTime(format);
                    deviceHistoryVo.setDayHour(new BigDecimal(todayEnergy).multiply(new BigDecimal(1000)).divide(new BigDecimal(nominalPower)).setScale(2, RoundingMode.HALF_UP));
                    deviceHistoryVos.add(deviceHistoryVo);
                }
            }
            return Result.ok(deviceHistoryVos);
        }
        if(dayType.equals("2")){
            ArrayList<DeviceHistoryVo> deviceHistoryVos = new ArrayList<>();
            QueryWrapper<PvDevice> pvDeviceQueryWrapper = new QueryWrapper<>();
            PvDevice deviceSn = iPvDeviceService.getOne(pvDeviceQueryWrapper.eq("device_sn", sn));
            String nominalPower = deviceSn.getNominalPower();
            //这个是月份
            for (int i = 1; i < 12; i++) {
                String totalEnergy = pvDeviceHistoryDataMapper.getTotalEnergy(sn, year, i);
                if(totalEnergy != null){
                    DeviceHistoryVo deviceHistoryVo = new DeviceHistoryVo();
                    deviceHistoryVo.setDayPower(new BigDecimal(totalEnergy).setScale(2, RoundingMode.HALF_UP));
                    //获取的是第一天的数据
                    deviceHistoryVo.setSn(sn);
                    deviceHistoryVo.setTime(year+"-"+i);
                    deviceHistoryVos.add(deviceHistoryVo);
                    deviceHistoryVo.setDayHour(new BigDecimal(totalEnergy).multiply(new BigDecimal(1000)).divide(new BigDecimal(nominalPower)).setScale(2, RoundingMode.HALF_UP));
                }
            }
            return Result.ok(deviceHistoryVos);
        }
       return Result.ok();
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

    public List<String> findEveryDay(String beginTime,String endTime) throws Exception {
        //创建一个放所有日期的集合
        List<String> dates = new ArrayList();

        //创建时间解析对象规定解析格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        //将传入的时间解析成Date类型,相当于格式化
        Date dBegin = sdf.parse(beginTime);
        Date dEnd = sdf.parse(endTime);

        //将格式化后的第一天添加进集合
        dates.add(sdf.format(dBegin));

        //使用本地的时区和区域获取日历
        Calendar calBegin = Calendar.getInstance();

        //传入起始时间将此日历设置为起始日历
        calBegin.setTime(dBegin);

        //判断结束日期前一天是否在起始日历的日期之后
        while (dEnd.after(calBegin.getTime())) {

            //根据日历的规则:月份中的每一天，为起始日历加一天
            calBegin.add(Calendar.DAY_OF_MONTH, 1);

            //得到的每一天就添加进集合
            dates.add(sdf.format(calBegin.getTime()));
            //如果当前的起始日历超过结束日期后,就结束循环
        }
        return dates;
    }


    public List<DeviceHistoryPowerResult.PageData.HistoryDevice> getList(String statrtime,String sn) throws Exception {
        ArrayList<DeviceHistoryPowerResult.PageData.HistoryDevice> historyDevices = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("token", "619vecr60t557370rx1d3atm474b7l9y");
            String url = "https://openapi-cn.growatt.com/v1/device/inverter/data?device_sn={0}&start_date={1}&end_date={2}&perpage={3}&page={4}";
            ResponseEntity<DeviceHistoryPowerResult> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), DeviceHistoryPowerResult.class, sn, statrtime, statrtime,100,j);
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                List<DeviceHistoryPowerResult.PageData.HistoryDevice> datas = response.getBody().getData().getDatas();
                if (datas.size() > 0) {
                    historyDevices.addAll(datas);
                } else {
                    break;
                }
            }
            else {
                return new ArrayList<>();
            }
        }
        return historyDevices;
    }
}
