package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("device_fault_history")
@Data
public class DeviceFaultHistory {


    /**
     * 主键ID
     * 自增长，对应数据库字段：id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备型号编码
     * 对应数据库字段：device_model_code
     */
    private String deviceModelCode;

    /**
     * 设备名称
     * 对应数据库字段：device_name
     */
    private String deviceName;

    /**
     * 故障唯一标识码
     * 对应数据库字段：fault_code
     */
    private String faultCode;

    /**
     * 故障描述
     * 对应数据库字段：fault_desc
     */
    private String faultDesc;

    /**
     * 故障级别
     * 1：重要 2：次要 3：一般 4：轻微 (传多个用逗号分割)
     * 对应数据库字段：fault_level
     */
    private Integer faultLevel;

    /**
     * 故障名称
     * 对应数据库字段：fault_name
     */
    private String faultName;

    /**
     * 故障类型
     * 1：故障 2：告警 3：提示 4：建议 (传多个用逗号分割)
     * 对应数据库字段：fault_type
     */
    private Integer faultType;


    /**
     * 故障类型（古瑞瓦特）
     */
    private String grwFaultType;


    /**
     * 故障类型编码
     * 对应数据库字段：fault_type_code
     */
    private Integer faultTypeCode;

    /**
     * 设备真实故障码
     * 多个用"|"分隔
     * 对应数据库字段：fault_reason
     */
    private String faultReason;

    /**
     * 故障恢复时间
     * 格式：yyyy-MM-dd HH:mm:ss
     * 对应数据库字段：over_time
     */
    private String overTime;

    /**
     * 故障发生时间
     * 格式：yyyy-MM-dd HH:mm:ss
     * 对应数据库字段：create_time
     */
    private String createTime;

    /**
     * 故障处理状态
     * 1：未确认 2：待处理 3：处理中 4：已解决 9：已关闭
     * 对应数据库字段：process_status
     */
    private Integer processStatus;

    /**
     * 电站ID
     * 对应数据库字段：ps_id
     */
    private Integer psId;

    /**
     * 设备ps_key
     * 对应数据库字段：ps_key
     */
    private String psKey;

    /**
     * 电站名称
     * 对应数据库字段：ps_name
     */
    private String psName;

    /**
     * 设备类型名称
     * 对应数据库字段：type_name
     */
    private String typeName;

    /**
     * 设备UUID
     * 对应数据库字段：uuid
     */
    private String uuid;

    /**
     * 是否并网故障
     * 0否，1是
     * 对应数据库字段：grid_connection_status
     */
    private Integer gridConnectionStatus;

    /**
     * 数据来源
     * 1-阳光云, 2-古瑞瓦特
     * 对应数据库字段：source
     */
    private Integer source;


    /**
     * 数据来源
     * 1-阳光云, 2-古瑞瓦特
     * 对应数据库字段：source
     */
    private String deviceSn;
}
