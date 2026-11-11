package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

/**
 * 设备数据实体类（无 lombok 版本）
 */

@Data
public class DeviceDataDto {
    /** 错误信息（空字符串表示无错误） */
    private String error_msg;
    /** 核心数据值 */
    private String data;
    /** 设备序列号 */
    private String device_sn;
    /** 数据采集器序列号 */
    private String dataloggerSn;
    /** 错误码（0 表示成功） */
    private Integer error_code;

}