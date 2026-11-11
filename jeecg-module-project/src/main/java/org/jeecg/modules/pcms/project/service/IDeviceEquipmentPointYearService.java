package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointYear;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDeviceEquipmentPointYearService extends IService<DeviceEquipmentPointYear> {
    void insert();


    void deleteYear(String year);

    Integer isHaveMonth();

    List<DevicePointEquipmentYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);

}
