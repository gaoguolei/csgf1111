package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattEnergyResponseDto;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailGrowatt;
import org.jeecg.modules.pcms.energy.entity.DevicePointDayGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.mapper.DevicePointDayGrwMapper;
import org.jeecg.modules.pcms.project.service.IDeviceDetailGrowattService;
import org.jeecg.modules.pcms.project.service.IDevicePointDayGrwService;
import org.jeecg.modules.pcms.project.service.IDeviceService;
import org.jeecg.modules.pcms.project.util.DateFormatterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DeviceDayPointGrwServiceImpl extends ServiceImpl<DevicePointDayGrwMapper, DevicePointDayGrw> implements IDevicePointDayGrwService {


    @Autowired
    private IDeviceService iDeviceService;


    @Value("${growatt.token}")
    private String growattToken;


    @Value("${growatt.historyEnergy}")
    private String historyEnergy;

    @Autowired
    private IDeviceDetailGrowattService iDeviceDetailGrowattService;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public void insert() {

        List<Integer> listId = iDeviceService.getListId();

        LocalDate yesterday = LocalDate.now().minusDays(1);
        String format = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        //获取所有设备列表集合
        this.fetchGrowattDeviceEquipmentList(listId, growattToken, "100", "1",format,format);
    }

    @Override
    public List<DevicePointDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo) {
        List<String> psKey = devicePageVo.getPsKey();
        String queryDate = devicePageVo.getQueryDate();
        String startDate = devicePageVo.getStartDate();
        String endDate = devicePageVo.getEndDate();
        String minType = devicePageVo.getMinType();
        String renterId = devicePageVo.getRenterId();
        List<String> notInPsKey = devicePageVo.getNotInPsKey();
        //是否全选
        String isAll = devicePageVo.getIsAll();
        // 动态分页参数
        QueryWrapper<DevicePointDayGrw> devicePointDayQueryWrapper = new QueryWrapper<>();
        List<String> psKeyListGrw = iDeviceService.getPsKeyListGrw(renterId);
        devicePointDayQueryWrapper.in("ps_key", psKeyListGrw);
        //全选
        if (StringUtils.isNotBlank(isAll)) {
            if (notInPsKey != null && notInPsKey.size() > 0) {
                devicePointDayQueryWrapper.notIn("ps_key", notInPsKey);
            }
        }
        if (psKey != null && psKey.size() > 0) {
            devicePointDayQueryWrapper.in("ps_key", psKey);
        }
        if ("day".equals(minType)) {
            if (StringUtils.isNotBlank(queryDate)) {
                devicePointDayQueryWrapper.eq("time_stamp", queryDate);
            }
        }
        //自定义
        if ("customize".equals(minType)) {
            if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
                devicePointDayQueryWrapper.between("time_stamp", startDate, endDate);
            }
        }
        //月
        if ("month".equals(minType)) {
            LocalDate date = LocalDate.parse(queryDate);
            String yyyyMM = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            devicePointDayQueryWrapper.like("time_stamp", yyyyMM);
        }
        devicePointDayQueryWrapper.orderByAsc("time_stamp");
        List<DevicePointDayGrw> list = this.list(devicePointDayQueryWrapper);

        List<DevicePointDayExportVO> devicePointDayExportVOS = this.changePointDayData(taskId, request, list);
        return devicePointDayExportVOS;
    }

    List<DevicePointDayExportVO> changePointDayData(String taskId, HttpServletRequest request,List<DevicePointDayGrw> list) {
        List<DevicePointDayExportVO> devicePointDayExportVOS = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String psKeyParam = list.get(i).getPsKey();
                String p83022 = list.get(i).getP83022();
                String timeStamp = list.get(i).getTimeStamp();
                PsInfoVo psInfoByPsKey = iDeviceService.getPsInfoByPsKeyForGrw(psKeyParam);
                DevicePointDayExportVO devicePointDayExportVO = new DevicePointDayExportVO();
                devicePointDayExportVO.setDayPower(p83022);
                DeviceDetailGrowatt deviceDetail = iDeviceDetailGrowattService.getDeviceDetail(psKeyParam);
                if (deviceDetail != null) {
                    devicePointDayExportVO.setTotalPower(deviceDetail.getTotalEnergy());
                }
                devicePointDayExportVO.setPeakPower(psInfoByPsKey.getDesignCapacity());
                devicePointDayExportVO.setDate(timeStamp);
                devicePointDayExportVO.setPsName(psInfoByPsKey.getPsName());
                devicePointDayExportVOS.add(devicePointDayExportVO);
            }
            redisTemplate.opsForList().leftPushAll("Task:" + taskId, devicePointDayExportVOS.toArray());
        }
        return devicePointDayExportVOS;
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
                params.put("time_unit","day");
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
                            DevicePointDayGrw devicePointDay = new DevicePointDayGrw();
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
