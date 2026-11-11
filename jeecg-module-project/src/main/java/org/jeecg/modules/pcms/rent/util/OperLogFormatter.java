package org.jeecg.modules.pcms.rent.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.entity.TOperLog;

/**
 * 把操作日志里的改前改后 JSON 翻译成人话。
 *
 * 需求稿第 11 条要求日志能直接看出改了什么，比如
 * "安装块数 由 10 改为 30"，而不是丢一段 JSON 给运营自己比对。
 *
 * 日志有两种写法：
 *   1. OperLogHelper.record     —— 存 {"before":{...},"after":{...}}，这里逐字段比对
 *   2. OperLogHelper.recordText —— 存一句已经写好的中文，原样用
 *
 * @Version: V1.0
 */
@Slf4j
public class OperLogFormatter {

    /** 字段名 -> 中文标签。没登记的字段不进摘要，免得把技术字段抖给运营看 */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    /** 摘要里最多列几个字段，新增/删除时字段很多，全列出来没法看 */
    private static final int MAX_ITEMS = 20;

    static {
        // 农户电站信息
        LABELS.put("farmerName", "农户姓名");
        LABELS.put("idCard", "身份证号");
        LABELS.put("bankCardNo", "银行卡号");
        LABELS.put("bankCardName", "银行卡姓名");
        LABELS.put("installAddr", "安装地址");
        LABELS.put("powerUserNo", "发电户号");
        LABELS.put("projectId", "所属项目");
        LABELS.put("regionCode", "所属大区");
        LABELS.put("stationCode", "项目编号");
        LABELS.put("salesman", "业务员");
        LABELS.put("gridDate", "并网日期");
        LABELS.put("actualGenDate", "实际发电日期");
        LABELS.put("rentDeadline", "租金截止日期");
        LABELS.put("totalCapacity", "安装总容量");
        LABELS.put("totalBlocks", "安装总块数");
        // 安装信息
        LABELS.put("installTypeId", "安装房型");
        LABELS.put("moduleModel", "组件型号");
        LABELS.put("blockCount", "安装块数");
        LABELS.put("capacityKw", "安装容量");
        LABELS.put("isMultiModel", "多型号混写");
        // 租金单价
        LABELS.put("priceTypeId", "租金档位");
        LABELS.put("unitPrice", "租金单价");
        LABELS.put("amount", "金额");
        // 租金台账
        LABELS.put("rentYear", "租金年度");
        LABELS.put("periodName", "租金期数");
        LABELS.put("periodBegin", "本期租金起算时间");
        LABELS.put("periodEnd", "本期租金截止时间");
        LABELS.put("periodDays", "本期计租天数");
        LABELS.put("actualGenDays", "本期累计实际发电天数");
        LABELS.put("cycleTotalAmount", "周期应付累计总金额");
        LABELS.put("periodPayable", "本期应付金额");
        LABELS.put("accumPayable", "截止本期累计应付总金额");
        LABELS.put("prevAccumPaid", "截止上期累计实付金额");
        LABELS.put("periodPaid", "本期实付金额");
        LABELS.put("accumPaid", "截止本期累计实付金额");
        LABELS.put("diffAmount", "超/欠付金额");
        LABELS.put("auditStatus", "审核状态");
        LABELS.put("payStatus", "支付状态");
        LABELS.put("reconStatus", "对账状态");
        LABELS.put("receiptAmount", "回单金额");
        LABELS.put("reconRemark", "对账异常说明");
        LABELS.put("disputeFlag", "纠纷暂停支付");
        LABELS.put("disputeRemark", "纠纷备注");
        LABELS.put("rejectRemark", "驳回备注");
        LABELS.put("voidFlag", "作废");
        LABELS.put("voidRemark", "作废原因");
        LABELS.put("remark", "备注");
    }

    /**
     * 字段值 -> 中文。这些都是纯编码，不查库。
     *
     * LABELS 只翻了「字段名」，字段值的翻译分两种：
     *   1. 纯编码（是否、各类状态）—— 就在这张表里
     *   2. 外键 ID（所属项目、安装房型、租金档位）—— 要查字典表，由调用方批量
     *      预查后通过 ValueResolver 传进来，避免逐条查库
     */
    private static final Map<String, Map<String, String>> CODE_LABELS = new LinkedHashMap<>();

    static {
        Map<String, String> yesNo = new LinkedHashMap<>();
        yesNo.put("0", "否");
        yesNo.put("1", "是");
        CODE_LABELS.put("isMultiModel", yesNo);
        CODE_LABELS.put("voidFlag", yesNo);
        CODE_LABELS.put("disputeFlag", yesNo);

        // 取值口径与 TRentLedger 上 @ApiModelProperty 的注释保持一致
        Map<String, String> auditStatus = new LinkedHashMap<>();
        auditStatus.put("0", "待提交");
        auditStatus.put("1", "待项目审核");
        auditStatus.put("2", "项目审核驳回");
        auditStatus.put("3", "待业务审核");
        auditStatus.put("4", "业务审核驳回");
        auditStatus.put("5", "待财务审核");
        auditStatus.put("6", "财务审核驳回");
        auditStatus.put("7", "待领导审核");
        auditStatus.put("8", "领导审核驳回");
        auditStatus.put("9", "领导审核通过");
        CODE_LABELS.put("auditStatus", auditStatus);

        Map<String, String> payStatus = new LinkedHashMap<>();
        payStatus.put("0", "未支付");
        payStatus.put("1", "已支付");
        CODE_LABELS.put("payStatus", payStatus);

        Map<String, String> reconStatus = new LinkedHashMap<>();
        reconStatus.put("0", "未对账");
        reconStatus.put("1", "已对账");
        reconStatus.put("2", "对账异常");
        CODE_LABELS.put("reconStatus", reconStatus);
    }

