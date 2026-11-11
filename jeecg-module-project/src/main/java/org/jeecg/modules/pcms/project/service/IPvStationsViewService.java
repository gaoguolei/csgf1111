package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.project.stationentity.PvStations;
import org.jeecg.modules.pcms.project.stationentity.PvStationsView;

import java.util.List;

public interface IPvStationsViewService extends IService<PvStationsView> {

    public void insert ();
}
