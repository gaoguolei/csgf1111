package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceEquipmentPointMinDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMinFiveGrw;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentPointMinFiveGrwMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinFiveGrwService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceEquipmentPointMinFiveGrwServiceImpl extends ServiceImpl<DeviceEquipmentPointMinFiveGrwMapper, DeviceEquipmentPointMinFiveGrw> implements IDeviceEquipmentPointMinFiveGrwService {

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;

    @Value("${growatt.token}")
    private String growattToken;

    @Value("${growatt.inverterHistory}")
    private String inverterHistoryUrl;


    @Override
    public void insert() {
        List<String> listSn = iDeviceEquipmentService.getListSn("2");
        this.fetchGrowattDeviceEquipmentList(listSn, growattToken, "100", "1");
    }


    /**
     * 古瑞瓦特获获取并插入设备数据
     *
     * @return
     */
    @Transactional
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
//        List<TimePeriod> timePeriods = generateTimePeriods("2025-10-01", "2025-10-31", 7);

        // 3. 遍历设备序列号和时间段
//        for (String deviceSn : listSN) {
//            for (TimePeriod period : timePeriods) {
//                try {
//                    fetchDataForPeriod(client, deviceSn, growattToken, pageSize, pageNum, period);
//                } catch (Exception e) {
//                    // 记录错误但继续处理其他请求
//                    System.err.println("请求失败 - 设备SN: " + deviceSn +
//                            ", 时间段: " + period.getStartDate() + " 至 " + period.getEndDate() +
//                            ", 错误: " + e.getMessage());
//                }
//            }
//        }


        for (String deviceSn : listSN) {
            // 循环11月的每一天（11月有30天）
                // 构建日期字符串，确保日期格式为两位数字
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String format = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 或者直接使用单行代码
            try {
                fetchDataForPeriod(client, deviceSn, growattToken, pageSize, pageNum,
                        new TimePeriod(format, format));
            } catch (Exception e) {
                // 根据需求处理异常，比如记录日志
                // e.printStackTrace();
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
            if (body != null) {
                GrowattDeviceEquipmentPointMinDto growattDeviceEquipmentPointMinDto = gson.fromJson(body.string(), GrowattDeviceEquipmentPointMinDto.class);
                this.saveEquipmentPointMinFiveGrw(growattDeviceEquipmentPointMinDto);
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
        HttpUrl baseUrl = HttpUrl.parse(inverterHistoryUrl);
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid base URL: " + inverterHistoryUrl);
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

    void saveEquipmentPointMinFiveGrw(GrowattDeviceEquipmentPointMinDto growattDeviceEquipmentPointMinDto){
        if(growattDeviceEquipmentPointMinDto != null){
            GrowattDeviceEquipmentPointMinDto.GrwPointMinData data = growattDeviceEquipmentPointMinDto.getData();
            if(data != null){
                String sn = data.getSn();
                List<GrowattDeviceEquipmentPointMinDto.InverterData> datas = data.getDatas();
                if(datas != null && datas.size() > 0){
                    for (int i = 0; i < datas.size(); i++) {
                        String vpv1 = datas.get(i).getVpv1();  //输入电压1路（V）
                        String vpv2 = datas.get(i).getVpv2();
                        String vpv3 = datas.get(i).getVpv3();
                        String vpv4 = datas.get(i).getVpv4();
                        String vpv5 = datas.get(i).getVpv5();
                        String vpv6 = datas.get(i).getVpv6();
                        String vpv7 = datas.get(i).getVpv7();
                        String vpv8 = datas.get(i).getVpv8();

                        String totalEnergy = datas.get(i).getTotal_energy();  //总发电量
                        String powerFactor = datas.get(i).getPower_factor();   //功率因素
                        String fac = datas.get(i).getFac();                 //频率（Hz）
                        String status = String.valueOf(datas.get(i).getStatus());  //	设备状态（0：等待，1：正常，3：故障）
                        String power = datas.get(i).getPower();                     //输出功率（W）
                        String temperature = datas.get(i).getTemperature();         //温度（℃）
                        String todayEnergy = datas.get(i).getToday_energy();     //今日发电量
                        String time = datas.get(i).getTime();                   //数据时间
//                        String currentString1 = datas.get(i).getCurrentString1();
//                        String currentString2 = datas.get(i).getCurrentString2();
//                        String currentString3 = datas.get(i).getCurrentString3();
//                        String currentString4 = datas.get(i).getCurrentString4();
//                        String currentString5 = datas.get(i).getCurrentString5();
//                        String currentString6 = datas.get(i).getCurrentString6();
//                        String currentString7 = datas.get(i).getCurrentString7();
//                        String currentString8 = datas.get(i).getCurrentString8();
//                        String currentString9 = datas.get(i).getCurrentString9();
//                        String currentString10 = datas.get(i).getCurrentString10();
//                        String currentString11 = datas.get(i).getCurrentString11();
//                        String currentString12 = datas.get(i).getCurrentString12();
//                        String currentString13 = datas.get(i).getCurrentString13();
//                        String currentString14 = datas.get(i).getCurrentString14();
//                        String currentString15 = datas.get(i).getCurrentString15();
//                        String currentString16 = datas.get(i).getCurrentString16();
//
//                        String vString1 = datas.get(i).getVString1();
//                        String vString2 = datas.get(i).getVString2();
//                        String vString3 = datas.get(i).getVString3();
//                        String vString4 = datas.get(i).getVString4();
//                        String vString5 = datas.get(i).getVString5();
//                        String vString6 = datas.get(i).getVString6();
//                        String vString7 = datas.get(i).getVString7();
//                        String vString8 = datas.get(i).getVString8();
//                        String vString9 = datas.get(i).getVString9();
//                        String vString10 = datas.get(i).getVString10();
//                        String vString11 = datas.get(i).getVString11();
//                        String vString12 = datas.get(i).getVString12();
//                        String vString13 = datas.get(i).getVString13();
//                        String vString14 = datas.get(i).getVString14();
//                        String vString15 = datas.get(i).getVString15();
//                        String vString16 = datas.get(i).getVString16();

                        String ppv = datas.get(i).getPpv();     //PV输入总功率
                        String ppv1 = datas.get(i).getPpv1();   //PV1输入功率
                        String ppv2 = datas.get(i).getPpv2();
                        String ppv3 = datas.get(i).getPpv3();
                        String ppv4 = datas.get(i).getPpv4();
                        String ppv5 = datas.get(i).getPpv5();
                        String ppv6 = datas.get(i).getPpv6();
                        String ppv7 = datas.get(i).getPpv7();
                        String ppv8 = datas.get(i).getPpv8();

                        String vac1 = datas.get(i).getVac1();     //输出电压1路（V）
                        String vac2 = datas.get(i).getVac2();
                        String vac3 = datas.get(i).getVac3();



                        String ipv1 = datas.get(i).getIpv1();     //输入电流1路（V）
                        String ipv2 = datas.get(i).getIpv2();
                        String ipv3 = datas.get(i).getIpv3();
                        String ipv4 = datas.get(i).getIpv4();
                        String ipv5 = datas.get(i).getIpv5();
                        String ipv6 = datas.get(i).getIpv6();
                        String ipv7 = datas.get(i).getIpv7();
                        String ipv8 = datas.get(i).getIpv8();

                        String iac1 = datas.get(i).getIac1();   //输出电流1路（V）
                        String iac2 = datas.get(i).getIac2();
                        String iac3 = datas.get(i).getIac3();


                        DeviceEquipmentPointMinFiveGrw deviceEquipmentPointMinFive = new DeviceEquipmentPointMinFiveGrw();

                        deviceEquipmentPointMinFive.setP25(ppv1);
                        //当日发电量
                        deviceEquipmentPointMinFive.setP1(todayEnergy);
                        //总发电量
                        deviceEquipmentPointMinFive.setP2(totalEnergy);
                        //数据发生时间
                        deviceEquipmentPointMinFive.setTimeStamp(time);
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
                        deviceEquipmentPointMinFive.setPsKey(sn);
                        deviceEquipmentPointMinFive.setSource("2");
                        this.save(deviceEquipmentPointMinFive);
                    }
                }
            }
        }
    }
}
