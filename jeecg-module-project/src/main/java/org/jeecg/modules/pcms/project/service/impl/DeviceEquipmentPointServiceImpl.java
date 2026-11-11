package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceEquipmentPointMinDto;
import org.jeecg.modules.pcms.energy.dto.growatt.InverterData;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPoint;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentPointMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceEquipmentPointServiceImpl extends ServiceImpl<DeviceEquipmentPointMapper, DeviceEquipmentPoint> implements IDeviceEquipmentPointService {

    private static final int BATCH_SIZE = 50;

    private static final List<String> FIXED_POINT_IDS = Arrays.asList(
            "1", "87", "88", "2", "67", "24", "25", "14", "43", "26",
            "18", "19", "20", "21", "22", "23", "94", "27", "7356", "3",
            "15", "16", "17", "95", "90", "120", "4", "33", "34", "35",
            "36", "37", "38", "39", "40", "41", "42", "5", "7", "9",
            "45", "47", "49", "51", "53", "55", "57", "6", "8",
            "10", "46", "48", "50", "52", "54", "56", "58", "96", "97", "98",
            "99", "100", "101", "102", "103", "104", "105", "70", "71", "72", "73", "74", "75", "76", "77",
            "78", "79","23001","23014","23008"
    );



    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationFaultEquipmentsPoint}")
    private String stationFaultEquipmentsPoint;

    @Value("${growatt.inverterHistory}")
    private String inverterHistoryUrl;

    @Value("${growatt.token}")
    private String growattToken;


    @Value("${growatt.inverterNew}")
    private String inverterNewUrl;

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
    private DeviceEquipmentPointMapper deviceEquipmentPointMapper;


    @Override
    public DeviceStatsVo getPointDataInPsKey(List<String> psKeyList) {
        QueryWrapper<DeviceEquipmentPoint> deviceEquipmentPointQueryWrapper = new QueryWrapper<>();
        deviceEquipmentPointQueryWrapper.in("ps_key", psKeyList);
        List<DeviceEquipmentPoint> list = this.list(deviceEquipmentPointQueryWrapper);
        DeviceStatsVo deviceStatsVo = new DeviceStatsVo();
        BigDecimal monthDecimal = new BigDecimal(0.00);
        BigDecimal yearDecimal = new BigDecimal(0.00);
        for (DeviceEquipmentPoint point : list) {
            // 月发电量（注意处理空值和非数字的情况）
            String p87 = point.getP87();
            if (p87 != null && !p87.isEmpty()) {
                monthDecimal = monthDecimal.add(new BigDecimal(p87));
            }
            // 年发电量
            String p88 = point.getP88();
            if (p88 != null && !p88.isEmpty()) {
                yearDecimal = yearDecimal.add(new BigDecimal(p88));
            }
            deviceStatsVo.setMonthlyEnergy(monthDecimal.toString());
            deviceStatsVo.setYearlyEnergy(yearDecimal.toString());
            return deviceStatsVo;
        }
        return new DeviceStatsVo();
    }

    @Override
    public void deleteBySource(String source) {
        deviceEquipmentPointMapper.deleteBySource(source);
    }


    @Override
    public DeviceEquipmentPoint getDeviceEquipmentByPsKey(String psKey) {
        QueryWrapper<DeviceEquipmentPoint> deviceGwQueryWrapper = new QueryWrapper<>();
        deviceGwQueryWrapper.eq("ps_key",psKey);
        DeviceEquipmentPoint one = this.getOne(deviceGwQueryWrapper);
        if(one != null){
            return one;
        }
        return null;
    }


    /**
     * 定时任务插入
     */
    @Override
    @Transactional
    public void insert() {
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();

            //阳光云获取设备sn集合数据
            List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();
            this.deleteBySource("1");
            this.fetchSolarDeviceEquipmentPointList(psKeyList, requestLoginToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertGrw() {
        //查古瑞瓦特数据
        //1、获取所有电站id集合
        //2、传参到古瑞瓦特接口获取所有设备数据
        List<String> listSn = iDeviceEquipmentService.getListSn("2");
//        获取所有设备列表集合
        this.deleteBySource("2");
        this.fetchGrowattDeviceEquipmentList(listSn, growattToken, "100", "1");
    }

    @Override
    public List<DeviceEquipmentPoint> getDeviceEquipmentByPsKeys(Set<String> psKeys) {
        QueryWrapper<DeviceEquipmentPoint> deviceEquipmentPointQueryWrapper = new QueryWrapper<>();
        deviceEquipmentPointQueryWrapper.in("ps_key",psKeys);
        List<DeviceEquipmentPoint> list = this.list(deviceEquipmentPointQueryWrapper);
        return list;
    }


    /**
     * 通过s阳光云登录接口获取token，方便下一步调用接口使用
     *
     * @param token
     * @return
     * @throws IOException
     */
    public SolarDeviceEquipmentDto fetchSolarDeviceEquipmentPointList (List < String > psKeyList, String token) throws
            IOException {
        for (int i = 0; i < psKeyList.size(); i += BATCH_SIZE) {
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
                if (solarDeviceEquipmentPointDto != null) {
                    SolarDeviceEquipmentPointDto.ResultData resultData = solarDeviceEquipmentPointDto.getResultData();
                    if (resultData != null) {
                        List<SolarDeviceEquipmentPointDto.DevicePointList> devicePointList = resultData.getDevicePointList();
                        if (devicePointList != null && devicePointList.size() > 0) {
                            for (int j = 0; j < devicePointList.size(); j++) {
                                DeviceEquipmentPoint deviceEquipmentPoint = new DeviceEquipmentPoint();
                                BeanUtils.copyProperties(devicePointList.get(j).getDevicePoint(), deviceEquipmentPoint);
                                deviceEquipmentPoint.setSource("1");
                                this.save(deviceEquipmentPoint);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }



    /**
     * 古瑞瓦特获获取并插入设备数据
     *
     * @return
     */
    public void fetchGrowattDeviceEquipmentList(List<String> listSN, String growattToken, String pageSize, String pageNum) {
        if (listSN == null || listSN.isEmpty()) {
            return;
        }

        // 1. 创建并重用OkHttpClient实例
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池
                .retryOnConnectionFailure(true) // 自动重试连接失败
                .build();

        // 2. 生成时间分段
//        List<TimePeriod> timePeriods = generateTimePeriods("2025-09-01", "2025-10-09", 7);

        TimePeriod timePeriod = new TimePeriod("2025-09-01", "2025-10-09");
        // 3. 遍历设备序列号和时间段
        for (String deviceSn : listSN) {
                try {
                    fetchDataForPeriod(client, deviceSn, growattToken, pageSize, pageNum, timePeriod);
                } catch (Exception e) {
                    // 记录错误但继续处理其他请求
                    System.err.println("请求失败 - 设备SN: " + deviceSn +
                            ", 时间段: " + timePeriod.getStartDate() + " 至 " + timePeriod.getEndDate() +
                            ", 错误: " + e.getMessage());
                }
        }
    }



    /**
     * 生成时间分段
     */
    private List<TimePeriod> generateTimePeriods(String startDateStr, String endDateStr, int dayInterval) {
        List<TimePeriod> periods = new ArrayList<>();

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(startDateStr, formatter);
            LocalDate endDate = LocalDate.parse(endDateStr, formatter);

            LocalDate currentStart = startDate;

            while (currentStart.isBefore(endDate)) {
                LocalDate currentEnd = currentStart.plusDays(dayInterval - 1);

                // 确保结束日期不超过总结束日期
                if (currentEnd.isAfter(endDate)) {
                    currentEnd = endDate;
                }

                periods.add(new TimePeriod(
                        currentStart.format(formatter),
                        currentEnd.format(formatter)
                ));

                currentStart = currentEnd.plusDays(1);

                // 如果下一个开始日期已经超过结束日期，则退出循环
                if (currentStart.isAfter(endDate)) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("时间参数解析错误", e);
        }

        return periods;
    }

    /**
     * 为特定时间段获取数据
     */
    private void fetchDataForPeriod(OkHttpClient client, String deviceSn, String growattToken,
                                    String pageSize, String pageNum, TimePeriod period) throws IOException {
        // 构建请求参数
        Map<String, String> params = buildRequestParams(deviceSn, pageSize, pageNum, period);

        // 构建URL
        HttpUrl url = buildUrlWithParams(params);

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", growattToken)
                .get()
                .build();

        Gson gson = new Gson();
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message() +
                        ", 设备: " + deviceSn + ", 时间段: " + period.getStartDate() + "至" + period.getEndDate());
            }

            ResponseBody body = response.body();
            InverterData inverterData = gson.fromJson(body.string(), InverterData.class);
            if(inverterData != null){
                this.saveEquipmentPointGrw(inverterData);
            }
        }
    }


    /**
     * 构建请求参数
     */
    private Map<String, String> buildRequestParams(String deviceSn, String pageSize,
                                                   String pageNum, TimePeriod period) {
        Map<String, String> params = new HashMap<>();
        int random = ThreadLocalRandom.current().nextInt(1, 11); // 1-10的随机数

        params.put("search_type", String.valueOf(random));
        params.put("page", pageNum);
        params.put("perpage", pageSize);
        params.put("device_sn", deviceSn);
        params.put("start_date", period.getStartDate());
        params.put("end_date", period.getEndDate());
        return params;
    }

    /**
     * 构建带参数的URL
     */
    private HttpUrl buildUrlWithParams(Map<String, String> params) {
        HttpUrl baseUrl = HttpUrl.parse(inverterNewUrl);
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid base URL: " + inverterNewUrl);
        }

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        return urlBuilder.build();
    }

    /**
     * 时间段封装类
     */
    private static class TimePeriod {
        private final String startDate;
        private final String endDate;

        public TimePeriod(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }
    }

    void saveEquipmentPointGrw(InverterData inverterData){
        if(inverterData != null){
            InverterData.DataDTO data = inverterData.getData();
            if(data != null){
                String sn = inverterData.getDevice_sn();
                String vpv1 = data.getVpv1();  //输入电压1路（V）
                String vpv2 = data.getVpv2();
                String vpv3 = data.getVpv3();
                String vpv4 = data.getVpv4();
                String vpv5 = data.getVpv5();
                String vpv6 = data.getVpv6();
                String vpv7 = data.getVpv7();
                String vpv8 = data.getVpv8();

                String totalEnergy = data.getPowerTotal();  //总发电量
                String powerFactor = data.getPf();   //功率因素
                String fac = data.getFac();                 //频率（Hz）
                String status = String.valueOf(data.getStatus());  //	设备状态（0：等待，1：正常，3：故障）
                String power = data.getPac();                     //输出功率（W）
                String temperature = data.getTemperature();         //温度（℃）
                String todayEnergy = data.getPowerToday();     //今日发电量
                String time = data.getTime();                   //数据时间

//                        String currentString1 = data.getCurrentString1();
//                        String currentString2 = data.getCurrentString2();
//                        String currentString3 = data.getCurrentString3();
//                        String currentString4 = data.getCurrentString4();
//                        String currentString5 = data.getCurrentString5();
//                        String currentString6 = data.getCurrentString6();
//                        String currentString7 = data.getCurrentString7();
//                        String currentString8 = data.getCurrentString8();
//                        String currentString9 = data.getCurrentString9();
//                        String currentString10 = data.getCurrentString10();
//                        String currentString11 = data.getCurrentString11();
//                        String currentString12 = data.getCurrentString12();
//                        String currentString13 = data.getCurrentString13();
//                        String currentString14 = data.getCurrentString14();
//                        String currentString15 = data.getCurrentString15();
//                        String currentString16 = data.getCurrentString16();
//
//                        String vString1 = data.getVString1();
//                        String vString2 = data.getVString2();
//                        String vString3 = data.getVString3();
//                        String vString4 = data.getVString4();
//                        String vString5 = data.getVString5();
//                        String vString6 = data.getVString6();
//                        String vString7 = data.getVString7();
//                        String vString8 = data.getVString8();
//                        String vString9 = data.getVString9();
//                        String vString10 = data.getVString10();
//                        String vString11 = data.getVString11();
//                        String vString12 = data.getVString12();
//                        String vString13 = data.getVString13();
//                        String vString14 = data.getVString14();
//                        String vString15 = data.getVString15();
//                        String vString16 = data.getVString16();
                String ppv = data.getPpv();     //PV输入总功率
                String ppv1 = data.getPpv1();   //PV1输入功率
                String ppv2 = data.getPpv2();
                String ppv3 = data.getPpv3();
                String ppv4 = data.getPpv4();
                String ppv5 = data.getPpv5();
                String ppv6 = data.getPpv6();
                String ppv7 = data.getPpv7();
                String ppv8 = data.getPpv8();

                String vac1 = data.getVacr();     //输出电压1路（V）
                String vac2 = data.getVacs();
                String vac3 = data.getVact();



                String ipv1 = data.getIpv1();     //输入电流1路（V）
                String ipv2 = data.getIpv2();
                String ipv3 = data.getIpv3();
                String ipv4 = data.getIpv4();
                String ipv5 = data.getIpv5();
                String ipv6 = data.getIpv6();
                String ipv7 = data.getIpv7();
                String ipv8 = data.getIpv8();

                String iac1 = data.getIacr();   //输出电流1路（V）
                String iac2 = data.getIacs();
                String iac3 = data.getIact();


                DeviceEquipmentPoint deviceEquipmentPointMinFive = new DeviceEquipmentPoint();

                deviceEquipmentPointMinFive.setP25(ppv1);
                //当日发电量
                deviceEquipmentPointMinFive.setP1(todayEnergy);
                //总发电量
                deviceEquipmentPointMinFive.setP2(totalEnergy);
                //温度
                deviceEquipmentPointMinFive.setP4(temperature);
                //总功率
                deviceEquipmentPointMinFive.setP24(power);

                deviceEquipmentPointMinFive.setP96(vpv1);
                deviceEquipmentPointMinFive.setP97(vpv2);
                deviceEquipmentPointMinFive.setP98(vpv3);
                deviceEquipmentPointMinFive.setP99(vpv4);
                deviceEquipmentPointMinFive.setP100(vpv5);
                deviceEquipmentPointMinFive.setP101(vpv6);
                deviceEquipmentPointMinFive.setP102(vpv7);
                deviceEquipmentPointMinFive.setP103(vpv8);

                deviceEquipmentPointMinFive.setP70(ipv1);
                deviceEquipmentPointMinFive.setP71(ipv2);
                deviceEquipmentPointMinFive.setP72(ipv3);
                deviceEquipmentPointMinFive.setP73(ipv4);
                deviceEquipmentPointMinFive.setP74(ipv5);
                deviceEquipmentPointMinFive.setP75(ipv6);
                deviceEquipmentPointMinFive.setP76(ipv7);
                deviceEquipmentPointMinFive.setP77(ipv8);
                //设备sn
                deviceEquipmentPointMinFive.setDeviceSn(sn);
                deviceEquipmentPointMinFive.setSource("2");
                this.save(deviceEquipmentPointMinFive);
            }
        }
    }

}
