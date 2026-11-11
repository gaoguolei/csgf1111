package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 年度租金台账生成任务
 *
 * 年度生成要遍历全部电站，几万个电站跑几分钟，同步接口会被网关 504 掉，
 * 所以改成"提交任务 → 后台跑 → 前端轮询状态"。
 * 和回单对账一样，状态落库而不是放内存：服务重启后还能查，多实例部署也不会查不到。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_generate_task")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_generate_task对象", description = "租金台账生成任务")
public class TRentGenerateTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 已建任务，等着跑 */
    public static final String STATUS_PENDING = "PENDING";
    /** 正在跑 */
    public static final String STATUS_RUNNING = "RUNNING";
    /** 跑完了（可能有跳过/失败的电站，看 count） */
    public static final String STATUS_DONE = "DONE";
    /** 任务本身挂了（线程异常等） */
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.INPUT)
    private String id;

    @ApiModelProperty(value = "PENDING待执行 RUNNING执行中 DONE完成 FAILED失败")
    private String status;

    @ApiModelProperty(value = "生成年度")
    private Integer rentYear;

    @ApiModelProperty(value = "参与生成的电站总数，进度分母")
    private Integer totalStations;

    @ApiModelProperty(value = "已处理电站数，前端用它算进度")
    private Integer progress;

    @ApiModelProperty(value = "新生成的台账条数")
    private Integer createdCount;

    @ApiModelProperty(value = "跳过的条数")
    private Integer skippedCount;

    @ApiModelProperty(value = "失败的电站数")
    private Integer failCount;

    @ApiModelProperty(value = "本次覆盖的时间段，如 2026-01-01 ~ 2026-12-31")
    private String periodRange;

    @ApiModelProperty(value = "跳过/失败原因明细，结构 {\"skip\":[],\"fail\":[]}")
    private String detailJson;

    @ApiModelProperty(value = "任务整体失败的原因")
    private String errorMsg;

    @ApiModelProperty(value = "耗时毫秒")
    private Long costMs;

    private String createBy;

    private Date createTime;

    private Date finishTime;
}
