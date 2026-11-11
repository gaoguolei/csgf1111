package org.jeecg.modules.pcms.rent.util;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Cell;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.metadata.ReadSheet;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 把 Excel 读成 ExcelRow 列表。
 *
 * 做了三件 EasyExcel 默认不做的事：
 *
 * 1. 按列索引取值，不按表头文字匹配。五个模板的表头有单元格内换行
 *    （"1-5年\n阳光房租金单价"）、有笔误（"后150年租金"）、还有整列空表头。
 *
 * 2. 额外抓一份单元格的原始数值（ReadCellData.getOriginalNumberValue）。
 *    日期在 Excel 里就是个序列号，取原始值才不受显示格式影响——五个模板的
 *    并网日期格式各不相同（中文年月日 / yyyy/mm/dd）。
 *
 * 3. 还原合并单元格。河南有 45 处、海南有 2 处合并，合并的正好是农户级信息
 *    （序号、发电户号、客户姓名、银行卡、安装地址）。EasyExcel 默认只有左上角
 *    那格有值，不还原的话后续行会被误判成"缺姓名/缺户号"。
 *
 * .xls 和 .xlsx 都由 EasyExcel 自动识别，海南的老格式无需单独处理。
 *
 * @Version: V1.0
 */
@Slf4j
public class ExcelRowReader {

    /** 发电户号在五个模板里都是第 2 列（0基索引 1） */
    private static final int COL_POWER_USER_NO = 1;

    private ExcelRowReader() {
    }

    /**
     * @param is                 输入流
     * @param preferredSheetName 期望的 sheet 名，找不到时退回第一个 sheet。
     *                           天津文件除"总表 (2)"外还有"表格修改记录"和全是 #REF! 的"HB"，
     *                           必须按名字定位，不能默认取第一个
     */
    public static Result read(InputStream is, String preferredSheetName) {
        Result result = new Result();
        List<CellExtra> merges = new ArrayList<>();
        ExcelReader reader = EasyExcel.read(is)
                .extraRead(CellExtraTypeEnum.MERGE)
                .build();
        try {
            List<ReadSheet> sheets = reader.excelExecutor().sheetList();
            if (sheets == null || sheets.isEmpty()) {
                throw new IllegalArgumentException("Excel 中没有任何 sheet");
            }
            ReadSheet target = matchSheet(sheets, preferredSheetName);
            result.setSheetName(target.getSheetName());

            ReadSheet readSheet = EasyExcel.readSheet(target.getSheetNo())
                    .headRowNumber(1)
                    .registerReadListener(new RowListener(result.getRows(), merges))
                    .build();
            reader.read(readSheet);
        } finally {
            reader.finish();
        }
        // 合并信息在 xlsx 里位于 sheetData 之后，必须等整表读完再还原
        applyMerges(result.getRows(), merges);
        return result;
    }

    private static ReadSheet matchSheet(List<ReadSheet> sheets, String preferredSheetName) {
        if (preferredSheetName != null) {
            for (ReadSheet s : sheets) {
                if (preferredSheetName.equals(s.getSheetName())) {
                    return s;
                }
            }
            // 名字不完全一致时退化为忽略空格比对，容忍"总表 (2)"被改成"总表(2)"
            String key = preferredSheetName.replaceAll("\\s", "");
            for (ReadSheet s : sheets) {
                String name = s.getSheetName() == null ? "" : s.getSheetName().replaceAll("\\s", "");
                if (name.equals(key)) {
                    return s;
                }
            }
            log.warn("未找到 sheet[{}]，退回第一个 sheet[{}]", preferredSheetName, sheets.get(0).getSheetName());
        }
        return sheets.get(0);
    }

    /**
     * 把合并区域左上角的值填充到区域内其余单元格。
     *
     * CellExtra 的行号是 sheet 内的绝对行号（0基），表头占第 0 行，
     * 所以数据行在 rows 列表里的下标 = sheet 行号 - 1。
     */
    private static void applyMerges(List<ExcelRow> rows, List<CellExtra> merges) {
        if (rows.isEmpty() || merges.isEmpty()) {
            return;
        }
        for (CellExtra m : merges) {
            Integer fr = m.getFirstRowIndex();
            Integer lr = m.getLastRowIndex();
            Integer fc = m.getFirstColumnIndex();
            Integer lc = m.getLastColumnIndex();
            if (fr == null || lr == null || fc == null || lc == null || lr <= fr) {
                // 只在纵向合并时需要向下填充，单行内的横向合并对按列取值没影响
                continue;
            }
            int anchorIdx = fr - 1;
            if (anchorIdx < 0 || anchorIdx >= rows.size()) {
                continue;
            }
            ExcelRow anchor = rows.get(anchorIdx);
            for (int c = fc; c <= lc; c++) {
                String t = anchor.rawText(c);
                BigDecimal n = anchor.rawNum(c);
                if (t == null && n == null) {
                    continue;
                }
                for (int r = fr + 1; r <= lr; r++) {
                    int idx = r - 1;
                    if (idx < 0 || idx >= rows.size()) {
                        continue;
                    }
                    ExcelRow row = rows.get(idx);
                    row.fill(c, t, n);
                    if (c == COL_POWER_USER_NO) {
                        // 户号也被合并 = 这几行是同一个电站，标记出来交给 Service 合并成一条
                        row.setUserNoFromMerge(true);
                    }
                }
            }
        }
    }

    @Data
    public static class Result {
        private String sheetName;
        private List<ExcelRow> rows = new ArrayList<>();
    }

    private static class RowListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<ExcelRow> rows;
        private final List<CellExtra> merges;

        RowListener(List<ExcelRow> rows, List<CellExtra> merges) {
            this.rows = rows;
            this.merges = merges;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            ExcelRow row = new ExcelRow();
            // EasyExcel 按单元格显示格式转好的字符串
            if (data != null) {
                for (Map.Entry<Integer, String> e : data.entrySet()) {
                    row.put(e.getKey(), e.getValue(), null);
                }
            }
            // 单元格格式化之前的原始数值，日期和数字都靠它
            Map<Integer, Cell> cellMap = context.readRowHolder() == null
                    ? null : context.readRowHolder().getCellMap();
            if (cellMap != null) {
                for (Map.Entry<Integer, Cell> e : cellMap.entrySet()) {
                    BigDecimal n = rawNumber(e.getValue());
                    if (n != null) {
                        row.put(e.getKey(), null, n);
                    }
                }
            }
            rows.add(row);
        }

        @Override
        public void extra(CellExtra extra, AnalysisContext context) {
            if (extra != null && CellExtraTypeEnum.MERGE == extra.getType()) {
                merges.add(extra);
            }
        }

        private BigDecimal rawNumber(Cell cell) {
            if (!(cell instanceof ReadCellData)) {
                return null;
            }
            ReadCellData<?> rc = (ReadCellData<?>) cell;
            // 日期单元格被格式化成字符串后，原始序列号保留在 originalNumberValue 里
            BigDecimal n = rc.getOriginalNumberValue();
            return n != null ? n : rc.getNumberValue();
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 无需处理
        }
    }
}
