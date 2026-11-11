package org.jeecg.modules.pcms.rent.ocr.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.DataSubImagesKvInfoKvDetailsValue;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextResponse;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextResponseBody;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.ocr.config.AliyunOcrClientFactory;
import org.jeecg.modules.pcms.rent.ocr.config.AliyunOcrProperties;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentListItemVO;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentReceiptVO;
import org.jeecg.modules.pcms.rent.ocr.service.IOcrService;
import org.jeecg.modules.pcms.rent.ocr.service.ReceiptFieldMapper;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云单据识别实现。
 *
 * 调用链：图片二进制流 -> RecognizeAllText(Type=PaymentRecord)
 *        -> subImages[].kvInfo.kvDetails -> 归类到具名字段 -> 按收款人匹配农户
 *
 * 图片直接以二进制流发给阿里云，不经过 OSS——省掉一次上传，
 * 也不用为了识别一张回单去申请 OSS 权限。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class OcrServiceImpl implements IOcrService {

    /** 银行回单/付款记录 */
    private static final String TYPE_PAYMENT_RECORD = "PaymentRecord";
    /** 表格识别。付款清单用这个，能拿到行×列结构 */
    private static final String TYPE_TABLE = "Table";
    /** 通用识别。兜底用，只取整页文字，字段标签由调用方自己抠 */
    private static final String TYPE_ADVANCED = "Advanced";
    /** 银行卡号最少多少位。比这个短的不当成账号，免得把序号、金额误认成账号 */
    private static final int ACCOUNT_MIN_DIGITS = 10;
    /** 候选农户最多返回几条 */
    private static final int MAX_CANDIDATES = 10;

    @Autowired
    private AliyunOcrClientFactory clientFactory;
    @Autowired
    private AliyunOcrProperties properties;
    @Autowired
    private ReceiptFieldMapper fieldMapper;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITProjectService projectService;

    @Override
    public PaymentReceiptVO recognizePaymentReceipt(MultipartFile file) {
        PaymentReceiptVO vo = recognize(file, TYPE_PAYMENT_RECORD);
        matchFarmers(vo);
        return vo;
    }

    @Override
    public PaymentReceiptVO recognize(MultipartFile file, String type) {
        checkFile(file);
        String realType = (type == null || type.trim().isEmpty()) ? TYPE_PAYMENT_RECORD : type.trim();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new JeecgBootException("读取文件失败：" + e.getMessage());
        }

        RecognizeAllTextResponseBody body = callOcr(bytes, realType);
        PaymentReceiptVO vo = new PaymentReceiptVO()
                .setRequestId(body.getRequestId())
                .setContent(body.getData().getContent());

        Map<String, PaymentReceiptVO.FieldVO> fields = collectFields(body.getData());
        vo.setRawFields(fields);
        fieldMapper.apply(vo, fields);

        log.info("OCR识别完成 type={} requestId={} 识别到{}个字段",
                realType, body.getRequestId(), fields.size());
        return vo;
    }

    @Override
    public List<PaymentReceiptVO> recognizeReceipts(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new ArrayList<>();
        }
        RecognizeAllTextResponseBody body = callOcr(imageBytes, TYPE_PAYMENT_RECORD);
        RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyData data = body.getData();
        String requestId = body.getRequestId();
        String content = data.getContent();

        // 阿里云会把一张图里的多份单据切成多个子图（一页并排两张回单就是两个子图），
        // 这里一个子图出一条结果 —— 合并成一份的话，几张回单的字段会糊在一起分不清谁是谁
        List<PaymentReceiptVO> receipts = new ArrayList<>();
        if (data.getSubImages() != null) {
            for (RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImages sub
                    : data.getSubImages()) {
                Map<String, PaymentReceiptVO.FieldVO> fields = collectFieldsOfSubImage(sub);
                if (fields.isEmpty()) {
                    continue;
                }
                PaymentReceiptVO vo = new PaymentReceiptVO()
                        .setRequestId(requestId)
                        .setContent(content);
                vo.setRawFields(fields);
                fieldMapper.apply(vo, fields);
                receipts.add(vo);
            }
        }
        if (receipts.isEmpty()) {
            // 一张结构化单据都没认出来。返回一条空结果而不是空列表，
            // 让调用方能给出"未识别出有效回单"的明确提示，不至于静默跳过
            receipts.add(new PaymentReceiptVO().setRequestId(requestId).setContent(content));
        }
        log.info("OCR识别完成 requestId={} 拆出{}张回单", requestId, receipts.size());
        return receipts;
    }

    @Override
    public List<PaymentListItemVO> recognizePaymentList(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new ArrayList<>();
        }
        RecognizeAllTextResponseBody body = callOcr(imageBytes, TYPE_TABLE);
        RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyData data = body.getData();

        List<PaymentListItemVO> rows = new ArrayList<>();
        if (data.getSubImages() == null) {
            return rows;
        }
        for (RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImages sub : data.getSubImages()) {
            RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImagesTableInfo table =
                    sub == null ? null : sub.getTableInfo();
            if (table == null || table.getTableDetails() == null) {
                continue;
            }
            for (RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImagesTableInfoTableDetails td
                    : table.getTableDetails()) {
                rows.addAll(parseTableRows(td));
            }
        }
        log.info("付款清单识别完成 requestId={} 解析出{}行", body.getRequestId(), rows.size());
        return rows;
    }

    /**
     * 把一张表解析成一行一笔。
     *
     * 阿里云返回的是散落的 cell（每个带 rowStart/columnStart），
     * 先按行列坐标还原成二维表，再按【表头文字】定位列——
     * 不写死列序，客户多加一列"部门"、少一列"手续费"都不影响。
     */
    private List<PaymentListItemVO> parseTableRows(
            RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImagesTableInfoTableDetails table) {
        List<PaymentListItemVO> rows = new ArrayList<>();
        if (table == null || table.getCellDetails() == null) {
            return rows;
        }

        // 1. 还原成 行号 -> (列号 -> 内容)
        Map<Integer, Map<Integer, String>> grid = new LinkedHashMap<>();
        for (RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImagesTableInfoTableDetailsCellDetails cell
                : table.getCellDetails()) {
            if (cell == null) {
                continue;
            }
            int r = cell.getRowStart() == null ? 0 : cell.getRowStart();
            int c = cell.getColumnStart() == null ? 0 : cell.getColumnStart();
            String raw = cell.getCellContent();
            String text = raw == null ? null : raw.trim();
            if (text == null || text.isEmpty()) {
                continue;
            }
            grid.computeIfAbsent(r, k -> new TreeMap<>()).put(c, text);
        }
        if (grid.isEmpty()) {
            return rows;
        }

        // 2. 找表头行：含"收款"或"金额"的那一行。
        //    续页往往没有表头（表头只印在清单第一页），所以找不到不算失败，
        //    下面按列内容猜列号
        Integer headerRow = null;
        Map<Integer, String> header = null;
        for (Map.Entry<Integer, Map<Integer, String>> e : grid.entrySet()) {
            String joined = String.join("", e.getValue().values());
            if (joined.contains("收款") || joined.contains("金额")) {
                headerRow = e.getKey();
                header = e.getValue();
                break;
            }
        }

        // 3. 定位列：优先按表头文字，没有表头就按列内容判断
        int nameCol;
        int acctCol;
        int amtCol;
        // 备注列：清单末尾会加一列写期数，没有这一列就是 -1。
        // 续页没有表头，取不到就靠 raw 里的整行文字兜底
        int remarkCol;
        if (header != null) {
            nameCol = findColumn(header, "收款人", "姓名", "客户名", "收款方");
            acctCol = findColumn(header, "收款账号", "账号", "卡号", "账户");
            amtCol = findColumn(header, "代发金额", "发放金额", "金额");
            remarkCol = findColumn(header, "备注", "用途", "摘要", "附言", "说明");
        } else {
            int[] guess = guessColumns(grid);
            nameCol = guess[0];
            acctCol = guess[1];
            amtCol = guess[2];
            remarkCol = -1;
            log.info("付款清单没找到表头（续页），按列内容定位：姓名列={} 账号列={} 金额列={}",
                    nameCol, acctCol, amtCol);
        }
        if (acctCol < 0 || amtCol < 0) {
            log.warn("付款清单定位不出收款账号或金额列，表头={}", header == null ? "无" : header.values());
            return rows;
        }

        // 4. 有表头时跳过表头行，没有表头时全部当数据行
        int rowNo = 0;
        for (Map.Entry<Integer, Map<Integer, String>> e : grid.entrySet()) {
            if (headerRow != null && e.getKey() <= headerRow) {
                continue;
            }
            rowNo++;
            Map<Integer, String> line = e.getValue();

            // 账号先按定位到的列取；取不到（空、或者不像账号）就在整行里找一个。
            // 印章遮挡、同一页多张子表列起点不一致，都可能让账号不在预期的那一列上
            String accountCell = line.get(acctCol);
            if (digits(accountCell).length() < ACCOUNT_MIN_DIGITS) {
                String fallback = findAccountInLine(line);
                if (fallback != null) {
                    accountCell = fallback;
                }
            }

            BigDecimal amount = fieldMapper.parseAmount(line.get(amtCol));
            // 账号和金额都没有的行（合计行、空行）跳过
            if (digits(accountCell).isEmpty() && amount == null) {
                continue;
            }
            rows.add(new PaymentListItemVO()
                    .setRowNo(rowNo)
                    .setPayeeName(nameCol < 0 ? null : line.get(nameCol))
                    .setPayeeAccount(accountCell)
                    .setAmount(amount)
                    .setRemark(remarkCol < 0 ? null : line.get(remarkCol))
                    .setRaw(String.join(" | ", line.values())));
        }
        return rows;
    }

    /**
     * 没有表头时按列内容猜列号。
     *
     * 续页只有数据行，但列的含义能从内容判断出来：
     *   账号列 —— 绝大多数值是一串长数字（10 位以上）
     *   金额列 —— 绝大多数值是带小数点的数字，而且值不是恒定的
     *            （手续费列全是 0.00 这种恒定值，要排除掉）
     *   姓名列 —— 账号列左边那一列（清单版式是 序号|收款人|收款账号|代发金额|…）
     */
    private int[] guessColumns(Map<Integer, Map<Integer, String>> grid) {
        Map<Integer, List<String>> byCol = new TreeMap<>();
        for (Map<Integer, String> line : grid.values()) {
            for (Map.Entry<Integer, String> c : line.entrySet()) {
                byCol.computeIfAbsent(c.getKey(), k -> new ArrayList<>()).add(c.getValue());
            }
        }
        int acctCol = -1;
        int amtCol = -1;
        for (Map.Entry<Integer, List<String>> e : byCol.entrySet()) {
            List<String> vals = e.getValue();
            if (acctCol < 0 && looksLikeAccountColumn(vals)) {
                // 取最左边命中的：序号列是短数字，长度不够，不会命中
                acctCol = e.getKey();
            } else if (amtCol < 0 && looksLikeAmountColumn(vals)) {
                amtCol = e.getKey();
            }
        }
        int nameCol = acctCol > 0 ? acctCol - 1 : -1;
        return new int[]{nameCol, acctCol, amtCol};
    }

    /** 列里超过八成的值是一串长数字，就当它是账号列 */
    private boolean looksLikeAccountColumn(List<String> values) {
        int total = 0;
        int hit = 0;
        for (String v : values) {
            String d = digits(v);
            if (d.isEmpty()) {
                continue;
            }
            total++;
            if (d.length() >= 10) {
                hit++;
            }
        }
        return total > 0 && hit * 10 >= total * 8;
    }

    /** 列里超过八成是"带小数点的数字"、且值不恒定，就当它是金额列 */
    private boolean looksLikeAmountColumn(List<String> values) {
        int total = 0;
        int hit = 0;
        Set<String> distinct = new HashSet<>();
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) {
                continue;
            }
            total++;
            distinct.add(v.trim());
            if (v.contains(".") && fieldMapper.parseAmount(v) != null) {
                hit++;
            }
        }
        // distinct > 1 是为了排除手续费列 —— 它每行都是 0.00，长得像金额但没意义
        return total > 0 && hit * 10 >= total * 8 && distinct.size() > 1;
    }

    /** 整行里找一个像收款账号的单元格：取数字最长、且超过最小位数的那一个 */
    private String findAccountInLine(Map<Integer, String> line) {
        String best = null;
        int bestLen = 0;
        for (String v : line.values()) {
            String d = digits(v);
            if (d.length() >= ACCOUNT_MIN_DIGITS && d.length() > bestLen) {
                best = v;
                bestLen = d.length();
            }
        }
        return best;
    }

    /** 在表头里找列号：命中任一关键词就算，找不到返回 -1 */
    private int findColumn(Map<Integer, String> header, String... keywords) {
        for (Map.Entry<Integer, String> e : header.entrySet()) {
            String h = e.getValue() == null ? "" : e.getValue().replaceAll("\\s", "");
            for (String kw : keywords) {
                if (h.contains(kw)) {
                    return e.getKey();
                }
            }
        }
        return -1;
    }

    private String digits(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    @Override
    public String recognizeText(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        RecognizeAllTextResponseBody body = callOcr(imageBytes, TYPE_ADVANCED);
        String content = body.getData().getContent();
        log.info("通用识别完成 requestId={} 文字长度={}",
                body.getRequestId(), content == null ? 0 : content.length());
        return content;
    }

    /**
     * 调用阿里云识别，返回原始响应体。
     * 抽出来是为了让"整份合并"和"按子图拆开"两条路径共用同一套调用与容错。
     */
    private RecognizeAllTextResponseBody callOcr(byte[] rawBytes, String type) {
        // 阿里云限制图片宽高不能超过 8192px，超过则等比缩到上限以内
        byte[] bytes = ensureMaxDimension(rawBytes, 8192);
        RecognizeAllTextResponse response;
        try {
            RecognizeAllTextRequest request = new RecognizeAllTextRequest()
                    .setType(type)
                    .setBody(new ByteArrayInputStream(bytes));
            response = clientFactory.getClient().recognizeAllTextWithOptions(request, new RuntimeOptions());
        } catch (Exception e) {
            log.error("阿里云OCR调用失败 type={}", type, e);
            throw new JeecgBootException("识别失败：" + e.getMessage());
        }
        if (response == null || response.getBody() == null) {
            throw new JeecgBootException("识别失败：阿里云返回为空");
        }
        RecognizeAllTextResponseBody body = response.getBody();
        if (body.getData() == null) {
            throw new JeecgBootException("识别失败：" + nvl(body.getMessage(), "阿里云未返回识别结果"));
        }
        return body;
    }

    /**
     * 取单个子图的字段。
     * 一张图里并排两张回单时阿里云会切成两个子图，一次取一个，
     * 两张回单的字段就不会互相污染。
     */
    private Map<String, PaymentReceiptVO.FieldVO> collectFieldsOfSubImage(
            RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImages sub) {
        Map<String, PaymentReceiptVO.FieldVO> result = new LinkedHashMap<>();
        if (sub == null || sub.getKvInfo() == null || sub.getKvInfo().getKvDetails() == null) {
            return result;
        }
        for (Map.Entry<String, DataSubImagesKvInfoKvDetailsValue> e
                : sub.getKvInfo().getKvDetails().entrySet()) {
            DataSubImagesKvInfoKvDetailsValue v = e.getValue();
            if (v == null) {
                continue;
            }
            String key = e.getKey();
            int seq = 2;
            while (result.containsKey(key)) {
                key = e.getKey() + "_" + seq++;
            }
            result.put(key, new PaymentReceiptVO.FieldVO()
                    .setKeyName(v.getKeyName())
                    .setValue(v.getValue())
                    .setConfidence(v.getValueConfidence()));
        }
        return result;
    }

    /**
     * 把所有子图的 kvDetails 汇总成一个扁平 Map。
     * 一张图可能被切成多个子图（比如一次拍了两张回单），这里全部合并，
     * key 重复时后面的加序号后缀，不覆盖。
     */
    private Map<String, PaymentReceiptVO.FieldVO> collectFields(
            RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyData data) {

        Map<String, PaymentReceiptVO.FieldVO> result = new LinkedHashMap<>();
        if (data.getSubImages() == null) {
            return result;
        }
        for (RecognizeAllTextResponseBody.RecognizeAllTextResponseBodyDataSubImages sub : data.getSubImages()) {
            for (Map.Entry<String, PaymentReceiptVO.FieldVO> e
                    : collectFieldsOfSubImage(sub).entrySet()) {
                String key = e.getKey();
                int seq = 2;
                while (result.containsKey(key)) {
                    key = e.getKey() + "_" + seq++;
                }
                result.put(key, e.getValue());
            }
        }
        return result;
    }

    /**
     * 按收款人账号和姓名去匹配农户。
     * 回单上的收款人就是收租金的农户，所以先用银行卡号精确匹配，
     * 匹配不到再用姓名模糊找，两种结果都返回，让运营自己挑。
     */
    private void matchFarmers(PaymentReceiptVO vo) {
        List<PaymentReceiptVO.FarmerCandidateVO> candidates = new ArrayList<>();
        List<Long> matchedIds = new ArrayList<>();

        // 卡号精确匹配，命中率最高也最可靠
        if (notBlank(vo.getPayeeAccount())) {
            List<TFarmerStation> byAccount = farmerStationService.list(new LambdaQueryWrapper<TFarmerStation>()
                    .eq(TFarmerStation::getDelFlag, 0)
                    .eq(TFarmerStation::getBankCardNo, vo.getPayeeAccount())
                    .last("limit " + MAX_CANDIDATES));
            for (TFarmerStation s : byAccount) {
                candidates.add(toCandidate(s, "ACCOUNT"));
                matchedIds.add(s.getId());
            }
        }

        // 姓名模糊匹配，银行卡姓名和农户姓名都找一遍
        if (notBlank(vo.getPayeeName()) && candidates.size() < MAX_CANDIDATES) {
            String name = vo.getPayeeName().trim();
            LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<TFarmerStation>()
                    .eq(TFarmerStation::getDelFlag, 0)
                    .and(q -> q.like(TFarmerStation::getBankCardName, name)
                            .or().like(TFarmerStation::getFarmerName, name))
                    .last("limit " + MAX_CANDIDATES);
            if (!matchedIds.isEmpty()) {
                w.notIn(TFarmerStation::getId, matchedIds);
            }
            for (TFarmerStation s : farmerStationService.list(w)) {
                candidates.add(toCandidate(s, "NAME"));
            }
        }

        // 项目名统一补，t_project 只有12行，一次读全比逐条查库划算
        if (!candidates.isEmpty()) {
            Map<Long, TProject> projectMap = new HashMap<>();
            for (TProject p : projectService.list()) {
                projectMap.put(p.getId(), p);
            }
            for (PaymentReceiptVO.FarmerCandidateVO c : candidates) {
                TProject p = projectMap.get(c.getProjectId());
                if (p != null) {
                    c.setProjectName(p.getProjectName());
                }
            }
        }
        vo.setCandidates(candidates);
    }

    private PaymentReceiptVO.FarmerCandidateVO toCandidate(TFarmerStation s, String matchBy) {
        return new PaymentReceiptVO.FarmerCandidateVO()
                .setId(s.getId())
                .setFarmerName(s.getFarmerName())
                .setBankCardName(s.getBankCardName())
                .setBankCardNo(s.getBankCardNo())
                .setPowerUserNo(s.getPowerUserNo())
                .setProjectId(s.getProjectId())
                .setMatchBy(matchBy);
    }

    /**
     * 如果图片宽或高超过最大像素限制，等比缩放到不超过限制。
     * 支持常见栅格格式（png/jpg/bmp/webp），PDF 跳过（阿里云服务端渲染 PDF，无需缩放）。
     */
    private byte[] ensureMaxDimension(byte[] imageBytes, int maxPx) {
        String contentType = null; // 从 bytes 判断太麻烦，先试解码
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            log.warn("无法解码图片，跳过尺寸校验，可能不是栅格格式（如PDF）");
            return imageBytes;
        }
        if (img == null) {
            log.warn("ImageIO 无法识别图片格式，跳过尺寸校验");
            return imageBytes;
        }

        int w = img.getWidth();
        int h = img.getHeight();
        log.info("图片原始尺寸: {}x{}", w, h);

        if (w <= maxPx && h <= maxPx) {
            return imageBytes; // 无需缩放
        }

        // 等比缩放，长边缩到 maxPx
        double scale = Math.min((double) maxPx / w, (double) maxPx / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(img, 0, 0, newW, newH, null);
        g2d.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // 从原始文件名推断输出格式，fallback jpg
            String format = "jpg";
            ImageIO.write(scaled, format, out);
        } catch (IOException e) {
            log.error("图片缩放后编码失败", e);
            return imageBytes; // fallback 发原图
        }

        byte[] result = out.toByteArray();
        log.info("图片已缩放: {}x{} -> {}x{} (scale={}), 体积: {} -> {}", w, h, newW, newH,
                String.format("%.2f", scale), imageBytes.length, result.length);
        return result;
    }

    private void checkFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择要识别的图片");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new JeecgBootException("图片超过 "
                    + (properties.getMaxFileSize() / 1024 / 1024) + "MB，阿里云不接受，请压缩后重试");
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String nvl(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s;
    }
}
