package org.jeecg.modules.pcms.util;

import org.jeecg.modules.pcms.energy.vo.DateIntervalVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {

    /**
     * 获取从指定日期到昨天（不包括今天）的日期字符串集合
     *
     * @param startDate 起始日期，格式为"yyyy-MM-dd"
     * @return 日期字符串集合，格式为"yyyy-MM-dd"
     */
    public static List<String> getDatesFromStartToYesterday(String startDate) {
        List<String> dateList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 解析起始日期
        LocalDate start = LocalDate.parse(startDate, formatter);
        // 获取昨天
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 如果起始日期在昨天之后，返回空列表
        if (start.isAfter(yesterday)) {
            return dateList;
        }

        // 生成从起始日期到昨天的所有日期
        LocalDate current = start;
        while (!current.isAfter(yesterday)) {
            dateList.add(current.format(formatter));
            current = current.plusDays(1);
        }

        return dateList;
    }


    /**
     * 获取从指定日期到昨天（不包括今天）的日期字符串集合
     *
     * @param startDate 起始日期，格式为"yyyy-MM-dd"
     * @return 日期字符串集合，格式为"yyyy-MM-dd"
     */
    public static List<DateIntervalVo> getMonthlyIntervalsFromStartToYesterday(String startDate, String endDate, Integer type) {
        List<DateIntervalVo> intervalList = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 确定实际起始和结束日期
        LocalDate actualStart;
        LocalDate actualEnd;

        if (type == 1) {
            // 模式1: 从startDate到endDate
            actualStart = LocalDate.parse(startDate, dateFormatter);
            actualEnd = LocalDate.parse(endDate, dateFormatter);
        } else if (type == 2) {
            // 模式2: 从endDate到昨天
            actualStart = LocalDate.parse(endDate, dateFormatter);
            actualEnd = LocalDate.now().minusDays(1);
        } else {
            throw new IllegalArgumentException("Invalid type parameter. Expected 1 or 2.");
        }

        // 验证日期有效性
        if (actualStart.isAfter(actualEnd)) {
            return intervalList;
        }

        // 处理第一个不完整的月份（如果有）
        LocalDate currentMonthStart = actualStart.withDayOfMonth(1);
        LocalDate currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());

        if (!actualStart.equals(currentMonthStart)) {
            LocalDate endDateForInterval = currentMonthEnd.isAfter(actualEnd) ? actualEnd : currentMonthEnd;
            addInterval(intervalList, actualStart, endDateForInterval, dateTimeFormatter);

            // 移动到下个月
            currentMonthStart = currentMonthStart.plusMonths(1).withDayOfMonth(1);
            currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());
        }

        // 处理完整的月份
        while (!currentMonthStart.isAfter(actualEnd)) {
            LocalDate endDateForInterval = currentMonthEnd.isAfter(actualEnd) ? actualEnd : currentMonthEnd;
            addInterval(intervalList, currentMonthStart, endDateForInterval, dateTimeFormatter);

            // 移动到下个月
            currentMonthStart = currentMonthStart.plusMonths(1).withDayOfMonth(1);
            currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());
        }

        return intervalList;
    }

    private static void addInterval(List<DateIntervalVo> intervalList, LocalDate startDate, LocalDate endDate, DateTimeFormatter formatter) {
        String startDateTime = startDate.atStartOfDay().format(formatter);
        String endDateTime = endDate.atTime(LocalTime.MAX).format(formatter);
        intervalList.add(new DateIntervalVo(startDateTime, endDateTime));
    }

    /**
     * 获取从指定日期到指定日期的日期字符串集合
     *
     * @param startDate 起始日期，格式为"yyyy-MM-dd"
     * @param endDate   结束日期，格式为"yyyy-MM-dd"
     * @return 日期字符串集合，格式为"yyyy-MM-dd"
     */
    public static List<String> getDateRange(String startDate, String endDate) {
        List<String> dateList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 解析起始日期和结束日期
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        // 如果起始日期在结束日期之后，返回空列表
        if (start.isAfter(end)) {
            return dateList;
        }

        // 生成从起始日期到结束日期的所有日期
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dateList.add(current.format(formatter));
            current = current.plusDays(1);
        }

        return dateList;
    }

    // 测试方法
