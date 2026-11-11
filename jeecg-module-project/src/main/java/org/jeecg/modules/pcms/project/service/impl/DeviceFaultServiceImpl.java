package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceFaultDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentDispatch;
import org.jeecg.modules.pcms.energy.entity.DeviceFault;
import org.jeecg.modules.pcms.energy.entity.DeviceFaultHistory;
import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentMapper;
import org.jeecg.modules.pcms.project.mapper.DeviceFaultMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentDispatchService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceFaultHistoryService;
import org.jeecg.modules.pcms.project.service.IDeviceFaultService;
import org.jeecg.modules.pcms.util.DateUtils;
import org.jeecg.modules.pcms.util.SolarGetTokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DeviceFaultServiceImpl  extends ServiceImpl<DeviceFaultMapper, DeviceFault> implements IDeviceFaultService {



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
    private IDeviceEquipmentService deviceEquipmentService;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;

    @Autowired
    private DeviceFaultMapper deviceFaultMapper;

    @Autowired
    private IDeviceEquipmentDispatchService iDeviceEquipmentDispatchService;

    @Autowired
    private IDeviceFaultHistoryService iDeviceFaultHistoryService;





    @Override
    public void saveDeviceFaultAll(List<SolarDeviceFaultDto.PageListItem> pageItems) {
        //存入光伏云来源的数据
        if(pageItems != null && pageItems.size()>0){
            for (int i = 0; i < pageItems.size(); i++) {
                SolarDeviceFaultDto.PageListItem pageListItem = pageItems.get(i);
                //唯一故障编码
                String faultCode = pageListItem.getFaultCode();
                DeviceFault deviceFault = new DeviceFault();
                Integer uuid = pageListItem.getUuid();
                BeanUtils.copyProperties(pageListItem,deviceFault);
                deviceFault.setSource(1);
                deviceFault.setUuid(String.valueOf(uuid));
                this.save(deviceFault);
                //生成未处理的历史数据的时候需要查询在工单表里面是否有这条数据，如果有的话工单表就不需要重新生成
                QueryWrapper<DeviceEquipmentDispatch> deviceEquipmentDispatchQueryWrapper = new QueryWrapper<>();
                deviceEquipmentDispatchQueryWrapper.eq("fault_code",faultCode);
                DeviceEquipmentDispatch one = iDeviceEquipmentDispatchService.getOne(deviceEquipmentDispatchQueryWrapper);
                //同步过来的数据先在工单表查询是否存在 如果不存在插入一条新的工单，如果存在需要查看是否是关闭状态的
                if(one != null && one.getDispatchStatus().equals("3")){

                    this.saveEquipmentDispatch(pageListItem);
                }
                if(one == null){
                    this.saveEquipmentDispatch(pageListItem);
                }
            }
        }
        //继续处理掉在阳光云已经处理过的数据但是在工单表里面数据没变的数据
        List<DeviceEquipmentDispatch> list = iDeviceEquipmentDispatchService.list();
        // 提取 pageItems 中所有的 faultCode
        Set<String> pageItemFaultCodes = pageItems.stream()
                .map(SolarDeviceFaultDto.PageListItem::getFaultCode)
                .collect(Collectors.toSet());

        // 找出在 list 中存在但在 pageItems 中不存在的记录
        List<DeviceEquipmentDispatch> result = list.stream()
                .filter(item -> !pageItemFaultCodes.contains(item.getFaultCode()))
                .collect(Collectors.toList());

        if(result != null && result.size()>0){
            for (int i = 0; i < result.size(); i++) {
                DeviceEquipmentDispatch deviceEquipmentDispatch = result.get(i);
                deviceEquipmentDispatch.setDispatchStatus("3");
                iDeviceEquipmentDispatchService.updateById(deviceEquipmentDispatch);
            }
        }
    }

    public void saveEquipmentDispatch(SolarDeviceFaultDto.PageListItem pageListItem){
        DeviceEquipmentDispatch deviceEquipmentDispatch = new DeviceEquipmentDispatch();
        deviceEquipmentDispatch.setPsName(pageListItem.getPsName());
        deviceEquipmentDispatch.setDispatchNo(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        deviceEquipmentDispatch.setDispatchType("1");
        deviceEquipmentDispatch.setFaultType(pageListItem.getFaultType().toString());
        deviceEquipmentDispatch.setFaultTime(pageListItem.getCreateTime());
        deviceEquipmentDispatch.setFaultCode(pageListItem.getFaultCode());
        deviceEquipmentDispatch.setFaultName(pageListItem.getFaultName());
        deviceEquipmentDispatch.setSource("1");
        deviceEquipmentDispatch.setDeviceId(pageListItem.getPsId().toString());
        iDeviceEquipmentDispatchService.save(deviceEquipmentDispatch);
    }
    @Override
    public List<DeviceFault> getFaultListByPlantId(DevicePageVo devicePageVo, String plantId, Integer source) {
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String faultName = devicePageVo.getFaultName();
        String faultReason = devicePageVo.getFaultReason();
        //阳光云
        //新数据
        if(source == 1){
            //先根据电站id获取设备的sn码集合
            List<String> listPsKyeByPlantId = deviceEquipmentService.getListPsKeyByPlantId(plantId);
            if(listPsKyeByPlantId != null && listPsKyeByPlantId.size()>0){
                QueryWrapper<DeviceFault> deviceFaultQueryWrapper = new QueryWrapper<>();
                deviceFaultQueryWrapper.in("ps_key",listPsKyeByPlantId);
                if(StringUtils.isNotBlank(startDate)){
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
                    deviceFaultQueryWrapper.ge("create_time",startTime);
                }
                if(StringUtils.isNotBlank(endDate)){
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime endTime = LocalDateTime.parse(endDate, formatter);
                    deviceFaultQueryWrapper.le("create_time",endTime);
                }
                if(StringUtils.isNotBlank(faultReason)){
                    deviceFaultQueryWrapper.like("fault_reason",faultReason);
                }
                if(StringUtils.isNotBlank(faultName)){
                    deviceFaultQueryWrapper.like("fault_reason",faultName);
                }
                deviceFaultQueryWrapper.orderByAsc("create_time");
                List<DeviceFault> listSolar = this.list(deviceFaultQueryWrapper);
                if(listSolar != null && listSolar.size()>0){
                    return listSolar;
                }
            }
        }
        //古瑞瓦特
        if(source == 2){
            List<String> listSnByPlantId = deviceEquipmentService.getListPsSnByPlantId(plantId);
            QueryWrapper<DeviceFault> deviceFaultQueryWrapper = new QueryWrapper<>();
            deviceFaultQueryWrapper.in("device_sn",listSnByPlantId);
            if(StringUtils.isNotBlank(startDate)){
                deviceFaultQueryWrapper.ge("create_time",startDate);
            }
            if(StringUtils.isNotBlank(endDate)){
                deviceFaultQueryWrapper.le("create_time",endDate);
            }
            if(StringUtils.isNotBlank(faultReason)){
                deviceFaultQueryWrapper.like("fault_reason",faultReason);
            }
            if(StringUtils.isNotBlank(faultName)){
                deviceFaultQueryWrapper.like("fault_name",faultName);
            }
            deviceFaultQueryWrapper.orderByAsc("create_time");
            List<DeviceFault> listSolar = this.list(deviceFaultQueryWrapper);
            if(listSolar != null && listSolar.size()>0){
                return listSolar;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<DeviceFaultHistory> getFaultListByPlantIdHistory(DevicePageVo devicePageVo, String plantId, Integer source) {
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String faultName = devicePageVo.getFaultName();
        String faultReason = devicePageVo.getFaultReason();
        if(source == 1){
            //先根据电站id获取设备的sn码集合
            List<String> listPsKyeByPlantId = deviceEquipmentService.getListPsKeyByPlantId(plantId);
            if(listPsKyeByPlantId != null && listPsKyeByPlantId.size()>0){
                QueryWrapper<DeviceFaultHistory> deviceFaultQueryWrapper = new QueryWrapper<>();
                deviceFaultQueryWrapper.in("ps_key",listPsKyeByPlantId);
                if(StringUtils.isNotBlank(startDate)){
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
                    deviceFaultQueryWrapper.ge("create_time",startTime);
                }
                if(StringUtils.isNotBlank(endDate)){
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime endTime = LocalDateTime.parse(endDate, formatter);
                    deviceFaultQueryWrapper.le("create_time",endTime);
                }
                if(StringUtils.isNotBlank(faultReason)){
                    deviceFaultQueryWrapper.like("fault_reason",faultReason);
                }
                if(StringUtils.isNotBlank(faultName)){
                    deviceFaultQueryWrapper.like("fault_name",faultName);
                }
                deviceFaultQueryWrapper.orderByAsc("create_time");
                List<DeviceFaultHistory> listSolar = iDeviceFaultHistoryService.list(deviceFaultQueryWrapper);
                if(listSolar != null && listSolar.size()>0){
                    return listSolar;
                }
            }
        }
        //古瑞瓦特
        if(source == 2) {
            List<String> listSnByPlantId = deviceEquipmentService.getListPsSnByPlantId(plantId);
            QueryWrapper<DeviceFaultHistory> deviceFaultQueryWrapper = new QueryWrapper<>();
            deviceFaultQueryWrapper.in("device_sn", listSnByPlantId);
            if (StringUtils.isNotBlank(startDate)) {
                deviceFaultQueryWrapper.ge("create_time", startDate);
            }
            if (StringUtils.isNotBlank(endDate)) {
                deviceFaultQueryWrapper.le("create_time", endDate);
            }
            if(StringUtils.isNotBlank(faultReason)){
                deviceFaultQueryWrapper.like("fault_reason",faultReason);
            }
            if(StringUtils.isNotBlank(faultName)){
                deviceFaultQueryWrapper.like("fault_name",faultName);
            }
            deviceFaultQueryWrapper.orderByAsc("create_time");
            List<DeviceFaultHistory> listSolar = iDeviceFaultHistoryService.list(deviceFaultQueryWrapper);
            if (listSolar != null && listSolar.size() > 0) {
                return listSolar;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 定时任务录入现有的告警信息
     */
    @Override
    @Transactional
    public void insert() {
        String requestLoginToken = null;
        try {
            requestLoginToken = SolarGetTokenUtils.sendLoginSolarRequest();
            //获取设备的psk
            List<String> psKeyList = iDeviceEquipmentService.getPsKeyList();
            //调用登录接口获取token，获取下一步的电站列表
            //psKey
            //设备投运时间
            LocalDate yesterday = LocalDate.now().minusDays(1);
            // 格式化为 yyyy-MM-dd
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = yesterday.format(formatter);
            Integer type = 1;

            //写一个方法把投运时间到昨天的时间整成一个集合传入到方法里面
            List<DateIntervalVo> monthlyIntervalsFromStartToYesterday = DateUtils.getMonthlyIntervalsFromStartToYesterday(formattedDate,formattedDate,type);
            deviceFaultMapper.deleteAllDeviceFault();
            //阳光云获取未处理的告警信息
            //先删除所有未处理的告警信息，再重新查询插入
            List<SolarDeviceFaultDto.PageListItem> pageItems = this.fetchSolarDeviceFaultList(psKeyList.get(0),monthlyIntervalsFromStartToYesterday,requestLoginToken);
            this.saveDeviceFaultAll(pageItems);
            //古瑞瓦特获取未处理的告警信息
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /***
     *  获取阳光云的故障全部数据(查询昨天到设备运行的时候这段时间内的所有故障历史记录)
     * @param token
     * @return
     * @throws IOException
     */
    public List<SolarDeviceFaultDto.PageListItem> fetchSolarDeviceFaultList(String psKey, List<DateIntervalVo> dateIntervalVos, String token) throws IOException {
        int pageSize = 100; // 固定每页100条
        int currentPage = 1; // 从第1页开始
        List<SolarDeviceFaultDto.PageListItem> allData = new ArrayList<>();

        // 发送第一页请求
        List<SolarDeviceFaultDto> firstPageResponse = this.sendDeviceListSolarRequest(psKey, dateIntervalVos, stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));

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
                List<SolarDeviceFaultDto> pageResponse = this.sendDeviceListSolarRequest(psKey, dateIntervalVos, stationFault, token, appKey, appSecret, String.valueOf(pageSize), String.valueOf(currentPage));

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
    public List<SolarDeviceFaultDto> sendDeviceListSolarRequest(String psKey,List<DateIntervalVo> dateIntervalVos,String stationFault, String token,
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
        params.put("process_status", "");
        params.put("fault_type", "1,2,3,4");
        params.put("fault_level", "1,2,3,4");
//        params.put("ps_key", psKey);
        List<SolarDeviceFaultDto> solarDeviceFaultDtos = new ArrayList<>();
        //循环传参
//        if(dateIntervalVos != null && dateIntervalVos.size() > 0){
//            for (int i = 0; i < dateIntervalVos.size(); i++) {
//                DateIntervalVo dateIntervalVo = dateIntervalVos.get(i);
//                params.put("startTime", dateIntervalVo.getStartDate());
//                params.put("endTime", dateIntervalVo.getEndDate());
//            }
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

//        }
        return solarDeviceFaultDtos;

    }



}
