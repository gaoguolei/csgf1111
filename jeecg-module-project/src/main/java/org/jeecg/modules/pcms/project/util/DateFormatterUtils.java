package org.jeecg.modules.pcms.project.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatterUtils {


    //把前端传来的 2025-09-08 00:00  转成20250704055000 格式
    public static String convertToDatabaseFormat(String frontendTime) {
        // 定义前端时间格式
        DateTimeFormatter frontendFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 解析前端时间字符串
        LocalDateTime dateTime = LocalDateTime.parse(frontendTime, frontendFormatter);

        // 定义数据库时间格式
        DateTimeFormatter databaseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 格式化为数据库时间字符串
        return dateTime.format(databaseFormatter);
    }


    public static String convertToFrontendFormat(String databaseTime) {
        try {
            // 定义数据库时间格式
            DateTimeFormatter databaseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            // 解析数据库时间字符串
            LocalDateTime dateTime = LocalDateTime.parse(databaseTime, databaseFormatter);

            // 定义前端时间格式
            DateTimeFormatter frontendFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // 格式化为前端时间字符串
            return dateTime.format(frontendFormatter);
        } catch (DateTimeParseException e) {
            // 处理格式错误的情况
            System.err.println("时间格式转换错误: " + e.getMessage());
            return null; // 或者返回默认值/抛出异常，根据业务需求决定
        }
    }

    public static String formatTimeString(String timeStr) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        LocalDateTime dateTime = LocalDateTime.parse(timeStr, inputFormatter);
        return dateTime.format(outputFormatter);
    }

    public static String formatTimeStringMonth(String timeStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate date = LocalDate.parse(timeStr, inputFormatter);
            return date.format(outputFormatter);
        } catch (Exception e) {
            // 处理解析异常，可以记录日志或返回默认值
            return timeStr; // 或者返回 null/空字符串，根据业务需求
        }
    }


    public static String formatTimeStringYear(String timeStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy");

            LocalDate date = LocalDate.parse(timeStr, inputFormatter);
            return date.format(outputFormatter);
        } catch (Exception e) {
            // 处理解析异常，可以记录日志或返回默认值
            return timeStr; // 或者返回 null/空字符串，根据业务需求
        }
    }

}
