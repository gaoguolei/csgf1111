package org.jeecg.modules.pcms.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StringUnitUtils {

    public static String divideByThousand(String numberStr) {
        try {
            // 将字符串转换为BigDecimal
            BigDecimal number = new BigDecimal(numberStr);

            // 除以1000并保留2位小数，使用四舍五入
            BigDecimal result = number.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);

            // 返回字符串形式
            return result.toString();
        } catch (NumberFormatException e) {
            // 如果输入不是有效的数字，返回原字符串或处理异常
            throw new IllegalArgumentException("输入字符串不是有效的数字格式: " + numberStr, e);
        }
    }
}
