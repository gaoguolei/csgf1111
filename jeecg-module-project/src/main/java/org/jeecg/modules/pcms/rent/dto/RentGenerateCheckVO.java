package org.jeecg.modules.pcms.rent.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 「生成租金」按钮点击后的前置检查结果。需求第25条。
 *
 * 前端拿到 action 就知道该弹什么：
 *   BLOCK_NO_INSTALL → 弹提示，不让生成（"请先添加组件安装信息"）
 *   CONFIRM_UPDATE   → 弹二次确认（"租金信息已存在，生成租金将更新现有的租金金额信息，请确认是否生成"）
 *   DIRECT           → 直接生成（可选的普通确认）
 *
 * 把判断放后端是为了和生成时用的是同一套条件：
 * 前端自己想判断"有没有安装信息"，得先拉农户详情再翻 installs 数组，
 * 而"有安装信息但没填单价"这种情况前端看不出来，点下去照样失败。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "生成租金前置检查", description = "生成租金前置检查")
public class RentGenerateCheckVO {

    /** 还没有安装信息（或没填租金单价），不能生成 */
    public static final String ACTION_BLOCK = "BLOCK_NO_INSTALL";
    /** 本年度租金已存在，生成会覆盖金额，要用户确认 */
    public static final String ACTION_CONFIRM = "CONFIRM_UPDATE";
    /** 首次生成，直接执行 */
    public static final String ACTION_DIRECT = "DIRECT";

    @ApiModelProperty(value = "1=该农户已有可用的安装信息和租金单价，0=没有（此时不能生成）")
    private Integer hasInstall;

    @ApiModelProperty(value = "1=本年度已有租金台账（生成会更新现有金额），0=还没生成过")
    private Integer hasLedger;

    @ApiModelProperty(value = "下一步动作 BLOCK_NO_INSTALL / CONFIRM_UPDATE / DIRECT")
    private String action;

    @ApiModelProperty(value = "给用户看的提示语，前端直接展示即可")
    private String message;

    @ApiModelProperty(value = "本次生成覆盖的时间段，如 2026-01-01 ~ 2026-12-31。"
            + "需求34：弹窗提示上要标注，让用户看清这次动的是哪一段租金；"
            + "算不出区间时为空")
    private String periodRange;

    @ApiModelProperty(value = "本年度已有的租金台账条数，仅作参考")
    private Integer ledgerCount;
}