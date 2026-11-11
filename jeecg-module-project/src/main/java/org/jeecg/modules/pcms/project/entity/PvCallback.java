package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/20 下午5:58
 * @Version 1.5
 */
@Data
@TableName("pv_callback")
public class PvCallback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String signFlowId;

    private String action;

    private Integer projectId;

    private String customBizNum;

    private Long timestamp;

    private Long operateTime;

    private String signOrder;

    private String signResult;

    private String resultDescription;
}
