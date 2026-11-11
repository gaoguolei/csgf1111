package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/16 上午8:02
 * @Version 1.5
 */

@Data
@TableName("pv_datalogger")
public class PvDatalogger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String plantId;
    private String sn;
    private String manufacturer;
    private String type;
    private String lastUpdateTime;
    private String status;
    private String netmode;
    private String tenantId;
}
