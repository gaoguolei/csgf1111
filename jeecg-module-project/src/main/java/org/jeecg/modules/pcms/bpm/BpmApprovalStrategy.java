package org.jeecg.modules.pcms.bpm;

import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;

/**
 * 流程审批策略
 *
 * @author wujun
 */
public interface BpmApprovalStrategy {
    /**
     * 流程审批引擎
     *
     * @param
     * @return
     */
    public int approval(PvProjectInfo pvProjectInfo,PvProjectBpm pvProjectBpm);

    /**
     * 流程审批完成处理
     *
     * @param id
     * @return
     */
    public int processComplete(Integer id);
}
