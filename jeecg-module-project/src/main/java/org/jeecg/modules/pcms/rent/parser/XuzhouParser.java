package org.jeecg.modules.pcms.rent.parser;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.springframework.stereotype.Component;

/**
 * 徐州导入解析器  sheet: 徐州项目   实测 396 行 × 22 列
 *
 * 列索引（0基）：
 * 0序号 1发电户号 2客户姓名 3所属项目 4银行卡姓名 5银行卡号 6安装地址 7并网日期 8安装总容量 9安装总块数
 * 10双坡/庭院型号   11块数 12前10年 13后15年
 * 14阳光房5°型号    15块数 16前10年 17后15年
 * 18阳光房10°型号   19块数 20前10年 21后15年
 *
 * 徐州特点：
 * 1. 每组房型各有独立的"组件型号"列（北京、天津、海南是全局共用一列）
 * 2. 表头笔误"后150年租金"，实为后15年，按列索引取值不受影响
 * 3. 无身份证、无编码
 * 4. 组件型号有 4 条是 Excel 公式算出的长小数（632.551020408163），原样存字符串
 * 5. 并网日期单元格格式是 yyyy"年"m"月"d"日"，走原始数值取
 *
 * @Version: V1.0
 */
@Component
public class XuzhouParser implements RentExcelParser {

    @Override
    public String regionCode() {
        return "XZ";
    }

    @Override
    public String sheetName() {
        return "徐州项目";
    }

    @Override
    public int minColumns() {
        return 22;
    }

    @Override
    public ImportRowDTO parse(ExcelRow row, int rowNo) {
        String powerUserNo = row.str(1);
        if (powerUserNo == null) {
            return null;
        }
        ImportRowDTO dto = new ImportRowDTO()
                .setRowNo(rowNo)
                .setPowerUserNo(powerUserNo)
                .setFarmerName(row.str(2))
                .setProjectName(row.str(3))
                .setBankCardName(row.str(4))
                .setBankCardNo(row.str(5))
                .setInstallAddr(row.flatStr(6))
                .setGridDate(row.date(7))
                .setTotalCapacity(row.decimal(8))
                .setTotalBlocks(row.blocks(9));

        dto.addInstall(buildInstall("XZ_DOUBLE", row, 10, 11, 12, 13));
        dto.addInstall(buildInstall("XZ_SUN5", row, 14, 15, 16, 17));
        dto.addInstall(buildInstall("XZ_SUN10", row, 18, 19, 20, 21));
        return dto;
    }

    private ImportRowDTO.InstallDTO buildInstall(String typeCode, ExcelRow row, int modelIdx,
                                                 int blockIdx, int firstIdx, int laterIdx) {
        return new ImportRowDTO.InstallDTO()
                .setInstallTypeCode(typeCode)
                .setModuleModel(row.flatStr(modelIdx))
                .setBlockCount(row.blocks(blockIdx))
                .addRent("XZ_Y1_10", row.price(firstIdx))
                .addRent("XZ_Y11_25", row.price(laterIdx));
    }
}
