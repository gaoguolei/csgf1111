package org.jeecg.modules.pcms.rent.util;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * 租金模块的 PDF 生成。一张单据（农户信息单 / 租金对账单）一个 PDF。
 *
 * 【中文字体】
 * iText 默认的 Helvetica 不含中文字形，直接写中文导出后是一片空白。
 * 这里用 itext-asian 提供的 STSong-Light（宋体），并在 pom 里显式依赖该包。
 * 字体在类加载时创建一次，BaseFont 只读，多线程共用安全。
 *
 * @Version: V1.0
 */
public final class RentPdfBuilder {

    /**
     * 中文字体，必须先于下面几个 Font 常量初始化 ——
     * Java 静态字段是按声明顺序执行的，放到后面的话 Font 拿到的是 null，
     * 编译能过，跑起来才发现导出的 PDF 里没有字。
     */
    private static final BaseFont CJK_FONT = cjk();

    /** 正文 */
    private static final Font FONT_BODY = font(9);
    /** 表头、小标题 */
    private static final Font FONT_BOLD = fontBold(9);
    /** 单据大标题 */
    private static final Font FONT_TITLE = fontBold(16);
    /** 字段名（表格左侧那一列） */
    private static final Font FONT_LABEL = fontBold(9);

    private static final BaseColor HEADER_BG = new BaseColor(240, 242, 245);
    private static final BaseColor BORDER = new BaseColor(200, 204, 210);

    /** 页面可用宽度：A4 宽 595pt，左右各 36pt 页边距（与 build 里的 Document 保持一致） */
    private static final float USABLE_WIDTH = PageSize.A4.getWidth() - 72;
    /** 图片高度上限：留出头尾标题的位置，免得一张竖版回单把整页撑爆 */
    private static final float USABLE_HEIGHT = PageSize.A4.getHeight() - 160;

    private RentPdfBuilder() {
    }

    /**
     * 生成一个 PDF。title 是单据标题，blocks 按顺序往下排。
     * 返回 PDF 字节，由调用方决定写文件还是塞进 zip。
     */
    public static byte[] build(String title, List<Block> blocks) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph head = new Paragraph(title, FONT_TITLE);
            head.setAlignment(Element.ALIGN_CENTER);
            head.setSpacingAfter(14);
            document.add(head);

            for (Block b : blocks) {
                if (b.getHeading() != null && !b.getHeading().isEmpty()) {
                    Paragraph h = new Paragraph(b.getHeading(), FONT_BOLD);
                    h.setSpacingBefore(10);
                    h.setSpacingAfter(5);
                    document.add(h);
                }
                document.add(b.getElement());
            }
            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("生成PDF失败：" + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ==================== 两种表格 ====================

    /** 字段 / 值 两列的信息表 */
    public static Block kv(String heading, List<String[]> pairs) {
        PdfPTable table = new PdfPTable(new float[]{30f, 70f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        for (String[] p : pairs) {
            table.addCell(labelCell(p[0]));
            table.addCell(valueCell(p.length > 1 ? p[1] : ""));
        }
        return new Block(heading, table);
    }

    /** 纯标题区块，用来分节（多条台账合并成一个 PDF 时，每条前面放一个） */
    public static Block heading(String text) {
        Paragraph p = new Paragraph(text, FONT_BOLD);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        return new Block(null, p);
    }

    /** 明细表：表头 + 若干数据行，widths 是各列宽度占比 */
    public static Block table(String heading, String[] headers, float[] widths, List<String[]> rows) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        for (String h : headers) {
            table.addCell(headerCell(h));
        }
        if (rows.isEmpty()) {
            PdfPCell empty = valueCell("（无数据）");
            empty.setColspan(headers.length);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        } else {
            for (String[] row : rows) {
                for (String cell : row) {
                    table.addCell(valueCell(cell));
                }
            }
        }
        return new Block(heading, table);
    }

    /**
     * 图片区块：把一张图片嵌进 PDF，下面配一行说明文字。
     *
     * 图片按可用页宽等比缩放，不会超出页面右边界；高度也不超过一页，
     * 免得一张竖版回单把整页撑爆。
     *
     * @param imageBytes 图片二进制
     * @param caption    图片下方的说明，如"银行回单 张三-202601.jpg"，可为空
     */
    public static Block image(String heading, byte[] imageBytes, String caption) {
        try {
            Image img = Image.getInstance(imageBytes);
            img.scaleToFit(USABLE_WIDTH, USABLE_HEIGHT);
            img.setAlignment(Element.ALIGN_CENTER);
            if (caption != null && !caption.isEmpty()) {
                // 用一张单格表把图片和说明框在一起，图片和文字不会跨页断开
                PdfPTable wrap = new PdfPTable(1);
                wrap.setWidthPercentage(100);
                wrap.setSpacingAfter(8);
                PdfPCell imgCell = new PdfPCell(img, false);
                imgCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imgCell.setPadding(0);
                wrap.addCell(imgCell);
                PdfPCell capCell = new PdfPCell(new Phrase(caption, FONT_BODY));
                capCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                capCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                capCell.setPaddingTop(3);
                wrap.addCell(capCell);
                return new Block(heading, wrap);
            }
            return new Block(heading, img);
        } catch (Exception e) {
            // 单张图坏了不能把整份 PDF 拖垮，降级成一行文字说明
            Paragraph fallback = new Paragraph(
                    (caption == null || caption.isEmpty() ? "图片" : caption) + "（图片无法渲染）", FONT_BODY);
            fallback.setSpacingAfter(6);
            return new Block(heading, fallback);
        }
    }

    /** 一个区块：可选的小标题 + 一块内容（表格 / 图片 / 一段文字） */
    public static class Block {
        private final String heading;
        /** 内容统一按 Element 处理 —— PdfPTable、Image、Paragraph 都是它的实现 */
        private final Element element;

        private Block(String heading, Element element) {
            this.heading = heading;
            this.element = element;
        }

        String getHeading() {
            return heading;
        }

        Element getElement() {
            return element;
        }
    }

    // ==================== 单元格 ====================

    private static PdfPCell labelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_LABEL));
        c.setBackgroundColor(HEADER_BG);
        c.setBorderColor(BORDER);
        c.setPadding(5);
        return c;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_BOLD));
        c.setBackgroundColor(HEADER_BG);
        c.setBorderColor(BORDER);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private static PdfPCell valueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, FONT_BODY));
        c.setBorderColor(BORDER);
        c.setPadding(5);
        return c;
    }

    // ==================== 字体 ====================

    private static BaseFont cjk() {
        try {
            // 不内嵌字体：PDF 体积小，靠阅读器的 CJK 字体渲染
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "初始化中文字体失败，请确认 pom 里已引入 com.itextpdf:itext-asian", e);
        }
    }

    private static Font font(float size) {
        return new Font(CJK_FONT, size, Font.NORMAL, BaseColor.BLACK);
    }

    private static Font fontBold(float size) {
        // STSong-Light 没有粗体，用样式位模拟（阅读器通常会做加粗替换）
        return new Font(CJK_FONT, size, Font.BOLD, BaseColor.BLACK);
    }
}
