package org.jeecg.modules.pcms.rent.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.ReconcileResultVO;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentListItemVO;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentReceiptVO;
import org.jeecg.modules.pcms.rent.ocr.service.IOcrService;
import org.jeecg.modules.pcms.rent.service.IRentAttachService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 银行回单对账。全系统唯一的对账实现。
 *
 * 【支持两种版式，自动识别】
 *   银行回单     —— 一张图一笔。用 PaymentRecord 识别，取收款方账号/户名/金额
 *   付款清单     —— 一张图几十笔（序号|收款人|收款账号|代发金额|…）。
 *                   这种版式没有回单特征，PaymentRecord 要么被阿里云以
 *                   "图片类型不符"拒掉、要么只认出零星字段，所以改用 Table 模式按行列取。
 *   先按回单试，认不出收款账号就自动降级按清单试，客户不用管自己发的是哪种。
 *
 * 【不看文件名，四要素依次匹配】
 * 客户直接把 PDF 甩过来，不要求命名。定位顺序和业务流程图一致：
 *   ① 银行卡姓名 ② 银行卡号 → 找农户（两边都对上最可靠，只有一边命中就用那一边）
 *   ③ 金额 → 在该农户的「已支付」台账里筛（容差 1 分）
 *   ④ 期数 → 金额分不出唯一一条时，再用备注里的期数（202601 / 第4期 / 2026年第4期）收敛
 * 四步都分不出来 —— 报「找不到对应期数，请人工处理」，
 * 宁可让人工介入，也不能猜一个把钱记到错的期上。
 *
 * 【两条定位路径】
 *   ledgerId 为空 —— 台账列表的批量对账，走上面的「已支付 → 金额 → 期数」逻辑
 *   ledgerId 有值 —— 详情页的单张对账，只认那一条台账，
 *                   但仍校验回单收款账号属于该台账的农户，防止传错单
 *
 * 【姓名 + 卡号一起匹配】
 * 收款人姓名（银行卡姓名 + 农户姓名，前缀匹配，兼容「王付军1」这种去重后缀）
 * 和收款账号一起参与定位：两边都能对上的那户最可靠，直接用；
 * 只有一边命中就用那一边；两边指向不同的人（交集为空）时按姓名优先，
 * 并在结果里标出冲突，让财务核对。
 *
 * 【金额比对口径】
 * 回单金额 vs 台账的「本次支付金额」= min(本期应付, 本期实付)，容差 1 分。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class PdfReceiptReconciler {

    /** 金额比对容差，1分。客户按分截断（440.00）与系统精算值（440.01）的差要放过 */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    /**
     * 单笔金额的合理上限。
     *
     * 用来挡住 OCR 明显读错的情况：把「1,440.00」读成「144000」这种，
     * 小数点一丢金额就大两个数量级，而且这种错如果放过去会把账记错，
     * 比"识别不出"危险得多。
     *
     * 实测正常单期租金在 180~3400 之间，大电站加上整年一期也就几万。
     * 如果以后真有这么大的租金，把这个值调大即可。
     */
    private static final BigDecimal MAX_PLAUSIBLE_AMOUNT = new BigDecimal("50000");

    static final String RESULT_OK = "OK";
    static final String RESULT_ERROR = "ERROR";
    static final String RESULT_FAIL = "FAIL";

    @Autowired
    private IOcrService ocrService;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITRentLedgerService ledgerService;
    @Autowired
    private IRentAttachService attachService;
    @Autowired
    private OperLogHelper operLog;

    /**
     * 处理一张图：先按回单识别，认不出就按付款清单识别，每笔单独对账。
     * 一页并排两张回单、或一页五十行清单，都会拆成多条结果。
     *
     * 整页一个独立事务（REQUIRES_NEW），一张图出问题不影响同一批里的其他图。
     *
     * @param ledgerId 详情页传当前台账ID（只认这一条）；列表页批量对账传 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public List<ReconcileResultVO.Item> handleImage(byte[] imageBytes, String displayName,
                                                   Long ledgerId, String operUser, Scope scope) {
        // ---------- ① 先按银行回单识别 ----------
        List<PaymentReceiptVO> receipts = null;
        String receiptError = null;
        try {
            receipts = ocrService.recognizeReceipts(imageBytes);
        } catch (Exception e) {
            // 阿里云对版式不符的图会直接拒（如清单按回单识别报 400），
            // 这里不急着报错，下面还要按清单再试一次
            receiptError = e.getMessage();
            log.info("按回单识别未通过 file={}：{}", displayName, receiptError);
        }
        boolean usableReceipt = receipts != null && receipts.stream()
                .anyMatch(r -> !digits(r.getPayeeAccount()).isEmpty());
        if (usableReceipt) {
            AttachCache cache = new AttachCache();
            List<ReconcileResultVO.Item> items = new ArrayList<>();
            boolean single = receipts.size() == 1;
            int index = 0;
            for (PaymentReceiptVO r : receipts) {
                index++;
                String label = single ? displayName : displayName + " 第" + index + "张";
                // 期数优先取单据自带的备注字段；一页只有一张时才用整页文字兜底 ——
                // 多张并排时 content 是整页混在一起的，抠出来不知道属于哪一张
                String remark = r.getRemark();
                PeriodHint hint = parsePeriod(remark);
                if (hint == null && single) {
                    hint = parsePeriod(r.getContent());
                }
                items.add(reconcileCore(r.getPayeeAccount(), r.getAmount(), r.getPayeeName(),
                        imageBytes, label, ledgerId, hint, remark, scope, cache, operUser));
            }
            return items;
        }

        // ---------- ② 回单认不出 → 按付款清单识别 ----------
        List<PaymentListItemVO> rows = new ArrayList<>();
        String listError = null;
        try {
            rows = ocrService.recognizePaymentList(imageBytes);
        } catch (Exception e) {
            listError = e.getMessage();
            log.info("按付款清单识别未通过 file={}：{}", displayName, listError);
        }
        if (!rows.isEmpty()) {
            // 整页共用一份落盘文件：一页几十行会匹配到几十条台账，
            // 每户存一份原图的话一张清单就能撑出几十 MB
            AttachCache cache = new AttachCache();
            List<ReconcileResultVO.Item> items = new ArrayList<>();
            for (PaymentListItemVO row : rows) {
                String label = displayName + " 第" + (row.getRowNo() == null ? "?" : row.getRowNo()) + "行";
                // 清单末尾加了备注列写期数；续页没有表头认不出备注列时，从整行文字里抠
                String remark = isBlank(row.getRemark()) ? null : row.getRemark();
                PeriodHint hint = parsePeriod(remark);
                if (hint == null) {
                    hint = parsePeriod(row.getRaw());
                }
                items.add(reconcileCore(row.getPayeeAccount(), row.getAmount(), row.getPayeeName(),
                        imageBytes, label, ledgerId, hint, remark, scope, cache, operUser));
            }
            return items;
        }

        // ---------- ③ 再试通用识别 + 按标签抠字段 ----------
        //    各家银行版式差别很大：宁波银行是表格版、海南银行是"标签：值"的记账回执版，
        //    阿里云的 PaymentRecord 只认常见版式，像海南银行这种就抽不出字段。
        //    通用识别会把整页文字都给出来，我们按"收款人账号："这种标签自己抠，反而更稳
        String text = null;
        try {
            text = ocrService.recognizeText(imageBytes);
        } catch (Exception e) {
            log.info("按文字识别未通过 file={}：{}", displayName, e.getMessage());
        }
        PaymentReceiptVO parsed = parseReceiptByLabel(text);
        if (!digits(parsed.getPayeeAccount()).isEmpty()) {
            // 这种版式没有结构化字段，期数只能从整页文字里找
            return Collections.singletonList(reconcileCore(
                    parsed.getPayeeAccount(), parsed.getAmount(), parsed.getPayeeName(),
                    imageBytes, displayName, ledgerId, parsePeriod(text), null, scope,
                    new AttachCache(), operUser));
        }

        // ---------- ④ 三种都认不出 ----------
        // 把识别到的文字片段带上，方便对着原件排查是版式问题还是识别问题
        return Collections.singletonList(new ReconcileResultVO.Item()
                .setFileName(displayName)
                .setAttached(0)
                .setResult(RESULT_FAIL)
                .setMessage(buildUnrecognizedMessage(receiptError, listError, text)));
    }

    /**
     * 从整页文字里按标签抠出收款账号、收款人、金额。
     *
     * 逐行找"[标签]：[值]"，比正则跨行匹配稳 —— OCR 出来的换行位置靠不住。
     * 两个容易踩的点：
     *   · "付款人账号"也含"账号"，所以必须要求标签里带"收款"，不能只看"账号"
     *   · "大写金额"也含"金额"，要排掉，否则抠出来的是"壹仟玖佰捌拾元整"
     */
    private PaymentReceiptVO parseReceiptByLabel(String text) {
        PaymentReceiptVO vo = new PaymentReceiptVO();
        if (text == null || text.trim().isEmpty()) {
            return vo;
        }
        for (String raw : text.split("[\\r\\n]+")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (vo.getPayeeAccount() == null && line.contains("收款")
                    && (line.contains("账号") || line.contains("帐号")
                        || line.contains("账户") || line.contains("卡号"))) {
                String v = digitsAfterColon(line);
                // 短于 8 位的不像卡号，多半抠错了
                if (v.length() >= 8) {
                    vo.setPayeeAccount(v);
                    continue;
                }
            }

            if (vo.getPayeeName() == null && line.contains("收款")
                    && (line.contains("名称") || line.contains("户名") || line.contains("姓名"))) {
                String v = textAfterColon(line);
                if (v != null && !v.isEmpty()) {
                    vo.setPayeeName(v);
                    continue;
                }
            }

            if (vo.getAmount() == null && line.contains("金额") && !line.contains("大写")) {
                BigDecimal v = amountAfterLabel(line);
                if (v != null) {
                    vo.setAmount(v);
                }
            }
        }
        // 逐行找不出来的字段，再用整页正则兜一遍
        fallbackByRegex(vo, text);
        return vo;
    }

    /** 收款方账号：收款(人)账号/帐号/账户/卡号 后面紧跟的那串数字（10-25 位） */
    private static final Pattern PAYEE_ACCOUNT_RE = Pattern.compile(
            "收款[^\\r\\n]{0,8}?(?:账号|帐号|账户|卡号)[：:]?\\s*([0-9]{10,25})");

    /** 收款方户名：收款(人)名称/户名/姓名 后面的那串非空白文字 */
    private static final Pattern PAYEE_NAME_RE = Pattern.compile(
            "收款[^\\r\\n]{0,8}?(?:名称|户名|姓名)[：:]?\\s*([^\\s\\d：:]{2,30})");

    /**
     * 金额：前面不能是"大写"。
     * 不排掉的话，"大写金额：壹仟玖佰捌拾元整"会被当成金额去解析，结果是 null 甚至解析错
     */
    private static final Pattern AMOUNT_RE = Pattern.compile(
            "(?<!大写)金额[：:]?\\s*(?:人民币|¥|￥)?\\s*([0-9][0-9,]*\\.?[0-9]*)");

    /**
     * 用整页正则兜底。
     *
     * 【为什么需要这一步】
     * OCR 的换行位置靠不住。像海南银行"记账回执"那种三列并排的版式
     * （接收机构 / 回单编号 / 回单类型 挤在一行），整页文字可能只输出成两三行，
     * "收款人账号"和"付款人账号""回单编号"全在同一行里。
     *
     * 这时逐行找会走成：textAfterColon 取"第一个冒号后面的**全部内容**"，
     * digits() 再把里面所有数字连起来 —— 实测抠出过 68 位的"账号"，
     * 开头是接收机构、中间是回单编号和付款人账号、结尾是打印时间。
     * 拿它去比对必然对不上，还会报成"不是这户的回单"，把人误导到"传错单"的方向。
     *
     * 所以改用"标签后面紧跟的那串数字"，不依赖换行。
     * 只在逐行结果不可信时才走这里，正常版式（一行一个字段）不受影响。
     */
    private void fallbackByRegex(PaymentReceiptVO vo, String text) {
        if (!looksLikeAccount(digits(vo.getPayeeAccount()))) {
            String fixed = firstGroup(PAYEE_ACCOUNT_RE, text);
            // 兜底也抠不出来就清空，让它走"账号没能识别"的明确提示，
            // 而不是拿着一条几十位的假账号去比
            vo.setPayeeAccount(looksLikeAccount(fixed) ? fixed : null);
        }
        if (isBlank(vo.getPayeeName())) {
            String name = firstGroup(PAYEE_NAME_RE, text);
            if (!isBlank(name)) {
                vo.setPayeeName(name.trim());
            }
        }
        if (vo.getAmount() == null) {
            String amt = firstGroup(AMOUNT_RE, text);
            if (amt != null) {
                vo.setAmount(toAmount(amt));
            }
        }
    }

    /**
     * 银行账号长度是否合理。
     *
     * 下限 8 沿用原来的判断（短于 8 位不像卡号）；上限卡在 30 ——
     * 银行账号最长也就三十来位，超过的基本都是 OCR 把同一行多个字段的数字连在一起了。
     */
    private boolean looksLikeAccount(String accountDigits) {
        return accountDigits != null
                && accountDigits.length() >= 8
                && accountDigits.length() <= MAX_ACCOUNT_DIGITS;
    }

    /** 银行账号位数上限，超过就认定是多个字段被连在了一起 */
    private static final int MAX_ACCOUNT_DIGITS = 30;

    /** 取冒号后面的数字。没冒号就取整行里的数字 */
    private String digitsAfterColon(String line) {
        String tail = textAfterColon(line);
        return digits(tail == null ? line : tail);
    }

    /** 取分隔符后面的内容。: 和 ：都认；后面没东西返回 null */
    private String textAfterColon(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '：' || c == ':') {
                String v = line.substring(i + 1).trim();
                return v.isEmpty() ? null : v;
            }
        }
        return null;
    }

    /** 取"金额"标签后面第一个数字。"币种：人民币 金额：1,980.00" 这种一行两个标签也能取对 */
    private BigDecimal amountAfterLabel(String line) {
        int idx = line.indexOf("金额");
        if (idx < 0) {
            return null;
        }
        String tail = line.substring(idx + 2);
        java.util.regex.Matcher m = AMOUNT_AFTER_LABEL.matcher(tail);
        if (m.find()) {
            BigDecimal v = null;
            try {
                v = new BigDecimal(m.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
            return v;
        }
        return null;
    }

    /** "金额" 后面跟的那个数字：可选冒号，千分位，两位小数 */
    private static final java.util.regex.Pattern AMOUNT_AFTER_LABEL =
            java.util.regex.Pattern.compile("[：:]?\\s*([0-9][0-9,]*\\.?[0-9]*)");

    /** 拼"两种版式都认不出"的提示，有具体的错误就往里带上 */
    private String buildUnrecognizedMessage(String receiptError, String listError, String text) {
        StringBuilder sb = new StringBuilder("识别不出内容：既不像银行回单，也不像付款清单");
        if (receiptError != null) {
            sb.append("；按回单识别：").append(brief(receiptError));
        }
        if (listError != null) {
            sb.append("；按清单识别：").append(brief(listError));
        }
        // 把识别到的文字片段带上：能直接看出是版式不认识（有字但抠不到标签）
        // 还是压根没识别出字（图片不清/DPI 太低）
        if (text != null && !text.trim().isEmpty()) {
            sb.append("。识别到的文字开头是：").append(brief(text.trim()));
        } else {
            sb.append("。没能识别出任何文字，可能是图片不清晰或分辨率太低");
        }
        return sb.toString();
    }

    /** 错误信息掐短一点，避免阿里云的整串英文塞满页面 */
    private String brief(String msg) {
        if (msg == null) {
            return "";
        }
        String s = msg.replace("\n", " ").trim();
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    /**
     * 单笔的对账。回单和清单行走的是同一套逻辑，只是字段来源不同。
     * 定位不到台账一律 FAIL，且不存附件。
     */
    private ReconcileResultVO.Item reconcileCore(String payeeAccount, BigDecimal amount, String payeeName,
                                                 byte[] imageBytes, String label, Long ledgerId,
                                                 PeriodHint hint, String receiptRemark, Scope scope,
                                                 AttachCache cache, String operUser) {
        ReconcileResultVO.Item item = new ReconcileResultVO.Item()
                .setFileName(label)
                .setAttached(0)
                .setReceiptAmount(amount)
                .setReceiptAccount(payeeAccount)
                .setReceiptName(payeeName)
                .setReceiptRemark(receiptRemark);
        if (hint != null) {
            item.setParsedYear(hint.year).setParsedPeriod(hint.periodNo);
        }

        // 1. 收款账号是首选定位依据；账号没认出来时，收款人姓名还能兜底
        String account = digits(payeeAccount);
        if (account.isEmpty() && isBlank(payeeName)) {
            // 账号和户名都没识别出来，没法定到位，把金额带上让人工去清单里找
            return item.setResult(RESULT_FAIL).setMessage(String.format(
                    "这一行的收款账号和收款人都没能识别出来（金额 %s 元），"
                            + "可能被印章遮挡或图片不清，无法定位到农户，请人工核对",
                    amount == null ? "未识别" : amount.toPlainString()));
        }

        // 1.5 账号合理性预检：位数不对说明 OCR 把同一行里多个字段的数字连在一起了。
        //     这种账号拿去比对必然对不上，不拦的话会报成"不是这户的回单"，
        //     把人误导到"是不是传错单了"的方向去查，其实回单本身是对的、是识别的问题
        if (!account.isEmpty() && !looksLikeAccount(account)) {
            return item.setResult(RESULT_FAIL).setMessage(String.format(
                    "回单的收款账号没能可靠识别：识别出来是 %d 位数字（正常银行账号 10-%d 位），"
                            + "多半是把同一行里多个字段的数字连在一起了。"
                            + "请换一张更清晰的原件重拍后重试，或改用手工对账",
                    account.length(), MAX_ACCOUNT_DIGITS));
        }

        // 2. 金额合理性预检：明显偏大的多半是 OCR 把小数点读丢了。
        //    这种错如果放过去会把账记错（比对时也可能"碰巧"对上某条），
        //    所以在定位台账之前就拦下来，让人工看一眼原件
        if (amount != null && amount.compareTo(MAX_PLAUSIBLE_AMOUNT) > 0) {
            String who = payeeName == null || payeeName.trim().isEmpty()
                    ? "" : "（收款人「" + payeeName.trim() + "」）";
            return item.setResult(RESULT_FAIL).setMessage(String.format(
                    "这笔金额识别为 %s 元%s，明显偏大（单笔租金一般不超过 %s 元），"
                            + "可能是识别时把小数点丢了，请核对原件后人工处理",
                    amount.toPlainString(), who, MAX_PLAUSIBLE_AMOUNT.toPlainString()));
        }

        // 3. 定位台账：详情页认死那一条，列表页按"卡号/姓名+已支付+备注期数/金额"找
        Locate loc = new Locate();
        TRentLedger target = ledgerId == null
                ? locateFromPaid(account, amount, payeeName, hint, item, loc, scope)
                : locateByLedgerId(account, payeeName, ledgerId, hint, item, loc, scope);
        if (target == null) {
            // 定位失败也要留痕 —— 运营在详情页要能看到"这一笔为什么没对上"。
            // 找到了农户就记在农户名下（农户详情页看得到），
            // 连农户都没找到的只能留在任务结果里，没有可挂的实体
            logReconcile(operUser, loc.stationId, null, label,
                    "对账未完成：" + item.getMessage());
            return item;
        }

        item.setLedgerId(target.getId()).setSystemAmount(target.currentPayAmount());

        // 4. 已对账的不允许再对一次 —— 重复上传会把上一次的结果覆盖掉，
        //    而对账异常是可以重来的（异常本来就是让人改了再来）
        if (target.getReconStatus() != null
                && target.getReconStatus() == TRentLedger.RECON_OK) {
            item.setResult(RESULT_FAIL).setMessage(
                    target.getFarmerName() + " " + target.getPeriodName()
                            + " 已经对过账了，不能重复对账");
            return item;
        }

        // 6. 历史年度只支持查看，对账会改状态，一并拦住（需求第9条）
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        if (target.getRentYear() != null && target.getRentYear() < thisYear) {
            item.setResult(RESULT_FAIL).setMessage(
                    target.getRentYear() + "年的租金属于历史租金，只能查看，不支持对账");
            logReconcile(operUser, null, target.getId(), label, "对账未完成：" + item.getMessage());
            return item;
        }

        // 7. 定位到了才存附件 —— 挂不上台账的收据存了也没人找得到
        try {
            attachReceipt(target.getId(), imageBytes, receiptFileName(label), cache);
            item.setAttached(1);
        } catch (Exception e) {
            log.error("回单存附件失败 ledgerId={} file={}", target.getId(), label, e);
        }

        // 8. 金额比对：回单 vs 本次支付金额
        List<String> problems = new ArrayList<>();
        compareAmount(target, amount, problems);
        boolean ok = problems.isEmpty();
        String remark = ok ? null : String.join("；", problems);

        Date now = new Date();
        ledgerService.updateById(new TRentLedger()
                .setId(target.getId())
                .setReceiptAmount(amount)
                .setReconStatus(ok ? TRentLedger.RECON_OK : TRentLedger.RECON_ERROR)
                .setReconRemark(remark)
                .setReconTime(now)
                .setUpdateBy(operUser == null || operUser.trim().isEmpty() ? "system" : operUser)
                .setUpdateTime(now));

        String who = target.getFarmerName() + " " + target.getPeriodName();
        // 按姓名兜底/姓名卡号冲突时标出来：财务核对时能知道这一笔是怎么认到人的
        String matchNote = loc.matchNote == null ? "" : loc.matchNote;
        logReconcile(operUser, null, target.getId(), label, (ok
                ? String.format("对账成功：回单金额 %s 元，与本次支付金额 %s 元一致",
                        plain(amount), plain(target.currentPayAmount()))
                : String.format("对账异常：回单金额 %s 元，本次支付金额 %s 元，差异：%s",
                        plain(amount), plain(target.currentPayAmount()), remark)) + matchNote);

        return item.setResult(ok ? RESULT_OK : RESULT_ERROR)
                .setMessage((ok ? who + " 对账一致" : who + "：" + remark) + matchNote);
    }

    /**
     * 记一条对账日志。
     *
     * 定位到台账就记在台账上（台账详情页看得到）；
     * 只找到农户没找到台账就记在农户上（农户详情页看得到）；
     * 两个都没有（连账号都认不出）只能留在任务结果里，没有可挂的实体。
     */
    private void logReconcile(String operUser, Long stationId, Long ledgerId,
                              String fileName, String text) {
        if (ledgerId != null) {
            operLog.recordText(operUser, TOperLog.ACTION_RECONCILE, TOperLog.BIZ_LEDGER,
                    ledgerId, "上传银行回单[" + fileName + "]" + text);
        } else if (stationId != null) {
            operLog.recordText(operUser, TOperLog.ACTION_RECONCILE, TOperLog.BIZ_FARMER,
                    stationId, "上传银行回单[" + fileName + "]" + text);
        }
    }

    /** 定位结果：找到的台账，以及找到的农户（台账没找到但农户找到了也要留痕） */
    private static class Locate {
        private Long stationId;
        /** 匹配口径的说明，只靠姓名定、或者姓名卡号冲突时才有值 */
        private String matchNote;
    }

    /**
     * 按银行卡姓名兜底找农户。
     *
     * 只在卡号没识别出来、或者库里没有这个卡号时才走这一步。姓名不唯一
     * （重名，系统里还有为了去重加过后缀的「王付军1」「王付军2」），
     * 所以按前缀匹配、把同名的一批都拿回来，再靠备注期数和金额收敛。
     */
    private List<TFarmerStation> findStationsByName(String payeeName, Scope scope) {
        String name = payeeName == null ? "" : payeeName.trim().replaceAll("[\\s\\u3000]+", "");
        if (name.isEmpty()) {
            return Collections.emptyList();
        }
        return farmerStationService.list(stationScope(scope)
                .and(q -> q.likeRight(TFarmerStation::getBankCardName, name)
                        .or().likeRight(TFarmerStation::getFarmerName, name))
                .orderByAsc(TFarmerStation::getId)
                .last("limit 200"));
    }

    /** 收款人姓名是否对得上这一户（银行卡姓名或农户姓名，库里可能带后缀） */
    private boolean nameMatches(TFarmerStation station, String payeeName) {
        String name = payeeName == null ? "" : payeeName.trim().replaceAll("[\\s\\u3000]+", "");
        if (name.isEmpty()) {
            return false;
        }
        return sameName(name, station.getBankCardName()) || sameName(name, station.getFarmerName());
    }

    private boolean sameName(String name, String stored) {
        if (stored == null) {
            return false;
        }
        String s = stored.trim().replaceAll("[\\s\\u3000]+", "");
        return s.equals(name) || s.startsWith(name);
    }

    /** 两个候选集合的交集，按第一个集合的顺序返回 */
    private List<TFarmerStation> intersect(List<TFarmerStation> a, List<TFarmerStation> b) {
        java.util.Set<Long> ids = b.stream().map(TFarmerStation::getId).collect(Collectors.toSet());
        return a.stream().filter(s -> ids.contains(s.getId())).collect(Collectors.toList());
    }

    /** 报错文案里的"这一户"。按姓名兜底时可能匹配到多户，不能只写第一个名字 */
    private String who(List<TFarmerStation> stations) {
        return stations.size() == 1
                ? stations.get(0).getFarmerName()
                : "匹配到的 " + stations.size() + " 户同名农户";
    }

    private String nameText(String payeeName) {
        return isBlank(payeeName) ? "未识别" : payeeName.trim();
    }

    /**
     * 挂回单附件。同一张图第一次真落盘，之后匹配到别的台账就复用同一个文件，
     * 避免一张付款清单页存几十份。
     */
    private void attachReceipt(Long ledgerId, byte[] imageBytes, String fileName, AttachCache cache) {
        if (cache == null || cache.file == null) {
            TRentAttach saved = attachService.saveReceipt(ledgerId, imageBytes, fileName);
            if (cache != null) {
                cache.file = saved;
            }
        } else {
            attachService.linkReceipt(ledgerId, cache.file);
        }
    }

    /** 一页图共用的落盘附件。首次真存盘，其余台账复用它 */
    private static class AttachCache {
        private TRentAttach file;
    }

    /**
     * 列表页的批量对账：姓名+卡号找农户 → 已支付台账 → 金额 → 期数。
     * 返回 null 表示没定位到，失败原因已写进 item。
     *
     * 金额优先、期数收敛，和业务流程图一致。金额唯一命中就直接用，
     * 备注里的期数只用来在金额分不出来时收敛；金额一条都没命中时，
     * 再按期数定位，让比对环节把这笔记成「对账异常」而不是找不到台账。
     */
    private TRentLedger locateFromPaid(String account, BigDecimal amount, String payeeName,
                                       PeriodHint hint, ReconcileResultVO.Item item, Locate loc,
                                       Scope scope) {
        // 姓名和卡号一起参与定位：
        //   两个都识别出来 → 取「姓名 ∩ 卡号」，两边都对得上最可靠；
        //   交集为空（姓名和卡号指向不同的人）→ 按姓名优先，结果里标出冲突让人核对；
        //   只有一个能匹配上 → 用那一个；都匹配不上 → 报错。
        // 姓名按银行卡姓名 + 农户姓名做前缀匹配，兼容「王付军1」这种去重后缀。
        List<TFarmerStation> byName = isBlank(payeeName)
                ? Collections.emptyList() : findStationsByName(payeeName, scope);
        List<TFarmerStation> byCard = account.isEmpty()
                ? Collections.emptyList() : findStationsByAccount(account, scope);

        List<TFarmerStation> stations;
        boolean conflict = false;
        if (!byName.isEmpty() && !byCard.isEmpty()) {
            stations = intersect(byName, byCard);
            if (stations.isEmpty()) {
                // 两边都有命中却没有交集：姓名和卡号指向不同的人。
                // 按姓名优先，但必须标出来 —— 这种多半是有一边识别错了
                stations = byName;
                conflict = true;
            }
        } else if (!byName.isEmpty()) {
            stations = byName;
        } else {
            stations = byCard;
        }

        if (stations.isEmpty()) {
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "找不到收款人「%s」、收款账号 %s 对应的农户，请先在农户信息里补录",
                    nameText(payeeName), account.isEmpty() ? "未识别" : mask(account)));
            return null;
        }
        // 标注匹配口径：只靠姓名定的、姓名和卡号打架的，财务核对时要能一眼看出来
        if (conflict) {
            loc.matchNote = "（姓名与卡号不一致，按姓名匹配，请核对）";
        } else if (byCard.isEmpty()) {
            loc.matchNote = "（按银行卡姓名匹配）";
        }
        item.setParsedName(stations.get(0).getFarmerName());
        // 后面即使定位不到台账，也要把"找到了哪一户"记下来，好把失败原因挂到农户上
        loc.stationId = stations.get(0).getId();

        // 找这些农户「已支付」的台账 —— 对账只认页面点过「已支付」的期
        List<Long> stationIds = stations.stream().map(TFarmerStation::getId).collect(Collectors.toList());
        List<TRentLedger> paid = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .in(TRentLedger::getStationId, stationIds)
                .eq(TRentLedger::getPayStatus, TRentLedger.PAY_PAID)
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0)
                .orderByAsc(TRentLedger::getRentYear)
                .orderByAsc(TRentLedger::getPeriodNo));
        if (paid.isEmpty()) {
            item.setResult(RESULT_FAIL).setMessage(
                    who(stations) + " 没有已支付的租金台账，"
                            + "请先在租金台账里标记「已支付」，再来对账");
            return null;
        }

        // 四要素依次匹配，顺序与业务流程图一致：
        //   ① 银行卡姓名 ② 银行卡号 → 定位农户（上面已经做完）
        //   ③ 金额 → 在已支付台账里筛
        //   ④ 期数 → 金额分不出唯一一条时再按期数收敛
        List<TRentLedger> pool = paid;
        boolean amountFiltered = false;
        if (amount != null) {
            List<TRentLedger> byAmount = paid.stream()
                    .filter(l -> amountMatches(l.currentPayAmount(), amount))
                    .collect(Collectors.toList());
            if (byAmount.size() == 1) {
                TRentLedger only = byAmount.get(0);
                // 金额唯一命中就定了；备注期数对不上说明有一边错了，标出来让人核对
                if (hint != null && !hint.matches(only)) {
                    loc.matchNote = String.format(
                            "（备注写的是 %s，与台账 %s 不一致，按金额匹配，请核对）",
                            hint.label(), only.getPeriodName());
                }
                return only;
            }
            if (byAmount.size() > 1) {
                pool = byAmount;
                amountFiltered = true;
            }
            // 一条都没中：金额对不上不代表找不到台账，接着用期数定位，
            // 定位到之后按"对账异常"记金额差异
        }

        // ④ 期数
        if (hint != null) {
            List<TRentLedger> byPeriod = pool.stream()
                    .filter(hint::matches)
                    .collect(Collectors.toList());
            if (byPeriod.size() == 1) {
                return byPeriod.get(0);
            }
            if (byPeriod.size() > 1) {
                item.setResult(RESULT_FAIL).setMessage(String.format(
                        "%s 有 %d 条符合 %s 的已支付台账（%s），请人工处理",
                        who(stations), byPeriod.size(), hint.label(), periodList(byPeriod)));
                return null;
            }
            if (amountFiltered) {
                item.setResult(RESULT_FAIL).setMessage(String.format(
                        "金额对上了 %d 条已支付台账（%s），但没有一条的期数和备注 %s 一致，请人工核对",
                        pool.size(), periodList(pool), hint.label()));
                return null;
            }
            // 金额没命中、期数也没命中：多半是备注写错或者这一期还没点支付，交给人工
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "%s 没有备注写的 %s 的已支付台账（已支付的是 %s），请核对回单备注",
                    who(stations), hint.label(), periodList(paid)));
            return null;
        }

        if (pool.size() == 1) {
            return pool.get(0);
        }
        if (amount == null) {
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "%s 有 %d 条已支付台账（%s），但金额没识别出来，无法确定是哪一期，请人工处理",
                    who(stations), pool.size(), periodList(pool)));
            return null;
        }
        if (amountFiltered) {
            // 金额多条相同，又没有备注期数 —— 分不出是哪一期，不猜
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "%s 有 %d 期已支付且金额相同（%s），找不到对应期数，请人工处理",
                    who(stations), pool.size(), periodList(pool)));
            return null;
        }
        item.setResult(RESULT_FAIL).setMessage(String.format(
                "%s 有 %d 条已支付台账（%s），本次金额 %s 元与它们都对不上，请人工核对",
                who(stations), pool.size(), periodList(pool), amount.toPlainString()));
        return null;
    }

    /**
     * 详情页的单张对账：只认传进来的那一条台账。
     * 仍要校验收款账号属于该台账的农户，防止在 A 户页面传了 B 户的单，把钱记错户。
     * 备注里写了期数时，还要对得上台账的期数 —— 对不上多半是传错了期的回单。
     */
    private TRentLedger locateByLedgerId(String account, String payeeName, Long ledgerId, PeriodHint hint,
                                         ReconcileResultVO.Item item, Locate loc, Scope scope) {
        TRentLedger ledger = ledgerService.getById(ledgerId);
        if (ledger == null || (ledger.getDelFlag() != null && ledger.getDelFlag() == 1)) {
            item.setResult(RESULT_FAIL).setMessage("租金台账不存在或已删除");
            return null;
        }
        if (ledger.voided()) {
            item.setResult(RESULT_FAIL).setMessage(ledger.getPeriodName() + "已作废，不能对账");
            return null;
        }
        TFarmerStation station = farmerStationService.getById(ledger.getStationId());
        if (station == null) {
            item.setResult(RESULT_FAIL).setMessage("台账对应的农户不存在或已删除");
            return null;
        }
        // 传了项目/租户范围时，越界的台账不能对 —— 防止在 A 项目的页面上把 B 项目的单对掉
        if (!inScope(scope, station)) {
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "「%s」不在当前项目/租户范围内，不能在这里对账",
                    station.getFarmerName()));
            return null;
        }
        item.setParsedName(station.getFarmerName());
        loc.stationId = station.getId();

        // 姓名优先、卡号兜底：两个都对不上才拦住，防止在 A 户页面传了 B 户的单
        boolean nameOk = nameMatches(station, payeeName);
        boolean cardOk = !account.isEmpty() && account.equals(digits(station.getBankCardNo()));
        if (!nameOk && !cardOk) {
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "收款人「%s」、收款账号 %s 都不是「%s」（银行卡姓名 %s、卡号 %s）的信息，"
                            + "请确认上传的是这一户的回单",
                    nameText(payeeName), account.isEmpty() ? "未识别" : mask(account),
                    station.getFarmerName(), nameText(station.getBankCardName()),
                    mask(digits(station.getBankCardNo()))));
            return null;
        }
        loc.matchNote = nameOk && !cardOk
                ? (account.isEmpty() ? "（按银行卡姓名匹配）" : "（卡号与这一户不一致，按姓名匹配，请核对）")
                : null;
        // 备注写了期数却和当前台账对不上，多半是传错了期的回单，别把钱记到这一期
        if (hint != null && !hint.matches(ledger)) {
            item.setResult(RESULT_FAIL).setMessage(String.format(
                    "回单备注写的是 %s，当前台账是 %s，期数对不上，请确认上传的是这一期的回单",
                    hint.label(), ledger.getPeriodName()));
            return null;
        }
        return ledger;
    }

    // ------------------------------------------------------------------

    /** 从备注/整页文字里抠出来的期数。year 为空表示备注只写了「第4期」 */
    private static class PeriodHint {
        private final Integer year;
        private final int periodNo;

        private PeriodHint(Integer year, int periodNo) {
            this.year = year;
            this.periodNo = periodNo;
        }

        private String label() {
            return year == null ? "第" + periodNo + "期" : year + "年第" + periodNo + "期";
        }

        private boolean matches(TRentLedger ledger) {
            if (ledger.getPeriodNo() == null || ledger.getPeriodNo() != periodNo) {
                return false;
            }
            // 备注没写年份时只比期数，跨年同名期由金额再筛一次
            return year == null || year.equals(ledger.getRentYear());
        }
    }

    /**
     * 备注里的期数，客户会写成「2026年第4期」「2026-4期」「第4期」「4期」「第四期」。
     *
     * 只认带「期」字的写法 —— 裸数字「4」在备注里可能指金额、序号、账号片段，
     * 猜错会把钱记到别的期上，宁可退回按金额匹配。
     * 另一种写法是纯数字的年月组合「202601」，见 {@link #PERIOD_YYYYMM_RE}。
     */
    private static final Pattern PERIOD_RE = Pattern.compile(
            "(?:(20[0-9]{2})\\s*年?\\s*[-/.\\s]*)?第?\\s*([0-9]{1,2}|[一二三四五六七八九十]{1,3})\\s*期");

    /**
     * 「202601」这种写法：前四位年份、后两位期数，客户约定这么写代表 2026 年第 1 期。
     *
     * 前后都卡着非数字：8 位的日期（20260115）和更长的流水号里即使出现 202601，
     * 也不能截一段当期数，否则会把一笔钱记到别的期上。
     */
    private static final Pattern PERIOD_YYYYMM_RE = Pattern.compile(
            "(?<![0-9])(20[0-9]{2})(0[1-9]|1[0-2])(?![0-9])");

    private PeriodHint parsePeriod(String text) {
        if (isBlank(text)) {
            return null;
        }
        java.util.regex.Matcher m = PERIOD_RE.matcher(text);
        while (m.find()) {
            Integer periodNo = toPeriodNo(m.group(2));
            if (periodNo == null) {
                continue;
            }
            Integer year = m.group(1) == null ? null : Integer.valueOf(m.group(1));
            if (year == null) {
                // 备注里可能两种写法都有（「202601 第1期」），期数对得上就把年份补上
                java.util.regex.Matcher mm = PERIOD_YYYYMM_RE.matcher(text);
                if (mm.find() && Integer.parseInt(mm.group(2)) == periodNo) {
                    year = Integer.valueOf(mm.group(1));
                }
            }
            return new PeriodHint(year, periodNo);
        }
        // 没有「期」字时再看年月组合：202601 = 2026 年第 1 期
        java.util.regex.Matcher mm = PERIOD_YYYYMM_RE.matcher(text);
        if (mm.find()) {
            return new PeriodHint(Integer.valueOf(mm.group(1)), Integer.valueOf(mm.group(2)));
        }
        return null;
    }

    /** 期数只可能是 1~12，超出范围的当识别错了 */
    private Integer toPeriodNo(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        int n = s.matches("[0-9]{1,2}") ? Integer.parseInt(s) : chineseNumber(s);
        return n >= 1 && n <= 12 ? n : null;
    }

    /** 一~十二转数字，转不了返回 0 */
    private int chineseNumber(String s) {
        String[] digits = {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
        if ("十".equals(s)) {
            return 10;
        }
        if (s.length() == 2 && s.startsWith("十")) {
            int unit = java.util.Arrays.asList(digits).indexOf(s.substring(1));
            return unit >= 0 ? 10 + unit + 1 : 0;
        }
        if (s.length() == 1) {
            int idx = java.util.Arrays.asList(digits).indexOf(s);
            return idx >= 0 ? idx + 1 : 0;
        }
        return 0;
    }

    // ------------------------------------------------------------------

    /** 按卡号找农户。库里的卡号可能带空格，比对时统一只留数字 */
    private List<TFarmerStation> findStationsByAccount(String account, Scope scope) {
        List<TFarmerStation> hit = new ArrayList<>();
        for (TFarmerStation s : farmerStationService.list(stationScope(scope))) {
            if (account.equals(digits(s.getBankCardNo()))) {
                hit.add(s);
            }
        }
        return hit;
    }

    /**
     * 对账的数据范围：当前项目 + 当前账号可操作的租户。
     *
     * 异步线程里拿不到请求参数，范围在提交时就定好传进来；
     * projectId 为 null 表示不限项目，tenantIds 为 null 表示不限租户。
     */
    public static class Scope {
        private final Long projectId;
        private final List<Integer> tenantIds;

        public Scope(Long projectId, List<Integer> tenantIds) {
            this.projectId = projectId;
            this.tenantIds = tenantIds;
        }
    }

    private LambdaQueryWrapper<TFarmerStation> stationScope(Scope scope) {
        LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<TFarmerStation>()
                .eq(TFarmerStation::getDelFlag, 0);
        if (scope == null) {
            return w;
        }
        w.eq(scope.projectId != null, TFarmerStation::getProjectId, scope.projectId);
        if (scope.tenantIds != null) {
            if (scope.tenantIds.isEmpty()) {
                // 传了租户一个都没解析出来：宁可查不到，也不能把别人的数据对上
                w.eq(TFarmerStation::getId, -1L);
            } else {
                w.in(TFarmerStation::getTenantId, scope.tenantIds);
            }
        }
        return w;
    }

    private boolean inScope(Scope scope, TFarmerStation station) {
        if (scope == null) {
            return true;
        }
        if (scope.projectId != null && !scope.projectId.equals(station.getProjectId())) {
            return false;
        }
        return scope.tenantIds == null || scope.tenantIds.contains(station.getTenantId());
    }

    private void compareAmount(TRentLedger ledger, BigDecimal receipt, List<String> problems) {
        if (receipt == null) {
            problems.add("未能识别出金额");
            return;
        }
        BigDecimal expected = ledger.currentPayAmount();
        if (receipt.subtract(expected).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            problems.add(String.format("金额不一致：回单 %s 元，本次支付金额 %s 元"
                            + "（本期应付 %s 元、本期实付 %s 元）",
                    receipt.toPlainString(), expected.toPlainString(),
                    plain(ledger.getPeriodPayable()), plain(ledger.getPeriodPaid())));
        }
    }

    /** 金额是否在容差内相等。有一边为空算不相等 */
    private boolean amountMatches(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.subtract(b).abs().compareTo(AMOUNT_TOLERANCE) <= 0;
    }

    /** 把候选台账列成「2026年3期、2026年4期」这样，方便人工认 */
    private String periodList(List<TRentLedger> ledgers) {
        return ledgers.stream()
                .map(TRentLedger::getPeriodName)
                .collect(Collectors.joining("、"));
    }

    /** 附件展示名。拆出来的图是 JPEG，扩展名必须留着，否则落盘格式判不出来 */
    private String receiptFileName(String label) {
        String base = label == null ? "回单" : label.replace("/", "_").replace("\\", "_");
        return base.matches(".*\\.(jpg|jpeg|png|bmp|gif|webp)$") ? base : base + ".jpg";
    }

    private String digits(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    /** 正则第一个捕获组；没匹配上返回 null */
    private String firstGroup(Pattern p, String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /** 把"1,980.00"这类字符串转成金额，转不了返回 null */
    private BigDecimal toAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 卡号只露前六后四，日志和返回值里不留全号 */
    private String mask(String cardNo) {
        if (cardNo == null || cardNo.length() <= 10) {
            return cardNo == null ? "" : cardNo;
        }
        return cardNo.substring(0, 6) + "****" + cardNo.substring(cardNo.length() - 4);
    }

    private String plain(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }
}