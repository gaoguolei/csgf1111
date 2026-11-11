package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattEnergyResponseDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointDayDto;
import org.jeecg.modules.pcms.energy.entity.DevicePointDay;
import org.jeecg.modules.pcms.energy.entity.DevicePointMinSixty;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointDayMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailSolarService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDevicePointDayService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;
import org.jeecg.modules.pcms.project.util.DateFormatterUtils;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DevicePointDayServiceImpl  extends ServiceImpl<DevicePointDayMapper, DevicePointDay> implements IDevicePointDayService {

    private static final int BATCH_SIZE = 50;

    private static final String FIXED_POINT_IDS = "p83019,p83022,p83024,p83025,p83033";

    private static final String[] INDICATORS = {"p83019", "p83022", "p83024", "p83025","p83033"};
    private static final String VALUE_KEY = "2";
    private static final String TIMESTAMP_KEY = "time_stamp";

    @Value("${solar.stationEquipmentsMinuteDMYPoint}")
    private String stationEquipmentsMinuteDMYPoint;

    @Value("${growatt.token}")
    private String growattToken;

    @Value("${growatt.historyEnergy}")
    private String historyEnergy;

    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private IDeviceDetailSolarService iDeviceDetailSolarService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DevicePointDayMapper devicePointDayMapper;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;

    @Override
    public void insert() {
        //调用登录接口获取token，获取下一步的电站列表
        //阳光云获取设备sn集合数据
        List<String> psKey = iDeviceDetailSolarService.getDevicePsKey();

        //从这个投运时间开始 到昨天的数据
        //获取昨日数据
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formatted = yesterday.format(formatter);

        List<DateIntervalVo> dateIntervalVos = DateUtils.splitTimeSegments(formatted, formatted);

        String dateType = "2";
        String queryType = "1";
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
//             遍历设备列表
            this.fetchSolarDeviceEquipmentMinPointList(dateIntervalVos, psKey, dateType, queryType, requestLoginToken);
            //查古瑞瓦特数据
            //1、获取所有电站id集合
            //2、传参到古瑞瓦特接口获取所有电站的id集合
            List<Integer> listId = iDeviceService.getListId();
            //获取所有设备列表集合
//            this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1",dateIntervalVos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String formatDate(String date) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.parse(date, inputFormatter);
        return outputFormatter.format(localDate);
    }


    @Override
    @Async("exportTaskExecutor")
    public List<DevicePointDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        //日，月，自定义报表
        List<String> psKey = devicePageVo.getPsKey();
        String queryDate = devicePageVo.getQueryDate();
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String minType = devicePageVo.getMinType();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //是否全选
        String isAll = devicePageVo.getIsAll();

        QueryWrapper<DevicePointDay> devicePointDayQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(isAll)){
            if(notInPsKey != null && notInPsKey.size()>0){
                devicePointDayQueryWrapper.notIn("ps_key",notInPsKey);
            }
        }
        //需要过滤一下只属于 当前羡慕的项目的电站pskey
        String renterId = devicePageVo.getRenterId();
        List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
        devicePointDayQueryWrapper.in("ps_key", psKeyList);

        if(psKey != null && psKey.size()>0){
            devicePointDayQueryWrapper.in("ps_key",psKey);
        }
        if("day".equals(minType)){
            if(StringUtils.isNotBlank(queryDate)){
                String s = this.formatDate(queryDate);
                devicePointDayQueryWrapper.eq("time_stamp",s);
            }
        }
        //自定义
        if("customize".equals(minType)){
            if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                String start = this.formatDate(startDate);
                String end = this.formatDate(endDate);
                devicePointDayQueryWrapper.between("time_stamp",start,end);
            }
        }
        //月
        if("month".equals(minType)){
            LocalDate date = LocalDate.parse(queryDate);
            String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            devicePointDayQueryWrapper.like("time_stamp",yyyyMM);
        }
        List<DevicePointDay> list = this.list(devicePointDayQueryWrapper);
        //转换一下easyExcel能导出的格式
        List<DevicePointDayExportVO> devicePointDayExportVOS = this.changePointDayData(taskId, request, list);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointDayExportVOS.toArray());
        return devicePointDayExportVOS;
    }

    @Override
    public List<StationCountVo> weeks(String renterId) {
        LocalDate currentDate = LocalDate.now();
        // 创建日期格式化对象
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 创建日期列表
        ArrayList<StationCountVo> stationCountVos = new ArrayList<>();
        // 循环获取最近7天的日期
        //根据项目id获取属于该项目下的所有设备 再送到周星期里面进行查询
        List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
        List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
        for (int i = 0; i < 7; i++) {
            StationCountVo stationCountVo = new StationCountVo();
            LocalDate targetDate = currentDate.minusDays(i);
            // 将日期格式化为字符串并添加到日期列表中
            // 格式1: 20251010
            String format1 = targetDate.format(formatter1);
            // 格式2: 2025-10-10
            String format2 = targetDate.format(formatter2);
            LocalDate date = LocalDate.parse(format2);

            int day = date.getDayOfMonth();
            BigDecimal week = devicePointDayMapper.getWeek(format1, format2,psKeyList,psKeyListGrw).divide(new BigDecimal(1000)).setScale(2, RoundingMode.UP);

            stationCountVo.setKey(day);
            stationCountVo.setPower(week);
            stationCountVo.setIndex(i);
            if(week !=null){
                stationCountVo.setIncome(week.multiply(new BigDecimal(0.95).setScale(2, RoundingMode.UP)));
            }
            stationCountVos.add(stationCountVo);
        }
        // 输出日期列表
        return stationCountVos;
    }

    List<DevicePointDayExportVO> changePointDayData(String taskId, HttpServletRequest request,List<DevicePointDay> list){
        //阳关云的转换
        List<DevicePointDayExportVO> devicePointDayExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DevicePointDay devicePointDay : list) {
                String psKey = devicePointDay.getPsKey();
                String timeStamp = devicePointDay.getTimeStamp();
                String formattedDate = LocalDate.parse(timeStamp, DateTimeFormatter.BASIC_ISO_DATE)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                DevicePointDayExportVO devicePointDayExportVO = new DevicePointDayExportVO();
                PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKey(psKey);
                if(psInfoByPsKey != null){
                    devicePointDayExportVO.setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                }
                devicePointDayExportVO.setDate(formattedDate);
                devicePointDayExportVO.setDayPower(new BigDecimal(devicePointDay.getP83022()).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString());
                devicePointDayExportVO.setPeakPower(psInfoByPsKey.getDesignCapacity());
                devicePointDayExportVO.setTotalPower(psInfoByPsKey.getTotalEnergy()+" "+psInfoByPsKey.getTotalEnergyUnit());
                devicePointDayExportVOS.add(devicePointDayExportVO);
            }
        }
        return devicePointDayExportVOS;

    }



    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     *
     * @param token
     * @return
     * @throws IOException
     */
    public SolarDeviceEquipmentDto fetchSolarDeviceEquipmentMinPointList(List<DateIntervalVo> datesFromStartToYesterday, List<String> psKeyList, String deteType, String queryType, String token) throws IOException {
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
                //查询日数据
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
        return null;
    }

    public void batchInsertData(Map<String, Map<String, List<Map<String, String>>>> resultData, int source,String queryType) {
        for (Map.Entry<String, Map<String, List<Map<String, String>>>> entry : resultData.entrySet()) {
            String psKey = entry.getKey();
            Map<String, List<Map<String, String>>> stationData = entry.getValue();

            if (stationData == null) continue;

            // 按时间戳分组数据
            List<DevicePointDay> batchList = new ArrayList<>();
            Map<String, DevicePointDay> timestampMap = processStationData(psKey, stationData, source);
            batchList.addAll(timestampMap.values());

            // 批量插入，避免内存溢出
            if (batchList.size() >= 1000) {
                batchInsertToDatabase(batchList,queryType);
                batchList.clear();
            }
            // 插入剩余数据
            if (!batchList.isEmpty()) {
                batchInsertToDatabase(batchList,queryType);
            }
        }
    }

    // 处理单个电站数据
    private Map<String, DevicePointDay> processStationData(String psKey,
                                                           Map<String, List<Map<String, String>>> stationData,
                                                           int source) {
        Map<String, DevicePointDay> timestampMap = new HashMap<>();

        for (String indicator : INDICATORS) {
            List<Map<String, String>> dataList = stationData.get(indicator);
            if (dataList == null) continue;

            for (Map<String, String> dataMap : dataList) {
                String timestamp = dataMap.get(TIMESTAMP_KEY);
                String value = dataMap.get(VALUE_KEY);

                if (timestamp == null || value == null) continue;

                // 获取或创建对应时间戳的记录
                DevicePointDay record = timestampMap.computeIfAbsent(timestamp,
                        k -> createEmptyRecord(psKey, timestamp, source));

                // 设置对应指标的值
                setIndicatorValue(record, indicator, value);
            }
        }

        return timestampMap;
    }


    // 创建空记录
    private DevicePointDay createEmptyRecord(String psKey, String timestamp, int source) {
        DevicePointDay record = new DevicePointDay();
        record.setPsKey(psKey);
        record.setTimeStamp(timestamp);
        record.setSource(source);
        return record;
    }



    // 设置指标值
    private void setIndicatorValue(DevicePointDay record, String indicator, String value) {
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
     * @param devicePointDays
     */
    // 批量插入数据库
    private void batchInsertToDatabase(List<DevicePointDay> devicePointDays,String queryType) {
        // 使用MyBatis批量插入或JDBC批量处理
        this.saveBatch(devicePointDays);
    }


    /**
     * 古瑞瓦特获获取并插入设备数据
     *
     * @return
     */
    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum,List<DateIntervalVo> dateIntervalVos) {
        if(dateIntervalVos != null && dateIntervalVos.size()>0){
            for (int k = 0; k < dateIntervalVos.size(); k++){
                if(listId != null && listId.size() > 0){
                    for (int i = 0; i < listId.size(); i++) {
                        //发电量的
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
                        params.put("time_unit","day");
                        params.put("start_date", dateIntervalVos.get(k).getStartDate());
                        params.put("end_date",dateIntervalVos.get(k).getEndDate());

                        // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                        HttpUrl baseUrl = HttpUrl.parse(historyEnergy);
                        if (baseUrl == null) {
                            throw new IllegalArgumentException("Invalid base URL: " + historyEnergy);
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
                            Gson gson = new Gson();
                            GrowattEnergyResponseDto batchResult = gson.fromJson(body.string(), GrowattEnergyResponseDto.class);
                            GrowattEnergyResponseDto.ReturnData data = batchResult.getData();
                            GrowattEnergyResponseDto.EnergyItem[] energys = data.getEnergys();
                            if(energys != null && energys.length > 0){
                                for (int j = 0; j < energys.length; j++) {
                                    GrowattEnergyResponseDto.EnergyItem energy = energys[j];
                                    DevicePointDay devicePointDay = new DevicePointDay();
                                    devicePointDay.setPsKey(listId.get(i).toString());
                                    devicePointDay.setP83022(energy.getEnergy());
                                    devicePointDay.setSource(2);
                                    this.save(devicePointDay);
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

    }
}
