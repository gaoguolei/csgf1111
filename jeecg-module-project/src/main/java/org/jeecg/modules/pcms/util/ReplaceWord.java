package org.jeecg.modules.pcms.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version V1.0
 * @Description：Word模板数据填充
 * @Date: 2022/11/16 17:47
 */
public class ReplaceWord {

    /***
     * @Description :替换段落文本
     * @param document docx解析对象
     * @param textMap  需要替换的信息集合
     * @return void
     * @Date 2022/11/17 17:22
     */
    public static void changeText(XWPFDocument document, Map<String, Object> textMap) {
        // 获取段落集合
        Iterator<XWPFParagraph> iterator = document.getParagraphsIterator();
        XWPFParagraph paragraph = null;
        while (iterator.hasNext()) {
            paragraph = iterator.next();
            // 判断此段落是否需要替换
            if (checkText(paragraph.getText())) {
                replaceValue(paragraph, textMap);
            }
        }
    }

    /***
     * @Description :检查文本中是否包含指定的字符(此处为“$”)
     * @param text
     * @return boolean
     * @Date 2022/11/17 17:22
     */
    public static boolean checkText(String text) {
        boolean check = false;
        if (text.contains("$")) {
            check = true;
        }
        return check;
    }

    /**
     * 替换图片
     *
     * @param document
     * @param picData
     * @throws Exception
     */

    public static void changePic(XWPFDocument document, Map<String, Object> picData) throws Exception {
        // 获取段落集合
        Iterator<XWPFParagraph> iterator = document.getParagraphsIterator();
        XWPFParagraph paragraph;
        while (iterator.hasNext()) {
            paragraph = iterator.next();
            // 判断此段落是否需要替换
            String text = paragraph.getText();
            if (checkText(text)) {
                replacePicValue(paragraph, picData);
            }
        }
    }

    /***
     * @Description :替换表格内的文字
     * @param document
     * @param data
     * @return void
     * @Date 2022/11/18 11:29
     */
    public static void changeTableText(XWPFDocument document, Map<String, Object> data) {
        // 获取文件的表格
        Iterator<XWPFTable> tableList = document.getTablesIterator();
        XWPFTable table;
        List<XWPFTableRow> rows;
        List<XWPFTableCell> cells;
        // 循环所有需要进行替换的文本，进行替换
        while (tableList.hasNext()) {
            table = tableList.next();
            if (checkText(table.getText())) {
                rows = table.getRows();
                // 遍历表格，并替换模板
                for (XWPFTableRow row : rows) {
                    cells = row.getTableCells();
                    for (XWPFTableCell cell : cells) {
                        // 判断单元格是否需要替换
                        if (checkText(cell.getText())) {
                            List<XWPFParagraph> paragraphs = cell.getParagraphs();
                            for (XWPFParagraph paragraph : paragraphs) {
                                replaceValue(paragraph, data);
                            }
                        }
                    }
                }
            }
        }
    }

    /***
     * @Description :替换表格内图片
     * @param document
     * @param picData
     * @return void
     * @Date 2022/11/18 11:29
     */
    public static void changeTablePic(XWPFDocument document, Map<String, Object> picData) throws Exception {
        // 获取文件的表格
        Iterator<XWPFTable> tableList = document.getTablesIterator();
        XWPFTable table;
        List<XWPFTableRow> rows;
        List<XWPFTableCell> cells;
        // 循环所有需要进行替换的文本，进行替换
        while (tableList.hasNext()) {
            table = tableList.next();
            if (checkText(table.getText())) {
                rows = table.getRows();
                // 遍历表格，并替换模板
                for (XWPFTableRow row : rows) {
                    cells = row.getTableCells();
                    for (XWPFTableCell cell : cells) {
                        // 判断单元格是否需要替换
                        if (checkText(cell.getText())) {
                            List<XWPFParagraph> paragraphs = cell.getParagraphs();
                            for (XWPFParagraph paragraph : paragraphs) {
                                replacePicValue(paragraph, picData);
                            }
                        }
                    }
                }
            }
        }
    }

    /***
     * @Description :替换内容
     * @param paragraph
     * @param textMap
     * @return void
     * @Date 2022/11/18 11:33
     */
    public static void replaceValue(XWPFParagraph paragraph, Map<String, Object> textMap) {
        XWPFRun run, nextRun;
        String runsText;
        List<XWPFRun> runs = paragraph.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            run = runs.get(i);
            runsText = run.getText(0);
            if(StringUtils.isNotBlank(runsText)){
                if (runsText.contains("${") || (runsText.contains("$") && runs.get(i + 1).getText(0).substring(0, 1).equals("{"))) {
                    while (!runsText.contains("}")) {
                        nextRun = runs.get(i + 1);
                        runsText = runsText + nextRun.getText(0);
                        //删除该节点下的数据
                        paragraph.removeRun(i + 1);
                    }
                    Object value = changeValue(runsText, textMap);
                    //判断key在Map中是否存在
                    if (textMap.containsKey(runsText)) {
                        run.setText(value.toString(), 0);
                    } else {
                        //如果匹配不到，则不修改
                        run.setText(runsText, 0);
                    }
                }
            }
        }
    }

    /***
     * @Description :替换图片内容
     * @param paragraph
     * @param picData
     * @return void
     * @Date 2022/11/18 11:33
     */
    public static void replacePicValue(XWPFParagraph paragraph, Map<String, Object> picData) throws Exception {
        List<XWPFRun> runs = paragraph.getRuns();
        for (XWPFRun run : runs) {
            Object value = changeValue(run.toString(), picData);
            if (picData.containsKey(run.toString())) {
                //清空内容
                run.setText("", 0);
                FileInputStream is = new FileInputStream((String) value);
                //图片宽度、高度
                int width = Units.toEMU(100), height = Units.toEMU(100);
                //添加图片信息，段落高度需要在模板中自行调整
                run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, (String) value, width, height);
            }
        }
    }

    /***
     * @Description :匹配参数
     * @param value
     * @param textMap
     * @return java.lang.Object
     * @Date 2022/11/18 11:33
     */
    public static Object changeValue(String value, Map<String, Object> textMap) {
        Object valu = "";
        for (Map.Entry<String, Object> textSet : textMap.entrySet()) {
            // 匹配模板与替换值 格式${key}
            String key = textSet.getKey();
            if (value.contains(key)) {
                valu = textSet.getValue();
            }
        }
        return valu;
    }
}

