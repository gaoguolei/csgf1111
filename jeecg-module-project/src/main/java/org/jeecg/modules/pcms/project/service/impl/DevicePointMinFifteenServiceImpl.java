package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.SolarDevicePointMinDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.entity.DevicePointMinFifteen;
import org.jeecg.modules.pcms.energy.entity.DevicePointMinFive;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointMinFifteenMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailSolarService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDevicePointMinFifteenService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
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
public class DevicePointMinFifteenServiceImpl extends ServiceImpl<DevicePointMinFifteenMapper, DevicePointMinFifteen> implements IDevicePointMinFifteenService {


    private static final int BATCH_SIZE = 50;
    private static final String FIXED_POINT_IDS = "p83019,p83022,p83024,p83025,p83033";
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

    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;




    @Override
    public void insert() {

        //调用登录接口获取token，获取下一步的电站列表
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            //获取电站的psKey集合
            List<String> plantPsKey = iDeviceDetailSolarService.getPlantPsKey();

            //数据开始时间 和 结束时间
//            String startTime="20250901000000";
//            String endTime="20251009235959";
            //开始时间和结束时间
            LocalDate yesterday = LocalDate.now().minusDays(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String startTime = yesterday.atStartOfDay().format(formatter);
            String endTime = yesterday.atTime(23, 59, 59).format(formatter);
            //获取设备
            Integer minuteInterval=15;
            //获取最终数据
            List<DateIntervalVo> timeIntervals = DateUtils.generateTimeSegments(startTime,endTime,0,1);
            //psKey集合
            this.fetchSolarDeviceEquipmentMinPointList(timeIntervals,plantPsKey,requestLoginToken,minuteInterval);

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
     * @param token
     * @return
     * @throws IOException
     */
    public SolarDeviceEquipmentDto fetchSolarDeviceEquipmentMinPointList(List<DateIntervalVo> startTimeStamps, List<String>psKeyList, String token, Integer minuteInterval) throws IOException {
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
                    params.put("minute_interval", minuteInterval);

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
                            SolarDevicePointMinDto batchResult = gson.fromJson(body.string(), SolarDevicePointMinDto.class);
                            Map<String, List<SolarDevicePointMinDto.DataPoint>> resultData = batchResult.getResultData();
                            if(resultData != null){
                                for (Map.Entry<String, List<SolarDevicePointMinDto.DataPoint>> entry : resultData.entrySet()) {
                                    //这是pskey
                                    String key = entry.getKey();
                                    List<SolarDevicePointMinDto.DataPoint> value = entry.getValue();
                                    //这是数据
                                    if(value != null && value.size()>0){
                                        for (int k = 0; k < value.size(); k++) {
                                            DevicePointMinFive devicePointMinFive = new DevicePointMinFive();
                                            SolarDevicePointMinDto.DataPoint dataPoint = value.get(k);
                                            String p83024 = dataPoint.getP83024();
                                            if (StringUtils.isNotBlank(p83024)) {
                                                devicePointMinFive.setP83024(p83024);
                                            }
                                            String p83019 = dataPoint.getP83019();
                                            if (StringUtils.isNotBlank(p83019)) {
                                                devicePointMinFive.setP83019(p83019);
                                            }
                                            String p83022 = dataPoint.getP83022();
                                            if (StringUtils.isNotBlank(p83022)) {
                                                devicePointMinFive.setP83022(p83022);
                                            }
                                            String p83025 = dataPoint.getP83025();
                                            if (StringUtils.isNotBlank(p83025)) {
                                                devicePointMinFive.setP83025(p83025);
                                            }
                                            String p83033= dataPoint.getP83033();
                                            if (StringUtils.isNotBlank(p83033)) {
                                                devicePointMinFive.setP83033(p83033);
                                            }
                                            String timeStamp = dataPoint.getTimeStamp();
                                            devicePointMinFive.setTimeStamp(timeStamp);
                                            devicePointMinFive.setSource(1);
                                            devicePointMinFive.setPsKey(key);

                                            DevicePointMinFifteen devicePointMinFifteen = new DevicePointMinFifteen();
                                            BeanUtils.copyProperties(devicePointMinFive,devicePointMinFifteen);
                                            this.save(devicePointMinFifteen);
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

    //     时间片段辅助类
    @Data
    private static class TimeSegment {
        String start;
        String end;

        TimeSegment(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    // 结果合并方法（需要根据实际DTO结构实现）
    private void mergeResults(SolarDeviceEquipmentDto mainResult, SolarDeviceEquipmentDto batchResult) {
        // 实现逻辑根据实际数据结构调整，例如：
        // mainResult.getDataList().addAll(batchResult.getDataList());
    }

}

