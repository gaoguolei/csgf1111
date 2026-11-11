package org.jeecg.modules.pcms.rent.ocr.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * 把上传的文件拆成一张张图片，交给 OCR 逐张识别。
 *
 * 【为什么要拆】
 * 客户直接发一个 PDF（可能几十页、一页上还并排两张回单），
 * 阿里云 OCR 是按"一张图 = 一份单据"来识别字段的，整个 PDF 丢过去
 * 拿回来的字段会混在一起，分不清哪几个字段属于哪一笔。
 * 所以这里先把 PDF 每页渲染成图，一页一张、逐张识别。
 *
 * 【为什么不直接用 iText】
 * 项目里已有 iText，但它只能生成/操作 PDF，做不了"把 PDF 渲染成位图"
 * 这件事（需要额外的 pdfrender 商业组件），所以引入 PDFBox。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class PdfPageSplitter {

    /** 渲染精度。150 DPI 足够 OCR 认字，再高图太大容易顶到阿里云 10MB 上限 */
    private static final int DEFAULT_DPI = 150;
    /**
     * 内容稀疏时用的高精度。
     * 付款清单这类单据印在 A4 上只占最上面一条，150 DPI 下那条表格
     * 本身只有四十来个像素高，裁了也认不出，得重渲一遍高清的。
     */
    private static final int DETAIL_DPI = 300;
    /** 内容面积占页面低于这个比例，就认为"稀疏"，改用高精度重渲 */
    private static final double SPARSE_RATIO = 0.35;
    /** 降级后仍超限时用的精度，再低就认不清小字了 */
    private static final int FALLBACK_DPI = 110;
    /** 单页图片大小上限，与阿里云对单张图的要求保持一致 */
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    /** 单个 PDF 最多渲染多少页，防止误传一个几百页的文档把服务拖死 */
    private static final int MAX_PAGES = 200;

    /**
     * 拆分入口：PDF 按页拆成多张图，其它格式（jpg/png…）原样当成一张图返回。
     *
     * @param bytes    文件字节
     * @param fileName 原始文件名，用来拼每页的展示名
     */
    public List<PageImage> split(byte[] bytes, String fileName) throws IOException {
        List<PageImage> pages = new ArrayList<>();
        if (bytes == null || bytes.length == 0) {
            return pages;
        }
        if (!looksLikePdf(bytes)) {
            // 本来就是图片，直接当一页处理
            pages.add(new PageImage(pageLabel(fileName, 1, 1), bytes));
            return pages;
        }

        try (PDDocument doc = PDDocument.load(bytes)) {
            int total = doc.getNumberOfPages();
            if (total <= 0) {
                return pages;
            }
            int limit = Math.min(total, MAX_PAGES);
            if (total > MAX_PAGES) {
                log.warn("PDF 共 {} 页，超过上限 {}，只处理前 {} 页：{}", total, MAX_PAGES, limit, fileName);
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < limit; i++) {
                byte[] jpg = renderPage(renderer, i, fileName);
                if (jpg != null) {
                    pages.add(new PageImage(pageLabel(fileName, i + 1, limit), jpg));
                }
            }
            log.info("PDF 拆分完成 file={} 共{}页 输出{}张", fileName, total, pages.size());
        }
        return pages;
    }

    /**
     * 渲染单页。按内容自适应精度，最后统一控体积。
     */
    private byte[] renderPage(PDFRenderer renderer, int pageIndex, String fileName) {
        BufferedImage img = renderWithTrim(renderer, pageIndex, DEFAULT_DPI);
        if (img == null) {
            return null;
        }
        byte[] jpg = toJpeg(img);
        // 高精度重渲之后可能顶到阿里云 10MB 上限，降精度再来一次
        if (jpg == null || jpg.length > MAX_BYTES) {
            log.info("第{}页图片超过{}MB上限，降到{}DPI重渲 file={}",
                    pageIndex + 1, MAX_BYTES / 1024 / 1024, FALLBACK_DPI, fileName);
            BufferedImage smaller = renderWithTrim(renderer, pageIndex, FALLBACK_DPI);
            if (smaller != null) {
                byte[] retry = toJpeg(smaller);
                if (retry != null) {
                    return retry;
                }
            }
        }
        return jpg;
    }

    /**
     * 渲染一页并裁掉页边空白，必要时提高精度重渲。
     *
     * 【为什么要裁】
     * 很多银行单据是 A4 竖版、内容只占最上面一条（付款清单尤其常见），
     * 整页送去识别时，表格在图上只有几十个像素高，OCR 根本认不出字。
     *
     * 【为什么要提高精度重渲】
     * 光是裁还不够 —— 150 DPI 下那条表格本身就只有 42 像素高，
     * 裁完还是认不出。所以先用低精度扫一遍定位内容，发现内容特别稀疏
     * （面积占比很小）就用高精度重渲，这样裁出来的字才够大。
     */
    private BufferedImage renderWithTrim(PDFRenderer renderer, int pageIndex, int dpi) {
        try {
            BufferedImage img = toRgb(renderer.renderImageWithDPI(pageIndex, dpi));
            int[] box = inkBox(img);
            if (box == null) {
                // 整页空白，原样返回 —— 让它走识别失败流程比静默丢弃好
                return img;
            }
            double ratio = (double) ((box[2] - box[0] + 1) * (box[3] - box[1] + 1))
                    / ((double) img.getWidth() * img.getHeight());
            if (dpi < DETAIL_DPI && ratio < SPARSE_RATIO) {
                BufferedImage hi = toRgb(renderer.renderImageWithDPI(pageIndex, DETAIL_DPI));
                int[] hiBox = inkBox(hi);
                if (hiBox != null) {
                    return crop(hi, hiBox);
                }
            }
            return crop(img, box);
        } catch (Exception e) {
            log.error("PDF 第{}页渲染失败，该页跳过", pageIndex + 1, e);
            return null;
        }
    }

    /** 转成不带 alpha 的 RGB —— JPEG 不支持透明通道，直接写会报错 */
    private BufferedImage toRgb(BufferedImage img) {
        BufferedImage rgb = new BufferedImage(
                img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(img, 0, 0, java.awt.Color.WHITE, null);
        return rgb;
    }

    /**
     * 找出"有内容"的区域，返回 [left, top, right, bottom]；整页空白返回 null。
     *
     * 隔行隔列采样：一页两百万像素逐点扫也就十几毫秒，抽样能快一截，
     * 而且框完还会加安全边距，采样误差不会把内容切掉。
     */
    private int[] inkBox(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int top = h;
        int left = w;
        int bottom = -1;
        int right = -1;
        int step = Math.max(1, Math.min(w, h) / 800);

        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if (!isInk(img.getRGB(x, y))) {
                    continue;
                }
                if (y < top) {
                    top = y;
                }
                if (y > bottom) {
                    bottom = y;
                }
                if (x < left) {
                    left = x;
                }
                if (x > right) {
                    right = x;
                }
            }
        }
        if (bottom < 0) {
            return null;
        }

        int pad = Math.max(8, step * 2);
        return new int[]{
                Math.max(0, left - pad),
                Math.max(0, top - pad),
                Math.min(w - 1, right + pad),
                Math.min(h - 1, bottom + pad)
        };
    }

    /** 按框裁剪。几乎铺满整页时原样返回，不做无谓重建 */
    private BufferedImage crop(BufferedImage img, int[] box) {
        int w = box[2] - box[0] + 1;
        int h = box[3] - box[1] + 1;
        if (w >= img.getWidth() * 0.95 && h >= img.getHeight() * 0.95) {
            return img;
        }
        return img.getSubimage(box[0], box[1], w, h);
    }

    private byte[] toJpeg(BufferedImage img) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("图片编码失败", e);
            return null;
        }
    }

    /** 这个像素算不算"有内容"：不是接近纯白就算 */
    private boolean isInk(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // 阈值 240：JPEG 压缩会在白底上留一点噪点，卡得太严会把噪点当成内容
        return r < 240 || g < 240 || b < 240;
    }

    /** 前几个字节是 %PDF 就当 PDF 处理 */
    private boolean looksLikePdf(byte[] bytes) {
        return bytes.length > 4
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    /** 展示名：原文件名 + 第N页。多页时才带页码，单页保持原样 */
    private String pageLabel(String fileName, int pageNo, int total) {
        String base = fileName == null ? "回单" : fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return total <= 1 ? base : base + "（第" + pageNo + "页）";
    }

    /** 拆出来的一页 */
    public static class PageImage {
        private final String fileName;
        private final byte[] bytes;

        public PageImage(String fileName, byte[] bytes) {
            this.fileName = fileName;
            this.bytes = bytes;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}