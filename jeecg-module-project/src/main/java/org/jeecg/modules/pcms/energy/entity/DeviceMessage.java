package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("device_message")
@Data
public class DeviceMessage {

    /**
     * 主键ID
     * 自增长，对应数据库字段：id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    //标题
    private String msgTitle;

    //消息类型
    private String msgType;

    //用户
    private String userId;

    //创建时间
    private Date createTime;

    //优先级
    private String priority;

    @TableField(exist = false)
    private String userName;
}
