package org.jeecg.modules.pcms.rent.service;

import java.util.function.IntConsumer;

import org.jeecg.modules.pcms.rent.dto.RentAuditDTO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateCheckVO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateResultVO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateTaskVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerDetailVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerEditDTO;
import org.jeecg.modules.pcms.rent.dto.RentRegenerateDTO;

/**
 * 租金台账的生成、编辑与审核流转。对应需求稿第 7、8、12 条。
 *
 * @Version: V1.0
 */
public interface IRentLedgerBizService {

    /**
     * 生成某年度全部电站的四期台账。每年 3 月 1 日由定时任务触发，也可手工补跑。
     * 已存在的期不会重复生成，重复跑安全。
     *
     * 这是同步版本，跑完才返回。几万个电站要跑几分钟，
     * HTTP 接口走 {@link #submitGenerate(Integer)}，别直接调这个。
     */
    RentGenerateResultVO generateYear(Integer year);

    /**
     * 年度生成的执行体，给后台任务用。
     *
     * @param operUser   发起人。异步线程里拿不到请求上下文，只能传进来
     * @param onProgress 每处理完一个电站回调一次已处理数，传 null 表示不需要进度
     * @param projectId  只生成这个项目的电站，null=不限
     * @param tenantIds  只生成这些租户的电站，null=不限；空列表表示没有可用租户
     */
    RentGenerateResultVO generateYear(Integer year, String operUser, IntConsumer onProgress,
                                      Long projectId, java.util.List<Integer> tenantIds);

    /**
     * 提交年度生成任务，立刻返回任务状态，遍历所有电站的活交给后台线程。
     *
     * 同一时间只允许一个年度生成任务在跑（单线程池 + 任务表挡重复提交），
     * 已有任务在跑时直接报错，不会排两个队互相拖慢。
     *
     * @param projectId 只生成这个项目的电站，null=按当前账号的租户范围
     */
    RentGenerateTaskVO submitGenerate(Integer year, Long projectId);

    /** 查询年度生成任务状态，前端拿 taskId 轮询这个 */
    RentGenerateTaskVO generateTaskStatus(String taskId);

    /**
     * 重新生成某电站的租金。需求第20、21、25条。
     *
     * 重算范围由 form 决定：
     * - wholeYear=1  → 本年度全额，第1期一直算到年末（需求第25条「生成租金」按钮）
     * - periodNo     → 只重出这一期（需求第20条详情页的重新生成按钮）
     * - fromDate     → 从该日所在期起算到年末（需求第21条安装信息修改后）
     *
     * 方式：**原地更新已有台账，不新增行**。台账 ID 不变，
     * 所以附件、纠纷记录、审核记录都还挂在原来那条上，也不用再人工作废旧行。
     *
     * 已提交审核、审核通过、已支付的期一律跳过（已结算的数据不受影响）。
     * 人工录入的实际发电天数按年月保留，不会因为重算被清掉。
     */
    RentGenerateResultVO regenerate(RentRegenerateDTO form);

    /**
     * 「生成租金」按钮的前置检查。需求第25条。
     *
     * 判断该农户有没有可用的安装信息、本年度是否已经生成过租金，
     * 返回前端该弹什么提示（不能生成 / 二次确认 / 直接生成）。
     *
     * 判断放后端是为了和真正生成时用同一套条件：前端自己想判断"有没有安装信息"，
     * 得先拉农户详情再翻 installs 数组，而"有安装信息但没填单价"这种情况前端看不出来。
     */
    RentGenerateCheckVO checkBeforeGenerate(Long stationId, Integer year);

    /** 租金详情 */
    RentLedgerDetailVO detail(Long id);

    /** 修改月度实际发电天数和本期实付金额 */
    void edit(RentLedgerEditDTO form);

    /**
     * 补算租金：根据录入的发电天数重新计算本期应付金额（只计算不保存）。
     * 需求第16条：修改发电天数后点击补算租金按钮，一键按实际发电天数重算。
     * 返回计算后的金额和月明细给前端确认，前端确认后再调 edit 保存。
     */
    RentLedgerDetailVO recalcByGenDays(RentLedgerEditDTO form);

    /** 提交审核 */
    void submit(RentAuditDTO form);

    /** 审核通过 */
    void approve(RentAuditDTO form);

    /** 审核驳回，必须填备注 */
    void reject(RentAuditDTO form);

    /** 标记纠纷暂停支付，必须填备注 */
    void markDispute(RentAuditDTO form);

    /** 取消纠纷暂停支付，必须填备注 */
    void cancelDispute(RentAuditDTO form);

    /** 作废，同一电站同一期只剩一条时不允许作废 */
    void voidLedger(RentAuditDTO form);

    /** 标记已支付，审核通过后才能标 */
    void markPaid(RentAuditDTO form);
}
