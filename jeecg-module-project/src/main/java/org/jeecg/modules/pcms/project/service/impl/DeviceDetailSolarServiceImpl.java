package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDetailDto;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarPlantDetailDto;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailSolar;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.vo.DeviceDataVo;
import org.jeecg.modules.pcms.energy.vo.DevicePointVo;
import org.jeecg.modules.pcms.project.mapper.DeviceDetailSolarMapper;
import org.jeecg.modules.pcms.project.mapper.DevicePointMinFiveMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailGrowattService;
import org.jeecg.modules.pcms.project.service.IDeviceDetailSolarService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class DeviceDetailSolarServiceImpl extends ServiceImpl<DeviceDetailSolarMapper, DeviceDetailSolar> implements IDeviceDetailSolarService {


    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
            .retryOnConnectionFailure(true) // 自动重试连接失败
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

//    @Override
//    public Map<String, List<String>> getPsKeyVoList() {
//        List<DeviceDataVo> psKeyVoList = deviceDetailSolarMapper.getPsKeyVoList();
//        List<String> haveDataPsKeyList = devicePointMinFiveMapper.getHaveDataPsKeyList();
//
//        //需要过滤一下，在详情表里面有但是在历史表里面没有的数据 筛选出来之后在做map
//        if (haveDataPsKeyList != null && !haveDataPsKeyList.isEmpty()) {
//            List<DeviceDataVo> pointVos = psKeyVoList.stream()
//                    .filter(vo -> !haveDataPsKeyList.contains(vo.getPsKey()))
//                    .collect(Collectors.toList());
//            Map<String, List<String>> resultMap = pointVos.stream()
//                    .collect(Collectors.groupingBy(
//                            DeviceDataVo::getFormattedInstallDate, // 以connectionDate作为key
//                            Collectors.mapping(
//                                    DeviceDataVo::getPsKey,      // 提取psKey
//                                    Collectors.toList()    // 收集为List
//                            )
//                    ));
//            return resultMap;
//        }else {
//            Map<String, List<String>> resultMap = psKeyVoList.stream()
//                    .collect(Collectors.groupingBy(
//                            DeviceDataVo::getFormattedInstallDate, // 以connectionDate作为key
//                            Collectors.mapping(
//                                    DeviceDataVo::getPsKey,      // 提取psKey
//                                    Collectors.toList()    // 收集为List
//                            )
//                    ));
//            return resultMap;
//        }
//
//    }

    @Override
    public List<String> getPsKey() {
        //获取全部电站设备的pskey来获取设备每天的数据
        List<String> psKeyList = this.getPlantPsKey();
        return psKeyList;
    }

    @Override
    @Transactional
    public void sendPlantDetailListSolarRequest(List<String> solarListId) {
        try {
            deviceDetailSolarMapper.deleteAll();
            String token = SolarGetTokenUtils.sendLoginSolarRequest();
            if(solarListId != null && solarListId.size() > 0) {
                // 分批处理（每20个ID一组）
                int batchSize = 20;
                for (int i = 0; i < solarListId.size(); i += batchSize) {
                    // 获取当前批次的ID子集
                    List<String> batchIds = solarListId.stream()
                            .skip(i)
                            .limit(batchSize)
                            .collect(Collectors.toList());

                    // 构建ID字符串（格式：1,2,3）
                    String idsString = batchIds.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));

                    // 构建请求参数
                    Map<String, String> params = new HashMap<>();
                    params.put("appkey", appKey);
                    params.put("token", token);
                    params.put("is_get_ps_remarks", "1");
                    params.put("lang", "_en_US");
                    params.put("ps_ids", idsString);  // 使用分批后的ID字符串

                    // 创建请求体
                    RequestBody requestBody = RequestBody.create(
                            JSON_MEDIA_TYPE,
                            GSON.toJson(params)
                    );

                    // 构建请求
                    Request request = new Request.Builder()
                            .url(stationDetailList)
                            .addHeader("x-access-key", appSecret)
                            .post(requestBody)
                            .build();

                    // 执行请求
                    try (Response response = CLIENT.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("请求失败: " + response.code() + " " + response.message());
                        }
                        ResponseBody body = response.body();
                        SolarPlantDetailDto result = JSONObject.parseObject(body.string(), SolarPlantDetailDto.class);
                        SolarPlantDetailDto.ResultData resultData = result.getResultData();
                        if(resultData != null){
                            List<SolarPlantDetailDto.PlantInfo> dataList = resultData.getDataList();
                            if(dataList != null && dataList.size() > 0){
                                for (int j = 0; j < dataList.size(); j++) {
                                    SolarPlantDetailDto.PlantInfo plantInfo = dataList.get(j);
                                    DeviceDetailSolar deviceDetailSolar = new DeviceDetailSolar();
                                    List<SolarPlantDetailDto.CommunicationDevDetail> communicationDevDetailList = plantInfo.getCommunicationDevDetailList();
                                    Map<String, String> psRemarks = plantInfo.getPsRemarks();
                                    String psRemarksString = JSONObject.toJSONString(psRemarks);
                                    String jsonString = JSONObject.toJSONString(communicationDevDetailList);
                                    BeanUtils.copyProperties(plantInfo,deviceDetailSolar);
                                    deviceDetailSolar.setCommunicationDevDetailList(jsonString);
                                    deviceDetailSolar.setPsRemarks(psRemarksString);
                                    deviceDetailSolar.setCreatedAt(new Date());
                                    this.save(deviceDetailSolar);
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






    /**
     * 查询到不在这个电站pskey集合里面的所有电站的ps_id集合
     * @param notInPsKeyList
     * @return
     */
    @Override
    public List<Long> notInPsKey(List<String> notInPsKeyList) {
        QueryWrapper<DeviceDetailSolar> detailSolarQueryWrapper = new QueryWrapper<>();
        detailSolarQueryWrapper.notIn("ps_key",notInPsKeyList);
        List<DeviceDetailSolar> list = this.list(detailSolarQueryWrapper);
        List<Long> idList = list.stream()
                .map(DeviceDetailSolar::getPsId)
                .distinct()
                .collect(Collectors.toList());
        return idList;
    }



    @Override
    public List<Long> inPsKey(List<String> inPsKeyList) {
        QueryWrapper<DeviceDetailSolar> detailSolarQueryWrapper = new QueryWrapper<>();
        detailSolarQueryWrapper.in("ps_key",inPsKeyList);
        List<DeviceDetailSolar> list = this.list(detailSolarQueryWrapper);
        List<Long> idList = list.stream()
                .map(DeviceDetailSolar::getPsId)
                .distinct()
                .collect(Collectors.toList());
        return idList;
    }


    /**
     * 电站详情录入定时任务
     */
    @Override
    public void insertDeviceDetail() {
        //阳光云获取电站id集合
        List<Integer> solarListId = iDeviceService.getSolarListId();
        List<String> stringList = solarListId.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        this.sendPlantDetailListSolarRequest(stringList);
    }

    @Override
    public List<String> getPlantPsKey() {
        List<DevicePointVo> psKeyVoList = deviceDetailSolarMapper.getPsKeyVoList();
        List<String> haveDataPsKeyList = devicePointMinFiveMapper.getHavePsKeyList();

        //需要过滤一下，在详情表里面有但是在历史表里面没有的数据 筛选出来之后在做map
        if (haveDataPsKeyList != null && !haveDataPsKeyList.isEmpty()) {
            List<String> psKyeList = psKeyVoList.stream()
                    .filter(vo -> !haveDataPsKeyList.contains(vo.getPsKey()))
                    .map(DevicePointVo::getPsKey)
                    .collect(Collectors.toList());
            return psKyeList;
        }
        return psKeyVoList.stream().map(DevicePointVo::getPsKey)
                .collect(Collectors.toList());
    }


    @Override
    public List<String> getDevicePsKey() {
        List<String> devicePsKey = deviceDetailSolarMapper.getDevicePsKey();
        return devicePsKey;
    }

    @Override
    public String plantPsKey(Integer deviceId) {
        return deviceDetailSolarMapper.plantPsKey(deviceId);
    }


}
