package org.jeecg.modules.pcms.rent.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 安装房型、租金档位的下拉选项。
 * 详情页编辑安装信息时用，取值范围由该农户所属大区决定——
 * 北京只能选阳光房/南坡/北坡/东西坡，河南只能选斜屋面/阳光房5°/阳光房10°。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "字典选项", description = "字典选项")
public class DictOptionVO {

    @ApiModelProperty(value = "选项ID，提交时传这个")
    private Long id;

    @ApiModelProperty(value = "编码")
    private String code;

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "租金档位专用：1按年 2按自然日期；安装类型为空")
    private Integer splitMode;

    @ApiModelProperty(value = "租金档位专用：适用区间描述，如 第1-15年 / 2025-04-01之后")
    private String rangeDesc;

    @ApiModelProperty(value = "租金档位专用：按该农户并网日期推算出的实际日期区间，"
            + "如 2025-03-21 ~ 2030-03-20。"
            + "与 rangeDesc 并存，不是在替换它：rangeDesc 说「第几年到第几年」，"
            + "这个说具体是哪天到哪天。没有并网日期（新建农户还没填）时为空")
    private String dateRangeDesc;

    @ApiModelProperty(value = "安装房型专用：1=系统内置，0=运营自建。前端可给自建的加个标记区分")
    private Integer builtin;
}