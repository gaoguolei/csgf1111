package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 年度台账生成任务的状态。提交接口立刻返回它，前端拿 taskId 轮询。
 *
 * status：
 *   PENDING / RUNNING → 继续轮询，用 progress/totalStations 显示进度
 *   DONE              → 读 createdCount/skippedCount/failCount/periodRange 渲染结果
 *   FAILED            → 读 errorMsg 报错
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "台账生成任务状态", description = "台账生成任务状态")
public class RentGenerateTaskVO {

    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @ApiModelProperty(value = "PENDING待执行 RUNNING执行中 DONE完成 FAILED失败")
    private String status;

    @ApiModelProperty(value = "状态中文名")
    private String statusName;

    @ApiModelProperty(value = "租金年度")
    private Integer rentYear;

    @ApiModelProperty(value = "参与生成的电站总数")
    private Integer totalStations;

    @ApiModelProperty(value = "已处理电站数。progress/totalStations 就是进度百分比")
    private Integer progress;

    @ApiModelProperty(value = "新生成的台账条数")
    private Integer createdCount;

    @ApiModelProperty(value = "跳过的条数：审核中等原因不生成的期")
    private Integer skippedCount;

    @ApiModelProperty(value = "失败的电站数")
    private Integer failCount;

    @ApiModelProperty(value = "本次覆盖的时间段，如 2026-01-01 ~ 2026-12-31")
    private String periodRange;

    @ApiModelProperty(value = "跳过原因明细")
    private List<String> skipDetails = new ArrayList<>();

    @ApiModelProperty(value = "失败原因明细")
    private List<String> failDetails = new ArrayList<>();

    @ApiModelProperty(value = "任务整体失败的原因，只有 FAILED 时才有")
    private String errorMsg;

    @ApiModelProperty(value = "耗时毫秒")
    private Long costMs;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发起时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "完成时间")
    private Date finishTime;
}
