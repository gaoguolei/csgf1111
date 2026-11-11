package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * PDF 导出请求：列表页勾选若干条，每条导出一个 PDF。
 * 前端把勾选行的 id 传进来即可，字段名与列表页的勾选集合保持一致，都叫 selections。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "PDF导出请求", description = "PDF导出请求")
public class RentPdfRequest {

    @ApiModelProperty(value = "勾选的农户ID 或 台账ID。一条一个 PDF，最终打包成一个 zip 下载",
            required = true)
    private List<Long> selections = new ArrayList<>();
}
