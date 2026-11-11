package org.jeecg.modules.pcms.rent.parser;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.springframework.stereotype.Component;

/**
 * 海南导入解析器  sheet: 海南项目清单   实测 265 行 × 26 列
 *
 * 列索引（0基）：
 * 0序号 1发电户号 2客户姓名 3所属项目 4银行卡姓名 5银行卡号 6安装地址 7并网日期 8安装总容量 9安装总块数
 * 10组件型号（全局，三种房型共用）
 * 11双坡/庭院块数  12前10年 13后15年 14前8年 15后17年
 * 16阳光房5°块数   17前10年 18后15年 19前8年 20后17年
 * 21阳光房10°块数  22前10年 23后15年 24前8年 25后17年
 *
 * 海南特点：
 * 1. 文件是 .xls 老格式，并网日期是日期型单元格，原始值就是序列号 45253，
 *    ExcelRow.date 统一按 1900 日期系统转换
 * 2. 每种房型有 4 个租金档位：前10/后15 和 前8/后17 两套并存
 * 3. 实测双坡/庭院和阳光房10° 的 265 行全空，块数为 0 不会生成安装行；
 *    模板列按项目经理决策保留
 * 4. 前8/后17 只有 4 行有值
 *
 * @Version: V1.0
 */
@Component
public class HainanParser implements RentExcelParser {

    @Override
    public String regionCode() {
        return "HAIN";
    }

    @Override
    public String sheetName() {
        return "海南项目清单";
    }

    @Override
    public int minColumns() {
        return 26;
    }

    @Override
    public ImportRowDTO parse(ExcelRow row, int rowNo) {
        String powerUserNo = row.str(1);
        if (powerUserNo == null) {
            return null;
        }
        // 三种房型共用第 10 列的组件型号
        String model = row.flatStr(10);

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

        dto.addInstall(buildInstall("HN_DOUBLE", model, row, 11, 12, 13, 14, 15));
        dto.addInstall(buildInstall("HN_SUN5", model, row, 16, 17, 18, 19, 20));
        dto.addInstall(buildInstall("HN_SUN10", model, row, 21, 22, 23, 24, 25));
        return dto;
    }

    private ImportRowDTO.InstallDTO buildInstall(String typeCode, String model, ExcelRow row,
                                                 int blockIdx, int y10Idx, int y15Idx, int y8Idx, int y17Idx) {
        return new ImportRowDTO.InstallDTO()
                .setInstallTypeCode(typeCode)
                .setModuleModel(model)
                .setBlockCount(row.blocks(blockIdx))
                .addRent("HN_Y1_10", row.price(y10Idx))
                .addRent("HN_Y11_25", row.price(y15Idx))
                .addRent("HN_Y1_8", row.price(y8Idx))
                .addRent("HN_Y9_25", row.price(y17Idx));
    }
}
