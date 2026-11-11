package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.SolarDevicePointMinDto;
import org.jeecg.modules.pcms.energy.dto.growatt.PowerDataResponseDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.entity.DevicePointMinFive;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointMinFiveMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailSolarService;
import org.jeecg.modules.pcms.project.service.IDevicePointMinFiveService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class DevicePointMinFiveServiceImpl  extends ServiceImpl<DevicePointMinFiveMapper, DevicePointMinFive> implements IDevicePointMinFiveService {

    private static final int BATCH_SIZE = 50;
    private static final String FIXED_POINT_IDS = "p83019,p83022,p83024,p83025,p83033";
    @Value("${solar.stationEquipmentsMinutePoint}")
    private String stationEquipmentsMinutePoint;
    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Value("${growatt.token}")
    private String growattToken;

    @Value("${growatt.dayPower}")
    private String dayPower;


    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;

    @Autowired
    private IDeviceService iDeviceService;

    @Override
    public void insert() {

        //调用登录接口获取token，获取下一步的电站列表
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            //获取电站的psKey集合
//            List<String> plantPsKey = iDeviceDetailSolarService.getPlantPsKey();

            //数据开始时间 和 结束时间
//            String startTime="20250901000000";
//            String endTime="20251009235959";
            //开始时间和结束时间

//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//            String startTime = yesterday.atStartOfDay().format(formatter);
//            String endTime = yesterday.atTime(23, 59, 59).format(formatter);

//            //获取设备
//            Integer minuteInterval=5;
//            //获取最终数据
//            List<DateIntervalVo> timeIntervals = DateUtils.generateTimeSegments(startTime,endTime,0,1);
//            //psKey集合
//            this.fetchSolarDeviceEquipmentMinPointList(timeIntervals,plantPsKey,requestLoginToken,minuteInterval);
//
//            //1、获取所有电站id集合
//            //2、传参到古瑞瓦特接口获取所有设备数据
            Calendar cal   =   Calendar.getInstance();
            cal.add(Calendar.DATE,   -1);  //-1代表今天的日期减一即为昨天
            List<Integer> listId = iDeviceService.getListId();
            String yesterday = new SimpleDateFormat( "yyyy-MM-dd").format(cal.getTime());
            this.fetchGrowattDeviceEquipmentList(listId, growattToken,yesterday);
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
                                            this.save(devicePointMinFive);
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



    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken,String date) {
        if(listId != null && listId.size() > 0){
            for (int i = 0; i < listId.size(); i++) {
                // 1. 创建OkHttpClient实例，并配置超时参数
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
                        .retryOnConnectionFailure(true) // 自动重试连接失败
                        .build();
                // 2. 构建请求参数Map
                // 1. 创建参数映射
                Map<String, String> params = new HashMap<>();
                int random = (int) ((Math.random() * 10) + 1);
                params.put("search_type", String.valueOf(random));
                params.put("plant_id",String.valueOf(listId.get(i)));
                params.put("date",date);

                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(dayPower);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + dayPower);
                }

                HttpUrl.Builder urlBuilder = baseUrl.newBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                HttpUrl urlWithParams = urlBuilder.build();

                // 3. 构建GET请求
                Request request = new Request.Builder()
                        .url(urlWithParams)
                        .addHeader("token", growattToken)
                        .get()  // GET方法（OkHttp默认就是GET，可省略）
                        .build();


                // 6. 执行请求并处理响应
                try (Response response = client.newCall(request).execute()) {

                    if (!response.isSuccessful()) {
                        // 优化点5：处理非200状态码的情况
                        throw new IOException("请求失败: " + response.code() + " " + response.message());
                    }
                    //反解析成java对象并返回token

                    // 2. 创建可重用的Gson实例
                    Gson gson = new Gson();
                    ResponseBody body = response.body();
                    PowerDataResponseDto powerDataResponseDto = gson.fromJson(body.string(), PowerDataResponseDto.class);
                    PowerDataResponseDto.DataBeanReturn data = powerDataResponseDto.getData();
                    List<PowerDataResponseDto.DataBeanReturn.PowersBean> powers = data.getPowers();
                    if(powers != null && powers.size() > 0){
                        for (int k = 0; k < powers.size(); k++) {
                            //功率  时间
                            String power = powers.get(k).getPower();
                            if(StringUtils.isNotBlank(power)){
                                String time = powers.get(k).getTime();
                                DevicePointMinFive devicePointMinFive = new DevicePointMinFive();
                                //转换一下时间格式
                                String formatted = LocalDateTime.parse(time,
                                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                                devicePointMinFive.setTimeStamp(formatted);
                                devicePointMinFive.setPsKey(listId.get(i).toString());
                                devicePointMinFive.setP83033(power);
                                devicePointMinFive.setSource(2);
                                this.save(devicePointMinFive);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


}
