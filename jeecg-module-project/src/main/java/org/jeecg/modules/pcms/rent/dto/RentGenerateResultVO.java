package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 台账生成/重新生成的结果。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "台账生成结果", description = "台账生成结果")
public class RentGenerateResultVO {

    @ApiModelProperty(value = "租金年度")
    private Integer rentYear;

    @ApiModelProperty(value = "本次生成/重算实际覆盖的时间段，如 2026-01-01 ~ 2026-12-31。"
            + "前端弹窗直接展示这个，让用户看清这次动的是哪一段租金")
    private String periodRange;

    @ApiModelProperty(value = "参与生成的电站数")
    private int stationCount;

    @ApiModelProperty(value = "新生成的台账条数")
    private int createdCount;

    @ApiModelProperty(value = "原地更新已有台账的条数。需求第21条：重新生成不改新增行，直接改现有的")
    private int updatedCount;

    @ApiModelProperty(value = "跳过的条数：审核中、审核通过或已支付的期不再重新生成")
    private int skippedCount;

    @ApiModelProperty(value = "跳过原因明细，最多返回200条")
    private List<String> skipDetails = new ArrayList<>();

    @ApiModelProperty(value = "失败的电站数")
    private int failCount;

    @ApiModelProperty(value = "失败原因明细，最多返回200条")
    private List<String> failDetails = new ArrayList<>();

    @ApiModelProperty(value = "没有并网日期、算不出起租时间的电站数。这类不逐户报错，汇总成一条")
    private int noGridDateCount;

    /** 缺并网日期的电站样本，只留几个用于提示文案 */
    private final List<String> noGridDateSamples = new ArrayList<>();

    @ApiModelProperty(value = "耗时毫秒")
    private long costMs;

    private static final int MAX_DETAIL = 200;
    private static final int MAX_NO_GRID_SAMPLES = 3;

    public void addSkip(String msg) {
        skippedCount++;
        if (skipDetails.size() < MAX_DETAIL) {
            skipDetails.add(msg);
        }
    }

    public void addFail(String msg) {
        failCount++;
        if (failDetails.size() < MAX_DETAIL) {
            failDetails.add(msg);
        }
    }

    /**
     * 缺并网日期、算不出起租时间的一户。
     *
     * 这类是"数据没录全"，不是系统出错，几百户逐条列出来会把真正要处理的问题淹掉，
     * 所以只计入失败数，明细合并成一条汇总（见 {@link #noGridDateSummary()}）。
     */
    public void addNoGridDate(String stationDesc) {
        failCount++;
        noGridDateCount++;
        if (noGridDateSamples.size() < MAX_NO_GRID_SAMPLES) {
            noGridDateSamples.add(stationDesc);
        }
    }

    /** 往明细最前面插一条汇总文案（不累加计数），让人一眼看到最大的那类问题 */
    public void addFailSummary(String msg) {
        if (failDetails.size() < MAX_DETAIL) {
            failDetails.add(0, msg);
        }
    }

    /** 缺并网日期的汇总文案；没有这类问题时返回 null */
    public String noGridDateSummary() {
        if (noGridDateCount <= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(noGridDateCount).append(" 户没有并网日期，算不出租金起算时间");
        if (!noGridDateSamples.isEmpty()) {
            sb.append("（如：").append(String.join("、", noGridDateSamples));
            if (noGridDateCount > noGridDateSamples.size()) {
                sb.append(" 等");
            }
            sb.append("）");
        }
        sb.append("，请补齐并网日期后重新生成");
        return sb.toString();
    }
}
