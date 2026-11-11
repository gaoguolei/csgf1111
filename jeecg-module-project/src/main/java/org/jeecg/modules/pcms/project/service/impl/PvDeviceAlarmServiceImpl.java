package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvDeviceAlarm;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.mapper.PvDeviceAlarmMapper;
import org.jeecg.modules.pcms.project.service.IPvDeviceAlarmService;
import org.jeecg.modules.pcms.project.service.IPvDeviceService;
import org.jeecg.modules.pcms.project.service.IPvStationsService;
import org.jeecg.modules.pcms.project.stationentity.PvDevice;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationvo.HistoryAlarmResultVo;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/2 下午2:27
 * @Version 1.5
 */
@Service
public class PvDeviceAlarmServiceImpl extends ServiceImpl<PvDeviceAlarmMapper, PvDeviceAlarm> implements IPvDeviceAlarmService {
    @Autowired
    private IPvDeviceService iPvDeviceService;
    @Autowired
    private PvDeviceAlarmMapper pvDeviceAlarmMapper;

    @Autowired
    private IPvStationsService iPvStationsService;

    @Autowired
    private StationConfig stationConfig;

    @Override
    @Transactional
    public void insert() {
        List<PvDevice> list = iPvDeviceService.list();
        for (int i = 0; i < list.size(); i++) {
            try {
            String deviceSn = list.get(i).getDeviceSn();
            String plantId = list.get(i).getPlantId();
            QueryWrapper<PvStations> pvStationsQueryWrapper = new QueryWrapper<>();
            pvStationsQueryWrapper.eq("plant_id", plantId);
            PvStations one = iPvStationsService.getOne(pvStationsQueryWrapper);
            String name = one.getName();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date endDate = new Date();
            String format1 = format.format(endDate);
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add("token", stationConfig.getToken());
                String url = stationConfig.getInverterAlarm()+"?device_sn={0}&date={1}&page={2}&perpage={3}";
                ResponseEntity<HistoryAlarmResultVo> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), HistoryAlarmResultVo.class, deviceSn, format1,1,100);
                if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                    HistoryAlarmResultVo.PageData data = response.getBody().getData();
                    String count = data.getCount();
                    if("1".equals(count)){
                        List<HistoryAlarmResultVo.PageData.Ararm> alarms = data.getAlarms();
                        //拿到数据之后插入到数据库
                        if (alarms != null && alarms.size() > 0) {
                            for (int k = 0; k < alarms.size(); k++) {
                                PvDeviceAlarm pvDeviceAlarm = new PvDeviceAlarm();
                                pvDeviceAlarm.setStatus(alarms.get(k).getStatus());
                                pvDeviceAlarm.setEndTime(alarms.get(k).getEndTime());
                                pvDeviceAlarm.setStartTime(alarms.get(k).getStartTime());
                                pvDeviceAlarm.setSn(deviceSn);
                                pvDeviceAlarm.setAlarmCode(alarms.get(k).getAlarmCode());
                                pvDeviceAlarm.setAlarmMessage(alarms.get(k).getAlarmMessage());
                                pvDeviceAlarm.setPlantName(name);
                                pvDeviceAlarm.setPlantId(Integer.valueOf(plantId));
//                                    long l = Duration.between(alarms.get(k).getStartTime(), alarms.get(k).getEndtime()).toHours();
//                                    pvDeviceAlarm.setAlarmHour(new BigDecimal(l));
                                this.save(pvDeviceAlarm);
                            }
                        }
//                        List<HistoryAlarmResultVo.PageData.Ararms> alarms = response.getBody().getData().getAlarms();
                        }
                    }
            } catch (Exception e) {
               e.printStackTrace();
            }
        }
    }

    @Override
    public void delete(Integer id) {
        pvDeviceAlarmMapper.deleteAlarmById(id);
    }

    @Override
    public void update(Integer id) {
        pvDeviceAlarmMapper.updateAlarm(id);
    }

    @Override
    public IPage<PvDeviceAlarm> stationAlarm(PageVo pageVo) {
        String plantName = pageVo.getPlantName();
        String status = pageVo.getStatus();
        Integer minHour = pageVo.getMinHour();
        Integer maxHour = pageVo.getMaxHour();
        Date startTime = pageVo.getStartTime();
        Date endTime = pageVo.getEndTime();
        String sn = pageVo.getSn();
        String alarmMessage = pageVo.getAlarmMessage();
        QueryWrapper<PvDeviceAlarm> pvDeviceAlarmQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(plantName)){
            String regex = "\\*";
            String result = plantName.replaceAll(regex, "");
            pvDeviceAlarmQueryWrapper.like("plant_name",result);
        }
        if(StringUtils.isNotBlank(status)){
            pvDeviceAlarmQueryWrapper.eq("status",status);
        }
        if(minHour != null){
            pvDeviceAlarmQueryWrapper.ge("alarm_hour",minHour);
        }
        if(maxHour != null){
            pvDeviceAlarmQueryWrapper.le("alarm_hour",maxHour);
        }
        if(startTime != null){
            pvDeviceAlarmQueryWrapper.ge("start_time",startTime);
        }
        if(endTime != null){
            pvDeviceAlarmQueryWrapper.le("start_time",endTime);
        }
        if(StringUtils.isNotBlank(sn)){
            String regex = "\\*";
            String result = sn.replaceAll(regex, "");
            pvDeviceAlarmQueryWrapper.like("sn",result);
        }
        if(StringUtils.isNotBlank(sn)){
            String regex = "\\*";
            String result = alarmMessage.replaceAll(regex, "");
            pvDeviceAlarmQueryWrapper.eq("alarm_message",result);
        }
        IPage<PvDeviceAlarm> page = new Page<>(pageVo.getPageNo(), pageVo.getPageSize());
        IPage<PvDeviceAlarm> pvDeviceAlarmPage = pvDeviceAlarmMapper.selectPage(page, pvDeviceAlarmQueryWrapper);
        List<PvDeviceAlarm> records = pvDeviceAlarmPage.getRecords();
        for (int i = 0; i < records.size(); i++) {
            String alarmCode = records.get(i).getAlarmCode();
            String sn1 = records.get(i).getSn();
            if(StringUtils.isNotBlank(alarmCode) && alarmCode.equals("302")){
                records.get(i).setMethod("1.确认电网是否正常连接。 2. 确认连接的电网是否正常。 3. 如果市电连接都正常，则需要联系我们的维修人员。");
            }
            if(StringUtils.isNotBlank(alarmCode) && alarmCode.equals("411")){
                records.get(i).setMethod("1. 如果偶然出现，可能是电网短时间异常，逆变器在检测到电网正常后会恢复正常工作，不需要人工干预； 2. 如果频繁出现，请检查电网电压，电网频率是否在逆变器允许范围内，如果否，请联系客服处理；如果是，请检查交流侧断路器与输出线缆是否连接正常； 3. 如果电网电压，电网频率在逆变器允许范围内，且交流侧接线确认无误，此报警仍频繁出现，在征得当地电力运营商同意后，请联系客服修改逆变器电网过欠压.过欠频保护点。");
            }
            //封装报警时长
            //获取最早的一条数据
            PvDeviceAlarm before = pvDeviceAlarmMapper.getMaxTime(sn1);
            String startTime1 = before.getStartTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            try {
                String s = this.dateDiff(startTime1, simpleDateFormat.format(date), "yyyy-MM-dd HH:mm:ss");
                records.get(i).setHour(s);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        pvDeviceAlarmPage.setRecords(records);

        return pvDeviceAlarmPage;
    }

    public  String dateDiff(String startTime, String endTime, String format) throws Exception {
        SimpleDateFormat sd = new SimpleDateFormat(format);
        long nd = 1000*24*60*60;//一天的毫秒数
        long nh = 1000*60*60;//一小时的毫秒数
        long diff;
        //计算两个时间的毫秒时间差异
        diff = sd.parse(endTime).getTime() - sd.parse(startTime).getTime();
        long day = diff/nd;
        long hour = diff%nd/nh;
        return day+"天"+hour+"小时";
    }

    public static List<String> findEveryDay(String beginTime, String endTime)
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
