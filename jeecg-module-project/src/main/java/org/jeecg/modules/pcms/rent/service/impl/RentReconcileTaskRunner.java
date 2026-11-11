package org.jeecg.modules.pcms.rent.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.ReconcileResultVO;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TRentReconcileTask;
import org.jeecg.modules.pcms.rent.ocr.util.PdfPageSplitter;
import org.jeecg.modules.pcms.rent.service.ITRentReconcileTaskService;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 回单对账的异步执行体。
 *
 * 单独拆成 Bean 是为了让 @Async 生效 —— 同类内部调用走不到代理，
 * 在 RentReconcileServiceImpl 里自调的话根本不会异步，还是在请求线程里跑。
 *
 * 执行过程会不断更新任务表的 progress，前端轮询这个字段显示进度。
 * 每页的对账各自独立事务（PdfReceiptReconciler 上是 REQUIRES_NEW），
 * 所以中途某页出错不会把前面已完成的对账回滚掉。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class RentReconcileTaskRunner {

    @Autowired
    private ITRentReconcileTaskService taskService;
    @Autowired
    private PdfPageSplitter pdfSplitter;
    @Autowired
    private PdfReceiptReconciler pdfReceiptReconciler;
    @Autowired
    private OperLogHelper operLog;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 后台跑一个对账任务。调用方拿到 taskId 就返回，不要等这个方法。
     *
     * @param taskId   任务ID
     * @param filePaths 已落盘的待处理文件
     * @param ledgerId 详情页传的台账ID，批量对账为 null
     * @param operUser 发起人，异步线程里没有请求上下文，只能靠传进来
     */
    @Async("exportTaskExecutor")
    public void run(String taskId, List<Path> filePaths, Long ledgerId, String operUser,
                    PdfReceiptReconciler.Scope scope) {
        long start = System.currentTimeMillis();
        List<ReconcileResultVO.Item> allItems = new ArrayList<>();
        int okCount = 0;
        int errorCount = 0;
        int failCount = 0;
        int totalPages = 0;
        int progress = 0;

        try {
            markRunning(taskId);

            // 先把所有文件拆完，拿到总页数，前端才能算出百分比
            List<PdfPageSplitter.PageImage> pages = new ArrayList<>();
            for (Path p : filePaths) {
                String name = p.getFileName().toString();
                try {
                    pages.addAll(pdfSplitter.split(Files.readAllBytes(p), name));
                } catch (Exception e) {
                    log.error("文件拆分失败 file={}", name, e);
                    allItems.add(failItem(name, "文件解析失败：" + e.getMessage()));
                    failCount++;
                }
            }
            totalPages = pages.size();
            taskService.updateById(new TRentReconcileTask()
                    .setId(taskId).setTotalPages(totalPages).setProgress(0));

            // 逐页识别 + 对账，每跑完一页就把进度写回库
            for (PdfPageSplitter.PageImage page : pages) {
                List<ReconcileResultVO.Item> items;
                try {
                    items = pdfReceiptReconciler.handleImage(
                            page.getBytes(), page.getFileName(), ledgerId, operUser, scope);
                } catch (Exception e) {
                    log.error("回单对账失败 file={}", page.getFileName(), e);
                    items = new ArrayList<>();
                    items.add(failItem(page.getFileName(), "对账失败：" + e.getMessage()));
                }
                for (ReconcileResultVO.Item item : items) {
                    allItems.add(item);
                    if (PdfReceiptReconciler.RESULT_OK.equals(item.getResult())) {
                        okCount++;
                    } else if (PdfReceiptReconciler.RESULT_ERROR.equals(item.getResult())) {
                        errorCount++;
                    } else {
                        failCount++;
                    }
                }
                progress++;
                taskService.updateById(new TRentReconcileTask()
                        .setId(taskId).setProgress(progress));
            }

            finish(taskId, allItems, okCount, errorCount, failCount);
            long cost = System.currentTimeMillis() - start;
            log.info("回单对账任务完成 taskId={} 页数={} 共{}条 已对账{} 异常{} 未处理{} 耗时{}ms",
                    taskId, totalPages, allItems.size(), okCount, errorCount, failCount, cost);
            operLog.recordText(operUser, TOperLog.ACTION_RECONCILE, TOperLog.BIZ_LEDGER, ledgerId,
                    String.format("回单对账任务完成：共%d页 %d条，已对账%d条，对账异常%d条，未能处理%d条，耗时%dms",
                            totalPages, allItems.size(), okCount, errorCount, failCount, cost));

        } catch (Exception e) {
            // 任务整体挂了（读文件失败、线程异常等），明细没生成
            log.error("回单对账任务失败 taskId={}", taskId, e);
            taskService.updateById(new TRentReconcileTask()
                    .setId(taskId)
                    .setStatus(TRentReconcileTask.STATUS_FAILED)
                    .setErrorMsg(brief(e.getMessage()))
                    .setProgress(progress)
                    .setFinishTime(new Date()));
            operLog.recordText(operUser, TOperLog.ACTION_RECONCILE, TOperLog.BIZ_LEDGER, ledgerId,
                    "回单对账任务失败：" + e.getMessage());
        } finally {
            cleanup(filePaths);
        }
    }

    private void markRunning(String taskId) {
        taskService.updateById(new TRentReconcileTask()
                .setId(taskId)
                .setStatus(TRentReconcileTask.STATUS_RUNNING));
    }

    /** 收尾：把明细序列化进任务记录，状态置成已完成 */
    private void finish(String taskId, List<ReconcileResultVO.Item> items,
                        int okCount, int errorCount, int failCount) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            // 明细存不进去不影响对账结果本身，日志留一笔就够了
            log.error("对账明细序列化失败 taskId={}", taskId, e);
        }
        taskService.updateById(new TRentReconcileTask()
                .setId(taskId)
                .setStatus(TRentReconcileTask.STATUS_DONE)
                .setTotal(items.size())
                .setOkCount(okCount)
                .setErrorCount(errorCount)
                .setFailCount(failCount)
                .setResultJson(json)
                .setFinishTime(new Date()));
    }

    private ReconcileResultVO.Item failItem(String fileName, String message) {
        return new ReconcileResultVO.Item()
                .setFileName(fileName)
                .setAttached(0)
                .setResult(PdfReceiptReconciler.RESULT_FAIL)
                .setMessage(message);
    }

    /** 临时文件用完就删，跑挂了的也删 */
    private void cleanup(List<Path> filePaths) {
        for (Path p : filePaths) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception e) {
                log.warn("删除临时文件失败 {}", p, e);
            }
        }
        // 顺带把这一批的临时目录也清掉
        if (!filePaths.isEmpty()) {
            File dir = filePaths.get(0).toFile().getParentFile();
            if (dir != null && !dir.delete()) {
                log.debug("临时目录未删净（可能还有文件）{}", dir);
            }
        }
    }

    private String brief(String msg) {
        if (msg == null) {
            return null;
        }
        String s = msg.replace("\n", " ").trim();
        return s.length() > 480 ? s.substring(0, 480) + "…" : s;
    }
}