// 定义时间格式
    public static List<DateIntervalVo> generateTimeSegments(String startTime, String endTime, Integer partition, Integer timeInterval) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 解析传入的开始时间和结束时间
        LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endTime, formatter);

        // 获取日期部分
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // 如果开始日期晚于结束日期，返回空列表
        if (startDate.isAfter(endDate)) {
            return new ArrayList<>();
        }

        // 生成日期列表
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        // 根据 partition 参数筛选日期
        int totalDays = dates.size();
        int fromIndex = 0;
        int toIndex = totalDays;

        if (partition != null) {
            if (partition == 2) {
                toIndex = (totalDays + 1) / 2;
            } else if (partition == 3) {
                fromIndex = (totalDays + 1) / 2;
            }
        }

        List<LocalDate> selectedDates = dates.subList(fromIndex, toIndex);

        // 定义每天的时间段划分（每3小时一段，共8段）
        LocalTime[] segmentStartTimes = {
                LocalTime.of(0, 0, 0),   // 0点
                LocalTime.of(3, 0, 0),   // 3点
                LocalTime.of(6, 0, 0),   // 6点
                LocalTime.of(9, 0, 0),   // 9点
                LocalTime.of(12, 0, 0),  // 12点
                LocalTime.of(15, 0, 0),  // 15点
                LocalTime.of(18, 0, 0),  // 18点
                LocalTime.of(21, 0, 0)   // 21点
        };

        LocalTime[] segmentEndTimes = {
                LocalTime.of(2, 59, 59),   // 2点59分59秒
                LocalTime.of(5, 59, 59),   // 5点59分59秒
                LocalTime.of(8, 59, 59),   // 8点59分59秒
                LocalTime.of(11, 59, 59),  // 11点59分59秒
                LocalTime.of(14, 59, 59),  // 14点59分59秒
                LocalTime.of(17, 59, 59),  // 17点59分59秒
                LocalTime.of(20, 59, 59),  // 20点59分59秒
                LocalTime.of(23, 59, 59)   // 23点59分59秒
        };

        // 生成时间段列表
        List<DateIntervalVo> timeSegments = new ArrayList<>();
        for (LocalDate date : selectedDates) {
            for (int i = 0; i < segmentStartTimes.length; i++) {
                LocalDateTime segmentStart = LocalDateTime.of(date, segmentStartTimes[i]);
                LocalDateTime segmentEnd = LocalDateTime.of(date, segmentEndTimes[i]);

                // 对于第一天，如果开始时间晚于当天的第一个时间段开始时间，则从实际开始时间算起
                if (date.equals(startDate) && segmentStart.isBefore(startDateTime)) {
                    segmentStart = startDateTime;
                }

                // 对于最后一天，如果结束时间早于当天的最后一个时间段结束时间，则调整到实际结束时间
                if (date.equals(endDate) && segmentEnd.isAfter(endDateTime)) {
                    segmentEnd = endDateTime;
                }

                // 只有当开始时间不晚于结束时间时才添加该时间段
                if (!segmentStart.isAfter(segmentEnd)) {
                    String startStr = segmentStart.format(formatter);
                    String endStr = segmentEnd.format(formatter);
                    timeSegments.add(new DateIntervalVo(startStr, endStr));
                }
            }
        }
        return timeSegments;
    }


    /**
     * 返回一个日期集合 从设备的投运时间到20250915 这段时间内 每100天的开始时间和结束时间的集合
     *
     * @param startTime
     * @param endTimeStr
     * @return
     */
    public static List<DateIntervalVo> splitTimeSegments(String startTime, String endTimeStr) {
        List<DateIntervalVo> segments = new ArrayList<>();

        // 使用支持带连字符的日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(startTime, formatter);
        LocalDate endDate = LocalDate.parse(endTimeStr, formatter);

        if (startDate.isAfter(endDate)) {
            return segments;
        }

        LocalDate currentStart = startDate;
        while (!currentStart.isAfter(endDate)) {
            LocalDate segmentEnd = currentStart.plusDays(99);
            if (segmentEnd.isAfter(endDate)) {
                segmentEnd = endDate;
            }

            // 输出格式保持为 yyyyMMdd（无分隔符）
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            segments.add(new DateIntervalVo(
                    currentStart.format(outputFormatter),
                    segmentEnd.format(outputFormatter)
            ));
            currentStart = segmentEnd.plusDays(1);
        }

        return segments;
    }


    public static List<DateIntervalVo> splitTimeSegmentsMonth(String startTime, String endTimeStr) {
        List<DateIntervalVo> segments = new ArrayList<>();

        try {
            // 动态解析日期格式
            YearMonth startYearMonth = parseToYearMonth(startTime);
            YearMonth endYearMonth = parseToYearMonth(endTimeStr);

            if (startYearMonth.isAfter(endYearMonth)) {
                return segments;
            }

            YearMonth currentStart = startYearMonth;
            while (!currentStart.isAfter(endYearMonth)) {
                YearMonth segmentEnd = currentStart.plusMonths(24).minusMonths(1); // 加上24个月，然后减1个月得到区间结束

                if (segmentEnd.isAfter(endYearMonth)) {
                    segmentEnd = endYearMonth;
                }

                // 输出格式为 yyyyMM
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMM");
                segments.add(new DateIntervalVo(
                        currentStart.format(outputFormatter),
                        segmentEnd.format(outputFormatter)
                ));

                currentStart = segmentEnd.plusMonths(1); // 下个区间从结束月份的下一个月开始
            }

        } catch (Exception e) {
            // 记录错误日志
            System.err.println("日期解析错误: " + e.getMessage());
            // 可以根据需要返回空列表或抛出自定义异常
        }

        return segments;
    }

    /**
     * 将字符串转换为YearMonth，支持多种格式
     */
    private static YearMonth parseToYearMonth(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("日期字符串不能为空");
        }

        // 移除可能的分隔符和空格
        String cleanedStr = dateStr.trim().replace("-", "").replace("/", "").replace(" ", "");

        // 根据长度判断格式
        if (cleanedStr.length() == 6) {
            // 格式: yyyyMM 或 yyMMdd（但当作yyyyMM处理）
            return YearMonth.parse(cleanedStr.substring(0, 6), DateTimeFormatter.ofPattern("yyyyMM"));
        } else if (cleanedStr.length() == 8) {
            // 格式: yyyyMMdd
            return YearMonth.parse(cleanedStr.substring(0, 6), DateTimeFormatter.ofPattern("yyyyMM"));
        } else if (cleanedStr.length() == 4) {
            // 格式: yyyy（当作yyyy01处理）
            return YearMonth.of(Integer.parseInt(cleanedStr), 1);
        } else {
            // 尝试常见格式解析
            try {
                // 尝试解析为完整日期
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return YearMonth.from(date);
            } catch (Exception e1) {
                try {
                    // 尝试解析为年月
                    return YearMonth.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                } catch (Exception e2) {
                    throw new IllegalArgumentException("不支持的日期格式: " + dateStr);
                }
            }
        }
    }

    // 如果需要更严格的格式验证，可以使用这个方法
    private static YearMonth strictParseToYearMonth(String dateStr) {
        // 定义支持的格式
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyyMM"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter.toString().contains("dd")) {
                    // 如果是包含日的格式，先解析为LocalDate再转换为YearMonth
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return YearMonth.from(date);
                } else {
                    // 直接解析为YearMonth
                    return YearMonth.parse(dateStr, formatter);
                }
            } catch (Exception e) {
                // 继续尝试下一个格式
                continue;
            }
        }

        throw new IllegalArgumentException("不支持的日期格式: " + dateStr);
    }



    public static String[] getYesterdayTimeRange() {
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 获取当前时间并减去一天
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        // 昨天的开始时间 (00:00:00)
        LocalDateTime startOfYesterday = yesterday.withHour(0).withMinute(0).withSecond(0);

        // 昨天的结束时间 (23:59:59)
        LocalDateTime endOfYesterday = yesterday.withHour(23).withMinute(59).withSecond(59);

        // 格式化为字符串
        String startTime = startOfYesterday.format(formatter);
        String endTime = endOfYesterday.format(formatter);

        return new String[]{startTime, endTime};
    }



    public static List<DateIntervalVo> splitTimeSegmentsYear(String startTime, String endTimeStr) {
        List<DateIntervalVo> segments = new ArrayList<>();
        try {
            // 动态解析日期格式
            YearMonth startYearMonth = parseToYearMonth(startTime);
            YearMonth endYearMonth = parseToYearMonth(endTimeStr);

            if (startYearMonth.isAfter(endYearMonth)) {
                return segments;
            }

            YearMonth currentStart = startYearMonth;
            while (!currentStart.isAfter(endYearMonth)) {
                YearMonth segmentEnd = currentStart.plusMonths(24).minusMonths(1); // 加上24个月，然后减1个月得到区间结束

                if (segmentEnd.isAfter(endYearMonth)) {
                    segmentEnd = endYearMonth;
                }

                // 输出格式为 yyyyMM
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy");
                segments.add(new DateIntervalVo(
                        currentStart.format(outputFormatter),
                        segmentEnd.format(outputFormatter)
                ));

                currentStart = segmentEnd.plusMonths(1); // 下个区间从结束月份的下一个月开始
            }

        } catch (Exception e) {
            // 记录错误日志
            System.err.println("日期解析错误: " + e.getMessage());
            // 可以根据需要返回空列表或抛出自定义异常
        }

        return segments;
    }
}