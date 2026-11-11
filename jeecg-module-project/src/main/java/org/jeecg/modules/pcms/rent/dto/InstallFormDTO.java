package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 安装信息新增/编辑表单，租金单价随安装信息一起提交。
 *
 * 租金单价不单独开接口，是因为它必须挂在某条安装信息下——
 * "阳光房5° 前15年 10元/块/年"这句话里，脱开"阳光房5°"和它的块数，
 * 单价就没有意义，金额也算不出来。所以整条安装信息带着它的档位一起存，
 * 保存时该安装信息下的旧租金记录会被整体替换。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "安装信息表单", description = "安装信息表单")
public class InstallFormDTO {

    @ApiModelProperty(value = "安装信息ID，新增时不传")
    private Long id;

    @ApiModelProperty(value = "所属农户ID，必填")
    private Long stationId;

    @ApiModelProperty(value = "安装类型ID。从下拉框选已有房型时传这个")
    private Long installTypeId;

    @ApiModelProperty(value = "安装房型名称。下拉框里没有、运营直接手输新房型时传这个，"
            + "后端会自动在该农户所属大区下建一条字典。两个字段至少传一个，"
            + "都传时以 installTypeId 为准")
    private String installTypeName;

    @ApiModelProperty(value = "安装型号，字符串。支持 550+580 这类多型号混写")
    private String moduleModel;

    @ApiModelProperty(value = "安装块数，必填且大于0")
    private Integer blockCount;

    @ApiModelProperty(value = "各档位租金单价，整体替换该安装信息下的旧记录")
    private List<RentItem> rents = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "租金单价项", description = "租金单价项")
    public static class RentItem {

        @ApiModelProperty(value = "租金档位ID。下拉框里已有这个档位时传它；"
                + "下拉框里没有、运营要新建档位时留空，改用下面的 priceTypeName 等字段")
        private Long priceTypeId;

        @ApiModelProperty(value = "新档位名称，如「前12年」。下拉框里没有时传这个，"
                + "后端会在该农户所属大区下自动建一条档位字典。"
                + "与 priceTypeId 至少传一个，都传时以 priceTypeId 为准")
        private String priceTypeName;

        @ApiModelProperty(value = "切分方式 1按年（相对每户并网日）2按自然日期（全体统一时点）。"
                + "新建档位时用，不传默认 1按年")
        private Integer splitMode;

        @ApiModelProperty(value = "按年档位：起始年，含。不传默认 1")
        private Integer startYear;

        @ApiModelProperty(value = "按年档位：结束年，含。不传默认 25")
        private Integer endYear;

        @ApiModelProperty(value = "按日期档位：生效起日，NULL=从并网日起")
        private java.util.Date effectDateFrom;

        @ApiModelProperty(value = "按日期档位：生效止日，NULL=到租期结束")
        private java.util.Date effectDateTo;

        @ApiModelProperty(value = "排序号。同一安装信息下多个档位时段重叠时数字小的优先。"
                + "不传排到最后")
        private Integer sortNo;

        @ApiModelProperty(value = "租金单价 元/块/年，必填")
        private BigDecimal unitPrice;
    }
}
