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
import org.jeecg.modules.pcms.rent.util.RentPeriodUtil;

/**
 * @Description: 项目档案表
 * @Version: V1.0
 */
@Data
@TableName("t_project")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_project对象", description = "项目档案表")
public class TProject implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "大区编码 BJ/TJLF/XZ/HAIN/HENAN")
    private String regionCode;

    @ApiModelProperty(value = "大区名称")
    private String regionName;

    @ApiModelProperty(value = "项目编码")
    private String projectCode;

    @ApiModelProperty(value = "项目全称，与Excel中所属项目原值一致")
    private String projectName;
    @ApiModelProperty(value = "一年结算几期租金。默认4期按季度，海南为1期整年结一次。"
            + "只能取 12 的约数：1/2/3/4/6/12")
    private Integer periodsPerYear;
    @ApiModelProperty(value = "租期年限")
    private Integer leaseYears;

    @ApiModelProperty(value = "1启用 0停用")
    private Integer status;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    /**
     * 一年几期，没配或配了非法值时按四期。
     *
     * 这里对脏数据做兜底而不是抛异常：生成租金是批量跑的，
     * 一个项目配错了不该让整年生成挂掉，按默认四期先出账，配错的值另行修。
     */
    public int periodsPerYear() {
        if (periodsPerYear == null || periodsPerYear < 1 || periodsPerYear > 12
                || 12 % periodsPerYear != 0) {
            return RentPeriodUtil.DEFAULT_PERIOD_COUNT;
        }
        return periodsPerYear;
    }

}
