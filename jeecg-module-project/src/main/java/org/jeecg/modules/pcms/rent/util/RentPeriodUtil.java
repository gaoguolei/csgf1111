package org.jeecg.modules.pcms.rent.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class RentPeriodUtil {

    /** 项目没配时按一年四期 */
    public static final int DEFAULT_PERIOD_COUNT = 4;

    private RentPeriodUtil() {
    }

    /**
     * 一年只能被均分成整数个自然月，所以期数必须是 12 的约数：
     * 1年1期 / 2期半年 / 3期四个月 / 4期季度 / 6期双月 / 12期月结
     */
    public static void checkPeriodCount(int periodCount) {
        if (periodCount < 1 || periodCount > 12 || 12 % periodCount != 0) {
            throw new IllegalArgumentException(
                    "一年的期数只能是 1、2、3、4、6、12 之一，收到 " + periodCount);
        }
    }

    /** 每期几个自然月 */
    public static int monthsPerPeriod(int periodCount) {
        checkPeriodCount(periodCount);
        return 12 / periodCount;
    }

    /** 第 periodNo 期的自然起日。四期制的3期 -> 07-01；一期制的1期 -> 01-01 */
    public static Date periodBegin(int year, int periodNo, int periodCount) {
        checkPeriod(periodNo, periodCount);
        return date(year, (periodNo - 1) * monthsPerPeriod(periodCount) + 1, 1);
    }

    /** 第 periodNo 期的自然止日。四期制的3期 -> 09-30；一期制的1期 -> 12-31 */
    public static Date periodEnd(int year, int periodNo, int periodCount) {
        checkPeriod(periodNo, periodCount);
        int month = periodNo * monthsPerPeriod(periodCount);
        Calendar c = calendar(date(year, month, 1));
        return date(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    /** 期数名称，如 2025年1期 */
    public static String periodName(int year, int periodNo) {
        return year + "年" + periodNo + "期";
    }

    /** 某天落在第几期 */
    public static int periodOf(Date d, int periodCount) {
        Calendar c = calendar(d);
        return c.get(Calendar.MONTH) / monthsPerPeriod(periodCount) + 1;
    }

    public static int yearOf(Date d) {
        return calendar(d).get(Calendar.YEAR);
    }

    /** 去掉时分秒，只留日期 */
    public static Date truncate(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = calendar(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** 含头含尾的天数，from 晚于 to 返回 0 */
    public static int daysBetween(Date from, Date to) {
        if (from == null || to == null) {
            return 0;
        }
        Date f = truncate(from);
        Date t = truncate(to);
        if (f.after(t)) {
            return 0;
        }
        // 用 UTC 毫秒差会被夏令时和时区偏移影响，这里逐月推进反而稳，
        // 但区间最长 25 年、按天推进太慢，所以先用毫秒估算再修正
        long diff = t.getTime() - f.getTime();
        int days = (int) Math.round(diff / (24.0 * 60 * 60 * 1000));
        return days + 1;
    }

    /** 当月自然天数 */
    public static int daysInMonth(Date d) {
        return calendar(d).getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** 当月最后一天 */
    public static Date endOfMonth(Date d) {
        Calendar c = calendar(d);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return truncate(c.getTime());
    }

    /** 当月第一天 */
    public static Date beginOfMonth(Date d) {
        Calendar c = calendar(d);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return truncate(c.getTime());
    }

    public static Date plusDays(Date d, int days) {
        Calendar c = calendar(d);
        c.add(Calendar.DAY_OF_MONTH, days);
        return truncate(c.getTime());
    }

    /** 次月1号。需求第21条：改价时新单价从次月1号起生效 */
    public static Date nextMonthFirst(Date d) {
        return plusDays(endOfMonth(d), 1);
    }

    /** yyyy-MM */
    public static String ym(Date d) {
        return new SimpleDateFormat("yyyy-MM").format(d);
    }

    /** yyyy-MM-dd */
    public static String ymd(Date d) {
        return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    /**
     * 并网当月剩余天数，含并网当天。
     * 3月21日并网 -> 31 - 21 + 1 = 11 天
     */
    public static int gridMonthRemainDays(Date gridDate) {
        if (gridDate == null) {
            return 0;
        }
        Calendar c = calendar(gridDate);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH) + 1;
    }

    /**
     * 并网月数：并网当月之后到 to 当月为止的整月数。
     * 并网当月是零头，由"并网当月剩余天数"单独表达，不算进整月里。
     * 2025-03-21 并网、到 2025-12-31，得 9（4月到12月）
     */
    public static int gridMonths(Date gridDate, Date to) {
        if (gridDate == null || to == null || truncate(gridDate).after(truncate(to))) {
            return 0;
        }
        Calendar g = calendar(gridDate);
        Calendar t = calendar(to);
        int months = (t.get(Calendar.YEAR) - g.get(Calendar.YEAR)) * 12
                + (t.get(Calendar.MONTH) - g.get(Calendar.MONTH));
        return Math.max(months, 0);
    }

    /**
     * 把 [from, to] 按自然月切成若干段。
     * 2025-03-21 ~ 2025-06-30 切成 3/21-3/31、4/1-4/30、5/1-5/31、6/1-6/30 四段。
     */
    public static List<Seg> monthSegments(Date from, Date to) {
        List<Seg> segs = new ArrayList<>();
        if (from == null || to == null) {
            return segs;
        }
        Date cursor = truncate(from);
        Date end = truncate(to);
        if (cursor.after(end)) {
            return segs;
        }
        // 25 年租期最多 300 段，加个上限防止日期异常时死循环
        for (int guard = 0; guard < 1000 && !cursor.after(end); guard++) {
            Date monthEnd = endOfMonth(cursor);
            Date segEnd = monthEnd.after(end) ? end : monthEnd;
            segs.add(new Seg(cursor, segEnd, ym(cursor), daysInMonth(cursor),
                    daysBetween(cursor, segEnd)));
            cursor = plusDays(monthEnd, 1);
        }
        return segs;
    }

    /** 两个闭区间的交集，无交集返回 null */
    public static Date[] intersect(Date aFrom, Date aTo, Date bFrom, Date bTo) {
        if (aFrom == null || aTo == null || bFrom == null || bTo == null) {
            return null;
        }
        Date from = truncate(aFrom).after(truncate(bFrom)) ? truncate(aFrom) : truncate(bFrom);
        Date to = truncate(aTo).before(truncate(bTo)) ? truncate(aTo) : truncate(bTo);
        return from.after(to) ? null : new Date[]{from, to};
    }

    // ------------------------------------------------------------------

    private static void checkPeriod(int periodNo, int periodCount) {
        checkPeriodCount(periodCount);
        if (periodNo < 1 || periodNo > periodCount) {
            throw new IllegalArgumentException(
                    "期数只能是 1-" + periodCount + "，收到 " + periodNo);
        }
    }

    private static Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day);
        return c.getTime();
    }

    private static Calendar calendar(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    /** 一个自然月内的计租片段 */
    public static class Seg {
        private final Date begin;
        private final Date end;
        private final String ym;
        private final int monthDays;
        private final int days;

        public Seg(Date begin, Date end, String ym, int monthDays, int days) {
            this.begin = begin;
            this.end = end;
            this.ym = ym;
            this.monthDays = monthDays;
            this.days = days;
        }

        public Date getBegin() {
            return begin;
        }

        public Date getEnd() {
            return end;
        }

        public String getYm() {
            return ym;
        }

        /** 当月自然天数，做分母 */
        public int getMonthDays() {
            return monthDays;
        }

        /** 该片段的天数，做分子 */
        public int getDays() {
            return days;
        }
    }
}