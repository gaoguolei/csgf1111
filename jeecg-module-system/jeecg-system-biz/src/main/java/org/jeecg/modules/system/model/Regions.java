package org.jeecg.modules.system.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 地区实体
 */
@Data
@TableName("set_regions")
public class Regions implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 父地区id
     */
    private Integer parentId;
    /**
     * 路径
     */
    private String regionPath;
    /**
     * 级别
     */
    private Integer regionGrade;
    /**
     * 名称
     */
    private String localName;
    /**
     * 邮编
     */
    private String zipcode;

    @TableField(exist = false)
    private  String tempId;

    @TableField(exist = false)
    public List<Regions> childList;
}
