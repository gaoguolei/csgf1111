package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 回单对账异步任务
 *
 * 客户传几十页 PDF 时，拆页 + 逐页 OCR 要跑几分钟，同步接口会超时，
 * 所以改成"提交任务 → 后台跑 → 前端轮询状态"。
 * 任务状态落库而不是放内存：服务重启后还能查，多实例部署也不会查不到。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_reconcile_task")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_reconcile_task对象", description = "租金回单对账任务")
public class TRentReconcileTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 已建任务，等着跑 */
    public static final String STATUS_PENDING = "PENDING";
    /** 正在跑 */
    public static final String STATUS_RUNNING = "RUNNING";
    /** 跑完了（各条明细里可能有成功也有失败，看 count） */
    public static final String STATUS_DONE = "DONE";
    /** 任务本身挂了（文件读不了、线程异常等），明细没生成 */
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.INPUT)
    private String id;

    @ApiModelProperty(value = "PENDING待执行 RUNNING执行中 DONE完成 FAILED失败")
    private String status;

    @ApiModelProperty(value = "上传的文件数")
    private Integer fileCount;

    @ApiModelProperty(value = "拆出来的总页数")
    private Integer totalPages;

    @ApiModelProperty(value = "已处理页数，前端用它显示进度")
    private Integer progress;

    @ApiModelProperty(value = "识别出的回单/清单行总条数")
    private Integer total;

    @ApiModelProperty(value = "对账一致条数")
    private Integer okCount;

    @ApiModelProperty(value = "对账异常条数")
    private Integer errorCount;

    @ApiModelProperty(value = "未能处理条数")
    private Integer failCount;

    @ApiModelProperty(value = "各条明细，JSON 数组")
    private String resultJson;

    @ApiModelProperty(value = "任务整体失败的原因")
    private String errorMsg;

    @ApiModelProperty(value = "详情页传的台账ID，列表页批量为空")
    private Long ledgerId;

    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 是否已经跑完（成功或失败都算） */
    public boolean finished() {
        return STATUS_DONE.equals(status) || STATUS_FAILED.equals(status);
    }

    public static String statusName(String status) {
        if (status == null) {
            return "待执行";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待执行";
            case STATUS_RUNNING:
                return "执行中";
            case STATUS_DONE:
                return "已完成";
            case STATUS_FAILED:
                return "执行失败";
            default:
                return status;
        }
    }
}