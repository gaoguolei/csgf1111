package org.jeecg.modules.pcms.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class StringMultiplier {
    private static final DecimalFormat df = new DecimalFormat("0.00");
    
    public static String multiplyAndFormat(String text) {
        try {
            BigDecimal number = new BigDecimal(text.trim());
            BigDecimal result = number.multiply(new BigDecimal("1000"));
            
            return df.format(result);
        } catch (NumberFormatException e) {
            return "错误：输入 '" + text + "' 不是有效的数字格式";
        }
    }

}