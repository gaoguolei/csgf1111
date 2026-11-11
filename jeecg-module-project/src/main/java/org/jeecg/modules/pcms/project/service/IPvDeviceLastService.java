package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceLast;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.vo.PageVo;

import java.util.List;

public interface IPvDeviceLastService extends IService<PvDeviceLast> {


    public void insert();


    IPage<PvStations> pageList(Page<PvStations> page, PageVo pageVo);


    List<PvStations> selectStationCount(String status);
}
