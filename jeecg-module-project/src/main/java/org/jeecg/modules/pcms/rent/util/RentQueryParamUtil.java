package org.jeecg.modules.pcms.rent.util;

import java.util.ArrayList;
import java.util.List;

import org.jeecg.common.exception.JeecgBootException;

/**
 * 列表查询参数的通用解析。
 *
 * 状态类参数前端有时一次传多个（如 auditStatus=2,4,6,8），DTO 里统一按字符串接收，
 * 在这里拆成数值列表。直接声明成 Integer 会绑定失败并报
 * "Failed to convert property value of type java.lang.String to required type java.lang.Integer"。
 *
 * @Version: V1.0
 */
public final class RentQueryParamUtil {

    private RentQueryParamUtil() {
    }

    /**
     * 逗号分隔的整数列表。空值返回空列表；有非法值直接报错，不静默忽略。
     */
    public static List<Integer> parseIntList(String value) {
        List<Integer> result = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return result;
        }
        for (String part : value.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.valueOf(s));
            } catch (NumberFormatException e) {
                throw new JeecgBootException("状态参数格式不正确：" + value);
            }
        }
        return result;
    }
}
