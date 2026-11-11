package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceFaultDto;
import org.jeecg.modules.pcms.energy.entity.DeviceFaultHistory;
import org.jeecg.modules.pcms.project.mapper.DeviceFaultHistoryMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceFaultHistoryService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class DeviceFaultHistoryServiceImpl extends ServiceImpl<DeviceFaultHistoryMapper, DeviceFaultHistory> implements IDeviceFaultHistoryService {





    @Value("${solar.login}")
    private String loginUrl;
    @Value("${solar.stationFault}")
    private String stationFault;

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


    @Override
    public void insert() {
        try {
            String requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();
            String[] yesterdayTimeRange = DateUtils.getYesterdayTimeRange();
            //写一个方法把投运时间到昨天的时间整成一个集合传入到方法里面
            //阳光云获取电站列表的数据
            for (int i = 0; i < psKeyList.size(); i++) {
                String s = psKeyList.get(i);
                List<SolarDeviceFaultDto.PageListItem> pageItems = this.fetchSolarDeviceFaultList(s,yesterdayTimeRange,requestLoginToken);
                this.saveDeviceFaultAll(pageItems);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void saveDeviceFaultAll(List<SolarDeviceFaultDto.PageListItem> pageItems) {
        //存入光伏云来源的数据
        if(pageItems != null && pageItems.size()>0){
            for (int i = 0; i < pageItems.size(); i++) {
                SolarDeviceFaultDto.PageListItem pageListItem = pageItems.get(i);
                DeviceFaultHistory deviceFault = new DeviceFaultHistory();
                Integer uuid = pageListItem.getUuid();
                BeanUtils.copyProperties(pageListItem,deviceFault);
                deviceFault.setSource(1);
                deviceFault.setUuid(String.valueOf(uuid));
                this.save(deviceFault);
            }
        }
    }





    /***
     *  获取阳光云的故障全部数据(查询昨天到设备运行的时候这段时间内的所有故障历史记录)
     * @param token
     * @return
     * @throws IOException
     */
    public List<SolarDeviceFaultDto.PageListItem> fetchSolarDeviceFaultList(String psKey, String[] dates, String token) throws IOException {
        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceFaultDto.PageListItem> allData = new ArrayList<>();

        // 发送第一页请求
        List<SolarDeviceFaultDto> firstPageResponse = this.sendDeviceListSolarRequest(psKey, dates, stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));

        // 检查响应是否为空或没有数据
        if (firstPageResponse == null || firstPageResponse.isEmpty()) {
            return allData;
        }

        // 获取第一个响应对象（假设列表中第一个元素包含分页信息）
        SolarDeviceFaultDto firstData = firstPageResponse.get(0);

        // 添加第一页数据
        if (firstData.getResultData() != null && firstData.getResultData().getPageList() != null) {
            allData.addAll(firstData.getResultData().getPageList());
        }

        if(firstData.getResultData() != null && firstData.getResultData().getPageList() != null){
            // 总数据条数
            int rowCount = firstData.getResultData().getRowCount();

            // 计算总页数
            int totalPages = (int) Math.ceil((double) rowCount / pageSize);

            // 循环获取剩余页的数据
            for (currentPage = 2; currentPage <= totalPages; currentPage++) {
                List<SolarDeviceFaultDto> pageResponse = this.sendDeviceListSolarRequest(psKey, dates, stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));

                if (pageResponse != null && !pageResponse.isEmpty()) {
                    SolarDeviceFaultDto pageData = pageResponse.get(0);
                    if (pageData.getResultData() != null && pageData.getResultData().getPageList() != null) {
                        allData.addAll(pageData.getResultData().getPageList());
                    }
                }

                // 添加适当的延迟，避免请求过于频繁
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("请求被中断", e);
                }
            }
        }
        return allData;
    }


    public  List<String> generateDatesInYearUntilSeptember10(int year) {
        List<String> dateList = new ArrayList<>();

        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 开始日期：当年1月1日
        LocalDate startDate = LocalDate.of(year, 1, 1);

        // 结束日期：当年9月10日
        LocalDate endDate = LocalDate.of(year, 9, 15);

        // 遍历从开始日期到结束日期的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // 格式化日期并添加到列表
            dateList.add(currentDate.format(formatter));
            // 增加一天
            currentDate = currentDate.plusDays(1);
        }

        return dateList;
    }
    /**
     * 古瑞瓦特通过设备sn码来获取设备故障信息存入数据库
     *
     * @return
     */

    /**
     * 阳光云获取设备告警数据，方便下一步调用接口使用
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
    public List<SolarDeviceFaultDto> sendDeviceListSolarRequest(String psKey,String [] dates,String stationFault, String token,
                                                                String appKey, String appSecret, String pageSize, String pageNum) throws IOException {
        //获取设备
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
        params.put("share_type", "0,1,2");
        params.put("process_status", "9");
        params.put("fault_type", "1,2,3,4");
        params.put("fault_level", "1,2,3,4");
//        params.put("ps_key", psKey);
        params.put("startTime", dates[0]);
        params.put("endTime", dates[1]);
        List<SolarDeviceFaultDto> solarDeviceFaultDtos = new ArrayList<>();
        //循环传参

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
            SolarDeviceFaultDto solarDeviceFaultDto = JSONObject.parseObject(body.string(), SolarDeviceFaultDto.class);
            solarDeviceFaultDtos.add(solarDeviceFaultDto);
        }

        return solarDeviceFaultDtos;

    }











}
