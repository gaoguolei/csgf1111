package org.jeecg.modules.pcms.rent.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.ReconcileResultVO;
import org.jeecg.modules.pcms.rent.dto.ReconcileTaskVO;
import org.jeecg.modules.pcms.rent.entity.TRentReconcileTask;
import org.jeecg.modules.pcms.rent.service.IRentReconcileService;
import org.jeecg.modules.pcms.rent.service.ITRentReconcileTaskService;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.jeecg.modules.pcms.rent.util.TenantScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 银行回单上传与对账（异步）。
 *
 * 本类只负责"存文件 + 建任务 + 丢给后台"，真正的拆页、OCR、对账在
 * RentReconcileTaskRunner 里跑。这样上传接口能立刻返回，
 * 前端拿 taskId 轮询进度，不会因为几十页 PDF 跑几分钟而超时。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentReconcileServiceImpl implements IRentReconcileService {

    /** 一次最多传多少个文件，再多前端等不起，OCR 也顶不住 */
    private static final int MAX_FILES = 100;
    /** 临时文件目录名，跑完就删 */
    private static final String TMP_DIR = "rent-reconcile-tmp";

    @Autowired
    private ITRentReconcileTaskService taskService;
    @Autowired
    private RentReconcileTaskRunner taskRunner;
    @Autowired
    private OperLogHelper operLog;
    /** 数据范围：普通用户按自己租户，管理员按顶部租户下拉 */
    @Autowired
    private TenantScopeHelper tenantScope;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RentRoleGuard roleGuard;

    @Value("${jeecg.path.upload}")
    private String uploadPath;

    @Override
    public String submitReconcile(MultipartFile[] files, Long ledgerId, Long projectId) {
        roleGuard.requirePaymentOps("提交对账");
        if (files == null || files.length == 0) {
            throw new JeecgBootException("请选择要上传的回单");
        }
        if (files.length > MAX_FILES) {
            throw new JeecgBootException(
                    "一次最多上传 " + MAX_FILES + " 个文件，本次 " + files.length + " 个，请分批上传");
        }

        // 异步线程里没有请求上下文（也读不到 renterId），范围必须在请求线程里先定下来，
        // 否则后台线程里 allowedTenantIds() 会退化成"不过滤"，别的项目/租户的数据也会被对上
        List<Integer> tenantIds = tenantScope.allowedTenantIds();
        if (tenantIds != null && tenantIds.isEmpty()) {
            throw new JeecgBootException("当前账号没有可操作的租户或项目，无法对账");
        }
        PdfReceiptReconciler.Scope scope = new PdfReceiptReconciler.Scope(projectId, tenantIds);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        String operUser = operLog.currentUser();
        Date now = new Date();

        // 先把文件落到临时目录 —— MultipartFile 出了请求就取不到了，
        // 后台线程读不到它，必须先存下来
        List<Path> saved = new ArrayList<>();
        File dir = new File(new File(uploadPath, TMP_DIR), taskId);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new JeecgBootException("临时目录创建失败：" + dir.getAbsolutePath());
        }
        int savedCount = 0;
        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String name = file.getOriginalFilename() == null
                        ? "未命名文件" : file.getOriginalFilename();
                File dest = new File(dir, savedCount + "_" + sanitize(name));
                file.transferTo(dest);
                saved.add(dest.toPath());
                savedCount++;
            }
        } catch (Exception e) {
            log.error("回单暂存失败 taskId={}", taskId, e);
            throw new JeecgBootException("文件保存失败：" + e.getMessage());
        }
        if (saved.isEmpty()) {
            throw new JeecgBootException("上传的文件都是空的");
        }

        taskService.save(new TRentReconcileTask()
                .setId(taskId)
                .setStatus(TRentReconcileTask.STATUS_PENDING)
                .setFileCount(saved.size())
                .setTotalPages(0)
                .setProgress(0)
                .setTotal(0)
                .setOkCount(0)
                .setErrorCount(0)
                .setFailCount(0)
                .setLedgerId(ledgerId)
                .setCreateBy(operUser)
                .setCreateTime(now));

        // 丢给后台线程，方法立刻返回
        taskRunner.run(taskId, saved, ledgerId, operUser, scope);
        log.info("回单对账任务已提交 taskId={} 文件数={} ledgerId={} 发起人={}",
                taskId, saved.size(), ledgerId, operUser);
        return taskId;
    }

    @Override
    public ReconcileTaskVO taskStatus(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new JeecgBootException("缺少任务ID");
        }
        TRentReconcileTask task = taskService.getById(taskId.trim());
        if (task == null) {
            throw new JeecgBootException("任务不存在或已被清理");
        }
        ReconcileTaskVO vo = new ReconcileTaskVO()
                .setTaskId(task.getId())
                .setStatus(task.getStatus())
                .setStatusName(TRentReconcileTask.statusName(task.getStatus()))
                .setProgress(nvl(task.getProgress()))
                .setTotalPages(nvl(task.getTotalPages()))
                .setTotal(nvl(task.getTotal()))
                .setOkCount(nvl(task.getOkCount()))
                .setErrorCount(nvl(task.getErrorCount()))
                .setFailCount(nvl(task.getFailCount()))
                .setErrorMsg(task.getErrorMsg())
                .setCreateTime(task.getCreateTime())
                .setFinishTime(task.getFinishTime());

        // 跑完了才返回明细，省得每次都把一大坨 JSON 解析出来
        if (TRentReconcileTask.STATUS_DONE.equals(task.getStatus())
                && task.getResultJson() != null && !task.getResultJson().isEmpty()) {
            try {
                vo.setItems(objectMapper.readValue(task.getResultJson(),
                        new TypeReference<List<ReconcileResultVO.Item>>() {
                        }));
            } catch (Exception e) {
                log.error("对账明细解析失败 taskId={}", taskId, e);
                // 明细坏了不影响"任务已完成"这个事实，返回空列表让前端能收尾
            }
        }
        return vo;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    /** 文件名里可能有路径分隔符，落盘前清掉，免得写到目录外面 */
    private String sanitize(String name) {
        String s = name.replace('\\', '_').replace('/', '_');
        return s.length() > 100 ? s.substring(s.length() - 100) : s;
    }
}