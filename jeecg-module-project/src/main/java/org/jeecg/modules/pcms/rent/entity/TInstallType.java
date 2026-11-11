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
 * @Description: 安装类型字典
 * @Version: V1.0
 */
@Data
@TableName("t_install_type")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_install_type对象", description = "安装类型字典")
public class TInstallType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 运营自建房型的编码前缀 */
    public static final String CUSTOM_CODE_PREFIX = "CUSTOM_";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属大区")
    private String regionCode;

    @ApiModelProperty(value = "安装类型编码")
    private String typeCode;

    @ApiModelProperty(value = "安装类型名称")
    private String typeName;

    @ApiModelProperty(value = "1=系统内置，导入接口依赖其编码，禁止删除和改编码；0=运营自建")
    private Integer isBuiltin;

    private Integer sortNo;

    @ApiModelProperty(value = "1启用 0停用。停用后不出现在新增下拉里，历史数据仍显示")
    private Integer status;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public boolean builtin() {
        return isBuiltin != null && isBuiltin == 1;
    }
}
