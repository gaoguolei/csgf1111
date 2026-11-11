package org.jeecg.modules.pcms.aspect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jun
 */
public class PvOperateLogStrategyFactory {
    private static Map<String,PvOperateLogStrategy> strategyMap = new ConcurrentHashMap<>();

    public static PvOperateLogStrategy getOperateLogStrategy(String TransactionTypeCode){
        return strategyMap.get(TransactionTypeCode);
    }

    public static void register(String TransactionTypeCode,PvOperateLogStrategy operateLogStrategy){
        strategyMap.put(TransactionTypeCode,operateLogStrategy);
    }
}
