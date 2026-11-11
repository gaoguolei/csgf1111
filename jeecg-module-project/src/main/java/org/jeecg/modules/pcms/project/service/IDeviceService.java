package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.dto.growatt.GrowattDeviceDto;
import org.jeecg.modules.pcms.energy.dto.solar.DeviceResultDto;
import org.jeecg.modules.pcms.energy.dto.solar.SolarDeviceDto;
import org.jeecg.modules.pcms.energy.entity.Device;
import org.jeecg.modules.pcms.energy.vo.DeviceReturnVo;
import org.jeecg.modules.pcms.energy.vo.DeviceStatsVo;
import org.jeecg.modules.pcms.energy.vo.PsInfoVo;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface IDeviceService extends IService<Device> {


    Result saveDeviceSolar(List<SolarDeviceDto.PageItem> pageItems);

    Result saveDeviceGrwAll( List<GrowattDeviceDto.Plant> plants);

    public List<DeviceResultDto> getDeviceName(String renterId);
    public List<DeviceResultDto> getDeviceNameGrw(String renterId);


    List<Integer> getListId();


    public DeviceStatsVo getStatsData(Integer plantId);


    public PsInfoVo getPsInfoByPsKey(String psKey);

    public PsInfoVo getPsInfoByPsKeyForGrw(String psId);




    public DeviceReturnVo getPsNameByPsId(Integer psId);

    List<Integer> getSolarListId();




    void insert();
    void insertGrw();


    String getPsName(Long psId);


    List<Long> getPsIdsByProjectType(String renterId);

    List<Object> getTaskStatus(String taskId);


    Integer getUnLineDeviceNumSolar(String renterId,Integer status);

    Integer getUnLineDeviceNumGrw(String renterId,Integer status);

    String countTotalPower(String projectType);

    List<String> getPsKeyList(String projectType);
    List<String> getPsKeyListGrw(String projectType);

    Map<String, String> getPsNameBatch(Set<Long> psIds);


    public Map<String, String> getPsNameBatchGrw(Set<Long> psIds);

}

