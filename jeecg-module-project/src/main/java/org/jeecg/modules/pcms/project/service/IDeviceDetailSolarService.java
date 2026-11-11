package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceDetailSolar;

import java.util.List;
import java.util.Map;

public interface IDeviceDetailSolarService  extends IService<DeviceDetailSolar> {


//    public Map<String, List<String>> getPsKeyVoList();

    public List<String> getPsKey();


    public void sendPlantDetailListSolarRequest(List<String> stringList);

    public List<Long> notInPsKey(List<String> notInPsKeyList);

    public List<Long> inPsKey(List<String> notInPsKeyList);

    void insertDeviceDetail();

    public List<String> getPlantPsKey();

    public List<String> getDevicePsKey();


    public String plantPsKey(Integer deviceId);

}
