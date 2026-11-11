package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.entity.DeviceGatherGrw;
import org.jeecg.modules.pcms.energy.entity.PlantData;
import org.jeecg.modules.pcms.project.mapper.DeviceGatherGrwMapper;
import org.jeecg.modules.pcms.project.service.IDeviceGatherGrwService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DeviceGatherGrwServiceImpl  extends ServiceImpl<DeviceGatherGrwMapper, DeviceGatherGrw> implements IDeviceGatherGrwService {


    @Value("${growatt.dataLogger}")
    private String dataLoggerUrl;

    @Autowired
    private IDeviceService iDeviceService;




    @Autowired
    private DeviceGatherGrwMapper deviceGatherGrwMapper;


    @Value("${growatt.token}")
    private String growattToken;


    @Override
    public void insert() {
        List<Integer> listId = iDeviceService.getListId();

//        ArrayList<Integer> strings = new ArrayList<>();
//        strings.add(929123);
        deviceGatherGrwMapper.deleteAll();
        this.fetchGrowattDeviceEquipmentList(listId,growattToken,"100","1");
    }

    @Override
    public List<String> listMessageSn(String psId) {
        return  deviceGatherGrwMapper.listMessageSn(psId);

    }

    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum) {
        if(listId != null && listId.size() > 0){
            for (int i = 0; i < listId.size(); i++) {
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
                params.put("plant_id",String.valueOf(listId.get(i)));

                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(dataLoggerUrl);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + dataLoggerUrl);
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
                    PlantData solarDeviceFaultDto = JSONObject.parseObject(body.string(), PlantData.class);
                    List<PlantData.DataLogger> dataloggers = solarDeviceFaultDto.getData().getDataloggers();
                    if(dataloggers != null && dataloggers.size()>0){
                        for (int j = 0; j < dataloggers.size(); j++) {
                            DeviceGatherGrw deviceGatherGrw = new DeviceGatherGrw();
                            String sn = dataloggers.get(j).getSn();
                            int type = dataloggers.get(j).getType();
                            String model = dataloggers.get(j).getModel();
                            String manufacturer = dataloggers.get(j).getManufacturer();
                            String netmode = dataloggers.get(j).getNetmode();
                            Boolean lost = dataloggers.get(j).lost;
                            deviceGatherGrw.setPsId(listId.get(i));
                            deviceGatherGrw.setLost(lost.toString());
                            deviceGatherGrw.setType(String.valueOf(type));
                            deviceGatherGrw.setModel(model);
                            deviceGatherGrw.setManufacturer(manufacturer);
                            deviceGatherGrw.setNetmode(netmode);
                            deviceGatherGrw.setSn(sn);
                            this.save(deviceGatherGrw);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
