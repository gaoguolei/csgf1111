package org.jeecg.modules.pcms.project.vo;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.Data;

import java.time.LocalDate;

/**
 * @Author gongzhou
 * @Date 2023/10/31 下午2:20
 * @Version 1.5
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class QueryDateParam {
    private LocalDate start;
    private LocalDate end;

    public QueryDateParam(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }
}
