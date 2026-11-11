package org.jeecg.modules.pcms.rent.controller;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.RentAuditDTO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateCheckVO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateResultVO;
import org.jeecg.modules.pcms.rent.dto.RentGenerateTaskVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerDetailVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerEditDTO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerListVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerQueryDTO;
import org.jeecg.modules.pcms.rent.dto.RentRegenerateDTO;
import org.jeecg.modules.pcms.rent.service.IRentLedgerBizService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租金支付管理 - 租金台账。
 * 对应需求稿第 6～10、12 条。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-租金台账")
@RestController
@RequestMapping("/rent/ledger")
public class RentLedgerController {

    @Autowired
    private IRentLedgerQueryService queryService;
    @Autowired
    private IRentLedgerBizService bizService;

    // ==================== 第 6、9 条：列表 ====================


    @ApiOperation(value = "租金台账分页列表",
            notes = "默认只返回本年度全部期数；历史租金页签传 history=1，返回往年的，"
                    + "历史台账只能看不能改。姓名、户号、地址、身份证号为模糊查询。"
                            + "状态页签传 status（DRAFT待提交/PENDING_AUDIT待审核/REJECTED驳回/"
                            + "PENDING_PAY待支付/PAID已支付/RECONCILED已对账/"
                            + "RECON_ERROR对账异常/DISPUTE纠纷暂停/VOID已作废）。"
                            + "其中 PENDING_AUDIT 包含项目、业务、财务、领导四级的待审，"
                            + "REJECTED 包含四级的驳回。"
                            + "明细状态（PENDING_PROJECT_AUDIT/PROJECT_REJECTED 等）仍然可以传，"
                            + "后端按原口径精确筛选，老前端不用改；"
                            + "auditStatus 数值也支持逗号分隔的多个值（如 2,4,6,8）。"
                            + "返回值里的 statusCode、statusName 仍是明细状态")
    @GetMapping("/list")
    public Result<IPage<RentLedgerListVO>> list(RentLedgerQueryDTO query,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(queryService.pageList(query, pageNo, pageSize));
    }

    @ApiOperation(value = "各状态页签的数量",
            notes = "入参和 /list 一样（姓名、户号、项目、年度、并网日期等），"
                    + "但 status 字段会被忽略 —— 一次返回全部状态的数量。"
                    + "返回 key：ALL/DRAFT/PENDING_AUDIT/REJECTED/PENDING_PAY/PAID/"
                    + "RECONCILED/RECON_ERROR/DISPUTE/VOID，审核不再细分到四级。"
                    + "前端页签的数字用它刷新，会随筛选条件变化")
    @GetMapping("/statusCount")
    public Result<Map<String, Integer>> statusCount(RentLedgerQueryDTO query) {
        return Result.OK(queryService.statusCount(query));
    }

    @ApiOperation(value = "某电站的租金台账",
            notes = "农户安装信息详情页的租金页签用，不传 year 取本年度")
    @GetMapping("/listByStation")
    public Result<List<RentLedgerListVO>> listByStation(@RequestParam(name = "stationId") Long stationId,
                                                        @RequestParam(name = "year", required = false) Integer year) {
        return Result.OK(queryService.listByStation(stationId, year));
    }

    // ==================== 第 7 条：详情与编辑 ====================

    @ApiOperation(value = "租金详情",
            notes = "返回台账主信息 + 本期各月明细 + 附件。月明细里的实际发电天数可录入")
    @GetMapping("/detail")
    public Result<RentLedgerDetailVO> detail(@RequestParam(name = "id") Long id) {
        return Result.OK(bizService.detail(id));
    }

    @ApiOperation(value = "补算租金",
            notes = "根据录入的发电天数重新计算本期应付金额，只计算不保存。"
                    + "修改发电天数后点击补算租金按钮，一键按实际发电天数重算当期租金。"
                    + "返回计算后的月明细和应付总额，前端展示给用户确认，确认后再调 edit 保存。"
                    + "实际发电天数可能超过当月天数最大值，已在后端放开限制")
    @PostMapping("/recalcByGenDays")
    public Result<RentLedgerDetailVO> recalcByGenDays(@RequestBody RentLedgerEditDTO form) {
        return Result.OK(bizService.recalcByGenDays(form));
    }

