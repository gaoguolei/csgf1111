package org.jeecg.modules.pcms.rent.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.pcms.rent.dto.FarmerStationListVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationQueryDTO;
import org.jeecg.modules.pcms.rent.dto.ProjectOptionVO;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;

/**
 * 农户信息管理的查询服务。
 *
 * @Version: V1.0
 */
public interface IFarmerStationQueryService {

    /**
     * 农户信息分页列表
     */
    IPage<FarmerStationListVO> pageList(FarmerStationQueryDTO query, int pageNo, int pageSize);

    /**
     * 导出农户用：按与列表完全相同的条件取全部命中行（不分页）。
     * 与 pageList 共用同一套筛选逻辑，保证"导出的就是页面上筛出来的"。
     */
    List<TFarmerStation> listForExport(FarmerStationQueryDTO query);

    /**
     * 导出农户用：按勾选的ID取。列表页勾选若干条后导出走这里。
     * 同样带租户隔离：拿着别的租户的ID来查也查不出来。
     */
    List<TFarmerStation> listByIdsForExport(List<Long> ids);

    /**
     * 所属项目下拉框，大区 -> 项目 两级
     */
    List<ProjectOptionVO> projectOptions();
}
