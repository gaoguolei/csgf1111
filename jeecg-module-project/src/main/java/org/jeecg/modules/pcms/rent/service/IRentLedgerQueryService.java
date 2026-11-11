package org.jeecg.modules.pcms.rent.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.pcms.rent.dto.RentLedgerListVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerQueryDTO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerSummaryVO;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;

/**
 * 租金台账查询。对应需求稿第 6、9、10 条。
 *
 * @Version: V1.0
 */
public interface IRentLedgerQueryService {

    /** 台账分页列表，本年度与历史租金两个页签共用 */
    IPage<RentLedgerListVO> pageList(RentLedgerQueryDTO query, int pageNo, int pageSize);

    /**
     * 各状态页签的数量。
     *
     * 入参用和列表完全相同的查询条件（status 字段除外 —— 会被忽略），
     * 返回 key 为统一状态编码（ALL/DRAFT/PENDING_PROJECT_AUDIT/.../VOID）的数量。
     * 前端一次请求就能把页签上的数字刷成当前筛选条件下的值。
     */
    Map<String, Integer> statusCount(RentLedgerQueryDTO query);

    /**
     * 导出台账用：按与列表完全相同的条件取全部命中行（不分页）。
     * 与 pageList 共用同一套筛选逻辑，保证"导出的就是页面上筛出来的"。
     */
    List<TRentLedger> listForExport(RentLedgerQueryDTO query);

    /**
     * 导出台账用：按勾选的ID取。列表页勾选若干条后导出走这里。
     * 同样带租户隔离：拿着别的租户的ID来查也查不出来。
     */
    List<TRentLedger> listByIdsForExport(List<Long> ids);

    /**
     * 按「项目 + 期数」汇总一批台账：条数、本期应付合计、本期实付合计。
     * 导出的"汇总"Sheet 用它，入参就是本次要导出的那批数据。
     */
    List<RentLedgerSummaryVO> summarize(List<TRentLedger> list);

    /** 某电站某年度的台账，农户详情页的租金页签用；year 不传取本年度 */
    List<RentLedgerListVO> listByStation(Long stationId, Integer year);

    /** 实体转列表行，详情接口复用 */
    RentLedgerListVO toListVO(TRentLedger ledger);
}
