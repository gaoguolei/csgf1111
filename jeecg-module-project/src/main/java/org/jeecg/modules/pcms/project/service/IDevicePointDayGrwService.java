package org.jeecg.modules.pcms.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointDayGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointDayExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointDayGrwService extends IService<DevicePointDayGrw> {

    void  insert();
    List<DevicePointDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);
}
