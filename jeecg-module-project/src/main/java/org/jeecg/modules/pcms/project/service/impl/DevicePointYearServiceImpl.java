package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointDayDto;
import org.jeecg.modules.pcms.energy.entity.DevicePointMonthGrw;
import org.jeecg.modules.pcms.energy.entity.DevicePointYear;
import org.jeecg.modules.pcms.energy.export.DevicePointYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointYearMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailSolarService;
import org.jeecg.modules.pcms.project.service.IDevicePointYearService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DevicePointYearServiceImpl extends ServiceImpl<DevicePointYearMapper, DevicePointYear> implements IDevicePointYearService {

    private static final int BATCH_SIZE = 50;

    private static final String FIXED_POINT_IDS = "p83019,p83022,p83024,p83025,p83033";

    private static final String[] INDICATORS = {"p83019", "p83022", "p83024", "p83025","p83033"};
    private static final String VALUE_KEY = "4";
    private static final String TIMESTAMP_KEY = "time_stamp";

    private static final String VALUE_KEY_MONTH = "4";

    @Value("${solar.stationEquipmentsMinuteDMYPoint}")
    private String stationEquipmentsMinuteDMYPoint;
    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;
    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;

    @Autowired DevicePointYearMapper devicePointYearMapper;


    @Override
    public void insert() {
        //调用登录接口获取token，获取下一步的电站列表
        //阳光云获取设备sn集合数据
        List<String> psKey = iDeviceDetailSolarService.getDevicePsKey();

        //从这个投运时间开始 到昨天的数据
        //获取昨日数据
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        String formatted = today.format(formatter);

        List<DateIntervalVo> dateIntervalVos = DateUtils.splitTimeSegmentsYear(formatted, formatted);
        String dateType = "4";
        String queryType = "3";
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            // 遍历设备列表
            this.fetchSolarDeviceEquipmentMinPointList(dateIntervalVos, psKey, dateType, queryType, requestLoginToken);
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


    @Override
    public Integer isHaveMonth() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        QueryWrapper<DevicePointYear> queryWrapper = new QueryWrapper<>();
        // 假设 time_stamp 格式是 20251020120000，我们匹配前6位
        queryWrapper.likeRight("time_stamp", currentMonth);
        // 使用selectCount查询数量
        Long count = devicePointYearMapper.selectCount(queryWrapper);
        return count != null && count > 0 ? 1 : 0;
    }

    @Override
    public void deleteMonth(String formatted) {
        devicePointYearMapper.deleteYear(formatted);
    }

    @Override
    @Async("exportTaskExecutor")
    public List<DevicePointYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        String queryDate = devicePageVo.getQueryDate();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        String isAll = devicePageVo.getIsAll();
        QueryWrapper<DevicePointYear> devicePointYearQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(isAll)){
            if(notInPsKey != null && notInPsKey.size()>0){
                devicePointYearQueryWrapper.notIn("ps_key",notInPsKey);
            }
        }

        //需要过滤一下只属于 当前羡慕的项目的电站pskey
        String renterId = devicePageVo.getRenterId();
        List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
        devicePointYearQueryWrapper.in("ps_key", psKeyList);

        if(psKey != null && psKey.size()>0){
            devicePointYearQueryWrapper.in("ps_key",psKey);
        }
        if(StringUtils.isNotBlank(queryDate)){
            LocalDate date = LocalDate.parse(queryDate);
            String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
            devicePointYearQueryWrapper.like("time_stamp",yyyy);
        }
        List<DevicePointYear> list = this.list(devicePointYearQueryWrapper);
        List<DevicePointYearExportVO> devicePointMonthExportVOS = this.changePointDayData(taskId, request, list);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointMonthExportVOS.toArray());
        return devicePointMonthExportVOS;
    }




    List<DevicePointYearExportVO> changePointDayData(String taskId, HttpServletRequest request,List<DevicePointYear> list){
        //阳关云的转换
        List<DevicePointYearExportVO> devicePointYearExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DevicePointYear devicePointDay : list) {
                String psKey = devicePointDay.getPsKey();
                String timeStamp = devicePointDay.getTimeStamp();
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy");
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy");
                String formattedDate = Year.parse(timeStamp, inputFormatter).format(outputFormatter);
                DevicePointYearExportVO devicePointYearExportVO = new DevicePointYearExportVO();
                PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKey);
                if(psInfoByPsKey != null){
                    devicePointYearExportVO.setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                }
                devicePointYearExportVO.setDate(formattedDate);
                devicePointYearExportVO.setPeakPower(psInfoByPsKey.getDesignCapacity());
                devicePointYearExportVO.setTotalPower(psInfoByPsKey.getTotalEnergy()+" "+psInfoByPsKey.getTotalEnergyUnit());
                devicePointYearExportVO.setYearPower(devicePointDay.getP83022());
                devicePointYearExportVOS.add(devicePointYearExportVO);
            }
        }
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointYearExportVOS.toArray());
        return devicePointYearExportVOS;
    }

    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     *
     * @param token
     * @return
     * @throws IOException
     */
    public void fetchSolarDeviceEquipmentMinPointList(List<DateIntervalVo> datesFromStartToYesterday, List<String> psKeyList, String deteType, String queryType, String token) throws IOException {
        Integer haveMonth = this.isHaveMonth();
        if(haveMonth == 1){
            String formatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            this.deleteMonth(formatted);
        }
        for (int j = 0; j < datesFromStartToYesterday.size(); j++) {
            String startDate = datesFromStartToYesterday.get(j).getStartDate();
            String endDate = datesFromStartToYesterday.get(j).getEndDate();
            for (int i = 0; i <psKeyList.size() ; i += BATCH_SIZE) {
                List<String> batchPsKeys = psKeyList.subList(i, Math.min(i + BATCH_SIZE, psKeyList.size()));
                // 1. 创建OkHttpClient实例，并配置超时参数
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
                        .retryOnConnectionFailure(true) // 自动重试连接失败
                        .build();
                // 2. 构建请求参数Map
                Map<String, Object> params = new HashMap<>();
                params.put("appkey", appKey);
                params.put("token", token);
                //日数据
                params.put("data_type", deteType);
                //开始时间
                params.put("start_time", startDate);
                //结束时间
                params.put("end_time", endDate);
                //查询月数据
                params.put("query_type", queryType);
                //排序
                params.put("order", 0);
                //设备key
                params.put("ps_key_list", batchPsKeys);
                //测点id
                params.put("data_point", FIXED_POINT_IDS);
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
                        .url(stationEquipmentsMinuteDMYPoint)
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
                    SolarDeviceEquipmentPointDayDto batchResult = gson.fromJson(body.string(), SolarDeviceEquipmentPointDayDto.class);
                    Map<String, Map<String, List<Map<String, String>>>> resultData = batchResult.getResult_data();
                    if(resultData != null ) {
                        this.batchInsertData(resultData,1,queryType);
                    }
                }
            }
        }
    }



    public void batchInsertData(Map<String, Map<String, List<Map<String, String>>>> resultData, int source,String queryType) {
        if("2".equals(queryType)){

        }
        for (Map.Entry<String, Map<String, List<Map<String, String>>>> entry : resultData.entrySet()) {
            String psKey = entry.getKey();
            Map<String, List<Map<String, String>>> stationData = entry.getValue();

            if (stationData == null) continue;

            List<DevicePointYear> batchList = new ArrayList<>();
            Map<String, DevicePointYear> timestampMap = processStationData(psKey, stationData, source);
            batchList.addAll(timestampMap.values());

            // 批量插入，避免内存溢出
            if (batchList.size() >= 1000) {
                batchInsertToDatabaseYear(batchList,queryType);
                batchList.clear();
            }
            // 插入剩余数据
            if (!batchList.isEmpty()) {
                batchInsertToDatabaseYear(batchList,queryType);
            }
        }


    }
    // 处理单个电站数据
    private Map<String, DevicePointYear> processStationData(String psKey,
                                                            Map<String, List<Map<String, String>>> stationData,
                                                            int source) {
        Map<String, DevicePointYear> timestampMap = new HashMap<>();

        for (String indicator : INDICATORS) {
            List<Map<String, String>> dataList = stationData.get(indicator);
            if (dataList == null) continue;

            for (Map<String, String> dataMap : dataList) {
                String timestamp = dataMap.get(TIMESTAMP_KEY);
                String value = dataMap.get(VALUE_KEY);

                if (timestamp == null || value == null) continue;

                // 获取或创建对应时间戳的记录
                DevicePointYear record = timestampMap.computeIfAbsent(timestamp,
                        k -> createEmptyYearRecord(psKey, timestamp, source));

                // 设置对应指标的值
                setIndicatorYearValue(record, indicator, value);
            }
        }

        return timestampMap;
    }

    private Map<String, DevicePointYear> processStationMonthData(String psKey,
                                                                 Map<String, List<Map<String, String>>> stationData,
                                                                 int source) {
        Map<String, DevicePointYear> timestampMap = new HashMap<>();

        for (String indicator : INDICATORS) {
            List<Map<String, String>> dataList = stationData.get(indicator);
            if (dataList == null) continue;

            for (Map<String, String> dataMap : dataList) {
                String timestamp = dataMap.get(TIMESTAMP_KEY);
                String value = dataMap.get(VALUE_KEY_MONTH);

                if (timestamp == null || value == null) continue;

                // 获取或创建对应时间戳的记录
                DevicePointYear record = timestampMap.computeIfAbsent(timestamp,
                        k -> createEmptyYearRecord(psKey, timestamp, source));

                // 设置对应指标的值
                setIndicatorYearValue(record, indicator, value);
            }
        }

        return timestampMap;
    }

    // 创建空记录

    private DevicePointYear createEmptyYearRecord(String psKey, String timestamp, int source) {
        DevicePointYear record = new DevicePointYear();
        record.setPsKey(psKey);
        record.setTimeStamp(timestamp);
        record.setSource(source);
        return record;
    }



    private void setIndicatorYearValue(DevicePointYear record, String indicator, String value) {
        switch (indicator) {
            case "p83019":
                record.setP83019(value);
                break;
            case "p83022":
                record.setP83022(value);
                break;
            case "p83024":
                record.setP83024(value);
                break;
            case "p83025":
                record.setP83025(value);
            case "p83033":
                record.setP83033(value);
                break;
        }
    }

    /**
     * 数据类型
     * @param devicePointYears
     */
    // 批量插入数据库
    private void batchInsertToDatabaseYear(List<DevicePointYear> devicePointYears, String queryType) {
        // 使用MyBatis批量插入或JDBC批量处理
        this.saveBatch(devicePointYears);
    }
}


