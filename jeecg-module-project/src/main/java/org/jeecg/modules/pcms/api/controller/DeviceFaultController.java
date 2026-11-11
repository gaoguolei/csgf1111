package org.jeecg.modules.pcms.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceFaultDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceFaultDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.entity.DeviceFault;
import org.jeecg.modules.pcms.energy.entity.DeviceFaultHistory;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.ResultLoginApiVo;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceFaultHistoryService;
import org.jeecg.modules.pcms.project.service.IDeviceFaultService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Api(tags = "电站设备告警模块")
@RestController
@RequestMapping(value = "project/deviceFault")
public class DeviceFaultController {



    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationFault}")
    private String stationFault;

    @Value("${growatt.stationList}")
    private String stationGrowattListUrl;

    @Value("${growatt.inverterAlarm}")
    private String inverterAlarm;

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
    private IDeviceFaultService iDeviceFaultService;

    @Autowired
    private IDeviceFaultHistoryService iDeviceFaultHistoryService;

    @Autowired
    private IDeviceService  iDeviceService;



    @ApiOperation("条件查询设备故障列表分页")
    @RequestMapping(value = "/deviceFaultPageList", method = RequestMethod.GET)
    public Result deviceEquipmentPageList(DevicePageVo devicePageVo) throws Exception {
        String status = devicePageVo.getStatus();
        Integer source = devicePageVo.getSource();
        String devicePsKye = devicePageVo.getDevicePsKey();
        String renterId = devicePageVo.getRenterId();
        //查询历史数据
        if("0".equals(status)){
            // 动态分页参数
            IPage<DeviceFaultHistory> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DeviceFaultHistory> deviceFaultHistoryQueryWrapper = new QueryWrapper<>();
            //查询条件 根据故障名称  模糊查询
            String faultName = devicePageVo.getFaultName();
            if(StringUtils.isNotBlank(faultName)){
                deviceFaultHistoryQueryWrapper.like("fault_name",faultName);
            }
            //查询条件 根据告警码 模糊查询
            String faultCode = devicePageVo.getFaultCode();
            if(StringUtils.isNotBlank(faultCode)){
                deviceFaultHistoryQueryWrapper.like("fault_reason",faultCode);
            }

            //只有plantId 递归查询所有的ps_key
            String plantId = devicePageVo.getPlantId();
            if(source != null && source.equals("1")){
                if (StringUtils.isNotBlank(plantId)) {
                    // 构建基础查询条件
                    if(source == 1){
                        QueryWrapper<DeviceEquipment> baseWrapper = new QueryWrapper<>();
                        baseWrapper.eq("ps_id", plantId).eq("source", 1);

                        // 处理设备类型条件
                        String deviceType = devicePageVo.getDeviceType();
                        if (StringUtils.isNotBlank(deviceType)) {
                            baseWrapper.eq("device_type", deviceType);
                        }

                        // 执行查询获取psKeys
                        List<String> psKeys = iDeviceEquipmentService.list(baseWrapper)
                                .stream()
                                .map(DeviceEquipment::getPsKey)
                                .collect(Collectors.toList());

                        // 处理设备psKey条件

                        if (StringUtils.isNotBlank(devicePsKye)) {
                            // 如果指定了具体设备psKey，则直接使用该值
                            deviceFaultHistoryQueryWrapper.in("ps_key", Arrays.asList(devicePsKye.split(",")));
                        } else {
                            // 否则使用查询得到的psKeys列表
                            deviceFaultHistoryQueryWrapper.in("ps_key", psKeys);
                        }
                    }
                }

            }
            //先查询出来各个项目下的电站id集合 在in查询一下
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                List<Long> psIdsByProjectType = iDeviceService.getPsIdsByProjectType(renterId);
                if(psIdsByProjectType != null && psIdsByProjectType.size() > 0){
                    deviceFaultHistoryQueryWrapper.in("ps_id",psIdsByProjectType);
                }
            }
            if(source != null){
                deviceFaultHistoryQueryWrapper.eq("source",1);
            }
            IPage<DeviceFaultHistory> page = iDeviceFaultHistoryService.page(pageData, deviceFaultHistoryQueryWrapper);
            List<DeviceFaultHistory> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    String psKey = records.get(i).getPsKey();
                    DeviceEquipment deviceEquipmentByPsKey = iDeviceEquipmentService.getDeviceEquipmentByPsKey(psKey);
                    if(deviceEquipmentByPsKey != null){
                        String deviceSn = deviceEquipmentByPsKey.getDeviceSn();
                        records.get(i).setDeviceSn(deviceSn);
                    }
                }
            }

            page.setRecords(records);
            return Result.ok(page);
        }

        //查询待处理数据
        if("1".equals(status)){
            // 动态分页参数
            IPage<DeviceFault> pageData = new Page<>(
                    devicePageVo.getPageNo(),
                    devicePageVo.getPageSize()
            );
            QueryWrapper<DeviceFault> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            //查询条件 根据故障名称  模糊查询
            String faultName = devicePageVo.getFaultName();
            if(StringUtils.isNotBlank(faultName)){
                deviceEquipmentQueryWrapper.like("fault_name",faultName);
            }
            //查询条件 根据告警码 模糊查询
            String faultCode = devicePageVo.getFaultCode();
            if(StringUtils.isNotBlank(faultCode)){
                deviceEquipmentQueryWrapper.like("fault_reason",faultCode);
            }

            //只有plantId 递归查询所有的ps_key
            String plantId = devicePageVo.getPlantId();
            if(source != null && "1".equals(source)){
                if (StringUtils.isNotBlank(plantId)) {
                    // 构建基础查询条件
                    if( "1".equals(source)){
                        QueryWrapper<DeviceEquipment> baseWrapper = new QueryWrapper<>();
                        baseWrapper.eq("ps_id", plantId).eq("source", 1);

                        // 处理设备类型条件
                        String deviceType = devicePageVo.getDeviceType();
                        if (StringUtils.isNotBlank(deviceType)) {
                            baseWrapper.eq("device_type", deviceType);
                        }

                        // 执行查询获取psKeys
                        List<String> psKeys = iDeviceEquipmentService.list(baseWrapper)
                                .stream()
                                .map(DeviceEquipment::getPsKey)
                                .collect(Collectors.toList());


                        // 处理设备psKey条件
                        if (StringUtils.isNotBlank(devicePsKye)) {
                            // 如果指定了具体设备psKey，则直接使用该值
                            deviceEquipmentQueryWrapper.in("ps_key", Arrays.asList(devicePsKye.split(",")));
                        } else {
                            // 否则使用查询得到的psKeys列表
                            deviceEquipmentQueryWrapper.in("ps_key", psKeys);
                        }
                    }
                }
                //有电站id和设备类型和设备psKey

            }
            if(source != null){
                deviceEquipmentQueryWrapper.eq("source",source);
            }

            //先查询出来各个项目下的电站id集合 在in查询一下
            if(StringUtils.isNotBlank(renterId)){
                if(renterId.equals("1964826626")){
                    renterId="-624947198";
                }
                List<Long> psIdsByProjectType = iDeviceService.getPsIdsByProjectType(renterId);
                if(psIdsByProjectType != null && psIdsByProjectType.size() > 0){
                    deviceEquipmentQueryWrapper.in("ps_id",psIdsByProjectType);
                }
            }

            IPage<DeviceFault> page = iDeviceFaultService.page(pageData, deviceEquipmentQueryWrapper);

            List<DeviceFault> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    String psKey = records.get(i).getPsKey();
                    DeviceEquipment deviceEquipmentByPsKey = iDeviceEquipmentService.getDeviceEquipmentByPsKey(psKey);
                    if(deviceEquipmentByPsKey != null){
                        String deviceSn = deviceEquipmentByPsKey.getDeviceSn();
                        records.get(i).setDeviceSn(deviceSn);
                    }
                }
            }
            page.setRecords(records);
            return Result.ok(page);
        }

        return Result.ok();
    }





    @ApiOperation("存入电站下设备数据数据")
    @RequestMapping(value = "/deviceFaultList", method = RequestMethod.POST)
    public Result deviceFaultList() throws Exception {
        //调用登录接口获取token，获取下一步的电站列表
        String requestLoginToken = this.sendLoginSolarRequest(loginUrl, username, password, appKey, appSecret);
        //阳光云获取电站列表的数据
        List<SolarDeviceFaultDto.PageListItem> pageItems = this.fetchSolarDeviceFaultList(requestLoginToken);
        iDeviceFaultService.saveDeviceFaultAll(pageItems);
        //2.查古瑞瓦特数据
        //获取古瑞瓦特的所有逆变器的sn码集合
        //再循环获取逆变器的告警信息
        List<String> listSN = iDeviceEquipmentService.getListSn("2");
        this.sendDeviceListGrowattRequest(listSN,growattToken);
        return Result.ok();
    }



    /**
     * 阳光云获取登录token
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
            if(token != null && !token.isEmpty()){
                return token;
            }
            return "";
        }
    }
    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     * @param stationFault
     * @param token
     * @param appKey
     * @param appSecret
     * @param pageSize
     * @param pageNum
     * @return
     * @throws IOException
     */
    public SolarDeviceFaultDto sendDeviceListSolarRequest(String stationFault, String token,
                                                          String appKey, String appSecret, String pageSize, String pageNum) throws IOException {
        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        Map<String, String> params = new HashMap<>();
        params.put("appkey", appKey);
        params.put("size", pageSize);
        params.put("curPage", pageNum);
        params.put("token", token);
        params.put("share_type", "0,1,2");
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
                .url(stationFault)
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
            return JSONObject.parseObject(body.string(), SolarDeviceFaultDto.class);
        }
    }


    /***
     *  获取阳光云的故障全部数据
     * @param token
     * @return
     * @throws IOException
     */
    public List<SolarDeviceFaultDto.PageListItem> fetchSolarDeviceFaultList(String token) throws IOException {

        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceFaultDto.PageListItem> allData = new ArrayList<>();
        SolarDeviceFaultDto firstData = this.sendDeviceListSolarRequest(stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
        // 添加第一页数据
        allData.addAll(firstData.getResultData().getPageList());
        //总数据条数
        int rowCount = firstData.getResultData().getRowCount();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) rowCount / 100);

        // 循环获取剩余页的数据
        for (currentPage = 2; currentPage <= totalPages; currentPage++) {
            SolarDeviceFaultDto pageData = this.sendDeviceListSolarRequest(stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
            if (pageData != null && pageData.getResultData().getPageList() != null) {
                allData.addAll(pageData.getResultData().getPageList());
            }

            // 可以添加适当的延迟，避免请求过于频繁
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return allData;
    }

    /**
     * 古瑞瓦特通过设备sn码来获取设备故障信息存入数据库
     *
     * @return
     */
    public void sendDeviceListGrowattRequest(List<String> listSn,String growattToken) {

        if(listSn != null && listSn.size() > 0){
            for (int i = 0; i < listSn.size(); i++) {
                // 1. 创建OkHttpClient实例，并配置超时参数
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                        .build();

                // 2. 构建请求参数Map
                // 1. 创建参数映射
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date endDate = new Date();
                String formatted = format.format(endDate);
                Map<String, String> params = new HashMap<>();
                int random = (int) ((Math.random() * 10) + 1);
                params.put("search_type", String.valueOf(random));
                params.put("date", formatted);
                params.put("device_sn", listSn.get(i));


                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(inverterAlarm);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + inverterAlarm);
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
                    ResponseBody body = response.body();
                    GrowattDeviceFaultDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceFaultDto.class);
                    if(growattDeviceDto != null){
                        if(growattDeviceDto.getData() != null){
                            if(growattDeviceDto.getData().getAlarms() != null ){
                                List<GrowattDeviceFaultDto.Alarm> deviceEquipmentDtos = growattDeviceDto.getData().getAlarms();
                                if(deviceEquipmentDtos != null){
                                    for (int j = 0; j < deviceEquipmentDtos.size(); j++) {
                                        GrowattDeviceFaultDto.Alarm alarm = deviceEquipmentDtos.get(j);
                                        DeviceFault deviceFault = new DeviceFault();
                                        deviceFault.setFaultCode(String.valueOf(alarm.getAlarmCode()));
                                        deviceFault.setFaultName(alarm.getAlarmMessage());
                                        deviceFault.setDeviceSn(listSn.get(i));
                                        deviceFault.setFaultName(alarm.getAlarmMessage());
                                        deviceFault.setGrwFaultType(alarm.getFaultType());
                                        deviceFault.setSource(2);
                                        iDeviceFaultService.save(deviceFault);
                                    }
                                }
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
