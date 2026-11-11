package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDetailDto;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDto;
import org.jeecg.modules.pcms.energy.dto.solar.DeviceResultDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailSolar;
import org.jeecg.modules.pcms.energy.vo.DeviceReturnVo;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DeviceMapper;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {



    @Autowired
    private DeviceMapper deviceMapper;

//    @Autowired
//    private IDeviceDetailSolarService iDeviceDetailSolarService;
//
//    @Autowired
//    private IDeviceDetailGrowattService iDeviceDetailGrowattService;

    @Value("${growatt.token}")
    private String growattToken;
    @Value("${growatt.stationList}")
    private String stationGrowattListUrl;

    @Value("${solar.stationList}")
    private String stationSolarListUrl;

    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Value("${growatt.stationData}")
    private String stationData;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<DeviceResultDto> getDeviceName(String renterId) {
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            List<DeviceResultDto> deviceName = deviceMapper.getDeviceNameByProjectType(renterId);
            return deviceName;
        }else {
            List<DeviceResultDto> deviceName = deviceMapper.getDeviceName();
            return deviceName;
        }
    }

    @Override
    public List<DeviceResultDto> getDeviceNameGrw(String renterId) {
        if(StringUtils.isNotBlank(renterId)){
            if(renterId.equals("1964826626")){
                renterId="-624947198";
            }
            List<DeviceResultDto> deviceName = deviceMapper.getDeviceNameByProjectTypeGrw(renterId);
            return deviceName;
        }else {
            List<DeviceResultDto> deviceName = deviceMapper.getDeviceName();
            return deviceName;
        }
    }


    @Override
    public List<Integer> getListId() {
        //获取古瑞瓦特的所有电站id
        QueryWrapper<Device> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","2");
        List<Integer> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(Device::getDeviceId)
                .collect(Collectors.toList());
        return gwIdList;
    }

    @Override
    public DeviceStatsVo getStatsData(Integer plantId) {
        return deviceMapper.getStatsData(plantId);
    }

    @Override
    public PsInfoVo getPsInfoByPsKey(String psKey) {
        PsInfoVo psInfoByPsKey = deviceMapper.getPsInfoByPsKey(psKey);
        return psInfoByPsKey;
    }

    @Override
    public PsInfoVo getPsInfoByPsKeyForGrw(String psId) {
        PsInfoVo psInfoByPsKey = deviceMapper.getPsInfoByPsKeyForGrw(psId);
        return psInfoByPsKey;
    }


    @Override
    public DeviceReturnVo getPsNameByPsId(Integer psId) {
        return deviceMapper.getPsNameByPsId(psId);
    }


    @Override
    public List<Integer> getSolarListId() {
        //获取古瑞瓦特的所有电站id
        QueryWrapper<Device> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("source","1");
        List<Integer> gwIdList = this.list(deviceGwQueryWrapper)
                .stream()
                .map(Device::getDeviceId)
                .collect(Collectors.toList());
        return gwIdList;
    }

    /**
     *  获取阳光云电站列表数据
     * @param stationListUrl
     * @param token
     * @param appKey
     * @param appSecret
     * @param pageSize
     * @param pageNum
     * @return
     * @throws IOException
     */
    public SolarDeviceDto sendDeviceListSolarRequest(String stationListUrl, String token,
                                                     String appKey, String appSecret,String pageSize,String pageNum) throws IOException {
        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
                .retryOnConnectionFailure(true) // 自动重试连接失败
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
                .url(stationListUrl)
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
            return JSONObject.parseObject(body.string(), SolarDeviceDto.class);
        }
    }


    /***
     *  获取阳光云的电站全部数据
     * @param token
     * @return
     * @throws IOException
     */
    public List<SolarDeviceDto.PageItem> fetchSolarDeviceList(String token) throws IOException {

        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceDto.PageItem> allData = new ArrayList<>();
        SolarDeviceDto firstData = this.sendDeviceListSolarRequest(stationSolarListUrl, token, appKey, appSecret, String.valueOf(pageSize),  String.valueOf(currentPage));


        // 添加第一页数据
        allData.addAll(firstData.getResultData().getPageList());
        //总数据条数
        int rowCount = firstData.getResultData().getRowCount();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) rowCount / 100);

        // 循环获取剩余页的数据
        for (currentPage = 2; currentPage <= totalPages; currentPage++) {
            SolarDeviceDto pageData = this.sendDeviceListSolarRequest(stationSolarListUrl, token, appKey, appSecret, String.valueOf(pageSize),  String.valueOf(currentPage));
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
     * 古瑞瓦特获取电站列表数据
     * @return
     */
    public GrowattDeviceDto sendDeviceListGrowattRequest(String growattToken,String pageSize,String pageNum){

        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        // 1. 创建参数映射
        Map<String, String> params = new HashMap<>();
        int random = (int) ((Math.random() * 10) + 1);
        params.put("search_type", String.valueOf(random));
        params.put("page", String.valueOf(pageNum));  // 转换为字符串类型
        params.put("perpage", String.valueOf(pageSize));

        // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
        HttpUrl baseUrl = HttpUrl.parse(stationGrowattListUrl);
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid base URL: " + stationGrowattListUrl);
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
            GrowattDeviceDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceDto.class);

            return growattDeviceDto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    /***
     *  获取古瑞瓦特的电站全部数据
     * @param token
     * @return
     * @throws IOException
     */
    public List<GrowattDeviceDto.Plant> fetchGrowattDeviceList(String token) throws IOException {
        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<GrowattDeviceDto.Plant> allData = new ArrayList<>();
        GrowattDeviceDto growattDeviceDtoFirst = this.sendDeviceListGrowattRequest(token, String.valueOf(pageSize), String.valueOf(currentPage));


        // 添加第一页数据
        allData.addAll(growattDeviceDtoFirst.getData().getPlants());
        //总数据条数
        int rowCount = growattDeviceDtoFirst.getData().getCount();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) rowCount / 100);

        // 循环获取剩余页的数据
        for (currentPage = 2; currentPage <= totalPages; currentPage++) {
            GrowattDeviceDto growattDeviceData = this.sendDeviceListGrowattRequest(token, String.valueOf(pageSize), String.valueOf(currentPage));
            if (growattDeviceData != null && growattDeviceData.getData().getPlants() != null) {
                allData.addAll(growattDeviceData.getData().getPlants());
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





    public GrowattDeviceDetailDto.DeviceDetail sendDeviceDetailGrowattRequest(Integer plantId){

        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        // 1. 创建参数映射
        Map<String, String> params = new HashMap<>();
        int random = (int) ((Math.random() * 10) + 1);
        params.put("search_type", String.valueOf(random));
        params.put("plant_id", String.valueOf(plantId));

        // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
        HttpUrl baseUrl = HttpUrl.parse(stationData);
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid base URL: " + stationData);
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
            GrowattDeviceDetailDto growattDeviceDto = JSONObject.parseObject(body.string(), GrowattDeviceDetailDto.class);

            GrowattDeviceDetailDto.DeviceDetail data = growattDeviceDto.getData();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 定时任务处理
     */
    @Override
    public void insert() {
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            List<SolarDeviceDto.PageItem> pageItems = this.fetchSolarDeviceList(requestLoginToken);
            //2.查古瑞瓦特数据
            this.saveDeviceSolar(pageItems);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //阳光云获取电站列表的数据

    }

    @Override
    public void insertGrw() {
        try {
            //2.查古瑞瓦特数据
            List<GrowattDeviceDto.Plant> plants = this.fetchGrowattDeviceList(growattToken);
            this.saveDeviceGrwAll(plants);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //阳光云获取电站列表的数据
    }

    @Override
    public String getPsName(Long psId) {
        return    deviceMapper.getPsName(psId);
    }

    @Override
    public List<Long> getPsIdsByProjectType(String renterId) {
        List<Long> psIdsByProjectType = deviceMapper.getPsIdsByProjectType(renterId);
        return psIdsByProjectType;
    }

    @Override
    public List<Object> getTaskStatus(String taskId) {
        return redisTemplate.opsForList().range("Task:"+taskId, 0, -1);
    }


    //获取离线或者在线电站数量(阳光)
    @Override
    public Integer getUnLineDeviceNumSolar(String renterId,Integer status) {
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("project_type",renterId);
        deviceQueryWrapper.eq("ps_status",1);
        deviceQueryWrapper.eq("source",1);
        List<Device> list = this.list(deviceQueryWrapper);

        QueryWrapper<Device> deviceQueryWrapperGrw = new QueryWrapper<>();
        deviceQueryWrapperGrw.eq("project_type",renterId);
        deviceQueryWrapperGrw.eq("source",2);
        deviceQueryWrapperGrw.eq("ps_status",1);
        List<Device> list1 = this.list(deviceQueryWrapperGrw);
        return list.size()+list1.size();
    }

    //获取离线或者在线电站数量(阳光)
    @Override
    public Integer getUnLineDeviceNumGrw(String renterId,Integer status) {
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("project_type",renterId);
        deviceQueryWrapper.eq("source",1);
        deviceQueryWrapper.ne("ps_status","1");
        List<Device> list = this.list(deviceQueryWrapper);

        QueryWrapper<Device> deviceQueryWrapperGrw = new QueryWrapper<>();
        deviceQueryWrapperGrw.eq("project_type",renterId);
        deviceQueryWrapperGrw.eq("source",2);
        deviceQueryWrapperGrw.ne("ps_status",1);
        List<Device> list1 = this.list(deviceQueryWrapperGrw);
        return list.size()+list1.size();
    }



    @Override
    public String countTotalPower(String projectType) {
        QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
        deviceQueryWrapper.eq("source",1);
        deviceQueryWrapper.eq("project_type",projectType);
        List<Device> list = this.list(deviceQueryWrapper);
        BigDecimal bigDecimal = new BigDecimal(0.00);
        BigDecimal bigDecimal1 = new BigDecimal(0.00);
        if(list != null && list.size()>0 ){
            for (int i = 0; i < list.size(); i++) {
                String totalEnergy = list.get(i).getTotalEnergy();
                String totalEnergyUnit = list.get(i).getTotalEnergyUnit();
                if("万度".equals(totalEnergyUnit)){
                    bigDecimal=bigDecimal.add(new BigDecimal(totalEnergy).multiply(new BigDecimal(10000)));
                }else {
                    bigDecimal= bigDecimal.add(new BigDecimal(totalEnergy));
                }
            }
        }

        QueryWrapper<Device> deviceQueryWrapper1 = new QueryWrapper<>();
        deviceQueryWrapper1.eq("source",2);
        deviceQueryWrapper1.eq("project_type",projectType);
        List<Device> list1 = this.list(deviceQueryWrapper1);

        if(list1 != null && list1.size()>0 ){
            for (int i = 0; i < list1.size(); i++) {
                String totalEnergy = list1.get(i).getTotalEnergy();
                bigDecimal1=bigDecimal1.add(new BigDecimal(totalEnergy));
            }
        }
        BigDecimal add = bigDecimal.add(bigDecimal1);

        return add.toString();
    }

    @Override
    public List<String> getPsKeyList(String projectType) {
       return deviceMapper.getPsKeyList(projectType);
    }

    @Override
    public List<String> getPsKeyListGrw(String projectType) {
        return deviceMapper.getPsKeyListGrw(projectType);
    }

    public Map<String, String> getPsNameBatch(Set<Long> psIds) {
        HashMap<String, String> stringHashMap = new HashMap<>();

        List<Map<String, String>> psNameBatch = deviceMapper.getPsNameBatch(psIds);
        if (psNameBatch == null || psNameBatch.isEmpty()) {
            return stringHashMap;
        }
        for (Map<String, String> item : psNameBatch) {
            // 提取device_id并转为String
            Object deviceIdObj = item.get("device_id");
            String deviceId = deviceIdObj != null ? deviceIdObj.toString() : null;

            // 提取device_name并转为String
            Object deviceNameObj = item.get("device_name");
            String deviceName = deviceNameObj != null ? deviceNameObj.toString() : null;

            // 仅当device_id不为空时存入Map
            if (StringUtils.isNotBlank(deviceId)) {
                stringHashMap.put(deviceId, StringUtils.defaultString(deviceName));
            }
        }

        return stringHashMap;
    }

    @Override
    public Map<String, String> getPsNameBatchGrw(Set<Long> psIds) {
        HashMap<String, String> stringHashMap = new HashMap<>();

        List<Map<String, String>> psNameBatch = deviceMapper.getPsNameBatch(psIds);
        if (psNameBatch == null || psNameBatch.isEmpty()) {
            return stringHashMap;
        }
        for (Map<String, String> item : psNameBatch) {
            // 提取device_id并转为String
            Object deviceIdObj = item.get("device_id");
            String deviceId = deviceIdObj != null ? deviceIdObj.toString() : null;

            // 提取device_name并转为String
            Object deviceNameObj = item.get("device_name");
            String deviceName = deviceNameObj != null ? deviceNameObj.toString() : null;

            // 仅当device_id不为空时存入Map
            if (StringUtils.isNotBlank(deviceId)) {
                stringHashMap.put(deviceId, StringUtils.defaultString(deviceName));
            }
        }

        return stringHashMap;
    }

    /**把第三方的电站数据存入到我方数据库
     * @param pageItems
     * @return
     */
    @Override
    @Transactional
    public Result saveDeviceSolar(List<SolarDeviceDto.PageItem> pageItems) {
        if(pageItems != null && pageItems.size()>0){
            deviceMapper.deleteSolar();
            for (int i = 0; i < pageItems.size(); i++) {
                SolarDeviceDto.PageItem pageItem = pageItems.get(i);
                Device device = new Device();
                BeanUtils.copyProperties(pageItem,device);
                //总发电量（度）
                String totalEnergyValue = pageItem.getTotalEnergy().getValue();
                device.setTotalEnergy(totalEnergyValue);
                //累计发电量单位
                String totalNnit = pageItem.getTotalEnergy().getUnit();
                device.setTotalEnergyUnit(totalNnit);
                //当前功率
                String currPowerValue = pageItem.getCurrPower().getValue();
                device.setCurrPower(currPowerValue);
                //总排放量
                String co2TotalValue = pageItem.getCo2ReduceTotal().getValue();
                device.setCo2ReduceTotal(co2TotalValue);
                //今日电量
                SolarDeviceDto.EnergyData todayEnergy = pageItem.getTodayEnergy();
                String todayEnergyValue = todayEnergy.getValue();
                device.setTodayEnergy(todayEnergyValue);

                //今日电量单位
                String unit = todayEnergy.getUnit();
                device.setTodayEnergyUnit(unit);

                //今日收益
                String todayIncomeValue = pageItem.getTodayIncome().getValue();
                device.setTodayIncome(todayIncomeValue);
                //二氧化碳今日排放量
                String co2Value = pageItem.getCo2Reduce().getValue();
                device.setCo2Reduce(co2Value);
                //总收益
                String totalIncomeValue = pageItem.getTotalIncome().getValue();
                device.setTotalIncome(totalIncomeValue);
                //年收益
                String yearIncomeValue = pageItem.getYearIncome().getValue();
                device.setYearIncome(yearIncomeValue);
                //今日在线时长
                String equivalentHourValue = pageItem.getEquivalentHour().getValue();
                device.setEquivalentHour(equivalentHourValue);
                //总容量
                String totalCapcityValue = pageItem.getTotalCapcity().getValue();
                device.setTotalCapcity(totalCapcityValue);
                //月收益
                String monthIncomeValue = pageItem.getMonthIncome().getValue();
                device.setMonthIncome(monthIncomeValue);

                //按照省市来区分这是什么项目
                String provinceName = pageItem.getProvinceName();
                String projectType = this.getProjectType(provinceName);
                device.setProjectType(projectType);
                //电站id
                device.setDeviceId(pageItem.getPsId());
                device.setSource(1);
                device.setDeviceName(pageItem.getPsName());
                Long gridConnectionTime = pageItem.getGridConnectionTime();
                String formatGridConnectionTime = this.formatGridConnectionTime(gridConnectionTime);
                device.setGridConnectionTime(formatGridConnectionTime);
                this.save(device);
            }
        }
        return Result.ok();
    }

    @Override
    @Transactional
    public Result saveDeviceGrwAll( List<GrowattDeviceDto.Plant> plants) {
//      古瑞瓦特电站信息录入
        if(plants != null && plants.size()>0){
            deviceMapper.deleteGrw();
            for (int i = 0; i < plants.size(); i++) {
                GrowattDeviceDto.Plant plant = plants.get(i);
                //累计发电量
                String totalEnergy = plant.getTotalEnergy();
                //电站id
                Integer plantId = plant.getPlantId();
                //国家名
                String country = plant.getCountry();
                //城市名
                String city = plant.getCity();
                //电站图片地址
                String imageUrl = plant.getImageUrl();
                //经度
                String longitude = plant.getLongitude();
                //纬度
                String latitude = plant.getLatitude();
                //当前功率（kW）
                String currentPower = plant.getCurrentPower();
                //峰值功率（kWp）
                String peakPower = plant.getPeakPower();
                //电站所属的用户ID
                String userId = plant.getUserId();
                //电站名称
                String name = plant.getName();
                String installer = plant.getInstaller();
                //建站日期
                String createDate = plant.getCreateDate();
                //电站状态
                Integer status = plant.getStatus();
                Device device = new Device();
                device.setTotalEnergy(totalEnergy);
                device.setDeviceId(plantId);
                device.setProvinceName(country);
                device.setCityName(city);
                if(StringUtils.isNotBlank(latitude)){
                    device.setLatitude(Double.valueOf(latitude));
                }
                if(StringUtils.isNotBlank(longitude)){
                    device.setLongitude(Double.valueOf(longitude));
                }

                String projectType = this.getProjectType(city);
                device.setProjectType(projectType);

                device.setCurrPower(currentPower);
                device.setTotalCapcity(peakPower);
                device.setDeviceName(name);
                device.setInstallDate(createDate);
                device.setInstaller(StringUtils.isNotBlank(installer) ? installer: "");
                device.setPsStatus(status);
                device.setSource(2);
                this.save(device);
            }
        }
        return Result.ok();
    }


    public String getProjectType(String cityName){
        if(StringUtils.isNotBlank(cityName)){
            if ("北京市".equals(cityName)) {
                return "-2103508991";
            }
            if("天津市".equals(cityName)){
                return "102";
            }
            if("江苏省".equals(cityName)){
                return "118337537";
            }
            if("海南省".equals(cityName)){
                return "-624947198";
            }
            if("河南省".equals(cityName)){
                return "507514882";
            }
            if("河北省".equals(cityName)){
                return "-1759715327";
            }
            if (cityName.contains("天津")) {
                return "102";
            } else if (cityName.contains("海南") || cityName.contains("儋州")) {
                return "-624947198";
            } else if (cityName.contains("河南") || cityName.contains("漯河")) {
                return "507514882";
            } else if (cityName.contains("徐州")) {
                return "118337537";
            } else if (cityName.contains("北京")) {
                return "-2103508991";
            } else if (cityName.contains("河北") || cityName.contains("廊坊")) {
                return "-1759715327";
            } else {
                return "other"; // 如果没有匹配到任何条件，返回null
            }
        }
        return "other";
    }

    public String formatGridConnectionTime(Long gridConnectionTime)  {
        if (gridConnectionTime == null) {
            return "";
        }else {
            String timeStr = String.valueOf(gridConnectionTime);

            // 如果长度超过14位，取前14位；如果不足，补零到14位
            if (timeStr.length() > 14) {
                timeStr = timeStr.substring(0, 14);
            } else if (timeStr.length() < 14) {
                timeStr = String.format("%-14s", timeStr).replace(' ', '0');
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return outputFormat.format(inputFormat.parse(timeStr));
            } catch (ParseException e) {
                e.printStackTrace();
                return "";
            }
        }

    }

}
