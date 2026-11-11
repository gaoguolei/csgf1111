package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pcms.energy.entity.DevicePointYear;
import org.jeecg.modules.pcms.energy.export.DevicePointYearExportVO;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IDevicePointYearService extends IService<DevicePointYear> {


    void insert();



    Integer isHaveMonth();

    void deleteMonth(String month);


    List<DevicePointYearExportVO> getExportExcelData(String taskId, HttpServletRequest request, DevicePageVo devicePageVo);

}
