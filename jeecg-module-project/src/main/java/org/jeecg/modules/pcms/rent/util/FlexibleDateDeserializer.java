package org.jeecg.modules.pcms.rent.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * 宽松的日期反序列化器：按几种常见格式依次尝试，容忍前端日期组件传值和接口约定格式不一致。
 *
 * 背景：@JsonFormat(pattern=...) 是严格匹配，前端传 "2026-09-29 16:25"（带时分）
 * 而 pattern 是 yyyy-MM-dd 时直接抛 InvalidFormatException；全局 ObjectMapper 的
 * 默认格式又是 yyyy-MM-dd HH:mm:ss，同样接不住纯日期串。表单类的日期字段挂上本
 * 反序列化器后，以下格式都能收：yyyy-MM-dd、yyyy-MM-dd HH:mm、yyyy-MM-dd HH:mm:ss、
 * 以及斜杠写法的 yyyy/MM/dd（带时分秒同理）。
 *
 * 实现说明：SimpleDateFormat 默认是前缀匹配（parse 到格式用完就停，尾巴忽略），
 * 所以 yyyy-MM-dd 这一档天然兼容带时分的串；setLenient(false) 关掉的是
 * "2026-13-45 这种越界值自动进位" 的宽松，不是前缀匹配。
 *
 * @Version: V1.0
 */
public class FlexibleDateDeserializer extends JsonDeserializer<Date> {

    /** 依次尝试，先解析成功的胜出；第一档顺带兼容带时分的串 */
    private static final String[] PATTERNS = {
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
    };

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();
        for (String pattern : PATTERNS) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            try {
                return sdf.parse(text);
            } catch (ParseException ignored) {
                // 这一档对不上，换下一档
            }
        }
        throw ctxt.weirdStringException(text, Date.class,
                "日期格式无法识别，支持 yyyy-MM-dd / yyyy-MM-dd HH:mm[:ss] / yyyy/MM/dd 等写法");
    }
}
