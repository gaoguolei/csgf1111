package org.jeecg.modules.pcms.rent.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.RentAttachVO;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.service.IRentAttachService;
import org.jeecg.modules.pcms.rent.service.ITRentAttachService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金附件实现。
 *
 * 文件按 rentAttach/年月/UUID.扩展名 存到 ${jeecg.path.upload} 下：
 * 落盘用 UUID 是为了挡住同名覆盖和目录穿越，列表展示仍用用户上传时的原始文件名，
 * 两者在 t_rent_attach 里一一对应。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class RentAttachServiceImpl implements IRentAttachService {

    /** 附件在上传根目录下的子目录 */
    private static final String SUB_DIR = "rentAttach";
    /** 单个附件大小上限 20MB */
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    /** 允许上传的扩展名，可执行文件一律挡掉 */
    private static final String ALLOW_EXT =
            ".jpg.jpeg.png.bmp.gif.webp.pdf.doc.docx.xls.xlsx.txt.zip.rar.";

    @Value("${jeecg.path.upload}")
    private String uploadPath;

    @Autowired
    private ITRentAttachService attachService;
    @Autowired
    private ITRentLedgerService ledgerService;
    @Autowired
    private OperLogHelper operLog;
    @Autowired
    private RentRoleGuard roleGuard;

    @Override
    public List<RentAttachVO> listByLedger(Long ledgerId) {
        List<RentAttachVO> list = new ArrayList<>();
        if (ledgerId == null) {
            return list;
        }
        TRentLedger ledger = ledgerService.getById(ledgerId);
        boolean ledgerEditable = ledger != null && ledger.editable() && !ledger.paid()
                && !isHistory(ledger);

        for (TRentAttach a : attachService.list(new LambdaQueryWrapper<TRentAttach>()
                .eq(TRentAttach::getLedgerId, ledgerId)
                .eq(TRentAttach::getDelFlag, 0)
                .orderByAsc(TRentAttach::getId))) {
            boolean image = a.getIsImage() != null && a.getIsImage() == 1;
            list.add(new RentAttachVO()
                    .setId(a.getId())
                    .setLedgerId(a.getLedgerId())
                    .setBizType(a.getBizType())
                    .setBizTypeName(TRentAttach.bizTypeName(a.getBizType()))
                    .setFileName(a.getFileName())
                    .setFileExt(a.getFileExt())
                    .setFileSize(a.getFileSize())
                    .setIsImage(image ? 1 : 0)
                    .setAuditRound(a.getAuditRound() == null ? 0 : a.getAuditRound())
                    .setDownloadUrl("/rent/attach/download?id=" + a.getId())
                    .setPreviewUrl(image ? "/rent/attach/preview?id=" + a.getId() : null)
                    // 银行回单是对账凭证、审核附件是审核留痕，两者任何状态下都不允许删
                    .setDeletable(!a.receipt() && !a.audit() && ledgerEditable ? 1 : 0)
                    .setCreateBy(a.getCreateBy())
                    .setCreateTime(a.getCreateTime()));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentAttachVO upload(Long ledgerId, MultipartFile file, String bizType) {
        // 审核附件是四级审核人的留痕，普通附件仍归租金操作人
        if (TRentAttach.BIZ_AUDIT.equals(bizType)) {
            roleGuard.requireAuditOps("上传审核附件");
        } else {
            roleGuard.requireLedgerOps("上传附件");
        }
        TRentLedger ledger = ledgerService.getById(ledgerId);
        if (ledger == null || (ledger.getDelFlag() != null && ledger.getDelFlag() == 1)) {
            throw new JeecgBootException("租金台账不存在或已删除");
        }
        if (isHistory(ledger)) {
            throw new JeecgBootException("历史年度的租金台账只能查看，不支持上传附件");
        }
        if (ledger.voided()) {
            throw new JeecgBootException("已作废的台账不能上传附件");
        }
        String type = TRentAttach.BIZ_AUDIT.equals(bizType)
                ? TRentAttach.BIZ_AUDIT : TRentAttach.BIZ_ATTACH;
        checkUploadable(ledger, type);

        TRentAttach attach = store(ledgerId, readBytes(file), originalName(file), type);
        operLog.recordText(TOperLog.ACTION_UPLOAD, TOperLog.BIZ_ATTACH, ledgerId,
                "上传" + ledger.getPeriodName() + "租金"
                        + TRentAttach.bizTypeName(type) + "：" + attach.getFileName());

        List<RentAttachVO> list = listByLedger(ledgerId);
        for (RentAttachVO vo : list) {
            if (vo.getId().equals(attach.getId())) {
                return vo;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRentAttach saveReceipt(Long ledgerId, MultipartFile file) {
        return store(ledgerId, readBytes(file), originalName(file), TRentAttach.BIZ_RECEIPT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRentAttach saveReceipt(Long ledgerId, byte[] bytes, String fileName) {
        return store(ledgerId, bytes, fileName, TRentAttach.BIZ_RECEIPT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRentAttach linkReceipt(Long ledgerId, TRentAttach origin) {
        if (origin == null) {
            throw new JeecgBootException("缺少要复用的附件");
        }
        TRentAttach link = new TRentAttach()
                .setLedgerId(ledgerId)
                .setBizType(TRentAttach.BIZ_RECEIPT)
                .setFileName(origin.getFileName())
                .setFilePath(origin.getFilePath())
                .setFileExt(origin.getFileExt())
                .setFileSize(origin.getFileSize())
                .setIsImage(origin.getIsImage())
                .setAuditRound(currentAuditRound(ledgerId))
                .setDelFlag(0)
                .setCreateBy(operLog.currentUser())
                .setCreateTime(new Date());
        attachService.save(link);
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long attachId) {
        roleGuard.requireLedgerOps("删除附件");
        TRentAttach attach = getExisting(attachId);
        if (attach.receipt()) {
            throw new JeecgBootException("银行回单是对账凭证，不允许删除");
        }
        if (attach.audit()) {
            throw new JeecgBootException("审核附件是审核留痕，不允许删除");
        }
        TRentLedger ledger = ledgerService.getById(attach.getLedgerId());
        if (ledger != null) {
            if (isHistory(ledger)) {
                throw new JeecgBootException("历史年度的租金台账只能查看，不支持删除附件");
            }
            // 可删：待提交、已驳回、纠纷暂停（可编辑阶段），以及待支付
            //   —— 待支付要传付款凭证，传错了得能撤，所以增删都放开
            // 不可删：审核中（提交后不能改）、已支付/已对账（凭证留痕，只能增不能撤）
            boolean deletable = ledger.editable()
                    || TRentLedger.STATUS_PENDING_PAY.equals(ledger.statusCode());
            if (!deletable || ledger.paid()) {
                throw new JeecgBootException(ledger.getPeriodName() + "当前为"
                        + ledger.statusName() + "，不能再删除附件");
            }
        }
        // 只逻辑删除，磁盘上的文件留着：审核追溯时还要拿得到原件
        attachService.updateById(new TRentAttach().setId(attachId).setDelFlag(1));
        operLog.recordText(TOperLog.ACTION_DELETE, TOperLog.BIZ_ATTACH, attach.getLedgerId(),
                "删除租金附件：" + attach.getFileName());
    }

    @Override
    public TRentAttach getExisting(Long attachId) {
        if (attachId == null) {
            throw new JeecgBootException("缺少附件ID");
        }
        TRentAttach attach = attachService.getById(attachId);
        if (attach == null || (attach.getDelFlag() != null && attach.getDelFlag() == 1)) {
            throw new JeecgBootException("附件不存在或已删除");
        }
        return attach;
    }

    @Override
    public File resolveFile(TRentAttach attach) {
        File file = new File(uploadPath, attach.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new JeecgBootException("附件文件已丢失：" + attach.getFileName());
        }
        return file;
    }

    // ------------------------------------------------------------------

    /**
     * 什么时候允许上传。
     *
     * 业务附件（需求第 7 条 + 第 15 条）：
     *   待提交、已驳回、纠纷暂停：可编辑阶段，随时增删
     *   待支付：审核通过后要传付款凭证（转账回单、付款截图），业务要求放开
     *   已支付：需求第 15 条明确"已支付的支持上传附件"，可补传凭证
     *   已对账：属于归档，不再接受人工上传
     *   审核中：需求第 15 条"提交审核后不能进行修改"，附件也算修改的一部分
     *
     * 审核附件（需求第 15 条）：
     *   只在审核中可以传——审核人驳回时随手带的材料，其他阶段没有这个动作。
     */
    private void checkUploadable(TRentLedger ledger, String bizType) {
        if (TRentAttach.BIZ_AUDIT.equals(bizType)) {
            // 四级审核过程中每一级都能带材料
            if (TRentLedger.pendingLevel(ledger.getAuditStatus()) == null) {
                throw new JeecgBootException("审核附件只能在审核过程中上传，"
                        + ledger.getPeriodName() + "当前不在审核中");
            }
            return;
        }
        String status = ledger.statusCode();
        if (TRentLedger.STATUS_RECONCILED.equals(status)) {
            throw new JeecgBootException(ledger.getPeriodName() + "已对账归档，不能再上传附件");
        }
        // 已支付：需求第 15 条明确支持
        // 待支付：支付环节要传付款凭证，也被放开（见类注释）
        if (ledger.paid() || TRentLedger.STATUS_PENDING_PAY.equals(status)) {
            return;
        }
        // 剩下的可编辑状态（待提交、已驳回、纠纷暂停）放行；
        // 审核中会走到这里被拦下——提交后连附件也不能改
        if (!ledger.editable()) {
            throw new JeecgBootException(ledger.getPeriodName() + "当前为"
                    + ledger.statusName() + "，不能上传附件");
        }
    }

    private TRentAttach store(Long ledgerId, byte[] bytes, String originalName, String bizType) {
        if (bytes == null || bytes.length == 0) {
            throw new JeecgBootException("请选择要上传的文件");
        }
        if (bytes.length > MAX_SIZE) {
            throw new JeecgBootException("附件不能超过 " + (MAX_SIZE / 1024 / 1024) + "MB");
        }
        String name = originalName == null || originalName.trim().isEmpty()
                ? "未命名文件" : originalName;
        String ext = ext(name);
        if (!ALLOW_EXT.contains("." + ext + ".")) {
            throw new JeecgBootException("不支持的文件类型：" + (ext.isEmpty() ? "无扩展名" : ext));
        }

        String monthDir = new SimpleDateFormat("yyyyMM").format(new Date());
        File dir = new File(new File(uploadPath, SUB_DIR), monthDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new JeecgBootException("附件目录创建失败：" + dir.getAbsolutePath());
        }
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        File dest = new File(dir, storedName);
        try {
            Files.write(dest.toPath(), bytes);
        } catch (IOException e) {
            log.error("附件落盘失败 ledgerId={} file={}", ledgerId, name, e);
            throw new JeecgBootException("附件保存失败：" + e.getMessage());
        }

        TRentAttach attach = new TRentAttach()
                .setLedgerId(ledgerId)
                .setBizType(bizType)
                .setFileName(name)
                .setFilePath(SUB_DIR + "/" + monthDir + "/" + storedName)
                .setFileExt(ext)
                .setFileSize((long) bytes.length)
                .setIsImage(TRentAttach.imageExt(ext) ? 1 : 0)
                // 记下上传时的审核轮次，前端才分得清这个附件属于哪一次驳回
                .setAuditRound(currentAuditRound(ledgerId))
                .setDelFlag(0)
                .setCreateBy(operLog.currentUser())
                .setCreateTime(new Date());
        attachService.save(attach);
        return attach;
    }

    /** 读上传文件的字节。读不出来说明请求体有问题，给个明确提示 */
    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择要上传的文件");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new JeecgBootException("读取上传文件失败：" + e.getMessage());
        }
    }

    /** 台账当前的审核轮次，取不到台账按 0 */
    private int currentAuditRound(Long ledgerId) {
        TRentLedger ledger = ledgerService.getById(ledgerId);
        return ledger == null ? 0 : ledger.auditRound();
    }

    /** 有的浏览器传的是完整路径，只取文件名部分 */
    private String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.trim().isEmpty()) {
            return "未命名文件";
        }
        name = name.replace('\\', '/');
        int idx = name.lastIndexOf('/');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private String ext(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isHistory(TRentLedger ledger) {
        return ledger.getRentYear() != null
                && ledger.getRentYear() < Calendar.getInstance().get(Calendar.YEAR);
    }
}