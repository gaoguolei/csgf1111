package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvDayPower;
import org.jeecg.modules.pcms.project.mapper.PvDayPowerMapper;
import org.jeecg.modules.pcms.project.service.IPvDayPowerService;
import org.jeecg.modules.pcms.project.service.IPvStationsService;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationvo.HistoryPowerResultVo;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午7:24
 * @Version 1.5
 */

@Service
public class PvDayPowerServiceImpl extends ServiceImpl<PvDayPowerMapper, PvDayPower> implements IPvDayPowerService {
    @Autowired
    private IPvStationsService iPvStationsService;

    @Autowired
    private StationConfig stationConfig;
    /**
     * 每天晚上更新今天的数据
     */
    @Override
    public void updatePower() {
        List<PvStations> list = iPvStationsService.list();
        for (int i = 0; i < list.size(); i++) {
            try {
                Integer plantId = list.get(i).getPlantId();
                Date endDate = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                String s = format.format(endDate);
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add("token", stationConfig.getToken());
                String url =stationConfig.getDayPower()+"?plant_id={0}&date={1}";
                ResponseEntity<HistoryPowerResultVo> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), HistoryPowerResultVo.class, plantId, s);
                if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                    List<HistoryPowerResultVo.PageData.Energys> powers = response.getBody().getData().getPowers();
                    //拿到数据之后插入到数据库
                    if (powers != null && powers.size() > 0) {
                        for (int k = 0; k < powers.size(); k++) {
                            BigDecimal energy = powers.get(k).getPower();
                            String time = powers.get(k).getTime();
                            PvDayPower pvEnergy = new PvDayPower();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                            try {
                                Date date = sdf.parse(time);
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);
                                int year = calendar.get(Calendar.YEAR);
                                int month = calendar.get(Calendar.MONTH) + 1;
                                int day = calendar.get(Calendar.DAY_OF_MONTH);
                                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                                int minute = calendar.get(Calendar.MINUTE);
                                pvEnergy.setYear(year);
                                pvEnergy.setMonth(month);
                                pvEnergy.setDay(day);
                                pvEnergy.setHour(hour);
                                pvEnergy.setMin(minute);
                                pvEnergy.setPlantId(plantId);
                                pvEnergy.setPower(energy);
                                this.save(pvEnergy);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void insert() {
        List<PvStations> list = iPvStationsService.list();
        for (int i = 0; i < list.size(); i++) {
            Integer plantId = list.get(i).getPlantId();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date endDate = new Date(new Date().getTime());
            try {
                List<String> everyDay = this.findEveryDay("2023-01-01",format.format(endDate));
                for (int j = 0; j < everyDay.size(); j++) {
                    String s = everyDay.get(j);
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("token", stationConfig.getToken());
                    String url = stationConfig.getDayPower()+"?plant_id={0}&date={1}";
                    try {
                        ResponseEntity<HistoryPowerResultVo> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), HistoryPowerResultVo.class, plantId, s);
                        if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                            List<HistoryPowerResultVo.PageData.Energys> powers = response.getBody().getData().getPowers();
                            //拿到数据之后插入到数据库
                            if(powers != null && powers.size()>0){
                                for (int k = 0; k <powers.size(); k++) {
                                    BigDecimal energy = powers.get(k).getPower();
                                    String time = powers.get(k).getTime();
                                    PvDayPower pvEnergy = new PvDayPower();
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                    try {
                                        if(energy != null){
                                            Date date = sdf.parse(time);
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(date);
                                            int year = calendar.get(Calendar.YEAR);
                                            int month = calendar.get(Calendar.MONTH) + 1;
                                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                                            int minute = calendar.get(Calendar.MINUTE);
                                            pvEnergy.setYear(year);
                                            pvEnergy.setMonth(month);
                                            pvEnergy.setDay(day);
                                            pvEnergy.setHour(hour);
                                            pvEnergy.setMin(minute);
                                            pvEnergy.setPlantId(plantId);
                                            pvEnergy.setPower(energy);
                                            this.save(pvEnergy);
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static   List<String> findEveryDay(String beginTime, String endTime)
            throws Exception {
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
}
