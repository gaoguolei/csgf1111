package org.jeecg.modules.pcms.bpm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程审批策略工厂
 * @author wujun
 */
public class BpmApprovalStrategyFactory {
    /**
     * 流程审批策略map
     */
    private static final Map<String,BpmApprovalStrategy> BPM_APPROVAL_STRATEGY_MAP = new ConcurrentHashMap<>();

    /**
     * 获取流程审批策略
     * @param processCategory
     * @return
     */
    public static BpmApprovalStrategy getBpmApprovalStrategy(String processCategory){
        return BPM_APPROVAL_STRATEGY_MAP.get(processCategory);
    }

    /**
     * 注册流程审批策略
     * @param processCategory
     * @param bpmApprovalStrategy
     */
    public static void register(String processCategory,BpmApprovalStrategy bpmApprovalStrategy){
        BPM_APPROVAL_STRATEGY_MAP.put(processCategory,bpmApprovalStrategy);
    }
}
