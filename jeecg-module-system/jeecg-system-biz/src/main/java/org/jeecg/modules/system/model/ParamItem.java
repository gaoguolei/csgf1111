package org.jeecg.modules.system.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/3/6 下午2:43
 * @Version 1.5
 */

@Data
@TableName("set_param_item")
public class ParamItem {
    private static final long serialVersionUID = 1L;


    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 参数组id
     */
    private Integer paramGroupId;

    /**
     * 参数名称
     */
    private String paramName;

    /**
     * 控件类型 （文件，文本，）
     */
    private String widgetType;

    /**
     * 控件参数
     */
    private String widgetParam;

    /**
     * 是否必填 0 非必填， 1必填
     */
    private String isRequired;

    /**
     * 排序
     */
    private Integer sortNo;


    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 所属部门
     */
    private String sysOrgCode;

    /**
     * 所属租户
     */
    private Integer tenantId;








}
