package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentPointDayDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMonth;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointYear;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentPointYearMapper;
import okhttp3.*;

import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointYearService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.util.DeviceDataAggregator;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DeviceEquipmentPointYearServiceImpl extends ServiceImpl<DeviceEquipmentPointYearMapper, DeviceEquipmentPointYear> implements IDeviceEquipmentPointYearService {


    private static final int BATCH_SIZE = 50;

    private static final String FIXED_POINT_IDS = "p1,p87,p88,p2,p67,p24,p25,p14,p43,p26,p18,p19,p20,p21,p22,p23,p94,p27,p7356,p3,p15,p16,p17,p95,p90,p120,p4,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p5,p7,p9,p45,p47,p49,p51,p53,p55,p57,p6,p8,p10,p46,p48,p50,p52,p54,p56,p58,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79";

    @Value("${solar.stationEquipmentsMinuteDMYPoint}")
    private String stationEquipmentsMinuteDMYPoint;
    @Value("${solar.appkey}")
    private String appKey;
    @Value("${solar.appsecret}")
    private String appSecret;

    @Autowired
    private DeviceEquipmentPointYearMapper deviceEquipmentPointYearMapper;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IDeviceService iDeviceService;



    @Override
    public void insert() {


        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();

            //调用登录接口获取token，获取下一步的电站列表
            //阳光云获取设备sn集合数据
            List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();

//            ArrayList<String> psKeyList = new ArrayList<>();
//            psKeyList.add("1613131_1_1_1");

            //获取的的日期类型 查询类型： 查询日数据：1 查询月数据：2 查询年数据：3
            String queryType="3";
            // 遍历设备列表
            this.fetchSolarDeviceEquipmentYearPointList(psKeyList, requestLoginToken, queryType);

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
    public void deleteYear(String year) {
        deviceEquipmentPointYearMapper.deleteYear(year);
    }

    @Override
    public Integer isHaveMonth() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        QueryWrapper<DeviceEquipmentPointYear> queryWrapper = new QueryWrapper<>();
        // 假设 time_stamp 格式是 20251020120000，我们匹配前6位
        queryWrapper.likeRight("time_stamp", currentMonth);
        // 使用selectCount查询数量
        Long count = deviceEquipmentPointYearMapper.selectCount(queryWrapper);
        return count != null && count > 0 ? 1 : 0;
    }

    @Override
    public List<DevicePointEquipmentYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {

        //电站的pskey 根据电站的psk获取设备的pskey
        //厂商来源 1：阳光云，2：古瑞瓦特
        //选中
        List<String> psKey = devicePageVo.getPsKey();
        //全选之后的取消选中
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //全选中之后的取消部分选中.
        String queryDate = devicePageVo.getQueryDate();
        String renterId = devicePageVo.getRenterId();
        //是否全选
        String isAll = devicePageVo.getIsAll();
        List<String> equipmentPsKeyList = new ArrayList<>();
        List<String> equipmentNotPsKeyList = new ArrayList<>();

        // 阳光云
        if (psKey != null && psKey.size() > 0) {
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.in("ps_id", psKey);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if (list != null && list.size() > 0) {
                equipmentPsKeyList = list.stream()
                        .map(DeviceEquipment::getPsKey)
                        .collect(Collectors.toList());
            }
        }
        if(StringUtils.isNotBlank(isAll)){
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            List<String> psKeyList = iDeviceService.getPsKeyList(renterId);
            deviceEquipmentQueryWrapper.in("ps_id", psKeyList);
            if (notInPsKey != null && notInPsKey.size() > 0) {
                deviceEquipmentQueryWrapper.notIn("ps_id", notInPsKey);
            }
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if (list != null && list.size() > 0) {
                equipmentNotPsKeyList = list.stream()
                        .map(DeviceEquipment::getPsKey)
                        .collect(Collectors.toList());
            }
        }
        QueryWrapper<DeviceEquipmentPointYear> deviceEquipmentPointYearQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(isAll)) {
            deviceEquipmentPointYearQueryWrapper.in("ps_key", equipmentNotPsKeyList);
        }

        if (psKey != null && psKey.size() > 0) {
            deviceEquipmentPointYearQueryWrapper.in("ps_key", equipmentPsKeyList);
        }

        if (StringUtils.isNotBlank(queryDate)) {
            LocalDate date = LocalDate.parse(queryDate);
            String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            deviceEquipmentPointYearQueryWrapper.eq("time_stamp", yyyyMM);
        }
        List<DeviceEquipmentPointYear> list = this.list(deviceEquipmentPointYearQueryWrapper);
        List<DevicePointEquipmentYearExportVO> devicePointEquipmentDayExportVOS = this.changePointDayData(list);
        redisTemplate.opsForList().leftPushAll("Task:" + taskId, devicePointEquipmentDayExportVOS.toArray());
        return devicePointEquipmentDayExportVOS;
    }

    List<DevicePointEquipmentYearExportVO> changePointDayData(List<DeviceEquipmentPointYear> list){
        //阳关云的转换
        List<DevicePointEquipmentYearExportVO> devicePointDayExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DeviceEquipmentPointYear deviceEquipmentPointYear : list) {
                String psKey = deviceEquipmentPointYear.getPsKey();
                PsInfoVo deviceInfo = iDeviceEquipmentService.getDeviceInfo(psKey);
                String timeStamp = deviceEquipmentPointYear.getTimeStamp();
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy");
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy");
                String formattedDate = Year.parse(timeStamp, inputFormatter).format(outputFormatter);
                DevicePointEquipmentYearExportVO devicePointEquipmentDayExportVO = new DevicePointEquipmentYearExportVO();
                //
                if(deviceInfo != null){
                    devicePointEquipmentDayExportVO.setPsName(StringUtils.isNotBlank(deviceInfo.getPsName()) ? deviceInfo.getPsName() : "");
                    devicePointEquipmentDayExportVO.setPeakPower(StringUtils.isNotBlank(deviceInfo.getDesignCapacity()) ? deviceInfo.getDesignCapacity() : "");
                }
                devicePointEquipmentDayExportVO.setDate(formattedDate);
//                devicePointEquipmentDayExportVO.setDayPower(devicePointDay.getP1());
                devicePointEquipmentDayExportVO.setTotalPower(new BigDecimal(deviceEquipmentPointYear.getP2()).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString());
                devicePointEquipmentDayExportVO.setEquipmentSn(StringUtils.isNotBlank(deviceInfo.getDeviceSn()) ? deviceInfo.getDeviceSn() : "");
                devicePointEquipmentDayExportVO.setEquipmentModelSn(StringUtils.isNotBlank(deviceInfo.getDeviceModelCode()) ? deviceInfo.getDeviceModelCode() : "");
                String status="";
                if(StringUtils.isNotBlank(deviceInfo.getDeviceStatus())){
                    if(deviceInfo.getDeviceStatus().equals("1")){
                        status="在线";
                    }
                    if(!deviceInfo.getDeviceStatus().equals("1")){
                        status="离线";
                    }
                }
                devicePointEquipmentDayExportVO.setEquipmentStatus(status);
                devicePointEquipmentDayExportVO.setMessageSn(StringUtils.isNotBlank(deviceInfo.getCommunicationDevSn()) ? deviceInfo.getCommunicationDevSn() : "");
                devicePointDayExportVOS.add(devicePointEquipmentDayExportVO);
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
    public void fetchSolarDeviceEquipmentYearPointList(List<String> psKeyList, String token, String queryType) throws IOException {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        String formatted = today.format(formatter);
        //先删除当月的数据，再插入
        Integer haveMonth = this.isHaveMonth();
        if(haveMonth == 1)
        {
            this.deleteYear(formatted);
        }
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
            //1：均值，2：峰值，3：谷值，4：合计值（日维度的数据无合计值，月年维度的数据才有合计值） 多个使用逗号隔开。
            // 如果是可以进行合计的数据，则： 查询日数据的时候，data_type为2， 查询月数据的时候，data_type为4， 查询年数据的时候，data_type为4
            params.put("data_type", "2");
            //开始时间
            params.put("start_time", formatted);
            //结束时间
            params.put("end_time", formatted);
            //查询类型： 查询日数据：1 查询月数据：2 查询年数据：3
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
                if (body != null) {
                    SolarDeviceEquipmentPointDayDto batchResult = gson.fromJson(body.string(), SolarDeviceEquipmentPointDayDto.class);
                    // 8. 解析并合并结果（需要根据实际响应结构实现）
                    Map<String, Map<String, List<Map<String, String>>>> resultData = batchResult.getResult_data();
                    DeviceDataAggregator deviceDataAggregator = new DeviceDataAggregator();
                    List<DeviceEquipmentPointYear> deviceEquipmentPointYears = deviceDataAggregator.convertToDatabaseYearRecords(resultData, "1");
                    if(deviceEquipmentPointYears != null && deviceEquipmentPointYears.size() > 0){
                        this.saveBatch(deviceEquipmentPointYears);
                    }
                }
            }
        }
    }
}
