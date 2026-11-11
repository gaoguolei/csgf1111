package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointDay;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointDayService extends IService<DevicePointDay> {
    void insert();



    List<DevicePointDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


    List<StationCountVo> weeks(String renterId);

}
