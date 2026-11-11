package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.entity.PvEnergy;
import org.jeecg.modules.pcms.project.stationvo.StationCountVo;
import org.jeecg.modules.pcms.project.vo.PageVo;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:15
 * @Version 1.5
 */
public interface IPvEnergyService extends IService<PvEnergy> {

    //插入
    public void insert();

    //更新
    public void  updatePvEnergy();

    /**
     * 获取数据
     * @param pageVo
     * @return
     */
    public Result getStationChart(PageVo pageVo);

    public List<StationCountVo> weeks();

    public List<StationCountVo> month();

    public Result getStationInfoStatistics(PageVo pageVo);
}
