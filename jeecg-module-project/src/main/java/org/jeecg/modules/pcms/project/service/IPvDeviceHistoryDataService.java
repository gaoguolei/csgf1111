package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.stationentity.PvDeviceHistoryData;
import org.jeecg.modules.pcms.project.vo.PageVo;

/**
 * @Author gongzhou
 * @Date 2023/11/11 下午4:30
 * @Version 1.5
 */
public interface IPvDeviceHistoryDataService extends IService<PvDeviceHistoryData> {

    public void insert();
    public void updateData();

    Result deviceReport(PageVo pageVo);
}
