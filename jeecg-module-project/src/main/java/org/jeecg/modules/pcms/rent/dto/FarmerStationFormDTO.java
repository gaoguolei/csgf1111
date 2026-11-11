package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.pcms.rent.util.FlexibleDateDeserializer;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 农户信息新增/编辑表单。对应需求稿第 4 条。
 *
 * 租金截止日期不在表单里——它由并网日期 + 项目租期年限自动算出，
 * 让人手填必然会和并网日期对不上。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "农户信息表单", description = "农户信息表单")
public class FarmerStationFormDTO {

    @ApiModelProperty(value = "主键ID，新增时不传，编辑时必传")
    private Long id;

    @ApiModelProperty(value = "农户姓名，必填")
    private String farmerName;

    @ApiModelProperty(value = "发电户号，必填。同一项目下不能重复")
    private String powerUserNo;

    @ApiModelProperty(value = "所属项目ID，必填")
    private Long projectId;

    @ApiModelProperty(value = "编号：北京编号/天津编码")
    private String stationCode;

    @ApiModelProperty(value = "银行卡姓名")
    private String bankCardName;

    @ApiModelProperty(value = "银行卡号")
    private String bankCardNo;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    @ApiModelProperty(value = "安装地址")
    private String installAddr;

    @ApiModelProperty(value = "业务员")
    private String salesman;

    // 反序列化走宽松解析：前端日期组件可能传 yyyy-MM-dd，也可能带时分（yyyy-MM-dd HH:mm），
    // @JsonFormat 严格匹配会直接 400；序列化仍按 yyyy-MM-dd 出，和详情/列表接口一致
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = FlexibleDateDeserializer.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "并网日期，改动后会自动重算租金截止日期")
    private Date gridDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = FlexibleDateDeserializer.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际发电日期")
    private Date actualGenDate;

    @ApiModelProperty(value = "安装总容量 kW")
    private BigDecimal totalCapacity;

    @ApiModelProperty(value = "安装总块数")
    private Integer totalBlocks;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "安装信息，选填。"
            + "新增农户时可以顺带把安装信息一起录进来，不用再单独调一次安装信息接口。"
            + "不传、传 null 或空数组都表示只建农户，不建安装信息。"
            + "每条的字段与 /rent/farmerStation/install/save 的请求体一致，"
            + "其中 stationId 不用传，后端会用刚建好的农户ID补上")
    private List<InstallFormDTO> installs = new ArrayList<>();
}