    /**
     * 外键 ID -> 名称的翻译器。
     *
     * 由调用方（OperLogQueryServiceImpl）先扫一遍本页日志，把出现过的 ID 收齐、
     * 一次性查字典表建好映射再传进来 —— 逐条查库会变成 N+1，一页 20 条就是 60 次查询。
     *
     * 翻译不出来（字典里没有 / 记录已删）时返回 null，调用方回落到显示原始值，
     * 不能因为一条脏数据让整页日志显示不出来。
     */
    @FunctionalInterface
    public interface ValueResolver {
        String resolve(String field, Object value);
    }

    private OperLogFormatter() {
    }

    /** 动作编码翻中文 */
    public static String actionName(String action) {
        if (action == null) {
            return "";
        }
        switch (action) {
            case TOperLog.ACTION_INSERT:
                return "新增";
            case TOperLog.ACTION_UPDATE:
                return "修改";
            case TOperLog.ACTION_DELETE:
                return "删除";
            case TOperLog.ACTION_IMPORT:
                return "导入";
            case TOperLog.ACTION_GENERATE:
                return "生成租金";
            case TOperLog.ACTION_SUBMIT:
                return "提交审核";
            case TOperLog.ACTION_APPROVE:
                return "审核通过";
            case TOperLog.ACTION_REJECT:
                return "审核驳回";
            case TOperLog.ACTION_DISPUTE:
                return "标记纠纷暂停支付";
            case TOperLog.ACTION_CANCEL_DISPUTE:
                return "取消纠纷暂停支付";
            case TOperLog.ACTION_VOID:
                return "作废";
            case TOperLog.ACTION_PAY:
                return "标记已支付";
            case TOperLog.ACTION_RECONCILE:
                return "回单对账";
            case TOperLog.ACTION_UPLOAD:
                return "上传附件";
            default:
                return action;
        }
    }

    /**
     * 生成变更摘要。
     * 内容不是 JSON（recordText 写的中文）就原样返回。
     *
     * 不带翻译器：外键 ID 只能显示成数字（"所属项目=6"）。
     * 展示日志请用 {@link #summary(TOperLog, ValueResolver)}。
     */
    public static String summary(TOperLog operLog) {
        return summary(operLog, null);
    }

    /**
     * 生成变更摘要，外键 ID 按 resolver 翻译成名称。
     */
    public static String summary(TOperLog operLog, ValueResolver resolver) {
        String content = operLog == null ? null : operLog.getOperContent();
        if (content == null || content.trim().isEmpty()) {
            return actionName(operLog == null ? null : operLog.getAction());
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            JSONObject obj = JSON.parseObject(trimmed);
            JSONObject before = obj.getJSONObject("before");
            JSONObject after = obj.getJSONObject("after");
            if (before == null && after == null) {
                return trimmed;
            }
            if (before == null) {
                return actionName(operLog.getAction()) + "：" + describe(after, resolver);
            }
            if (after == null) {
                return actionName(operLog.getAction()) + "：" + describe(before, resolver);
            }
            String diff = diff(before, after, resolver);
            return diff.isEmpty() ? actionName(operLog.getAction()) + "（内容无变化）" : diff;
        } catch (Exception e) {
            // 日志展示不能因为一条脏数据整页报错
            log.warn("操作日志内容解析失败，原样返回：{}", content, e);
            return trimmed;
        }
    }

    // ------------------------------------------------------------------

    /** 逐字段比对，只列真正变了的 */
    private static String diff(JSONObject before, JSONObject after, ValueResolver resolver) {
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            String key = entry.getKey();
            if (!after.containsKey(key) && !before.containsKey(key)) {
                continue;
            }
            Object bv = before.get(key);
            Object av = after.get(key);
            // 比较用原始值：projectId 由 6 改成 7 要能识别出来
            if (value(bv).equals(value(av))) {
                continue;
            }
            items.add(entry.getValue() + " 由 " + display(key, bv, resolver)
                    + " 改为 " + display(key, av, resolver));
            if (items.size() >= MAX_ITEMS) {
                items.add("……");
                break;
            }
        }
        return String.join("；", items);
    }

    /** 新增和删除时列出关键字段的取值 */
    private static String describe(JSONObject obj, ValueResolver resolver) {
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            Object v = obj.get(entry.getKey());
            if (v == null || value(v).equals("空")) {
                continue;
            }
            items.add(entry.getValue() + "=" + display(entry.getKey(), v, resolver));
            if (items.size() >= MAX_ITEMS) {
                items.add("……");
                break;
            }
        }
        return String.join("，", items);
    }

    /**
     * 展示值：先问翻译器（外键 ID → 名称），再查内置编码表（状态、是否），
     * 都没有就原样输出。
     */
    private static String display(String field, Object v, ValueResolver resolver) {
        if (v != null && resolver != null) {
            String resolved = resolver.resolve(field, v);
            if (resolved != null && !resolved.trim().isEmpty()) {
                return resolved;
            }
        }
        if (v != null) {
            Map<String, String> codes = CODE_LABELS.get(field);
            if (codes != null) {
                String label = codes.get(String.valueOf(v).trim());
                if (label != null) {
                    return label;
                }
            }
        }
        return value(v);
    }

    /** 空值统一显示成"空"，日期只保留到日 */
    private static String value(Object v) {
        if (v == null) {
            return "空";
        }
        if (v instanceof Date) {
            return RentPeriodUtil.ymd((Date) v);
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equals(s)) {
            return "空";
        }
        // fastjson 把 Date 序列化成时间戳，长度13的纯数字按日期还原
        if (s.length() == 13 && s.matches("\\d{13}")) {
            return RentPeriodUtil.ymd(new Date(Long.parseLong(s)));
        }
        return s;
    }
}
