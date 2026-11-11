package org.jeecg.modules.pcms.rent.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;

/**
 * Excel 的一行。同时持有两份数据：
 *
 * 1. text：EasyExcel 按单元格显示格式转出来的字符串
 * 2. num ：单元格格式化之前的原始数值（ReadCellData.getOriginalNumberValue）
 *
 * 取日期和数字一律优先用 num，因为 text 完全取决于单元格的显示格式，靠不住。
 * 五个模板的并网日期格式就各不相同：
 *   北京/徐州/河南  yyyy"年"m"月"d"日";@  ->  text 是 "2024年10月17日"
 *   天津            yyyy/mm/dd           ->  text 是 "2023/03/21"
 *   海南(.xls)      日期型                ->  text 是 "45253"
 * 而它们的 num 全都是同一种东西：Excel 日期序列号。
 *
 * @Version: V1.0
 */
public class ExcelRow {

    /** Excel 日期序列号的合理区间：约 1968-01-01 ~ 2118-01-01 */
    private static final double SERIAL_MIN = 25000D;
    private static final double SERIAL_MAX = 80000D;

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd",
            "yyyy年MM月dd日", "yyyy年M月d日",
            "yyyy.MM.dd", "yyyyMMdd"
    };

    private final Map<Integer, String> text = new LinkedHashMap<>();
    private final Map<Integer, BigDecimal> num = new LinkedHashMap<>();

    /**
     * 本行的发电户号是不是从上方合并单元格继承来的。
     * 为 true 说明它和上一行共用同一个户号，属于同一个电站，
     * 由 RentImportService 合并成一条记录，而不是当成重复户号加 -1/-2 后缀。
     */
    private boolean userNoFromMerge;

    public void put(int index, String textValue, BigDecimal numValue) {
        if (textValue != null) {
            this.text.put(index, textValue);
        }
        if (numValue != null) {
            this.num.put(index, numValue);
        }
    }

    /** 还原合并单元格时用，直接覆盖 */
    public void fill(int index, String textValue, BigDecimal numValue) {
        if (textValue == null) {
            this.text.remove(index);
        } else {
            this.text.put(index, textValue);
        }
        if (numValue == null) {
            this.num.remove(index);
        } else {
            this.num.put(index, numValue);
        }
    }

    public String rawText(int index) {
        return text.get(index);
    }

    public BigDecimal rawNum(int index) {
        return num.get(index);
    }

    public boolean isUserNoFromMerge() {
        return userNoFromMerge;
    }

    public void setUserNoFromMerge(boolean userNoFromMerge) {
        this.userNoFromMerge = userNoFromMerge;
    }

    /** 整行是否为空，Excel 尾部经常带一串空行 */
    public boolean isBlank() {
        if (!num.isEmpty()) {
            return false;
        }
        for (String v : text.values()) {
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 按列索引取字符串，自动 trim，空串返回 null */
    public String str(int index) {
        String v = text.get(index);
        if (v == null) {
            // 纯数字单元格可能没有 text，只有 num，比如发电户号被存成了数值
            BigDecimal n = num.get(index);
            return n == null ? null : n.stripTrailingZeros().toPlainString();
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    /** 取字符串并把单元格内换行压成空格，用于地址、组件型号等字段 */
    public String flatStr(int index) {
        String v = str(index);
        return v == null ? null : v.replaceAll("\\s*\\r?\\n\\s*", " ").trim();
    }

    /**
     * 取数值。优先用原始数值，取不到再退回解析字符串。
     * 徐州那 4 条 632.551020408163 的长小数原样保留
     */
    public BigDecimal decimal(int index) {
        BigDecimal n = num.get(index);
        if (n != null) {
            return n;
        }
        String v = str(index);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取单价。0 视为"没有这个档位"，返回 null，避免把 0 元单价写进租金表 */
    public BigDecimal price(int index) {
        BigDecimal v = decimal(index);
        if (v == null || v.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return v;
    }

    /** 取块数。空或非数字返回 0，块数为 0 的安装类型不落库 */
    public Integer blocks(int index) {
        BigDecimal v = decimal(index);
        return v == null ? 0 : v.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 取日期。三条路径依次尝试：
     * 1. 原始数值在日期序列号区间 -> 按 Excel 1900 日期系统转换（五个模板都走这条）
     * 2. 字符串按已知格式解析（兜底，防止有人把日期存成了文本）
     * 3. 都不行返回 null
     */
    public Date date(int index) {
        BigDecimal n = num.get(index);
        if (n != null) {
            double serial = n.doubleValue();
            if (serial > SERIAL_MIN && serial < SERIAL_MAX) {
                return DateUtil.getJavaDate(serial);
            }
        }
        String v = str(index);
        if (v == null) {
            return null;
        }
        // 文本形态的纯数字也可能是序列号
        if (v.matches("^\\d+(\\.\\d+)?$")) {
            double serial = Double.parseDouble(v);
            if (serial > SERIAL_MIN && serial < SERIAL_MAX) {
                return DateUtil.getJavaDate(serial);
            }
        }
        String normalized = v.replace("年", "-").replace("月", "-").replace("日", "").trim();
        for (String pattern : DATE_PATTERNS) {
            Date d = tryParse(v, pattern);
            if (d != null) {
                return d;
            }
        }
        return tryParse(normalized, "yyyy-MM-dd");
    }

    private Date tryParse(String value, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            return sdf.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
