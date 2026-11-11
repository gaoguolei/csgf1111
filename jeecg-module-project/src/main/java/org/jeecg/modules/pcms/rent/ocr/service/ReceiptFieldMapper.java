package org.jeecg.modules.pcms.rent.ocr.service;

import java.math.BigDecimal;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentReceiptVO;
import org.springframework.stereotype.Component;

/**
 * 把阿里云返回的键值对归类到银行回单的具名字段上。
 *
 * 为什么要这么绕：阿里云对 PaymentRecord 没有公开固定字段名，
 * 各家银行回单的叫法也不统一——同一个意思可能叫"付款人""付方户名""借方户名""汇款人"。
 * 所以这里不做 key 的精确匹配，而是按关键词判断语义：
 *   先看是不是账号类（含 账号/卡号/账户）
 *   再看是不是金额类（含 金额/发生额）
 *   再看日期、流水号、摘要
 *   最后才按户名类归到付款方或收款方
 *
 * 归类不上的字段不会丢，全在 VO 的 rawFields 里，前端能看到。
 * 实际跑过真实回单后，照着 rawFields 里的 key 往下面的判断里补关键词即可。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class ReceiptFieldMapper {

    /** 付款方的叫法 */
    private static final String[] PAYER = {"付款", "付方", "借方", "汇款", "转出", "出账"};
    /** 收款方的叫法 */
    private static final String[] PAYEE = {"收款", "收方", "贷方", "收方", "转入", "入账", "对方"};

    public void apply(PaymentReceiptVO vo, Map<String, PaymentReceiptVO.FieldVO> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (Map.Entry<String, PaymentReceiptVO.FieldVO> e : fields.entrySet()) {
            String key = normalize(e.getKey());
            String keyName = e.getValue() == null ? null : e.getValue().getKeyName();
            if (keyName != null) {
                // 有些版式 map 的 key 是编号，真正的字段名在 keyName 上
                key = key + normalize(keyName);
            }
            String value = e.getValue() == null ? null : trim(e.getValue().getValue());
            if (value == null) {
                continue;
            }
            classify(vo, key, value);
        }
    }

    private void classify(PaymentReceiptVO vo, String key, String value) {
        // 账号类要先判，否则"付款人账号"会被下面的户名规则抢走
        if (contains(key, "账号", "卡号", "账户", "帐号")) {
            if (isPayer(key)) {
                setIfEmpty(vo::getPayerAccount, vo::setPayerAccount, cleanAccount(value));
            } else if (isPayee(key)) {
                setIfEmpty(vo::getPayeeAccount, vo::setPayeeAccount, cleanAccount(value));
            }
            return;
        }
        // 金额类，大写要先判，不然会被"金额"抢走
        if (contains(key, "大写")) {
            setIfEmpty(vo::getAmountInWords, vo::setAmountInWords, value);
            return;
        }
        if (contains(key, "金额", "发生额", "小写")) {
            if (vo.getAmount() == null) {
                vo.setAmount(parseAmount(value));
            }
            return;
        }
        if (contains(key, "日期", "时间")) {
            setIfEmpty(vo::getTradeTime, vo::setTradeTime, value);
            return;
        }
        if (contains(key, "流水", "凭证号", "凭证编号", "业务编号", "回单编号", "交易编号", "序号")) {
            setIfEmpty(vo::getSerialNo, vo::setSerialNo, value);
            return;
        }
        if (contains(key, "用途", "摘要", "备注", "附言", "说明")) {
            setIfEmpty(vo::getRemark, vo::setRemark, value);
            return;
        }
        if (contains(key, "开户行", "开户机构", "银行", "行名", "网点")) {
            setIfEmpty(vo::getBankName, vo::setBankName, value);
            return;
        }
        // 剩下的按户名归类
        if (contains(key, "户名", "名称", "姓名", "人")) {
            if (isPayer(key)) {
                setIfEmpty(vo::getPayerName, vo::setPayerName, value);
            } else if (isPayee(key)) {
                setIfEmpty(vo::getPayeeName, vo::setPayeeName, value);
            }
        }
    }

    private boolean isPayer(String key) {
        return contains(key, PAYER);
    }

    private boolean isPayee(String key) {
        return contains(key, PAYEE);
    }

    private boolean contains(String key, String... words) {
        if (key == null) {
            return false;
        }
        for (String w : words) {
            if (key.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 金额去掉货币符号、千分位、单位，转成 BigDecimal。转不了就返回 null，不抛异常
     */
    public BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replaceAll("[¥￥$,，\\s]", "")
                .replace("元", "")
                .replace("人民币", "")
                .trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            log.debug("金额解析失败，原样丢弃：{}", raw);
            return null;
        }
    }

    /** 银行账号识别结果里常有空格分组，比对时要去掉 */
    private String cleanAccount(String raw) {
        return raw == null ? null : raw.replaceAll("[\\s-]", "");
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("[\\s:：()（）]", "");
    }

    private String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void setIfEmpty(java.util.function.Supplier<String> getter,
                            java.util.function.Consumer<String> setter, String value) {
        if (value != null && (getter.get() == null || getter.get().isEmpty())) {
            setter.accept(value);
        }
    }
}
