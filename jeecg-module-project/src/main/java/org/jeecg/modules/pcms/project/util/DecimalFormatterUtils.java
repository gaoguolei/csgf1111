package org.jeecg.modules.pcms.project.util;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class DecimalFormatterUtils {


    /**
     * 处理字符串类型的小数：
     * - 大于0的数值：四舍五入保留2位小数
     * - 小于等于0的数值：直接返回原始字符串
     *
     * @param decimalStr 字符串类型的小数，可以是整数或小数形式
     * @return 处理后的字符串（大于0的数值保留2位小数，其他情况返回原始值）
     * @throws IllegalArgumentException 如果输入为null、空或不是有效的数字格式
     */
    public static String formatPositiveToTwoDecimals(String decimalStr) {
        // 校验输入参数
        if (decimalStr == null || decimalStr.trim().isEmpty()) {
            throw new IllegalArgumentException("输入字符串不能为null或空");
        }
        try {
            // 转换为BigDecimal并去除前后空格
            BigDecimal decimal = new BigDecimal(decimalStr.trim());

            // 判断是否大于0
            if (decimal.compareTo(BigDecimal.ZERO) > 0) {
                // 大于0的数值：四舍五入保留2位小数
                BigDecimal formattedDecimal = decimal.setScale(2, RoundingMode.HALF_UP);
                return formattedDecimal.toPlainString();
            } else {
                // 小于等于0的数值：直接返回原始字符串（已去除首尾空格）
                return decimalStr.trim();
            }
        } catch (NumberFormatException e) {
            return decimalStr;
        }
    }

    public static String formatPositiveToTwoDecimalsDivide(String decimalStr) {
        // 校验输入参数
        if (decimalStr == null || decimalStr.trim().isEmpty()) {
            throw new IllegalArgumentException("输入字符串不能为null或空");
        }
        try {
            // 转换为BigDecimal并去除前后空格
            BigDecimal decimal = new BigDecimal(decimalStr.trim());

            // 判断是否大于0
            if (decimal.compareTo(BigDecimal.ZERO) > 0) {
                // 大于0的数值：四舍五入保留2位小数
                BigDecimal formattedDecimal = decimal.divide(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP);
                return formattedDecimal.toPlainString();
            } else {
                // 小于等于0的数值：直接返回原始字符串（已去除首尾空格）
                return decimalStr.trim();
            }
        } catch (NumberFormatException e) {
            return decimalStr;
        }
    }
}

