package org.jeecg.modules.pcms.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointDayGrw;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentDayExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentMonthExportVO;
import org.jeecg.modules.pcms.energy.export.DevicePointEquipmentYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDeviceEquipmentPointDayGrwService extends IService<DeviceEquipmentPointDayGrw> {

    void insert();



    List<DevicePointEquipmentDayExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


    List<DevicePointEquipmentMonthExportVO> getExportExcelDataMonth(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


    List<DevicePointEquipmentYearExportVO> getExportExcelDataYear(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);


}