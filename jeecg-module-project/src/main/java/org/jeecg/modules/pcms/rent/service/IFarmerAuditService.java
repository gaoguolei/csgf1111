package org.jeecg.modules.pcms.rent.service;

import java.util.Date;

import org.jeecg.modules.pcms.rent.entity.TFarmerChangeRequest;

/**
 * 农户信息变更审核。需求 28~33。
 *
 * 【流程】
 *   新增/修改农户  → 提交后进入"待项目审核"
 *   项目审核通过    → 转"待业务审核"
 *   业务审核通过    → 转"待领导审核"
 *   领导审核通过    → 状态恢复正常，租金解锁，申请完结
 *   任意一级驳回    → 申请终止，修改类数据回滚到修改前，新增类保留但不可用
 *   取消修改        → 同驳回，数据回滚
 *
 * 【锁定边界】
 *   按"提交审核时间"判断：某期只要还没结束（periodEnd >= 提交日），就算"提交之后"，
 *   一律锁住。这样审核期间没人能按旧数据把进行中的那期提交掉。
 *
 * @Version: V1.0
 */
public interface IFarmerAuditService {

    /** 农户当前是否有待审核的变更 */
    boolean isPending(Long stationId);

    /** 农户当前待审核的申请，没有返回 null */
    TFarmerChangeRequest pendingOf(Long stationId);

    /** 按申请ID查，审核弹窗用 */
    TFarmerChangeRequest getById(Long requestId);

    /**
     * 提交变更申请（由新增/修改农户的流程调用）。
     *
     * @param stationId  农户ID
     * @param changeType INSERT / UPDATE
     * @param beforeJson 修改前快照，新增传 null
     * @param afterJson  修改后快照
     */
    void submitChange(Long stationId, String changeType, String beforeJson, String afterJson);

    /**
     * 把最新快照并进该农户正在审核的申请。
     *
     * 审核期间又增删改了安装信息/租金时用：只刷新 afterJson，beforeJson 和提交时间都不动，
     * 否则审核弹窗比对的是旧快照看不出这次改动，驳回回滚也会把新数据删掉。
     *
     * @param stationId 农户ID
     * @param afterJson 最新快照
     * @return true=已合并；false=该农户没有待审核的申请
     */
    boolean mergePendingSnapshot(Long stationId, String afterJson);

    /** 审核通过：按申请当前阶段流转，领导级通过后状态恢复、租金解锁 */
    void approve(Long requestId, String remark);

    /** 审核驳回：填原因，终止本次申请，数据回滚到修改前 */
    void reject(Long requestId, String remark);

    /** 提交人在审核前取消：数据回滚 */
    void cancel(Long requestId);

    /**
     * 某一期租金是否因为农户审核而被锁。
     *
     * @param stationId 农户ID
     * @param periodEnd 该期计租截止日
     * @return true=锁定，不能做任何操作
     */
    boolean periodLocked(Long stationId, Date periodEnd);

    /** 生成完整快照（农户主表 + 安装信息 + 租金档位） */
    String snapshot(Long stationId);

    /** 按快照回滚。快照为空时什么都不做 */
    void restore(Long stationId, String json);
}
