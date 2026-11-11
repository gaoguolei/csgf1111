package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 农户详情。对应需求稿第 5 条：
 * 农户姓名、银行卡姓名、安装房型、安装型号、安装块数、租金单价。
 *
 * 结构是三层：农户 -> 多条安装信息 -> 每条安装信息下多个租金档位。
 * 一个农户会有多条安装信息，是因为同一个电站可能有好几种房型
 * （比如河南老窝镇政府：斜屋面102块 + 阳光房5°120块 + 阳光房5°62块）。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "农户详情", description = "农户详情")
public class FarmerStationDetailVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "农户姓名，详情页只读")
    private String farmerName;

    @ApiModelProperty(value = "银行卡姓名")
    private String bankCardName;

    @ApiModelProperty(value = "银行卡号")
    private String bankCardNo;

    @ApiModelProperty(value = "身份证号，仅天津/廊坊有")
    private String idCard;

    @ApiModelProperty(value = "编号：北京编号/天津编码")
    private String stationCode;

    @ApiModelProperty(value = "发电户号")
    private String powerUserNo;

    @ApiModelProperty(value = "安装地址")
    private String installAddr;

    @ApiModelProperty(value = "业务员，仅天津/廊坊")
    private String salesman;

    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @ApiModelProperty(value = "所属项目名称")
    private String projectName;

    @ApiModelProperty(value = "大区编码，决定安装房型和租金档位的可选范围")
    private String regionCode;

    @ApiModelProperty(value = "大区名称")
    private String regionName;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期")
    private Date gridDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际发电日期，仅河南有")
    private Date actualGenDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "租金截止日期，由并网日期加租期年限自动算出")
    private Date rentDeadline;

    @ApiModelProperty(value = "安装总容量 kW")
    private BigDecimal totalCapacity;

    @ApiModelProperty(value = "安装总块数")
    private Integer totalBlocks;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "年租金合计（元/年），按各档位第一年单价算，仅供参考")
    private BigDecimal firstYearRent;

    @ApiModelProperty(value = "安装信息列表")
    private List<InstallVO> installs = new ArrayList<>();

    /**
     * 一条安装信息 = 一种安装房型
     */
    @Data
    @Accessors(chain = true)
    @ApiModel(value = "安装信息", description = "安装信息")
    public static class InstallVO {

        @ApiModelProperty(value = "安装信息ID")
        private Long id;

        @ApiModelProperty(value = "安装类型ID")
        private Long installTypeId;

        @ApiModelProperty(value = "安装房型名称，如 斜屋面/阳光房5°/南坡")
        private String installTypeName;

        @ApiModelProperty(value = "安装型号，字符串。可能是多型号混写如 550+580")
        private String moduleModel;

        @ApiModelProperty(value = "1=多型号混写待人工拆分")
        private Integer isMultiModel;

        @ApiModelProperty(value = "安装块数")
        private Integer blockCount;

        @ApiModelProperty(value = "该房型容量 kW，多型号行为空")
        private BigDecimal capacityKw;

        @ApiModelProperty(value = "该房型下的各档位租金单价")
        private List<RentVO> rents = new ArrayList<>();
    }

    /**
     * 一个租金档位的单价
     */
    @Data
    @Accessors(chain = true)
    @ApiModel(value = "租金单价", description = "租金单价")
    public static class RentVO {

        @ApiModelProperty(value = "租金记录ID")
        private Long id;

        @ApiModelProperty(value = "租金档位ID")
        private Long priceTypeId;

        @ApiModelProperty(value = "档位名称，如 前15年/后10年/2025年4月1日之后")
        private String priceTypeName;

        @ApiModelProperty(value = "按本户并网日期推算的档位实际日期区间，如 2025-03-21 ~ 2030-03-20。"
                + "与 priceTypeName 并存：名称说「第几年到第几年」，这个说具体哪天到哪天")
        private String dateRangeDesc;

        @ApiModelProperty(value = "切分方式 1按年 2按自然日期")
        private Integer splitMode;

        @ApiModelProperty(value = "档位排序号，同一安装信息下数字小的档位先生效，"
                + "前端无排序需求时可不展示")
        private Integer sortNo;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
        @ApiModelProperty(value = "单价生效起日，NULL=从并网日或档位起始日起。"
                + "改过价的档位会有值（新价从次月1号起生效）")
        private Date effectDateFrom;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
        @ApiModelProperty(value = "单价生效止日，NULL=到租期结束。"
                + "改过价的旧档位会有值（旧价只生效到月底）")
        private Date effectDateTo;

        @ApiModelProperty(value = "租金单价 元/块/年")
        private BigDecimal unitPrice;

        @ApiModelProperty(value = "该档位年金额 = 单价 × 块数")
        private BigDecimal amount;
    }
}
