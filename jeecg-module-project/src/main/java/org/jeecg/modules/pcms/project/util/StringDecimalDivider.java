package org.jeecg.modules.pcms.project.util;

import java.text.DecimalFormat;
import java.math.BigDecimal;

public class StringDecimalDivider {
    
    public static String divideByThousandWithFormat(String decimalStr) {
        try {
            BigDecimal number = new BigDecimal(decimalStr);
            BigDecimal result = number.divide(new BigDecimal("1000"));
            
            // 使用DecimalFormat格式化输出
            DecimalFormat df = new DecimalFormat("#0.00");
            return df.format(result);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("输入字符串不是有效的数字格式: " + decimalStr);
        }
    }
}