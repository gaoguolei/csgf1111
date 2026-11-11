package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDetailDto;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;
import org.jeecg.modules.pcms.project.mapper.DeviceDetailGrowattMapper;
import org.jeecg.modules.pcms.project.mapper.DeviceDetailSolarMapper;
import org.jeecg.modules.pcms.project.mapper.DevicePointMinFiveMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailGrowattService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class DeviceDetailGrowattServiceImpl extends ServiceImpl<DeviceDetailGrowattMapper, DeviceDetailGrowatt> implements IDeviceDetailGrowattService {



    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");


    @Value("${growatt.stationData}")
    private String stationData;

    @Value("${growatt.token}")
    private String growattToken;
    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;
    @Value("${solar.stationDetailList}")
    private String stationDetailList;


    @Autowired
    private DeviceDetailSolarMapper deviceDetailSolarMapper;
    @Autowired
    private DevicePointMinFiveMapper devicePointMinFiveMapper;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;


    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private DeviceDetailGrowattMapper deviceDetailGrowattMapper;
    @Override
    public void deleteAll() {
        deviceDetailGrowattMapper.deleteAll();
    }

    @Override
    @Transactional
    public void insert() {
        //2.存入古瑞瓦特的电站橄榄数据  古瑞瓦特电站列表接口调用完成之后再继续调用
        List<Integer> listId = iDeviceService.getListId();
        if(listId != null && listId.size() > 0){
            this.deleteAll();
            for (int i = 0; i < listId.size(); i++) {
                Integer plantId = listId.get(i);
                GrowattDeviceDetailDto.DeviceDetail deviceDetail = this.sendDeviceDetailGrowattRequest(plantId);
                if(deviceDetail != null){
                    DeviceDetailGrowatt deviceDetailGrowatt = new DeviceDetailGrowatt();
                    BeanUtils.copyProperties(deviceDetail,deviceDetailGrowatt);
                    deviceDetailGrowatt.setSource(2);
                    deviceDetailGrowatt.setPlantId(plantId);
                    this.save(deviceDetailGrowatt);
                }
            }
        }

    }

    @Override
    public DeviceDetailGrowatt getDeviceDetail(String psId) {
        QueryWrapper<DeviceDetailGrowatt> deviceDetailGrowattQueryWrapper = new QueryWrapper<>();
        deviceDetailGrowattQueryWrapper.eq("plant_id",psId);
        DeviceDetailGrowatt one = this.getOne(deviceDetailGrowattQueryWrapper);
        return one;
    }

    public GrowattDeviceDetailDto.DeviceDetail sendDeviceDetailGrowattRequest(Integer plantId){

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
}
