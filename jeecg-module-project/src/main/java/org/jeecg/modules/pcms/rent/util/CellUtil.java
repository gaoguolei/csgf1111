package org.jeecg.modules.pcms.rent.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

/**
 * 与单元格无关的纯计算工具。
 * 取值相关的逻辑都在 ExcelRow 上。
 *
 * @Version: V1.0
 */
public class CellUtil {

    private CellUtil() {
    }

    /**
     * 判断组件型号是否为多型号混写，这类行 capacity_kw 留空、标记 is_multi_model=1
     */
    public static boolean isMultiModel(String moduleModel) {
        if (moduleModel == null) {
            return false;
        }
        // 实测的多型号写法：550+580 / 630*50 635*269 / 630W50块 635W8块 / 705 18块 700 18块
        return moduleModel.contains("+") || moduleModel.contains("*") || moduleModel.contains("块")
                || moduleModel.contains("\n") || moduleModel.contains("\r");
    }

    /**
     * 单一型号时算容量：型号(W) × 块数 ÷ 1000 = kW。多型号或型号非数字返回 null
     */
    public static BigDecimal capacityKw(String moduleModel, Integer blockCount) {
        if (moduleModel == null || blockCount == null || blockCount <= 0 || isMultiModel(moduleModel)) {
            return null;
        }
        try {
            return new BigDecimal(moduleModel.trim())
                    .multiply(new BigDecimal(blockCount))
                    .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 租金截止日期 = 并网日期满 leaseYears 年的最后一天。
     *
     * 一年的口径是"并网日到次年同月同日的前一天"：
     *   2025-03-20 并网，第1年到 2026-03-19，25年租期到 2050-03-19
     * 所以加完年份要再减一天。
     *
     * 闰日特例：2024-02-29 并网，加25年后 Calendar 会归到 2049-02-28
     * （2049 不是闰年，没有 2月29 这天）。这时 02-28 本身就是"周年日"，
     * 再减一天变成 02-27 就少算了一天，所以归位过的不减。
     * 判断依据是加完年份后"日"变了没有——整年相加只有 2月29 会被归位。
     *   2024-02-29 + 25年 -> 2049-02-28（归位了）-> 保持 2049-02-28
     *   2024-02-29 +  4年 -> 2028-02-29（闰年没归位）-> 减一天得 2028-02-28
     */
    public static Date rentDeadline(Date gridDate, int leaseYears) {
        if (gridDate == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(gridDate);
        int dayBefore = c.get(Calendar.DAY_OF_MONTH);
        c.add(Calendar.YEAR, leaseYears);
        if (c.get(Calendar.DAY_OF_MONTH) == dayBefore) {
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return c.getTime();
    }

    /**
     * 第 endYear 年的最后一天，与租金截止日期同一口径
     */
    public static Date yearRangeEnd(Date gridDate, int endYear) {
        return rentDeadline(gridDate, endYear);
    }

    /**
     * 第 startYear 年的第一天 = 上一年最后一天的次日。
     *
     * 不直接用"并网日 + (startYear-1) 年"，是因为闰日归位后会和上一档的
     * 结束日撞在同一天：2024-02-29 的第15年结束于 2039-02-28，
     * 若第16年起始也算 2039-02-28 就重叠了，这样推次日得 2039-03-01，
     * 保证各档位既不重叠也不留空档。
     */
    public static Date yearRangeBegin(Date gridDate, int startYear) {
        if (gridDate == null) {
            return null;
        }
        if (startYear <= 1) {
            return gridDate;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(yearRangeEnd(gridDate, startYear - 1));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }
}