    @ApiOperation(value = "修改租金详情",
            notes = "只能改每月实际发电天数和本期实付金额，其余金额系统按公式重算；"
                    + "本期实付一改，该电站后面所有期的累计实付会同步重排")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody RentLedgerEditDTO form) {
        bizService.edit(form);
        return Result.OK("保存成功");
    }

    // ==================== 第 7、12 条：生成 ====================

    @ApiOperation(value = "提交年度租金台账生成任务（异步）",
            notes = "★ 本接口不等待生成完成，立刻返回 taskId 和初始状态，前端拿 taskId 轮询 /generateTask。"
                    + "年度生成要遍历该年度全部电站，几万个电站跑几分钟，同步接口会被网关 504 掉。"
                    + "范围：传 projectId 只生成这个项目；不传则按当前账号的租户范围"
                    + "（普通用户=自己的租户，管理员=顶部租户下拉的 renterId，都没有=全部）。"
                    + "同一时间只允许一个生成任务在跑，已有任务时本接口会报错。"
                    + "已经生成过的期会跳过，可以重复提交。正常由每年 3 月 1 日的定时任务触发，这个接口用于手工补跑")
    @PostMapping("/generate")
    public Result<RentGenerateTaskVO> generate(@RequestParam(name = "year", required = false) Integer year,
                                               @RequestParam(name = "projectId", required = false) Long projectId) {
        return Result.OK(bizService.submitGenerate(year, projectId));
    }

    @ApiOperation(value = "查询年度生成任务状态",
            notes = "前端循环调用，直到 status 变成 DONE 或 FAILED。"
                    + "RUNNING 时用 progress/totalStations 显示进度；"
                    + "DONE 时读 createdCount/skippedCount/failCount/periodRange，"
                    + "跳过和失败的原因在 skipDetails/failDetails 里；"
                    + "FAILED 时看 errorMsg。建议 2 秒轮一次")
    @GetMapping("/generateTask")
    public Result<RentGenerateTaskVO> generateTask(@RequestParam(name = "taskId") String taskId) {
        return Result.OK(bizService.generateTaskStatus(taskId));
    }

    @ApiOperation(value = "重新生成某电站的租金",
            notes = "需求第20条：详情页的重新生成按钮，传当前期即可。"
                    + "需求第21条：安装信息修改后按修改当日所属期起算。"
                    + "需求第25条：农户详情的「生成租金」按钮传 wholeYear=1，出本年度全额。"
                    + "范围是「起始期到年末」，不是只算一期；已有的台账原地更新，不新增行，"
                    + "所以附件、纠纷记录都还在，也不用再人工作废旧行。"
                    + "已提交审核、已审核通过、已支付的期一律跳过（已结算的数据不受影响）。"
                    + "人工录入的实际发电天数会保留。"
                    + "请求体：{\"stationId\": 4288, \"year\": 2026, \"wholeYear\": 1}；"
                    + "或 {\"stationId\": 4288, \"periodNo\": 4} 只从第4期起算；"
                    + "或 {\"stationId\": 4288, \"fromDate\": \"2026-10-13\"} 从该日所在期起算")
    @PostMapping("/regenerate")
    public Result<RentGenerateResultVO> regenerate(@RequestBody RentRegenerateDTO form) {
        return Result.OK(bizService.regenerate(form));
    }

    @ApiOperation(value = "生成租金前置检查（需求第25条）",
            notes = "农户详情页点「生成租金」时先调这个，按返回的 action 决定下一步："
                    + "BLOCK_NO_INSTALL 弹提示不让生成（message=请先添加组件安装信息）；"
                    + "CONFIRM_UPDATE 弹二次确认（租金已存在，生成会覆盖金额）；"
                    + "DIRECT 直接生成。message 是给用户看的现成文案")
    @GetMapping("/generateCheck")
    public Result<RentGenerateCheckVO> generateCheck(
            @RequestParam(name = "stationId") Long stationId,
            @RequestParam(name = "year", required = false) Integer year) {
        return Result.OK(bizService.checkBeforeGenerate(stationId, year));
    }

    // ==================== 第 8 条：审核流转 ====================

    @ApiOperation(value = "提交审核",
            notes = "支持批量。提交后进入项目审核（待项目审核），由项目经理/管理员自审。"
                    + "标记了纠纷暂停支付的不能提交；"
                    + "同一电站同一期已有台账在审、已通过或已支付时，不能重复提交。"
                    + "管理员、项目经理、业务专员、公司领导可以提交")
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody RentAuditDTO form) {
        bizService.submit(form);
        return Result.OK("提交成功");
    }

    @ApiOperation(value = "审核通过",
            notes = "支持批量。按当前状态自动判断阶段——"
                    + "待项目审核时只有项目经理/管理员能通过，通过后进入待业务审核；"
                    + "待业务审核时只有业务专员能通过，通过后进入待财务审核；"
                    + "待财务审核时只有财务专员能通过，通过后进入待领导审核；"
                    + "待领导审核时只有公司领导能通过，通过后进入待支付")
    @PostMapping("/approve")
    public Result<String> approve(@RequestBody RentAuditDTO form) {
        bizService.approve(form);
        return Result.OK("审核通过");
    }

    @ApiOperation(value = "审核驳回",
            notes = "驳回原因非必填。需要附材料的，先用 /rent/attach/upload 传 bizType=AUDIT 的附件再调本接口。"
                    + "任何一级驳回都回到可修改状态，改完重新提交后从项目审核重新开始。"
                    + "每一级只有本级审核角色能驳回")
    @PostMapping("/reject")
    public Result<String> reject(@RequestBody RentAuditDTO form) {
        bizService.reject(form);
        return Result.OK("已驳回");
    }

    @ApiOperation(value = "标记纠纷暂停支付", notes = "必须填纠纷说明。标记后不能提交审核")
    @PostMapping("/dispute")
    public Result<String> dispute(@RequestBody RentAuditDTO form) {
        bizService.markDispute(form);
        return Result.OK("已标记纠纷暂停支付");
    }

    @ApiOperation(value = "取消纠纷暂停支付", notes = "必须填说明。原纠纷说明不会被清掉，新说明追加在后面")
    @PostMapping("/cancelDispute")
    public Result<String> cancelDispute(@RequestBody RentAuditDTO form) {
        bizService.cancelDispute(form);
        return Result.OK("已取消纠纷标记");
    }

    @ApiOperation(value = "作废台账",
            notes = "必须填作废原因。同一电站同一期只剩一条台账时不允许作废；"
                    + "已提交审核、已通过、已支付的也不能作废")
    @PostMapping("/void")
    public Result<String> voidLedger(@RequestBody RentAuditDTO form) {
        bizService.voidLedger(form);
        return Result.OK("已作废");
    }

    @ApiOperation(value = "标记已支付",
            notes = "支持批量。领导审核通过后才能标记，只有业务专员可以操作")
    @PostMapping("/markPaid")
    public Result<String> markPaid(@RequestBody RentAuditDTO form) {
        bizService.markPaid(form);
        return Result.OK("已标记支付");
    }
}
