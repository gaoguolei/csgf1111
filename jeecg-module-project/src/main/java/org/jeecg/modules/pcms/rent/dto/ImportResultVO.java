package org.jeecg.modules.pcms.rent.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导入结果。失败明细不落库，直接同步返回给前端，前端可导出失败行 Excel。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "导入结果", description = "导入结果")
public class ImportResultVO {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "实际读取的sheet名")
    private String sheetName;

    @ApiModelProperty(value = "大区编码")
    private String regionCode;

    @ApiModelProperty(value = "Excel总数据行数")
    private int total;

    @ApiModelProperty(value = "新增农户数")
    private int insertCount;

    @ApiModelProperty(value = "更新农户数")
    private int updateCount;

    @ApiModelProperty(value = "跳过行数，如整行为空")
    private int skipCount;

    @ApiModelProperty(value = "因Excel合并单元格共用发电户号而被并入上一条的行数")
    private int mergedRowCount;

    @ApiModelProperty(value = "失败行数")
    private int failCount;

    @ApiModelProperty(value = "耗时毫秒")
    private long costMs;

    @ApiModelProperty(value = "失败明细")
    private List<ImportErrorVO> errors = new ArrayList<>();

    @ApiModelProperty(value = "告警明细，数据已入库但需人工复核")
    private List<ImportErrorVO> warnings = new ArrayList<>();

    public int getSuccessCount() {
        return insertCount + updateCount;
    }

    public void addError(int rowNo, String powerUserNo, String msg) {
        this.errors.add(new ImportErrorVO().setRowNo(rowNo).setPowerUserNo(powerUserNo).setMessage(msg));
        this.failCount++;
    }

    public void addWarning(int rowNo, String powerUserNo, String msg) {
        this.warnings.add(new ImportErrorVO().setRowNo(rowNo).setPowerUserNo(powerUserNo).setMessage(msg));
    }

    @Data
    @Accessors(chain = true)
    @ApiModel(value = "导入失败明细", description = "导入失败明细")
    public static class ImportErrorVO {
        @ApiModelProperty(value = "Excel行号")
        private int rowNo;
        @ApiModelProperty(value = "发电户号")
        private String powerUserNo;
        @ApiModelProperty(value = "错误信息")
        private String message;
    }
}
