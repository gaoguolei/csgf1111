package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMonth;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentMonthExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDeviceEquipmentPointMonthService extends IService<DeviceEquipmentPointMonth> {

    void insert();

    void deleteMonty(String month);

    Integer isHaveMonth();


    List<DevicePointEquipmentMonthExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);
}
