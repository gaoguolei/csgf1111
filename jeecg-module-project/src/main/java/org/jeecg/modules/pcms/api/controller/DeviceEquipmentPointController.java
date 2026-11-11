package org.jeecg.modules.pcms.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import okhttp3.*;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPoint;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.ResultLoginApiVo;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


@Api(tags = "文件操作接口")
@RestController
@RequestMapping(value = "project/deviceEquipmentPoint")
public class DeviceEquipmentPointController {


    private static final int BATCH_SIZE = 50;

    private static final List<String> FIXED_POINT_IDS = Arrays.asList(
            "1", "87", "88", "2", "67", "24", "25", "14", "43", "26",
            "18", "19", "20", "21", "22", "23", "94", "27", "7356", "3",
            "15", "16", "17", "95", "90", "120", "4", "33", "34", "35",
            "36", "37", "38", "39", "40", "41", "42", "5", "7", "9",
            "45", "47", "49", "51", "53", "55", "57", "6", "8",
            "10", "46", "48", "50", "52", "54", "56", "58", "96", "97", "98",
            "99", "100", "101", "102", "103", "104", "105", "70", "71", "72", "73", "74", "75", "76", "77",
            "78", "79"
    );

    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationFaultEquipmentsPoint}")
    private String stationFaultEquipmentsPoint;

    @Value("${growatt.deviceList}")
    private String deviceGrwListUrl;

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
    private IDeviceService iDeviceService;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;


    @Autowired
    private IDeviceEquipmentPointService iDeviceEquipmentPointService;


    @ApiOperation("根据设备sn码获取实时测点数据")
    @RequestMapping(value = "/deviceEquipmentPointInfo", method = RequestMethod.GET)
    public Result deviceEquipmentPointInfo(DevicePageVo devicePageVo) throws Exception {
        //调用登录接口获取token，获取下一步的电站列表
        String deviceEquipmentSn = devicePageVo.getDeviceEquipmentSn();
        QueryWrapper<DeviceEquipmentPoint> deviceEquipmentPointQueryWrapper = new QueryWrapper<>();
        deviceEquipmentPointQueryWrapper.eq("device_sn",deviceEquipmentSn);
        DeviceEquipmentPoint one = iDeviceEquipmentPointService.getOne(deviceEquipmentPointQueryWrapper);
        //如果是阳光云就重新封装一下数据保留2位小数
        if(one != null){
           if(one.getSource() != null && one.getSource().equals("2")){
               String p1 = one.getP1();
               if(StringUtils.isNotBlank(p1) && !p1.equals("0")){
                   String s = new BigDecimal(p1).multiply(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP1(s);
               }
               String p2 = one.getP2();
               if(StringUtils.isNotBlank(p2) && !p2.equals("0")){
                   String s = new BigDecimal(p2).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP2(s);
               }
               String p24 = one.getP24();
               if(StringUtils.isNotBlank(p24) && !p2.equals("0")){
                   String s = new BigDecimal(p24).multiply(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP24(s);
               }
               String p25 = one.getP25();
               if(StringUtils.isNotBlank(p25) && !p2.equals("0")){
                   String s = new BigDecimal(p25).multiply(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP25(s);
               }
               String p96 = one.getP96();
               if(StringUtils.isNotBlank(p96) && !p2.equals("0")){
                   String s = new BigDecimal(p96).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP96(s);
               }
               String p97 = one.getP97();
               if(StringUtils.isNotBlank(p97) && !p2.equals("0")){
                   String s = new BigDecimal(p97).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP97(s);
               }
               String p98 = one.getP98();
               if(StringUtils.isNotBlank(p98) && !p2.equals("0")){
                   String s = new BigDecimal(p98).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP98(s);
               }
               String p99 = one.getP99();
               if(StringUtils.isNotBlank(p99) && !p2.equals("0")){
                   String s = new BigDecimal(p99).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP99(s);
               }
               String p100 = one.getP100();
               if(StringUtils.isNotBlank(p100) && !p2.equals("0")){
                   String s = new BigDecimal(p100).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP100(s);
               }
               String p101 = one.getP101();
               if(StringUtils.isNotBlank(p101) && !p2.equals("0")){
                   String s = new BigDecimal(p101).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP101(s);
               }

               String p102 = one.getP102();
               if(StringUtils.isNotBlank(p102) && !p2.equals("0")){
                   String s = new BigDecimal(p102).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP102(s);
               }
               String p103 = one.getP103();
               if(StringUtils.isNotBlank(p103) && !p2.equals("0")){
                   String s = new BigDecimal(p103).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP103(s);
               }

               String p71 = one.getP71();
               if(StringUtils.isNotBlank(p71) && !p2.equals("0")){
                   String s = new BigDecimal(p71).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP71(s);
               }
               String p70 = one.getP70();
               if(StringUtils.isNotBlank(p70) && !p2.equals("0")){
                   String s = new BigDecimal(p70).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP70(s);
               }
               String p72 = one.getP72();
               if(StringUtils.isNotBlank(p72) && !p72.equals("0")){
                   String s = new BigDecimal(p72).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP72(s);
               }
               String p73 = one.getP73();
               if(StringUtils.isNotBlank(p73) && !p73.equals("0")){
                   String s = new BigDecimal(p73).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP73(s);
               }
               String p74 = one.getP74();
               if(StringUtils.isNotBlank(p74) && !p74.equals("0")){
                   String s = new BigDecimal(p74).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP74(s);
               }
               String p75 = one.getP75();
               if(StringUtils.isNotBlank(p75) && !p75.equals("0")){
                   String s = new BigDecimal(p75).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP75(s);
               }
               String p76 = one.getP76();
               if(StringUtils.isNotBlank(p76) && !p76.equals("0")){
                   String s = new BigDecimal(p76).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP76(s);
               }
               String p77 = one.getP77();
               if(StringUtils.isNotBlank(p77) && !p77.equals("0")){
                   String s = new BigDecimal(p77).setScale(2, RoundingMode.HALF_UP).toString();
                   one.setP77(s);
               }

           }
        }
        return Result.ok(one);
    }






    @ApiOperation("存入电站下设备数据数据")
    @RequestMapping(value = "/deviceEquipmentPointList", method = RequestMethod.POST)
    public Result deviceEquipmentPointList() throws Exception {
        //调用登录接口获取token，获取下一步的电站列表
        String requestLoginToken = this.sendLoginSolarRequest(loginUrl, username, password, appKey, appSecret);
        //阳光云获取设备sn集合数据
        List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();
        this.fetchSolarDeviceEquipmentPointList(psKeyList,requestLoginToken);
        //查古瑞瓦特数据
        //1、获取所有电站id集合
        //2、传参到古瑞瓦特接口获取所有设备数据
//        List<Integer> listId = iDeviceService.getListId();
        //获取所有设备列表集合
//        this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1");
//        iDeviceEquipmentService.saveDeviceEquipmentList(pageItems);
        return Result.ok();
    }


    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     * @param token
     * @return
     * @throws IOException
     */
    public void fetchSolarDeviceEquipmentPointList(List<String> psKeyList, String token) throws IOException {
        for (int i = 0; i <psKeyList.size() ; i += BATCH_SIZE) {
            List<String> batchPsKeys = psKeyList.subList(i, Math.min(i + BATCH_SIZE, psKeyList.size()));
            // 1. 创建OkHttpClient实例，并配置超时参数
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                    .build();

            // 2. 构建请求参数Map
            Map<String, Object> params = new HashMap<>();
            params.put("appkey", appKey);
            params.put("token", token);
            //逆变器类型的
            params.put("device_type", "1");
            //设备key
            params.put("ps_key_list", batchPsKeys);
            //测点id
            params.put("point_id_list", FIXED_POINT_IDS);

            // 3. 转换为JSON字符串（使用Gson库，需引入依赖）
            Gson gson = new Gson();
            String jsonBody = gson.toJson(params);
            // 4. 创建JSON类型的请求体
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody  // 明确指定JSON类型和编码
            );

            // 5. 构建请求
            Request request = new Request.Builder()
                    .url(stationFaultEquipmentsPoint)
                    .addHeader("x-access-key", appSecret)  // 添加请求头
                    .post(requestBody)                     // 设置JSON请求体
                    .build();

            // 6. 执行请求并处理响应
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    // 优化点5：处理非200状态码的情况
                    throw new IOException("请求失败: " + response.code() + " " + response.message());
                }
                //反解析成java对象
                ResponseBody body = response.body();
                SolarDeviceEquipmentPointDto solarDeviceEquipmentPointDto = JSONObject.parseObject(body.string(), SolarDeviceEquipmentPointDto.class);
                if(solarDeviceEquipmentPointDto !=null){
                    SolarDeviceEquipmentPointDto.ResultData resultData = solarDeviceEquipmentPointDto.getResultData();
                    if(resultData != null){
                        List<SolarDeviceEquipmentPointDto.DevicePointList> devicePointList = resultData.getDevicePointList();
                        if(devicePointList != null && devicePointList.size() > 0){
                            for (int j = 0; j < devicePointList.size(); j++) {
                                DeviceEquipmentPoint deviceEquipmentPoint = new DeviceEquipmentPoint();
                                BeanUtils.copyProperties(devicePointList.get(j).getDevicePoint(), deviceEquipmentPoint);
                                deviceEquipmentPoint.setSource("1");
                                iDeviceEquipmentPointService.save(deviceEquipmentPoint);
                            }
                        }
                    }
                }
            }
        }
    }




    /**
     * 古瑞瓦特获取电站列表数据
     *
     * @return
     */
