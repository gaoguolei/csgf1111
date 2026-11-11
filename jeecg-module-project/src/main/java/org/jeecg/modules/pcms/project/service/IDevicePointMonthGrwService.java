package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointMonthGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointMonthExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointMonthGrwService extends IService<DevicePointMonthGrw> {

    void insert();



    List<DevicePointMonthExportVO> getExportGrwExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);
}
