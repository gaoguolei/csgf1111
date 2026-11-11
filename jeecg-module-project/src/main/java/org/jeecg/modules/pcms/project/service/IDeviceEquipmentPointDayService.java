package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointDay;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentDayExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDeviceEquipmentPointDayService extends IService<DeviceEquipmentPointDay> {

    void insert();

    List<DevicePointEquipmentDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


}