//    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum) {
//        if(listId != null && listId.size() > 0){
//            for (int i = 0; i < listId.size(); i++) {
//                // 1. 创建OkHttpClient实例，并配置超时参数
//                OkHttpClient client = new OkHttpClient.Builder()
//                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
//                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
//                        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
//                        .build();
//
//                // 2. 构建请求参数Map
//                // 1. 创建参数映射
//                Map<String, String> params = new HashMap<>();
//                int random = (int) ((Math.random() * 10) + 1);
//                params.put("search_type", String.valueOf(random));
//                params.put("page", String.valueOf(pageNum));  // 转换为字符串类型
//                params.put("perpage", String.valueOf(pageSize));
//                params.put("plant_id",String.valueOf(listId.get(i)));
//
//                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
//                HttpUrl baseUrl = HttpUrl.parse(deviceGrwListUrl);
//                if (baseUrl == null) {
//                    throw new IllegalArgumentException("Invalid base URL: " + deviceGrwListUrl);
//                }
//
//                HttpUrl.Builder urlBuilder = baseUrl.newBuilder();
//                for (Map.Entry<String, String> entry : params.entrySet()) {
//                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
//                }
//                HttpUrl urlWithParams = urlBuilder.build();
//
//                // 3. 构建GET请求
//                Request request = new Request.Builder()
//                        .url(urlWithParams)
//                        .addHeader("token", growattToken)
//                        .get()  // GET方法（OkHttp默认就是GET，可省略）
//                        .build();
//
//
//                // 6. 执行请求并处理响应
//                try (Response response = client.newCall(request).execute()) {
//
//                    if (!response.isSuccessful()) {
//                        // 优化点5：处理非200状态码的情况
//                        throw new IOException("请求失败: " + response.code() + " " + response.message());
//                    }
//                    //反解析成java对象并返回token
//                    ResponseBody body = response.body();
//                    GrowattDeviceEquipmentDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceEquipmentDto.class);
//                    System.err.println(growattDeviceDto);
//                    if(growattDeviceDto != null){
//                        if(growattDeviceDto.getData() != null){
//                            if(growattDeviceDto.getData().getDevices() != null ){
//                                List<GrowattDeviceEquipmentDto.DeviceEquipmentDto> deviceEquipmentDtos = growattDeviceDto.getData().getDevices();
//                                if(deviceEquipmentDtos != null){
//                                    for (int j = 0; j < deviceEquipmentDtos.size(); j++) {
//                                        GrowattDeviceEquipmentDto.DeviceEquipmentDto deviceEquipmentDto = deviceEquipmentDtos.get(j);
//                                        System.err.println(deviceEquipmentDto);
//                                        if(deviceEquipmentDto.getType()==1){
//                                            DeviceEquipment deviceEquipment = new DeviceEquipment();
//                                            deviceEquipment.setDeviceSn(deviceEquipmentDto.getDeviceSn());
//                                            deviceEquipment.setFactoryName(deviceEquipmentDto.getManufacturer());
//                                            deviceEquipment.setDeviceModelCode(deviceEquipmentDto.getModel());
//                                            deviceEquipment.setDeviceType(deviceEquipmentDto.getType());
//                                            deviceEquipment.setPsId(Long.valueOf(listId.get(i)));
//                                            deviceEquipment.setDevStatus(deviceEquipmentDto.getLost());
//                                            deviceEquipment.setGrwStatus(String.valueOf(deviceEquipmentDto.getStatus()));
//                                            deviceEquipment.setSource("2");
//                                            System.out.println(deviceEquipment);
//                                            iDeviceEquipmentService.save(deviceEquipment);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }


    /**
     * 阳光云获取登录token
     *
     * @param loginUrl
     * @param username
     * @param password
     * @param appKey
     * @param appSecret
     * @return
     * @throws IOException
     */
    public String sendLoginSolarRequest(String loginUrl, String username, String password,
                                        String appKey, String appSecret) throws IOException {
        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        Map<String, String> params = new HashMap<>();
        params.put("user_account", username);
        params.put("user_password", password);
        params.put("appkey", appKey);

        // 3. 转换为JSON字符串（使用Gson库，需引入依赖）
        Gson gson = new Gson();
        String jsonBody = gson.toJson(params);

        // 4. 创建JSON类型的请求体
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody  // 明确指定JSON类型和编码
        );

        // 5. 构建请求
        Request request = new Request.Builder()
                .url(loginUrl)
                .addHeader("x-access-key", appSecret)  // 添加请求头
                .post(requestBody)                     // 设置JSON请求体
                .build();

        // 6. 执行请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 优化点5：处理非200状态码的情况
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }
            //反解析成java对象并返回token
            ResponseBody body = response.body();
            ResultLoginApiVo resultLoginApiVo = JSONObject.parseObject(body.string(), ResultLoginApiVo.class);
            String token = resultLoginApiVo.getResultData().getToken();
            if (token != null && !token.isEmpty()) {
                return token;
            }
            return "";
        }
    }



}
