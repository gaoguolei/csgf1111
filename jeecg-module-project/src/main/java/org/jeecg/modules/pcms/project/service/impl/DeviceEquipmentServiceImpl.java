package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.vo.DeviceEquipmentPointVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.energy.vo.ResultLoginApiVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentMapper;
import org.jeecg.modules.pcms.project.service.*;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DeviceEquipmentServiceImpl extends ServiceImpl<DeviceEquipmentMapper, DeviceEquipment> implements IDeviceEquipmentService {




    @Value("${solar.stationEquipments}")
    private String stationEquipmentsUrl;

    @Value("${growatt.deviceList}")
    private String deviceGrwListUrl;

    @Value("${growatt.token}")
    private String growattToken;

    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Autowired
    private DeviceEquipmentMapper deviceEquipmentMapper;

    @Autowired
    private IDeviceService iDeviceService;


    @Override
    public void saveDeviceEquipment(DeviceEquipment deviceEquipment) {
        this.save(deviceEquipment);
    }

    /**
     * 插入阳光云和古瑞瓦特的电站数据
     * @param pageItems
     */
    @Override
    @Transactional
    public void saveDeviceEquipmentList(List<SolarDeviceEquipmentDto.PageItem> pageItems) {
        deviceEquipmentMapper.deleteDeviceEquipmentSolar();
        if(pageItems != null && pageItems.size()>0){
            for (int j = 0; j < pageItems.size(); j++) {
                SolarDeviceEquipmentDto.PageItem pageItem = pageItems.get(j);
                DeviceEquipment deviceEquipment = new DeviceEquipment();
                BeanUtils.copyProperties(pageItem,deviceEquipment);
                deviceEquipment.setSource("1");
                this.save(deviceEquipment);
            }
        }
    }


    /**
     * 获取电站的sn码集合
     * @param type
     * @return
     */
    @Override
    public List<String> getListSn(String type) {

        //获取古瑞瓦特的所有设备sn码集合
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        if("2".equals(type)){
            deviceGwQueryWrapper.eq("source","2");
        }
        //获取阳光云的所有设备sn码集合
        if("1".equals(type)){
            deviceGwQueryWrapper.eq("source","1");
        }
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getDeviceSn)
                .collect(Collectors.toList());
        return gwIdList;
    }




    /**
     * 获取设备key集合
     * @return
     */
    @Override
    public List<String> getPsKeyList() {
        //获取阳光云云设备key集合
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","1");
        deviceGwQueryWrapper.eq("device_type","1");
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getPsKey)
                .collect(Collectors.toList());
        return gwIdList;
    }


    /**
     * 通过电站id获取阳光云的逆变器ps_key集合
     * @param plantIds
     * @return
     */
    @Override
    public List<String> getPsKeyListByPlantId(List<Long> plantIds) {
        //获取阳光云云设备key集合
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","1");
        deviceGwQueryWrapper.eq("device_type","1");
        deviceGwQueryWrapper.in("ps_id",plantIds);
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getPsKey)
                .collect(Collectors.toList());
        return gwIdList;
    }

    @Override
    public List<String> getPsKeyListByPlantIdInteger(List<Integer> plantIds) {
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","1");
        deviceGwQueryWrapper.eq("device_type","1");
        deviceGwQueryWrapper.in("ps_id",plantIds);
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getPsKey)
                .collect(Collectors.toList());
        return gwIdList;
    }


    /**
     * 根据电站id获取电站设备key(阳光云)
     * @param plantId
     * @return
     */
    @Override
    public List<String> getListPsKeyByPlantId(String plantId) {
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","1");
        deviceGwQueryWrapper.eq("device_type","1");
        deviceGwQueryWrapper.eq("ps_id",plantId);
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getPsKey)
                .collect(Collectors.toList());
        return gwIdList;
    }

    @Override
    public List<String> getListEquipmentSnByPlantId(String plantId) {
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("device_type","1");
        deviceGwQueryWrapper.eq("ps_id",plantId);
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getDeviceSn)
                .collect(Collectors.toList());
        return gwIdList;
    }

    /**
     * 根据电站id获取电站设备sn码(古瑞瓦特)
     * @param plantId
     * @return
     */
    @Override
    public List<String> getListPsSnByPlantId(String plantId) {
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","2");
        deviceGwQueryWrapper.eq("device_type","1");
        deviceGwQueryWrapper.eq("ps_id",plantId);
        List<String> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(DeviceEquipment::getDeviceSn)
                .collect(Collectors.toList());
        return gwIdList;
    }

    @Override
    public DeviceEquipment getDeviceEquipmentByPsKey(String psKey) {
        QueryWrapper<DeviceEquipment> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("ps_key",psKey);
        DeviceEquipment one = this.getOne(deviceGwQueryWrapper);
        return one;
    }




    /**
     * 定时任务录入
     */
    @Override

    public void insert() {
        //调用登录接口获取token，获取下一步的电站列表
        try {
            String requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            //阳光云获取电站列表的数据
            List<SolarDeviceEquipmentDto.PageItem> pageItems = this.fetchSolarDeviceEquipmentList(requestLoginToken);
            //查古瑞瓦特数据
            //1、获取所有电站id集合
            //2、传参到古瑞瓦特接口获取所有设备数据
            List<Integer> listId = iDeviceService.getListId();
            //获取所有设备列表集合
            this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1");
            this.saveDeviceEquipmentList(pageItems);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> getPsKeyVoListFault() {
        List<DeviceEquipmentPointVo> psKeyVoList = deviceEquipmentMapper.getPsKeyVoListOfFault();
        if(psKeyVoList != null){
            Map<String, List<String>> resultMap = psKeyVoList.stream()
                    .collect(Collectors.groupingBy(
                            DeviceEquipmentPointVo::getConnectionDate, // 以connectionDate作为key
                            Collectors.mapping(
                                    DeviceEquipmentPointVo::getPsKey,      // 提取psKey
                                    Collectors.toList()    // 收集为List
                            )
                    ));
            return resultMap;
        }
        return null;

    }

    @Override
    public PsInfoVo getDeviceInfo(String psKey) {
        PsInfoVo deviceInfo = deviceEquipmentMapper.getDeviceInfo(psKey);
        return deviceInfo;
    }

    @Override
    public PsInfoVo getDeviceInfoGrw(String psKey) {
        PsInfoVo deviceInfo = deviceEquipmentMapper.getDeviceInfoGrw(psKey);
        return deviceInfo;
    }

    @Override
    public List<PsInfoVo> getDeviceInfos(Set<String> psKey) {
        List<PsInfoVo> deviceInfos = deviceEquipmentMapper.getDeviceInfos(psKey);
        return deviceInfos;
    }

    @Override
    public List<String> getPsKeyByProjectType(String renterId) {
        return deviceEquipmentMapper.getPsKeyByProjectType(renterId);
    }

    @Override
    public List<String> getPsKeyByProjectTypeGrw(String renterId) {
        return deviceEquipmentMapper.getPsKeyByProjectTypeGrw(renterId);
    }


    public List<SolarDeviceEquipmentDto.PageItem> fetchSolarDeviceEquipmentList(String token) throws IOException {

        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceEquipmentDto.PageItem> allData = new ArrayList<>();
        SolarDeviceEquipmentDto firstData = this.sendDeviceListSolarRequest(stationEquipmentsUrl, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
        // 添加第一页数据
        allData.addAll(firstData.getResultData().getPageList());
        //总数据条数
        int rowCount = firstData.getResultData().getRowCount();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) rowCount / 100);

        // 循环获取剩余页的数据
        for (currentPage = 2; currentPage <= totalPages; currentPage++) {
            SolarDeviceEquipmentDto pageData = this.sendDeviceListSolarRequest(stationEquipmentsUrl, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));
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
    public SolarDeviceEquipmentDto sendDeviceListSolarRequest(String stationFault, String token,
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
            return JSONObject.parseObject(body.string(), SolarDeviceEquipmentDto.class);
        }
    }


    /**
     * 古瑞瓦特获获取并插入设备数据
     *
     * @return
     */
    @Transactional
    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum) {
        if(listId != null && listId.size() > 0){
            deviceEquipmentMapper.deleteDeviceEquipmentGrw();
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
                params.put("page", String.valueOf(pageNum));  // 转换为字符串类型
                params.put("perpage", String.valueOf(pageSize));
                params.put("plant_id",String.valueOf(listId.get(i)));

                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(deviceGrwListUrl);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + deviceGrwListUrl);
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
//                    System.err.println(body.string());
                    GrowattDeviceEquipmentDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceEquipmentDto.class);
                    if(growattDeviceDto != null){
                        if(growattDeviceDto.getData() != null){
                            if(growattDeviceDto.getData().getDevices() != null ){
                                List<GrowattDeviceEquipmentDto.DeviceEquipmentDto> deviceEquipmentDtos = growattDeviceDto.getData().getDevices();
                                if(deviceEquipmentDtos != null){
                                    for (int j = 0; j < deviceEquipmentDtos.size(); j++) {
                                        GrowattDeviceEquipmentDto.DeviceEquipmentDto deviceEquipmentDto = deviceEquipmentDtos.get(j);
                                        if(deviceEquipmentDto.getType()==1){
                                            DeviceEquipment deviceEquipment = new DeviceEquipment();
                                            deviceEquipment.setDeviceSn(deviceEquipmentDto.getDeviceSn());
                                            deviceEquipment.setFactoryName(deviceEquipmentDto.getManufacturer());
                                            deviceEquipment.setDeviceModelCode(deviceEquipmentDto.getModel());
                                            deviceEquipment.setDeviceType(deviceEquipmentDto.getType());
                                            deviceEquipment.setPsId(Long.valueOf(listId.get(i)));
                                            deviceEquipment.setDevStatus(deviceEquipmentDto.getLost());
                                            deviceEquipment.setGrwStatus(String.valueOf(deviceEquipmentDto.getStatus()));
                                            deviceEquipment.setSource("2");
                                            this.save(deviceEquipment);
                                        }
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
