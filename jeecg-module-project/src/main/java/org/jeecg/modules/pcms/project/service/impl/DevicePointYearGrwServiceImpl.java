package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattEnergyResponseDto;
import org.jeecg.modules.pcms.energy.entity.DevicePointYearGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointYearGrwMapper;
import org.jeecg.modules.pcms.project.service.IDevicePointYearGrwService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DevicePointYearGrwServiceImpl  extends ServiceImpl<DevicePointYearGrwMapper, DevicePointYearGrw> implements IDevicePointYearGrwService {


    @Autowired
    private IDeviceService iDeviceService;

    @Autowired private DevicePointYearGrwMapper  devicePointYearGrwMapper;


    @Value("${growatt.token}")
    private String growattToken;


    @Value("${growatt.historyEnergy}")
    private String historyEnergy;




    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void insert() {
        List<Integer> listId = iDeviceService.getListId();

        //先删除再更新
        // 获取当月第一天
        // 获取当年第一天
        int currentYear = LocalDate.now().getYear();
        String startTime = LocalDate.of(currentYear, 1, 1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 获取当年最后一天
        String endTime = LocalDate.of(currentYear, 12, 31)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        QueryWrapper<DevicePointYearGrw> devicePointMonthGrwQueryWrapper = new QueryWrapper<>();
        devicePointMonthGrwQueryWrapper.eq("time_stamp",currentMonth);
        List<DevicePointYearGrw> list = this.list(devicePointMonthGrwQueryWrapper);
        if(list != null && list.size() > 0){
            devicePointYearGrwMapper.deleteByTimeStamp(currentMonth);
        }
        //获取所有设备列表集合
        this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1",startTime,endTime);

    }

    @Override
    @Async("exportTaskExecutor")
    public List<DevicePointYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        String queryDate = devicePageVo.getQueryDate();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        String isAll = devicePageVo.getIsAll();
        String renterId = devicePageVo.getRenterId();
        // 动态分页参数
        QueryWrapper<DevicePointYearGrw> devicePointYearGrwQueryWrapper = new QueryWrapper<>();

        List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
        devicePointYearGrwQueryWrapper.in("ps_key", psKeyListGrw);
        if(StringUtils.isNotBlank(isAll)){
            if(notInPsKey != null && notInPsKey.size()>0){
                devicePointYearGrwQueryWrapper.notIn("ps_key",notInPsKey);
            }
        }
        if(psKey != null && psKey.size()>0){
            devicePointYearGrwQueryWrapper.in("ps_key",psKey);
        }
        if(StringUtils.isNotBlank(queryDate)){
            LocalDate date = LocalDate.parse(queryDate);
            String yyyy = date.format(DateTimeFormatter.ofPattern("yyyy"));
            devicePointYearGrwQueryWrapper.like("time_stamp",yyyy);
        }
        devicePointYearGrwQueryWrapper.orderByAsc("time_stamp");
        List<DevicePointYearGrw> list = this.list(devicePointYearGrwQueryWrapper);
        List<DevicePointYearExportVO> devicePointMonthExportVOS = this.changePointDayData(taskId, request, list);
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointMonthExportVOS.toArray());
        return devicePointMonthExportVOS;
    }



    List<DevicePointYearExportVO> changePointDayData(String taskId, HttpServletRequest request,List<DevicePointYearGrw> list){
        //阳关云的转换
        List<DevicePointYearExportVO> devicePointYearExportVOS = new ArrayList<>();
        if(list != null && list.size()>0){
            for (DevicePointYearGrw devicePointDay : list) {
                String psKey = devicePointDay.getPsKey();
                String timeStamp = devicePointDay.getTimeStamp();
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy");
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy");
                String formattedDate = Year.parse(timeStamp, inputFormatter).format(outputFormatter);
                DevicePointYearExportVO devicePointYearExportVO = new DevicePointYearExportVO();
                PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKey);
                if(psInfoByPsKey != null){
                    devicePointYearExportVO.setPsName(StringUtils.isNotBlank(psInfoByPsKey.getPsName()) ? psInfoByPsKey.getPsName() : "");
                }
                devicePointYearExportVO.setDate(formattedDate);
                devicePointYearExportVO.setYearPower(devicePointDay.getP83022());
                devicePointYearExportVO.setPeakPower(psInfoByPsKey.getDesignCapacity());
                devicePointYearExportVO.setTotalPower(psInfoByPsKey.getTotalEnergy()+" "+psInfoByPsKey.getTotalEnergyUnit());
                devicePointYearExportVOS.add(devicePointYearExportVO);
            }
        }
        redisTemplate.opsForList().leftPushAll("Task:"+taskId, devicePointYearExportVOS.toArray());
        return devicePointYearExportVOS;
    }

    /**
     * 古瑞瓦特获获取并插入设备数据
     *
     * @return
     */
    public void fetchGrowattDeviceEquipmentList(List<Integer> listId, String growattToken, String pageSize, String pageNum,String startTime,String endTime) {
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
                params.put("time_unit","year");
                params.put("start_date", startTime);
                params.put("end_date",endTime);

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
                            DevicePointYearGrw devicePointDay = new DevicePointYearGrw();
                            devicePointDay.setPsKey(listId.get(i).toString());
                            devicePointDay.setP83022(energy.getEnergy());
                            devicePointDay.setTimeStamp(energy.getDate());
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

