package org.jeecg.modules.pcms.rent.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.dto.FarmerStationDetailVO;
import org.jeecg.modules.pcms.rent.dto.RentAttachVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerDetailVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerListVO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerSummaryVO;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.service.IFarmerStationBizService;
import org.jeecg.modules.pcms.rent.service.IRentAttachService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 把农户 / 租金台账的数据组装成 PDF。需求第24条。
 *
 * 【为什么一个记录一个 PDF】
 * 这两个 PDF 是给农户的对账单、给运营的单户档案，按户分发。
 * 合成一个 PDF 反而不好拆，所以列表页多选 N 条就出 N 个文件，
 * 最后打成一个 zip 一次性下载（避免浏览器连弹 N 个下载框）。
 *
 * 数据直接复用详情接口的那两个 VO：农户详情已经把安装信息和各档位单价都组装好了，
 * 台账详情的安装信息也是同一份，不用再拼一遍查询。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class RentPdfAssembler {

    /** 文件名里不能出现的字符，统一替换掉，否则 zip 里会出现奇怪的名字 */
    private static final String ILLEGAL_FILENAME_CHARS = "[\\\\/:*?\"<>|\\r\\n]";

    /** 文件名长度上限，太长 Windows 保存会失败 */
    private static final int MAX_NAME_LEN = 60;

    @Autowired
    private IFarmerStationBizService farmerStationBizService;
    @Autowired
    private IRentLedgerBizService rentLedgerBizService;
    @Autowired
    private IRentAttachService attachService;

    /** 一个农户信息单：内容 + 建议的文件名 */
    public Doc farmerDoc(Long stationId) {
        FarmerStationDetailVO d = farmerStationBizService.detail(stationId);
        List<RentPdfBuilder.Block> blocks = new ArrayList<>();
        blocks.add(RentPdfBuilder.kv("基本信息", farmerBase(d)));
        blocks.add(RentPdfBuilder.table("安装信息", INSTALL_HEADERS, INSTALL_WIDTHS,
                installRows(d.getInstalls())));
        String name = safe(d.getFarmerName()) + "-" + safe(d.getPowerUserNo());
        return new Doc(name, RentPdfBuilder.build("农户信息单", blocks));
    }

    /** 一条租金对账单：内容 + 建议的文件名 */
    public Doc ledgerDoc(Long ledgerId) {
        RentLedgerDetailVO d = rentLedgerBizService.detail(ledgerId);
        List<RentPdfBuilder.Block> blocks = new ArrayList<>();
        appendLedgerBlocks(blocks, d);

        RentLedgerListVO b = d.getBase();
        // 带上台账ID：同一农户同期可能有两条未作废的台账（重新生成产生的），文件名不能撞
        String name = safe(b.getFarmerName()) + "-" + safe(b.getPeriodName()) + "-" + ledgerId;
        return new Doc(name, RentPdfBuilder.build("租金对账单", blocks));
    }

    /**
     * 多条台账合成一个 PDF：最前面是跨户汇总，之后逐条列出。
     *
     * 与 Excel 的「汇总 + 明细」两个 Sheet 对应：
     *   汇总按「项目 + 期数」统计条数和金额；明细是每条台账的完整对账单。
     * 每条之间用「【农户 期数】」标题分隔，内容多了 iText 会自动分页。
     */
    public Doc ledgerBatchDoc(List<Long> ledgerIds) {
        List<RentLedgerDetailVO> details = new ArrayList<>();
        for (Long id : ledgerIds) {
            details.add(rentLedgerBizService.detail(id));
        }

        List<RentPdfBuilder.Block> blocks = new ArrayList<>();
        blocks.add(RentPdfBuilder.table("汇总",
                new String[]{"项目", "期数", "租金条数", "本期应付合计(元)", "本期实付合计(元)"},
                new float[]{28f, 16f, 12f, 22f, 22f},
                summaryRows(details)));

        for (RentLedgerDetailVO d : details) {
            RentLedgerListVO b = d.getBase();
            List<RentPdfBuilder.Block> one = new ArrayList<>();
            appendLedgerBlocks(one, d);
            blocks.add(RentPdfBuilder.heading(
                    "【" + safe(b.getFarmerName()) + "  " + safe(b.getPeriodName()) + "】"));
            blocks.addAll(one);
        }
        return new Doc("租金台账", RentPdfBuilder.build("租金台账", blocks));
    }

    /** 一条台账的完整区块：基本信息 + 金额 + 月明细 + 安装信息 + 附件 */
    private void appendLedgerBlocks(List<RentPdfBuilder.Block> blocks, RentLedgerDetailVO d) {
        blocks.add(RentPdfBuilder.kv("基本信息", ledgerBase(d)));
        blocks.add(RentPdfBuilder.kv("金额信息", ledgerMoney(d)));
        blocks.add(RentPdfBuilder.table("本期月明细",
                new String[]{"月份", "当月天数", "计租天数", "实际发电天数", "当月金额(元)"},
                new float[]{24f, 19f, 19f, 19f, 19f},
                monthRows(d)));
        blocks.add(RentPdfBuilder.table("安装信息", INSTALL_HEADERS, INSTALL_WIDTHS,
                installRows(d.getInstalls())));
        // 附件和银行回单：图片直接嵌进 PDF，其他格式列文件名
        appendAttachBlocks(blocks, d);
    }

    /**
     * 汇总行：按「项目 + 期数」分组统计。
     * 用和 Excel 汇总一样的口径，导出两个格式看到的是同一组数字。
     */
    private List<String[]> summaryRows(List<RentLedgerDetailVO> details) {
        Map<String, RentLedgerSummaryVO> grouped = new java.util.LinkedHashMap<>();
        for (RentLedgerDetailVO d : details) {
            RentLedgerListVO b = d.getBase();
            String key = b.getProjectName() + "|" + b.getPeriodName();
            RentLedgerSummaryVO vo = grouped.computeIfAbsent(key, k -> new RentLedgerSummaryVO()
                    .setProjectName(b.getProjectName())
                    .setPeriodName(b.getPeriodName())
                    .setCount(0)
                    .setPayableSum(BigDecimal.ZERO)
                    .setPaidSum(BigDecimal.ZERO));
            vo.setCount(vo.getCount() + 1);
            vo.setPayableSum(vo.getPayableSum().add(
                    b.getPeriodPayable() == null ? BigDecimal.ZERO : b.getPeriodPayable()));
            vo.setPaidSum(vo.getPaidSum().add(
                    b.getPeriodPaid() == null ? BigDecimal.ZERO : b.getPeriodPaid()));
        }
        List<String[]> rows = new ArrayList<>();
        for (RentLedgerSummaryVO vo : grouped.values()) {
            rows.add(new String[]{
                    vo.getProjectName(), vo.getPeriodName(),
                    String.valueOf(vo.getCount()),
                    money(vo.getPayableSum()), money(vo.getPaidSum())});
        }
        return rows;
    }

    /**
     * 把多个单据打成一个 zip。
     * 文件名用 UTF-8 写入，中文名在 Windows 资源管理器里也能正常显示。
     */
    public byte[] zip(List<Doc> docs) {
        if (docs == null || docs.isEmpty()) {
            throw new JeecgBootException("没有可导出的内容，请先勾选要导出的记录");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (Doc doc : docs) {
                zos.putNextEntry(new ZipEntry(doc.fileName + ".pdf"));
                zos.write(doc.content);
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new JeecgBootException("打包下载失败：" + e.getMessage());
        }
        return out.toByteArray();
    }

    /**
     * 把附件和银行回单塞进 PDF。
     *
     * 图片（回单、付款凭证基本都是拍照或截图）直接嵌到页面里，一张一个区块；
     * PDF、Office、压缩包这些没法直接渲染的，只列一行文件名 ——
     * 至少让人知道这笔台账挂了哪些材料，要看清内容再回系统里点开。
     *
     * 单张图读失败不影响其余的：文件可能被手工删了、或路径对不上，
     * 那种情况下 PDF 里给一行"图片无法渲染"，其余照常出。
     */
    private void appendAttachBlocks(List<RentPdfBuilder.Block> blocks, RentLedgerDetailVO d) {
        List<RentAttachVO> attaches = d.getAttaches();
        if (attaches == null || attaches.isEmpty()) {
            return;
        }
        List<String> notImage = new ArrayList<>();
        boolean headingUsed = false;
        for (RentAttachVO a : attaches) {
            String caption = a.getBizTypeName() + "  " + a.getFileName();
            byte[] bytes = (a.getIsImage() != null && a.getIsImage() == 1)
                    ? readAttach(a.getId()) : null;
            if (bytes != null) {
                // 第一个图片区块带标题，后面几张就不再重复这个标题了
                blocks.add(RentPdfBuilder.image(headingUsed ? null : "附件与银行回单", bytes, caption));
                headingUsed = true;
            } else {
                notImage.add(caption);
            }
        }
        if (!notImage.isEmpty()) {
            List<String[]> rows = new ArrayList<>();
            for (String name : notImage) {
                rows.add(new String[]{name});
            }
            blocks.add(RentPdfBuilder.table(headingUsed ? null : "附件与银行回单",
                    new String[]{"非图片附件（内容请到系统里查看）"}, new float[]{100f}, rows));
        }
    }

    /** 读附件文件内容；读不到返回 null，不往外抛 */
    private byte[] readAttach(Long attachId) {
        try {
            TRentAttach att = attachService.getExisting(attachId);
            File file = attachService.resolveFile(att);
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            log.warn("导出PDF时读取附件失败 attachId={}", attachId, e);
            return null;
        }
    }

    /** 一个导出单据：文件名（不含扩展名）+ PDF 内容 */
    public static class Doc {
        private final String fileName;
        private final byte[] content;

        Doc(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }
    }

    // ==================== 表头常量 ====================

    private static final String[] INSTALL_HEADERS =
            {"安装房型", "组件型号", "块数", "容量(kW)", "租金档位", "单价(元/块/年)", "年金额(元)"};
    private static final float[] INSTALL_WIDTHS =
            {16f, 14f, 8f, 11f, 17f, 17f, 17f};

    // ==================== 农户 ====================

    private List<String[]> farmerBase(FarmerStationDetailVO d) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"农户名称", d.getFarmerName()});
        rows.add(new String[]{"发电户号", d.getPowerUserNo()});
        rows.add(new String[]{"编号", d.getStationCode()});
        rows.add(new String[]{"身份证号", d.getIdCard()});
        rows.add(new String[]{"银行卡号", d.getBankCardNo()});
        rows.add(new String[]{"银行卡姓名", d.getBankCardName()});
        rows.add(new String[]{"安装地址", d.getInstallAddr()});
        rows.add(new String[]{"所属项目", d.getProjectName()});
        rows.add(new String[]{"所属大区", d.getRegionName()});
        rows.add(new String[]{"并网日期", ymd(d.getGridDate())});
        rows.add(new String[]{"实际发电日期", ymd(d.getActualGenDate())});
        rows.add(new String[]{"租金截止日期", ymd(d.getRentDeadline())});
        rows.add(new String[]{"安装总容量(kW)", num(d.getTotalCapacity())});
        rows.add(new String[]{"安装总块数", num(d.getTotalBlocks())});
        rows.add(new String[]{"业务员", d.getSalesman()});
        rows.add(new String[]{"备注", d.getRemark()});
        return rows;
    }

    // ==================== 台账 ====================

    private List<String[]> ledgerBase(RentLedgerDetailVO d) {
        RentLedgerListVO b = d.getBase();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"农户名称", b.getFarmerName()});
        rows.add(new String[]{"发电户号", b.getPowerUserNo()});
        rows.add(new String[]{"身份证号", b.getIdCard()});
        rows.add(new String[]{"银行卡号", b.getBankCardNo()});
        rows.add(new String[]{"银行卡姓名", b.getBankCardName()});
        rows.add(new String[]{"安装地址", b.getInstallAddr()});
        rows.add(new String[]{"所属项目", b.getProjectName()});
        rows.add(new String[]{"安装总块数", num(b.getTotalBlocks())});
        rows.add(new String[]{"安装总容量(kW)", num(b.getTotalCapacity())});
        rows.add(new String[]{"并网日期", ymd(b.getGridDate())});
        rows.add(new String[]{"并网当月剩余天数", num(b.getGridMonthRemainDays())});
        rows.add(new String[]{"并网月数", num(b.getGridMonths())});
        rows.add(new String[]{"租金年度", num(b.getRentYear())});
        rows.add(new String[]{"期数", b.getPeriodName()});
        rows.add(new String[]{"计租区间", ymd(b.getPeriodBegin()) + " ~ " + ymd(b.getPeriodEnd())});
        rows.add(new String[]{"实际发电天数", num(b.getActualGenDays())});
        rows.add(new String[]{"当前状态", b.getStatusName()});
        // 纠纷暂停状态下把暂停原因也带出来，导出后不用再回系统查
        if (b.getDisputeRemark() != null && !b.getDisputeRemark().trim().isEmpty()) {
            rows.add(new String[]{"纠纷暂停原因", b.getDisputeRemark()});
        }
        return rows;
    }

    private List<String[]> ledgerMoney(RentLedgerDetailVO d) {
        RentLedgerListVO b = d.getBase();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"累计应付金额1(全周期，元)", money(b.getCycleTotalAmount())});
        rows.add(new String[]{"本期应付金额(元)", money(b.getPeriodPayable())});
        rows.add(new String[]{"截止本期累计应付(元)", money(b.getAccumPayable())});
        rows.add(new String[]{"截止上期累计实付(元)", money(b.getPrevAccumPaid())});
        rows.add(new String[]{"本期实付金额(元)", money(b.getPeriodPaid())});
        rows.add(new String[]{"截止本期累计实付(元)", money(b.getAccumPaid())});
        rows.add(new String[]{"本次支付金额(元)", money(b.getCurrentPayAmount())});
        rows.add(new String[]{"超/欠付金额(元)", money(b.getDiffAmount())});
        rows.add(new String[]{"对账状态", b.getReconStatusName()});
        rows.add(new String[]{"对账说明", b.getReconRemark()});
        rows.add(new String[]{"最近驳回原因", b.getRejectRemark()});
        return rows;
    }

    private List<String[]> monthRows(RentLedgerDetailVO d) {
        List<String[]> rows = new ArrayList<>();
        for (RentLedgerDetailVO.MonthVO m : d.getMonths()) {
            rows.add(new String[]{
                    m.getYm(),
                    num(m.getMonthDays()),
                    num(m.getRentDays()),
                    num(m.getGenDays()),
                    money(m.getAmount())});
        }
        return rows;
    }

    // ==================== 公共 ====================

    /**
     * 安装信息摊平成表格行：一条安装信息下有几个租金档位就出几行，
     * 房型/型号/块数那几列在第二行起留空，看起来是一组。
     */
    private List<String[]> installRows(List<FarmerStationDetailVO.InstallVO> installs) {
        List<String[]> rows = new ArrayList<>();
        if (installs == null) {
            return rows;
        }
        for (FarmerStationDetailVO.InstallVO iv : installs) {
            String type = iv.getInstallTypeName();
            String model = iv.getModuleModel();
            if (iv.getIsMultiModel() != null && iv.getIsMultiModel() == 1) {
                model = (model == null ? "" : model) + "（多型号待拆分）";
            }
            String blocks = num(iv.getBlockCount());
            String capacity = num(iv.getCapacityKw());
            if (iv.getRents() == null || iv.getRents().isEmpty()) {
                rows.add(new String[]{type, model, blocks, capacity, "未录入", "", ""});
                continue;
            }
            boolean first = true;
            for (FarmerStationDetailVO.RentVO r : iv.getRents()) {
                rows.add(new String[]{
                        first ? type : "",
                        first ? model : "",
                        first ? blocks : "",
                        first ? capacity : "",
                        r.getPriceTypeName(),
                        money(r.getUnitPrice()),
                        money(r.getAmount())});
                first = false;
            }
        }
        return rows;
    }

    private String safe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "未命名";
        }
        String cleaned = s.trim().replaceAll(ILLEGAL_FILENAME_CHARS, "_");
        return cleaned.length() > MAX_NAME_LEN ? cleaned.substring(0, MAX_NAME_LEN) : cleaned;
    }

    private String ymd(Date d) {
        return d == null ? "" : RentPeriodUtil.ymd(d);
    }

    private String num(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String money(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }
}
