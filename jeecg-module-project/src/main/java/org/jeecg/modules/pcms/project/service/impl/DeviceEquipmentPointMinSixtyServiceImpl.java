package org.jeecg.modules.pcms.project.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointMinDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMinSixty;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentPointMinSixtyMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinSixtyService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceEquipmentPointMinSixtyServiceImpl extends ServiceImpl<DeviceEquipmentPointMinSixtyMapper, DeviceEquipmentPointMinSixty> implements IDeviceEquipmentPointMinSixtyService {
    private static final int BATCH_SIZE = 50;

    private static final String FIXED_POINT_IDS = "p1,p87,p88,p2,p67,p24,p25,p14,p43,p26,p18,p19,p20,p21,p22,p23,p94,p27,p7356,p3,p15,p16,p17,p95,p90,p120,p4,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p5,p7,p9,p45,p47,p49,p51,p53,p55,p57,p6,p8,p10,p46,p48,p50,p52,p54,p56,p58,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p23001,p23014,p23008";
    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationEquipmentsMinutePoint}")
    private String stationEquipmentsMinutePoint;

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
    private IDeviceEquipmentService iDeviceEquipmentService;


    @Override
    public void insert() {
        //调用登录接口获取token，获取下一步的电站列表
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            //获取电站的投运时间
            List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();
            //从这个投运时间开始 到昨天的数据
            // 获取昨天日期
            LocalDate yesterday = LocalDate.now().minusDays(1);
            //调用登录接口获取token，获取下一步的电站列表
            //获取的数据分钟级别  单位分钟
            String minType="60";
            //拿到所有的设备的 pskey

            //获取电站的投运时间
            //设置录取数据的起止时间(起步数据是从 20250901这天开始录入)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            String startTime = yesterday.atStartOfDay().format(formatter);
            String endTime = yesterday.atTime(23, 59, 59).format(formatter);
            List<DateIntervalVo> timeIntervals = DateUtils.generateTimeSegments(startTime,endTime,0,1);
            // 遍历设备列表
            this.fetchSolarDeviceEquipmentMinPointList(timeIntervals,psKeyList,requestLoginToken,minType);

//
            //查古瑞瓦特数据
            //1、获取所有电站id集合
            //2、传参到古瑞瓦特接口获取所有设备数据
//        List<Integer> listId = iDeviceService.getListId();
            //获取所有设备列表集合
//        this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1");
//        iDeviceEquipmentService.saveDeviceEquipmentList(pageItems);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     *
     * @param token
     * @return
     * @throws IOException
     */
    public SolarDeviceEquipmentDto fetchSolarDeviceEquipmentMinPointList(List<DateIntervalVo> startTimeStamps, List<String>psKeyList, String token, String minType) throws IOException {
        // 0. 初始化结果对象（根据实际DTO结构调整）
        SolarDeviceEquipmentDto resultDto = new SolarDeviceEquipmentDto();

        // 1. 创建可重用的OkHttpClient（移出循环）
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
                .retryOnConnectionFailure(true) // 自动重试连接失败
                .build();

        // 2. 创建可重用的Gson实例
        Gson gson = new Gson();


        //4. 创建6个时间段，每段4小时
        if(startTimeStamps != null && startTimeStamps.size() > 0){
            // 4. 双重循环：设备批次 + 时间片段
            for (int j = 0; j < psKeyList.size(); j += BATCH_SIZE) {
                List<String> batchPsKeys = psKeyList.subList(j, Math.min(j + BATCH_SIZE, psKeyList.size()));
                for (DateIntervalVo startTimeStamp : startTimeStamps) {
                    // 5. 构建请求参数
                    Map<String, Object> params = new HashMap<>();
                    params.put("appkey", appKey);
                    params.put("token", token);
                    params.put("ps_key_list", batchPsKeys);
                    params.put("points", FIXED_POINT_IDS);
                    params.put("start_time_stamp", startTimeStamp.getStartDate());  // 设置时间片段起始
                    params.put("end_time_stamp", startTimeStamp.getEndDate());      // 设置时间片段结束
                    params.put("minute_interval", minType);

                    // 6. 构建请求
                    String jsonBody = gson.toJson(params);
                    RequestBody requestBody = RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(stationEquipmentsMinutePoint)
                            .addHeader("x-access-key", appSecret)
                            .post(requestBody)
                            .build();

                    // 7. 执行请求并处理响应
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("请求失败: " + response.code() + " " + response.message());
                        }

                        ResponseBody body = response.body();
                        if (body != null) {
                            // 8. 解析并合并结果（需要根据实际响应结构实现）
                            SolarDeviceEquipmentPointMinDto batchResult = gson.fromJson(body.string(), SolarDeviceEquipmentPointMinDto.class);
                            Map<String, List<SolarDeviceEquipmentPointMinDto.DataPoint>> resultData = batchResult.getResultData();
                            if(resultData != null){
                                for (Map.Entry<String, List<SolarDeviceEquipmentPointMinDto.DataPoint>> entry : resultData.entrySet()) {
                                    //这是pskey
                                    String key = entry.getKey();
                                    //这是数据
                                    List<SolarDeviceEquipmentPointMinDto.DataPoint> value = entry.getValue();
                                    if(value != null && value.size()>0){
                                        for (int k = 0; k < value.size(); k++) {
                                            SolarDeviceEquipmentPointMinDto.DataPoint dataPoint = value.get(k);
                                            String p24 = dataPoint.getP24();
                                            if(minType.equals("60")){
                                                if (StringUtils.isNotBlank(p24)) {
                                                    DeviceEquipmentPointMinSixty deviceEquipmentPointMinSixty = new DeviceEquipmentPointMinSixty();
                                                    BeanUtils.copyProperties(dataPoint,deviceEquipmentPointMinSixty);
                                                    deviceEquipmentPointMinSixty.setSource("2");
                                                    //设置分钟级别类型 1:5分钟  2:15分钟 3:30分钟 4:1小时
                                                    deviceEquipmentPointMinSixty.setPsKey(key);
                                                    this.save(deviceEquipmentPointMinSixty);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return resultDto;
    }

    //时间片段辅助类
    @Data
    private static class TimeSegment {
        String start;
        String end;

        TimeSegment(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }


}
