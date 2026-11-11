package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.config.station.StationConfig;
import org.jeecg.modules.pcms.project.entity.PvDatalogger;
import org.jeecg.modules.pcms.project.mapper.PvDataloggerMapper;
import org.jeecg.modules.pcms.project.mapper.PvStationsMapper;
import org.jeecg.modules.pcms.project.service.IPvDataloggerService;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationvo.PvDataloggerRuselt;
import org.jeecg.modules.pcms.project.stationvo.PvDataloggerVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/11/16 上午8:07
 * @Version 1.5
 */

@Service
public class PvDataloggerServiceImpl extends ServiceImpl<PvDataloggerMapper, PvDatalogger> implements IPvDataloggerService {

    @Resource
    private PvStationsMapper pvStationsMapper;

    @Resource
    private PvDataloggerMapper pvDataloggerMapper;

    @Autowired
    private StationConfig stationConfig;

    @Override
    @Transactional
    public void insert() {
        QueryWrapper<PvStations> objectQueryWrapper = new QueryWrapper<>();
        List<PvStations> pvStations = pvStationsMapper.selectList(objectQueryWrapper);
        pvDataloggerMapper.deleteAll();
        for (int i = 0; i < pvStations.size(); i++) {
            //先删除表数据 然后再插入
            try {
                Integer plantId = pvStations.get(i).getPlantId();
                HashMap<String, Object> paramMap = new HashMap<>();
                paramMap.put("plant_id", plantId);
                paramMap.put("page", 1);
                paramMap.put("perpage", 98);
                String result2 = HttpRequest.get(stationConfig.getDataLogger())
                        .header("token", stationConfig.getToken())//头信息，多个头信息多次调用此方法即可
                        .form(paramMap)//表单内容
                        .timeout(20000)//超时，毫秒
                        .execute()
                        .body();
                JSONObject jsonObj = JSONObject.parseObject(result2);
                if(jsonObj != null){
                    String data = jsonObj.getString("data");
                    PvDataloggerVo dataloggerResult = JSON.parseObject(data, PvDataloggerVo.class);
                    if(dataloggerResult !=null){
                        List<PvDataloggerRuselt> dataloggers = dataloggerResult.getDataloggers();
                        if(dataloggers != null && dataloggers.size()>0){
                            for (int j = 0; j < dataloggers.size(); j++) {
                                PvDatalogger pvDatalogger = new PvDatalogger();
                                String sn = dataloggers.get(j).getSn();
                                String type = dataloggers.get(j).getType();
                                String lost = dataloggers.get(j).getLost();
                                String netmode = dataloggers.get(j).getNetmode();
                                String manufacturer = dataloggers.get(j).getManufacturer();
                                String lastUpdateTime = dataloggers.get(j).getLastUpdateTime();
                                pvDatalogger.setPlantId(plantId.toString());
                                pvDatalogger.setSn(sn);
                                if(lost.equals("true")){
                                    pvDatalogger.setStatus("0");
                                }
                                if(lost.equals("false")){
                                    pvDatalogger.setStatus("1");
                                }
                                pvDatalogger.setType(type);
                                pvDatalogger.setLastUpdateTime(lastUpdateTime);
                                pvDatalogger.setManufacturer(manufacturer);
                                pvDatalogger.setNetmode(netmode);
                                this.save(pvDatalogger);
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
