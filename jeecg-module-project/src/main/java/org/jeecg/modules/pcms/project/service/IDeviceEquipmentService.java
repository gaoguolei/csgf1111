package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.dto.solar.DeviceResultDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceEquipmentDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipment;
import org.jeecg.modules.pcms.energy.vo.DevicePointVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;
import org.jeecg.modules.pcms.project.stationvo.DeviceInfoVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IDeviceEquipmentService extends IService<DeviceEquipment> {

    public void saveDeviceEquipment(DeviceEquipment deviceEquipment);

    public void saveDeviceEquipmentList(List<SolarDeviceEquipmentDto.PageItem> pageItems);



    public List<String> getListSn(String type);


    public List<String> getPsKeyList();

    public List<String> getPsKeyListByPlantId(List<Long> plantIds);

    public List<String> getPsKeyListByPlantIdInteger(List<Integer> plantIds);



    public List<String> getListPsKeyByPlantId(String plantId);

    public List<String> getListEquipmentSnByPlantId(String plantId);

    public List<String> getListPsSnByPlantId(String plantId);



    public DeviceEquipment getDeviceEquipmentByPsKey(String psKey);


    void insert();

    public Map<String,List<String>> getPsKeyVoListFault();

    public PsInfoVo getDeviceInfo(String psKey);

    public PsInfoVo getDeviceInfoGrw(String psKey);

    public List<PsInfoVo>getDeviceInfos(Set<String> psKey);

    public List<String> getPsKeyByProjectType(String renterId);
    public List<String> getPsKeyByProjectTypeGrw(String renterId);


}
