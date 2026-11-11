package org.jeecg.modules.pcms.rent.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.ImportResultVO;
import org.jeecg.modules.pcms.rent.service.IRentImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金台账导入。
 *
 * 按项目经理要求五个项目各用一个独立接口——五张 Excel 的列布局确实完全不同，
 * 从列数、字段顺序到房型分组方式都不一样，共用一个接口只会写成一堆 if。
 * 但五个接口背后是同一个 RentImportService，差异只在各自的 Parser 里。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-台账导入")
@RestController
@RequestMapping("/rent/import")
public class RentImportController {

    @Autowired
    private IRentImportService rentImportService;

    @ApiOperation(value = "北京导入", notes = "sheet: 租金支付，29列。四种坡向含东西坡，无身份证银行卡")
    @PostMapping("/beijing")
    public Result<ImportResultVO> importBeijing(@RequestParam("file") MultipartFile file) {
        return doImport(file, "BJ");
    }

    @ApiOperation(value = "天津、廊坊导入", notes = "sheet: 总表 (2)，29列。容量单位是W，入库自动÷1000")
    @PostMapping("/tjlf")
    public Result<ImportResultVO> importTjlf(@RequestParam("file") MultipartFile file) {
        return doImport(file, "TJLF");
    }

    @ApiOperation(value = "徐州导入", notes = "sheet: 徐州项目，22列。每种房型各有独立组件型号列")
    @PostMapping("/xuzhou")
    public Result<ImportResultVO> importXuzhou(@RequestParam("file") MultipartFile file) {
        return doImport(file, "XZ");
    }

    @ApiOperation(value = "海南导入", notes = "sheet: 海南项目清单，26列。.xls格式，并网日期是Excel序列号")
    @PostMapping("/hainan")
    public Result<ImportResultVO> importHainan(@RequestParam("file") MultipartFile file) {
        return doImport(file, "HAIN");
    }

    @ApiOperation(value = "河南导入", notes = "sheet: 河南漯河项目总台账，29列。按年档位与按日期档位并存")
    @PostMapping("/henan")
    public Result<ImportResultVO> importHenan(@RequestParam("file") MultipartFile file) {
        return doImport(file, "HENAN");
    }

    private Result<ImportResultVO> doImport(MultipartFile file, String regionCode) {
        ImportResultVO vo = rentImportService.importExcel(file, regionCode);
        String msg = String.format("导入完成：新增 %d 户，更新 %d 户，合并 %d 行，跳过 %d 行，失败 %d 行",
                vo.getInsertCount(), vo.getUpdateCount(), vo.getMergedRowCount(),
                vo.getSkipCount(), vo.getFailCount());
        if (vo.getFailCount() > 0) {
            // 失败明细在 result 里，前端可直接渲染或导出失败行
            return Result.OK(msg, vo);
        }
        return Result.OK(msg, vo);
    }
}
