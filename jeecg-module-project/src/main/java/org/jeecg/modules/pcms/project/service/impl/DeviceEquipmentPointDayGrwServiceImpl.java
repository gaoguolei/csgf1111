package org.jeecg.modules.pcms.project.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.growatt.DeviceDataDto;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDetailDto;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointDayGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentDayExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DeviceEquipmentPointDayGrwMapper;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointDayGrwService;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class DeviceEquipmentPointDayGrwServiceImpl extends ServiceImpl<DeviceEquipmentPointDayGrwMapper, DeviceEquipmentPointDayGrw> implements IDeviceEquipmentPointDayGrwService {

    @Value("${growatt.dayEnergy}")
    private String dayEnergy;

    @Value("${growatt.token}")
    private String growattToken;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;



    @Autowired
    private IDeviceService iDeviceService;

    @Autowired
    private IDeviceEquipmentService iDeviceEquipmentService;



    @Override
    public void insert() {
        List<String> listSn = iDeviceEquipmentService.getListSn("2");
        ArrayList<String> strings = new ArrayList<>();

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 或者使用自定义格式器
        String formattedDate2 = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        strings.add(formattedDate2);
        for (String string : strings) {
            this.sendDeviceDetailGrowattRequest(listSn,string);
        }
    }

    @Override
    public List<DevicePointEquipmentDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //全选中之后的取消部分选中.
        String queryDate = devicePageVo.getQueryDate();
        String minType = devicePageVo.getMinType();
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String renterId = devicePageVo.getRenterId();

        //是否全选
        String isAll = devicePageVo.getIsAll();
        List<String> equipmentPsKeyList =new ArrayList<>();
        List<String> equipmentNotPsKeyList =new ArrayList<>();
        if( psKey != null && psKey.size()>0){
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.in("ps_id",psKey);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if(list != null && list.size()>0){
                equipmentPsKeyList=list.stream()
                        .map(DeviceEquipment::getDeviceSn)
                        .collect(Collectors.toList());
            }

        }
        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        //全选
        if(StringUtils.isNotBlank(isAll)){
            List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
            deviceEquipmentQueryWrapper.in("ps_id",psKeyListGrw);
            if( notInPsKey != null && notInPsKey.size()>0){
                deviceEquipmentQueryWrapper.notIn("ps_id",notInPsKey);
            }
        }
        List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
        if(list != null && list.size()>0){
            equipmentNotPsKeyList=list.stream()
                    .map(DeviceEquipment::getDeviceSn)
                    .collect(Collectors.toList());
        }
        // 动态分页参数
        QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(isAll)){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
        }

        if(psKey != null && psKey.size()>0){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
        }

        if("day".equals(minType)){
            if(StringUtils.isNotBlank(queryDate)){
                deviceEquipmentPointDayGrwQueryWrapper.eq("time_stamp",queryDate);
            }
        }
        //自定义
        if("customize".equals(minType)){
            if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
                deviceEquipmentPointDayGrwQueryWrapper.between("time_stamp",startDate,endDate);
            }
        }
        //月
        if("month".equals(minType)){
            LocalDate date = LocalDate.parse(queryDate);
            String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
        }
        deviceEquipmentPointDayGrwQueryWrapper.orderByAsc("time_stamp");
        List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = this.list(deviceEquipmentPointDayGrwQueryWrapper);
        List<DevicePointEquipmentDayExportVO> devicePointEquipmentDayExportVOS = this.changePointDayData(taskId, deviceEquipmentPointDayGrws);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointEquipmentDayExportVOS.toArray());
        return devicePointEquipmentDayExportVOS;
    }

    @Override
    public List<DevicePointEquipmentMonthExportVO> getExportExcelDataMonth(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //全选中之后的取消部分选中.
        String queryDate = devicePageVo.getQueryDate();
        String renterId = devicePageVo.getRenterId();

        //是否全选
        String isAll = devicePageVo.getIsAll();
        List<String> equipmentPsKeyList =new ArrayList<>();
        List<String> equipmentNotPsKeyList =new ArrayList<>();
        if( psKey != null && psKey.size()>0){
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.in("ps_id",psKey);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);


            if(list != null && list.size()>0){
                equipmentPsKeyList=list.stream()
                        .map(DeviceEquipment::getDeviceSn)
                        .collect(Collectors.toList());
            }

        }
        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(isAll)){
            List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
            deviceEquipmentQueryWrapper.in("ps_id",psKeyListGrw);
            if( notInPsKey != null && notInPsKey.size()>0){
                deviceEquipmentQueryWrapper.notIn("ps_id",notInPsKey);
            }
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if(list != null && list.size()>0){
                equipmentNotPsKeyList=list.stream()
                        .map(DeviceEquipment::getDeviceSn)
                        .collect(Collectors.toList());
            }
        }

        // 动态分页参数
        QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(isAll)){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
        }

        if(psKey != null && psKey.size()>0){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
        }

        if(StringUtils.isNotBlank(queryDate)){
            LocalDate date = LocalDate.parse(queryDate);
            String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy"));
            deviceEquipmentPointDayGrwQueryWrapper.like("time_stamp",yyyyMM);
        }
        List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = this.list(deviceEquipmentPointDayGrwQueryWrapper);
        //处理一下逻辑，把这一年里面的数据按照字段time_stamp分组,分组逻辑是把类似“2025-11-11” 按照月份来分组，然后在计算一下这个组里面的字段p1的值计算和，再返回list集合
        List<DeviceEquipmentPointDayGrw> list = deviceEquipmentPointDayGrws.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            // 创建复合键：月份 + device_sn
                            String month = item.getTimeStamp().substring(0, 7); // 提取年月 "2025-11"
                            String deviceSn = item.getDeviceSn();
                            return month + "|" + deviceSn; // 使用分隔符避免冲突
                        },
                        Collectors.summingDouble(item -> {
                            try {
                                return item.getP1() != null ? Double.parseDouble(item.getP1()) : 0.0;
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        })
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    // 解析复合键
                    String[] keys = entry.getKey().split("\\|");
                    String month = keys[0];
                    String deviceSn = keys[1];

                    // 创建新的汇总对象
                    DeviceEquipmentPointDayGrw summary = new DeviceEquipmentPointDayGrw();
                    summary.setTimeStamp(month);        // 设置月份 "2025-11"
                    summary.setDeviceSn(deviceSn);      // 设置设备序列号
                    summary.setP1(String.format("%.2f", entry.getValue())); // 设置求和结果，保留2位小数

                    return summary;
                })
                .collect(Collectors.toList());
        List<DevicePointEquipmentMonthExportVO> devicePointEquipmentDayExportVOS = this.changePointMonthData(taskId, list);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointEquipmentDayExportVOS.toArray());
        return devicePointEquipmentDayExportVOS;
    }

    @Override
    public List<DevicePointEquipmentYearExportVO> getExportExcelDataYear(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //是否全选
        String isAll = devicePageVo.getIsAll();
        String renterId = devicePageVo.getRenterId();
        List<String> equipmentPsKeyList =new ArrayList<>();
        List<String> equipmentNotPsKeyList =new ArrayList<>();
        QueryWrapper<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrwQueryWrapper = new QueryWrapper<>();
        //是否全选
        if( psKey != null && psKey.size()>0){
            QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
            deviceEquipmentQueryWrapper.in("ps_id",psKey);
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);


            if(list != null && list.size()>0){
                equipmentPsKeyList=list.stream()
                        .map(DeviceEquipment::getDeviceSn)
                        .collect(Collectors.toList());
            }

        }
        QueryWrapper<DeviceEquipment> deviceEquipmentQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(renterId)){
            List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
            deviceEquipmentQueryWrapper.in("ps_id",psKeyListGrw);
            if( notInPsKey != null && notInPsKey.size()>0){
                deviceEquipmentQueryWrapper.notIn("ps_id",notInPsKey);
            }
            List<DeviceEquipment> list = iDeviceEquipmentService.list(deviceEquipmentQueryWrapper);
            if(list != null && list.size()>0){
                equipmentNotPsKeyList=list.stream()
                        .map(DeviceEquipment::getDeviceSn)
                        .collect(Collectors.toList());
            }
        }


        if(StringUtils.isNotBlank(isAll)){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentNotPsKeyList);
        }

        if(psKey != null && psKey.size()>0){
            deviceEquipmentPointDayGrwQueryWrapper.in("device_sn",equipmentPsKeyList);
        }

        // 动态分页参数
        List<DeviceEquipmentPointDayGrw> deviceEquipmentPointDayGrws = this.list(deviceEquipmentPointDayGrwQueryWrapper);
        //处理一下逻辑，把这一年里面的数据按照字段time_stamp分组,分组逻辑是把类似“2025-11-11” 按照月份来分组，然后在计算一下这个组里面的字段p1的值计算和，再返回list集合
        // 按年份和device_sn分组，计算p1字段的和
        List<DeviceEquipmentPointDayGrw> list = deviceEquipmentPointDayGrws.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            // 创建复合键：年份 + device_sn
                            String year = item.getTimeStamp().substring(0, 4); // 提取年份 "2025"
                            String deviceSn = item.getDeviceSn();
                            return year + "|" + deviceSn; // 使用分隔符避免冲突
                        },
                        Collectors.summingDouble(item -> {
                            try {
                                return item.getP1() != null ? Double.parseDouble(item.getP1()) : 0.0;
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        })
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    // 解析复合键
                    String[] keys = entry.getKey().split("\\|");
                    String year = keys[0];
                    String deviceSn = keys[1];

                    // 创建新的汇总对象
                    DeviceEquipmentPointDayGrw summary = new DeviceEquipmentPointDayGrw();
                    summary.setTimeStamp(year);         // 设置年份 "2025"
                    summary.setDeviceSn(deviceSn);      // 设置设备序列号
                    summary.setP1(String.format("%.2f", entry.getValue())); // 设置年度求和结果，保留2位小数

                    return summary;
                })
                .collect(Collectors.toList());

        List<DevicePointEquipmentYearExportVO> devicePointEquipmentDayExportVOS = this.changePointYearData(taskId, list);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointEquipmentDayExportVOS.toArray());
        return devicePointEquipmentDayExportVOS;

    }


    List<DevicePointEquipmentDayExportVO> changePointDayData(String taskId,List<DeviceEquipmentPointDayGrw> list){
        //阳关云的转换
        List<DevicePointEquipmentDayExportVO> devicePointDayExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DeviceEquipmentPointDayGrw devicePointDay : list) {
                String psKey = devicePointDay.getDeviceSn();
                PsInfoVo deviceInfo = iDeviceEquipmentService.getDeviceInfoGrw(psKey);
                String timeStamp = devicePointDay.getTimeStamp();
                String formattedDate = LocalDate.parse(timeStamp, DateTimeFormatter.ISO_LOCAL_DATE)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                DevicePointEquipmentDayExportVO devicePointEquipmentDayExportVO = new DevicePointEquipmentDayExportVO();
                //
                if(deviceInfo != null){
                    devicePointEquipmentDayExportVO.setPsName(StringUtils.isNotBlank(deviceInfo.getPsName()) ? deviceInfo.getPsName() : "");
                    devicePointEquipmentDayExportVO.setPeakPower(StringUtils.isNotBlank(deviceInfo.getDesignCapacity()) ? deviceInfo.getDesignCapacity() : "");
                }
                devicePointEquipmentDayExportVO.setDate(formattedDate);
                devicePointEquipmentDayExportVO.setDayPower(new BigDecimal(devicePointDay.getP1()).setScale(2, RoundingMode.UP).toString());
                devicePointEquipmentDayExportVO.setEquipmentSn(StringUtils.isNotBlank(deviceInfo.getDeviceSn()) ? deviceInfo.getDeviceSn() : "");
                devicePointEquipmentDayExportVO.setEquipmentModelSn(StringUtils.isNotBlank(deviceInfo.getDeviceModelCode()) ? deviceInfo.getDeviceModelCode() : "");
                String status="";
                if(StringUtils.isNotBlank(deviceInfo.getDeviceStatus())){
                    if(deviceInfo.getDeviceStatus().equals("1")){
                        status="故障";
                    }
                    if(deviceInfo.getDeviceStatus().equals("2")){
                        status="告警";
                    }
                    if(deviceInfo.getDeviceStatus().equals("4")){
                        status="正常";
                    }
                }
                devicePointEquipmentDayExportVO.setEquipmentStatus(status);
                devicePointEquipmentDayExportVO.setMessageSn(StringUtils.isNotBlank(deviceInfo.getCommunicationDevSn()) ? deviceInfo.getCommunicationDevSn() : "");
                devicePointDayExportVOS.add(devicePointEquipmentDayExportVO);
            }
        }
        return devicePointDayExportVOS;
    }


    List<DevicePointEquipmentMonthExportVO> changePointMonthData(String taskId,List<DeviceEquipmentPointDayGrw> list){
        //阳关云的转换
        List<DevicePointEquipmentMonthExportVO> devicePointDayExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DeviceEquipmentPointDayGrw devicePointDay : list) {
                String psKey = devicePointDay.getDeviceSn();
                PsInfoVo deviceInfo = iDeviceEquipmentService.getDeviceInfoGrw(psKey);
                String timeStamp = devicePointDay.getTimeStamp();
                YearMonth yearMonth = YearMonth.parse(timeStamp,
                        DateTimeFormatter.ofPattern("yyyy-MM"));
                String formattedDate = yearMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                DevicePointEquipmentMonthExportVO devicePointEquipmentDayExportVO = new DevicePointEquipmentMonthExportVO();
                //
                if(deviceInfo != null){
                    devicePointEquipmentDayExportVO.setPsName(StringUtils.isNotBlank(deviceInfo.getPsName()) ? deviceInfo.getPsName() : "");
                    devicePointEquipmentDayExportVO.setPeakPower(StringUtils.isNotBlank(deviceInfo.getDesignCapacity()) ? deviceInfo.getDesignCapacity() : "");
                }
                devicePointEquipmentDayExportVO.setDate(formattedDate);
                devicePointEquipmentDayExportVO.setMonthPower(new BigDecimal(devicePointDay.getP1()).setScale(2, RoundingMode.UP).toString());
                devicePointEquipmentDayExportVO.setEquipmentSn(StringUtils.isNotBlank(deviceInfo.getDeviceSn()) ? deviceInfo.getDeviceSn() : "");
                devicePointEquipmentDayExportVO.setEquipmentModelSn(StringUtils.isNotBlank(deviceInfo.getDeviceModelCode()) ? deviceInfo.getDeviceModelCode() : "");
                String status="";
                if(StringUtils.isNotBlank(deviceInfo.getDeviceStatus())){
                    if(deviceInfo.getDeviceStatus().equals("1")){
                        status="故障";
                    }
                    if(deviceInfo.getDeviceStatus().equals("2")){
                        status="告警";
                    }
                    if(deviceInfo.getDeviceStatus().equals("4")){
                        status="正常";
                    }
                }
                devicePointEquipmentDayExportVO.setEquipmentStatus(status);
                devicePointEquipmentDayExportVO.setMessageSn(StringUtils.isNotBlank(deviceInfo.getCommunicationDevSn()) ? deviceInfo.getCommunicationDevSn() : "");
                devicePointDayExportVOS.add(devicePointEquipmentDayExportVO);
            }
        }
        return devicePointDayExportVOS;
    }


    List<DevicePointEquipmentYearExportVO> changePointYearData(String taskId,List<DeviceEquipmentPointDayGrw> list){
        //阳关云的转换
        List<DevicePointEquipmentYearExportVO> devicePointDayExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DeviceEquipmentPointDayGrw devicePointDay : list) {
                String psKey = devicePointDay.getDeviceSn();
                PsInfoVo deviceInfo = iDeviceEquipmentService.getDeviceInfoGrw(psKey);
                String timeStamp = devicePointDay.getTimeStamp();
                Year yearMonth = Year.parse(timeStamp,
                        DateTimeFormatter.ofPattern("yyyy"));
                String formattedDate = yearMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                DevicePointEquipmentYearExportVO devicePointEquipmentDayExportVO = new DevicePointEquipmentYearExportVO();
                //
                if(deviceInfo != null){
                    devicePointEquipmentDayExportVO.setPsName(StringUtils.isNotBlank(deviceInfo.getPsName()) ? deviceInfo.getPsName() : "");
                    devicePointEquipmentDayExportVO.setPeakPower(StringUtils.isNotBlank(deviceInfo.getDesignCapacity()) ? deviceInfo.getDesignCapacity() : "");
                }
                devicePointEquipmentDayExportVO.setDate(formattedDate);
                devicePointEquipmentDayExportVO.setTotalPower(new BigDecimal(devicePointDay.getP1()).setScale(2, RoundingMode.UP).toString());
                devicePointEquipmentDayExportVO.setEquipmentSn(StringUtils.isNotBlank(deviceInfo.getDeviceSn()) ? deviceInfo.getDeviceSn() : "");
                devicePointEquipmentDayExportVO.setEquipmentModelSn(StringUtils.isNotBlank(deviceInfo.getDeviceModelCode()) ? deviceInfo.getDeviceModelCode() : "");
                String status="";
                if(StringUtils.isNotBlank(deviceInfo.getDeviceStatus())){
                    if(deviceInfo.getDeviceStatus().equals("1")){
                        status="故障";
                    }
                    if(deviceInfo.getDeviceStatus().equals("2")){
                        status="告警";
                    }
                    if(deviceInfo.getDeviceStatus().equals("4")){
                        status="正常";
                    }
                }
                devicePointEquipmentDayExportVO.setEquipmentStatus(status);
                devicePointEquipmentDayExportVO.setMessageSn(StringUtils.isNotBlank(deviceInfo.getCommunicationDevSn()) ? deviceInfo.getCommunicationDevSn() : "");
                devicePointDayExportVOS.add(devicePointEquipmentDayExportVO);
            }
        }
        return devicePointDayExportVOS;
    }


    public GrowattDeviceDetailDto.DeviceDetail sendDeviceDetailGrowattRequest(List<String> deviceSn , String time) {
        if (deviceSn != null && deviceSn.size() > 0) {
            for (int i = 0; i < deviceSn.size(); i++) {
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
                params.put("device_sn", String.valueOf(deviceSn.get(i)));
                params.put("date", time);

                // 2. 使用OkHttp的HttpUrl.Builder构建带参数的URL
                HttpUrl baseUrl = HttpUrl.parse(dayEnergy);
                if (baseUrl == null) {
                    throw new IllegalArgumentException("Invalid base URL: " + dayEnergy);
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
                    DeviceDataDto solarDeviceEquipmentPointDto = JSONObject.parseObject(body.string(), DeviceDataDto.class);
                    if (solarDeviceEquipmentPointDto != null) {
                        String deviceSnData = solarDeviceEquipmentPointDto.getDevice_sn();
                        String dataloggerSn = solarDeviceEquipmentPointDto.getDataloggerSn();
                        String data = solarDeviceEquipmentPointDto.getData();
                        DeviceEquipmentPointDayGrw deviceEquipmentPointDayGrw = new DeviceEquipmentPointDayGrw();
                        deviceEquipmentPointDayGrw.setDeviceSn(deviceSnData);
                        deviceEquipmentPointDayGrw.setP1(data);
                        deviceEquipmentPointDayGrw.setDataloggerSn(dataloggerSn);
                        deviceEquipmentPointDayGrw.setTimeStamp(time);
                        this.save(deviceEquipmentPointDayGrw);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }



}
