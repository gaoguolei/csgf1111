package org.jeecg.modules.pcms.rent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.pcms.rent.dto.OperLogVO;

/**
 * 操作日志查询。需求稿第 11 条：所有操作页面右侧要显示操作日志，
 * 而且得看得出改了什么、谁改的。
 *
 * @Version: V1.0
 */
public interface IOperLogQueryService {

    /** 按业务类型和业务主键查，bizType/bizId 都可不传 */
    IPage<OperLogVO> pageList(String bizType, Long bizId, String operUser, int pageNo, int pageSize);

    /**
     * 某农户相关的全部日志：农户本身 + 名下安装信息 + 名下租金台账，
     * 农户详情页右侧直接用这个，不用前端拼三次请求。
     */
    IPage<OperLogVO> pageByStation(Long stationId, int pageNo, int pageSize);

    /** 某条租金台账相关的日志：台账本身 + 它的附件 */
    IPage<OperLogVO> pageByLedger(Long ledgerId, int pageNo, int pageSize);
}
