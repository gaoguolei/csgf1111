package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationvo.Device;
import org.jeecg.modules.pcms.project.vo.PageVo;

import java.util.List;

public interface IPvStationsService extends IService<PvStations> {


     public void insert();
     public void updateStationAndDetails(List<PvStations> plantVos);

     public void updateStationDetails (List<PvStations> plantVos);


     public void updateDevice(List<Device> devices);



     public Result stationReport(PageVo pageVo);

     public Result powerReport(PageVo pageVo);



     public String getStatus(Integer plantId);

     public List<String> getUserIds(String createId);
}
