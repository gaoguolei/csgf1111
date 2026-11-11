package org.jeecg.modules.pcms.rent.service;

import org.jeecg.modules.pcms.rent.dto.ReconcileTaskVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 银行支付回单上传与对账。对应需求稿第 13、18、19 条。
 *
 * 对账是异步的：一次几十页 PDF 要逐页 OCR，同步接口必超时，
 * 所以上传接口只负责存文件、建任务、丢给后台线程，立刻返回 taskId；
 * 前端拿 taskId 轮询 taskStatus，跑完再取明细。
 *
 * @Version: V1.0
 */
public interface IRentReconcileService {

    /**
     * 提交回单对账任务。**不等待执行**，立刻返回任务ID。
     *
     * 客户不用把姓名和期数写进文件名，PDF 直接传，系统自己拆页；
     * 定位靠 OCR 出的【收款账号】找农户（姓名重名太多、还被加过后缀，不可靠）。
     *
     * 两条定位路径：
     *   ledgerId 为空 —— 台账列表的批量对账。先找该农户【已支付】的台账，
     *                   多条时用回单金额筛；多条金额还相同就报
     *                   "找不到对应期数，请人工处理"，不猜。
     *   ledgerId 有值 —— 详情页的单张对账。只认这一条台账，
     *                   校验回单收款账号是否属于该农户，金额按这条台账比对。
     *
     * 一页里并排多张回单、或一页几十行付款清单，都会拆成多条结果分别对账；
     * 定位不到台账的不存附件。
     *
     * @param files    可多个，每个可以是 PDF（自动拆页）或图片
     * @param ledgerId 详情页传当前台账ID；列表页批量对账不传
     * @param projectId 只在当前项目里找农户；不传按账号的租户范围
     * @return 任务ID，拿去轮询 taskStatus
     */
    String submitReconcile(MultipartFile[] files, Long ledgerId, Long projectId);

    /**
     * 查询对账任务状态。前端循环调这个，直到 status 变成 DONE 或 FAILED。
     *
     * @param taskId submitReconcile 返回的任务ID
     */
    ReconcileTaskVO taskStatus(String taskId);
}