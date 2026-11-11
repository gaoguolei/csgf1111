package org.jeecg.modules.pcms.rent.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 附件列表项。列表按用户上传时的原始文件名显示。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "租金附件", description = "租金附件")
public class RentAttachVO {

    @ApiModelProperty(value = "附件ID")
    private Long id;

    @ApiModelProperty(value = "台账ID")
    private Long ledgerId;

    @ApiModelProperty(value = "业务类型 ATTACH普通附件 RECEIPT银行回单")
    private String bizType;

    @ApiModelProperty(value = "业务类型名称")
    private String bizTypeName;

    @ApiModelProperty(value = "用户上传时的文件名")
    private String fileName;

    @ApiModelProperty(value = "扩展名")
    private String fileExt;

    @ApiModelProperty(value = "字节数")
    private Long fileSize;

    @ApiModelProperty(value = "1=图片，可在线预览")
    private Integer isImage;
    @ApiModelProperty(value = "上传时台账处于第几轮审核，0=升级前的历史数据。"
            + "审核附件按这个值归到对应那次驳回下面")
    private Integer auditRound;
    @ApiModelProperty(value = "下载地址")
    private String downloadUrl;

    @ApiModelProperty(value = "预览地址，非图片为空")
    private String previewUrl;

    @ApiModelProperty(value = "1=当前状态允许删除。银行回单是对账凭证，永远不允许删")
    private Integer deletable;

    @ApiModelProperty(value = "上传人")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "上传时间")
    private Date createTime;
}
