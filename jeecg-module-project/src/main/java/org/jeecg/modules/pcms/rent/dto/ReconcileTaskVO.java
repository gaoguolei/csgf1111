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
 * 回单对账任务的状态。前端拿着 taskId 轮询这个。
 *
 * 一次对账可能要跑几分钟（几十页 PDF 逐页 OCR），所以上传接口只返回 taskId，
 * 前端按 status 判断要不要继续轮询：
 *   PENDING / RUNNING → 继续轮
 *   DONE              → 读 items 渲染结果
 *   FAILED            → 读 errorMsg 报错
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "回单对账任务状态", description = "回单对账任务状态")
public class ReconcileTaskVO {

    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @ApiModelProperty(value = "PENDING待执行 RUNNING执行中 DONE完成 FAILED失败")
    private String status;

    @ApiModelProperty(value = "状态中文名")
    private String statusName;

    @ApiModelProperty(value = "已处理页数")
    private Integer progress;

    @ApiModelProperty(value = "总页数。progress/totalPages 就是进度百分比")
    private Integer totalPages;

    @ApiModelProperty(value = "识别出的回单/清单行总条数")
    private Integer total;

    @ApiModelProperty(value = "对账一致条数")
    private Integer okCount;

    @ApiModelProperty(value = "对账异常条数")
    private Integer errorCount;

    @ApiModelProperty(value = "未能处理条数")
    private Integer failCount;

    @ApiModelProperty(value = "任务整体失败的原因，只有 FAILED 时才有")
    private String errorMsg;

    @ApiModelProperty(value = "各条明细，只有 DONE 时才有。结构与对账接口的 items 一致")
    private List<ReconcileResultVO.Item> items = new ArrayList<>();

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
}