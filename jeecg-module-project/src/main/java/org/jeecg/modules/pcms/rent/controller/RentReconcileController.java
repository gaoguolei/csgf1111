package org.jeecg.modules.pcms.rent.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.ReconcileTaskVO;
import org.jeecg.modules.pcms.rent.service.IRentReconcileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金支付管理 - 银行回单对账。对应需求稿第 13、18、19 条。
 *
 * 对账是异步的：一次几十页 PDF 要逐页 OCR，跑几分钟很正常，
 * 同步接口必超时。所以：
 *   ① submit 上传文件，立刻返回 taskId
 *   ② 前端拿 taskId 轮询 status，跑完再取明细
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-回单对账")
@RestController
@RequestMapping("/rent/reconcile")
public class RentReconcileController {

    @Autowired
    private IRentReconcileService reconcileService;

    @ApiOperation(value = "提交回单对账任务（异步）",
            notes = "客户直接把 PDF 或图片传过来即可，不用把姓名和期数写进文件名。"
                    + "系统自动把 PDF 拆成一页一页、把一页里并排的多张回单或几十行付款清单拆成多笔。"
                    + "【不传 ledgerId】台账列表的批量对账：用 OCR 出的收款账号找农户 → "
                    + "找该农户【已支付】的台账 → 用金额筛出唯一那一期；多条已支付且金额相同时"
                    + "报「找不到对应期数，请人工处理」，不会乱猜。"
                    + "【传了 ledgerId】详情页的单张对账：只认这一条台账，"
                    + "校验回单收款账号是否属于该农户。定位不到台账的回单不会存附件。"
                    + "范围：传 projectId 只在这个项目里找农户；不传按当前账号的租户范围。"
                    + "★ 本接口不等待对账完成，立刻返回 taskId，前端拿它轮询 /taskStatus")
    @PostMapping("/submit")
    public Result<String> submit(@RequestParam("files") MultipartFile[] files,
                                 @RequestParam(name = "ledgerId", required = false) Long ledgerId,
                                 @RequestParam(name = "projectId", required = false) Long projectId) {
        return Result.OK("任务已提交", reconcileService.submitReconcile(files, ledgerId, projectId));
    }

    @ApiOperation(value = "查询对账任务状态",
            notes = "前端循环调用，直到 status 变成 DONE 或 FAILED。"
                    + "RUNNING 时看 progress/totalPages 显示进度；"
                    + "DONE 时 items 里是每条回单的对账结果（结构同对账接口）；"
                    + "FAILED 时看 errorMsg。建议 1~2 秒轮一次")
    @GetMapping("/taskStatus")
    public Result<ReconcileTaskVO> taskStatus(@RequestParam(name = "taskId") String taskId) {
        return Result.OK(reconcileService.taskStatus(taskId));
    }
}