package org.jeecg.modules.pcms.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointMonth;
import org.jeecg.modules.pcms.energy.export.DevicePointMonthExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointMonthService extends IService<DevicePointMonth> {


    void insert();

    Integer isHaveMonth();

    void deleteMonth(String month);


    List<DevicePointMonthExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


    List<StationCountVo> month(String renterId);


}
