package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 所属项目下拉框数据，做成"大区 -> 项目"两级。
 *
 * 需求稿写的是 6 个大区，但实际数据有 12 个具体项目
 * （北京一/二/三期、天津一~五期、廊坊、徐州、海南、河南），
 * 所以下拉做二级联动：先选大区，再选具体期数；只选大区就查该大区全部项目。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "项目下拉选项", description = "项目下拉选项")
public class ProjectOptionVO {

    @ApiModelProperty(value = "大区编码 BJ/TJLF/XZ/HAIN/HENAN")
    private String regionCode;

    @ApiModelProperty(value = "大区名称")
    private String regionName;

    @ApiModelProperty(value = "该大区下的具体项目")
    private List<ProjectItem> children = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "项目选项", description = "项目选项")
    public static class ProjectItem {
        @ApiModelProperty(value = "项目ID，作为列表查询的 projectId 传回")
        private Long id;

        @ApiModelProperty(value = "项目编码")
        private String projectCode;

        @ApiModelProperty(value = "项目全称")
        private String projectName;
    }
}
