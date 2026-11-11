package org.jeecg.modules.pcms.rent.parser;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;

/**
 * 五个项目各实现一个 Parser。
 * Parser 只做一件事：把自己那张 Excel 的一行，翻译成统一的 ImportRowDTO。
 * 落库、去重、字典匹配全部由 RentImportService 统一处理。
 *
 * @Version: V1.0
 */
public interface RentExcelParser {

    /**
     * 大区编码，同时是接口路由键：BJ / TJLF / XZ / HAIN / HENAN
     */
    String regionCode();

    /**
     * 期望读取的 sheet 名，返回 null 表示取第一个 sheet
     */
    String sheetName();

    /**
     * 该模板的最小列数，用于导入前校验用户是否传错了文件
     */
    int minColumns();

    /**
     * 解析一行。整行为空或关键字段缺失时返回 null，由调用方计入跳过
     */
    ImportRowDTO parse(ExcelRow row, int rowNo);
}
