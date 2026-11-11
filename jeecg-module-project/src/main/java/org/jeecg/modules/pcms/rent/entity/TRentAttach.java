package org.jeecg.modules.pcms.rent.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 租金附件
 *
 * 需求稿第 7 条的普通附件和第 13 条的银行回单都存这张表，用 biz_type 区分：
 * 普通附件提交审核前可删，银行回单是对账凭证，一律不允许删。
 *
 * @Version: V1.0
 */
@Data
@TableName("t_rent_attach")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "t_rent_attach对象", description = "租金附件")
public class TRentAttach implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 业务类型：租金详情页人工上传的附件 */
    public static final String BIZ_ATTACH = "ATTACH";
    /** 业务类型：批量上传的银行支付回单 */
    public static final String BIZ_RECEIPT = "RECEIPT";
    /** 业务类型：审核人驳回时上传的附件，需求稿第 15 条 */
    public static final String BIZ_AUDIT = "AUDIT";

    private static final String IMAGE_EXT = ".jpg.jpeg.png.bmp.gif.webp.";

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "台账ID")
    private Long ledgerId;

    @ApiModelProperty(value = "业务类型 ATTACH普通附件 RECEIPT银行回单")
    private String bizType;

    @ApiModelProperty(value = "用户上传时的原始文件名，列表按这个名字显示")
    private String fileName;

    @ApiModelProperty(value = "服务器相对存储路径")
    private String filePath;

    @ApiModelProperty(value = "扩展名，小写不带点")
    private String fileExt;

    @ApiModelProperty(value = "字节数")
    private Long fileSize;

    @ApiModelProperty(value = "1=图片，前端可在线预览")
    private Integer isImage;
    @ApiModelProperty(value = "上传时台账处于第几轮审核。审核附件按它分组，"
            + "才能分清哪几个附件属于哪一次驳回。0=升级前的历史数据，轮次不详")
    private Integer auditRound;

    @ApiModelProperty(value = "0正常 1删除")
    private Integer delFlag;

    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 按扩展名判断是不是图片 */
    public static boolean imageExt(String ext) {
        if (ext == null || ext.trim().isEmpty()) {
            return false;
        }
        return IMAGE_EXT.contains("." + ext.trim().toLowerCase(Locale.ROOT) + ".");
    }

    public boolean receipt() {
        return BIZ_RECEIPT.equals(bizType);
    }

    public boolean audit() {
        return BIZ_AUDIT.equals(bizType);
    }

    public static String bizTypeName(String bizType) {
        if (BIZ_RECEIPT.equals(bizType)) {
            return "银行回单";
        }
        if (BIZ_AUDIT.equals(bizType)) {
            return "审核附件";
        }
        return "附件";
    }
}
