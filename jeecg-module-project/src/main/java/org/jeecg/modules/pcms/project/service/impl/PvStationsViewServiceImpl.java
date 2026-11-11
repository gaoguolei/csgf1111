package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.mapper.PvStationsViewMapper;
import org.jeecg.modules.pcms.project.service.IPvStationsService;
import org.jeecg.modules.pcms.project.service.IPvStationsViewService;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;
import org.jeecg.modules.pcms.project.stationvo.StationsViewVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class PvStationsViewServiceImpl extends ServiceImpl<PvStationsViewMapper, PvStationsView> implements IPvStationsViewService {

    @Autowired
    private IPvStationsService iPvStationsService;
    @Autowired
    private PvStationsViewMapper pvStationsViewMapper;

    @Autowired
    private StationConfig stationConfig;
    @Override
    @Transactional
    public void insert() {
        List<PvStations> list = iPvStationsService.list();
        pvStationsViewMapper.deleteAll();
        if(list != null && list.size()>0){
            for (int i = 0; i < list.size(); i++) {
                //先删除表数据 然后再插入
                try {
                    Integer plantId = list.get(i).getPlantId();
                    HashMap<String, Object> paramMap = new HashMap<>();
                    paramMap.put("plant_id", plantId);
                    String result2 = HttpRequest.get(stationConfig.getStationData())
                            .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                            .form(paramMap)//表单内容
                            .timeout(20000)//超时，毫秒
                            .execute()
                            .body();
                    StationsViewVo stationsVo = JSON.parseObject(result2, StationsViewVo.class);
                    if(stationsVo != null){
                        PvStationsView data = stationsVo.getData();
                        data.setPlantId(plantId);
                        data.setUpdateTime(new Date());
                        pvStationsViewMapper.insert(data);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

}
