package org.jeecg.modules.system.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/3/6 上午10:11
 * @Version 1.5
 */
@Data
@TableName("set_param_group")
public class ParamGroup {

    private static final long serialVersionUID = 1L;


    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 参数组名
     */
    private String groupName;

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

    /**
     * 项目id
     */
    private Integer areaProjectId;


    /**
     * 排序
     */
    private Integer sortNo;

}
