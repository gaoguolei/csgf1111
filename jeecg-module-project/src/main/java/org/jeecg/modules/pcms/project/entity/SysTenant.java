package org.jeecg.modules.pcms.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/5/17 下午3:27
 * @Version 1.5
 *
 */
@TableName("sys_tenant")
@Data
public class SysTenant {

    private static final long serialVersionUID = 1L;

    /**
     * 编码
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;


    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 开始时间
     */
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date beginDate;

    /**
     * 结束时间
     */
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    /**
     * 状态 1正常 0冻结
     */
    @Dict(dicCode = "tenant_status")
    private Integer status;

    /**
     * 地区
     */
    private String areaAddress;


    /**
     * 项目编号
     */
    private String areaProjectCode;


    /**
     * 详细地址
     */
    private String fullAddress;

    /**
     * 项目描述
     */
    private String remark;
}
