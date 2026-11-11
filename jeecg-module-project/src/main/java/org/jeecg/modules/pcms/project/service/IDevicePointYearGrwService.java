package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointYearGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointYearGrwService extends IService<DevicePointYearGrw> {

    void insert();

    List<DevicePointYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);

}
