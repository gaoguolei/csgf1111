package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.mapper.PvDeviceLastMapper;
import org.jeecg.modules.pcms.project.mapper.PvStationsMapper;
import org.jeecg.modules.pcms.project.service.IPvDeviceLastService;
import org.jeecg.modules.pcms.project.service.IPvDeviceService;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceLast;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationvo.*;
import org.jeecg.modules.pcms.project.vo.PageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PvDeviceLastServiceImpl extends ServiceImpl<PvDeviceLastMapper, PvDeviceLast> implements IPvDeviceLastService {

    @Autowired
    private IPvDeviceService iPvDeviceService;

    @Autowired
    private PvStationsMapper pvStationsMapper;
    @Autowired
    private PvDeviceLastMapper pvDeviceLastMapper;

    @Autowired
    private StationConfig stationConfig;

    @Override
    @Transactional
    public void insert() {
        List<Device> devices = this.getDeviceSnList();
        if(devices != null && devices.size()>0){
            pvDeviceLastMapper.deleteAll();
            for (int i = 0; i < devices.size(); i++) {
                //先删除表数据 然后再插入
                try {
                    String deviceSn = devices.get(i).getDeviceSn();
                    String plantId = devices.get(i).getPlantId();
                    HashMap<String, Object> paramMap = new HashMap<>();
                    paramMap.put("device_sn", deviceSn);
                    String result2 = HttpRequest.get(stationConfig.getInverterNew())
                            .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                            .form(paramMap)//表单内容
                            .timeout(20000)//超时，毫秒
                            .execute()
                            .body();
                    DeviceLastVo stationsVo = JSON.parseObject(result2, DeviceLastVo.class);
                    if(stationsVo != null){
                        PvDeviceLast data = stationsVo.getData();
                        data.setPlantId(plantId);
                        pvDeviceLastMapper.insert(stationsVo.getData());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public IPage<PvStations> pageList(Page<PvStations> page,PageVo pageVo) {
        IPage<PvStations> pvStationsIPage = pvStationsMapper.pageList(page, pageVo);
        return pvStationsIPage;
    }

    @Override
    public List<PvStations> selectStationCount(String status) {
        return pvStationsMapper.selectStationCount(status);
    }

    /**
     * 获取逆变器sn码集合
     */
    public List<Device> getDeviceSnList(){
        Integer count = this.getCount();
        //如果总数小于100
        if(count < 100 ){
            List<PlantResultVo> data = this.getData(1, 100);
            ArrayList<StationListVo> plants = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                List<StationListVo> stationListVos = data.get(i).getData().getPlants();
                stationListVos.addAll(plants);
            }
            if(plants != null && plants.size()>0){
                List<Device> devices = new ArrayList<>();
                for (int i = 0; i < plants.size(); i++) {
                    try {
                        Integer plantId = plants.get(i).getPlantId();
                        HashMap<String, Object> paramMap = new HashMap<>();
                        paramMap.put("plant_id", plantId);
                        String result2 = HttpRequest.get(stationConfig.getStationList())
                                .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                                .form(paramMap)//表单内容
                                .timeout(20000)//超时，毫秒
                                .execute()
                                .body();

                        DeviceVo deviceVo = JSON.parseObject(result2, DeviceVo.class);
                        List<Device> deviceList = deviceVo.getData().getDevices();
                        List<Device> collect = deviceList.stream().filter(j -> j.getType().equals("1")).collect(Collectors.toList());
                        collect.stream().forEach(j->{
                            j.setPlantId(plantId.toString());
                        });
                        devices.addAll(collect);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                return devices;
            }
        }
        if(count > 100){
            int i = count / 100;
            List<PlantResultVo> data = this.getData(1, count);
            ArrayList<StationListVo> plants = new ArrayList<>();
            for (int k = 0; k < data.size(); k++) {
                List<StationListVo> stationListVos = data.get(k).getData().getPlants();
                plants.addAll(stationListVos);
            }
            if(plants != null && plants.size()>0){
                try {
                    List<Device> devices = new ArrayList<>();
                    for (int k = 0; k < plants.size(); k++) {
                        Integer plantId = plants.get(k).getPlantId();
                        HashMap<String, Object> paramMap = new HashMap<>();
                        paramMap.put("plant_id", plantId);
                        String result2 = HttpRequest.get(stationConfig.getDeviceList())
                                .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                                .form(paramMap)//表单内容
                                .timeout(20000)//超时，毫秒
                                .execute()
                                .body();
                        DeviceVo deviceVo = JSON.parseObject(result2, DeviceVo.class);
                        List<Device> deviceList = deviceVo.getData().getDevices();
                        deviceList.stream().filter(m->m.getType().equals("1")).forEach(m->{
                            m.setPlantId(plantId.toString());
                        });
                        devices.addAll(deviceList);
                    }
                    return devices;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>();
    }

    public Integer getCount(){
        HashMap<String, Object> paramMap = new HashMap<>();
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
        String result2 = HttpRequest.get(stationConfig.getStationList())
                .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
        Integer count = plantResultVo.getData().getCount();
        return count;
    }



    public List<PlantResultVo> getData(Integer page, Integer perpage) {
        if (perpage >= 100) {
            int i = perpage / 100;
            ArrayList<PlantResultVo> plantResultVos = new ArrayList<>();
            for (int j = 1; j <= i + 1; j++) {
                HashMap<String, Object> paramMap = new HashMap<>();
                int random = (int) ((Math.random() * 10) + 1);
                paramMap.put("search_type", random);
                paramMap.put("page", j);
                paramMap.put("perpage", 100);
                String result2 = HttpRequest.get(stationConfig.getStationList())
                        .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                        .form(paramMap)//表单内容
                        .timeout(20000)//超时，毫秒
                        .execute()
                        .body();
                PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
                plantResultVos.add(plantResultVo);

            }
            return plantResultVos;
        } else {
            ArrayList<PlantResultVo> plantResultVos = new ArrayList<>();
            HashMap<String, Object> paramMap = new HashMap<>();
            int random = (int) ((Math.random() * 10) + 1);
            paramMap.put("search_type", random);
            paramMap.put("page", page);
            paramMap.put("perpage", perpage);
            String result2 = HttpRequest.get(stationConfig.getStationList())
                    .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                    .form(paramMap)//表单内容
                    .timeout(20000)//超时，毫秒
                    .execute()
                    .body();
            PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
            plantResultVos.add(plantResultVo);
            return plantResultVos;
        }
    }
}
