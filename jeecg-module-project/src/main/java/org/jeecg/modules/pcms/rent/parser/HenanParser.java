package org.jeecg.modules.pcms.rent.parser;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.springframework.stereotype.Component;

/**
 * 河南导入解析器  sheet: 河南漯河项目总台账   实测 122 行 × 29 列
 *
 * 列索引（0基）：
 * 0序号 1发电户号 2客户姓名 3所属项目 4银行卡姓名 5银行卡号 6安装地址 7安装总容量 8安装总块数
 * 9并网日期 10实际发电日期 11型号（斜屋面用）
 * 12斜屋面块数    13前15年 14后10年 15 2025-03-31之前 16 2025-04-01之后
 * 17阳光房5°型号  18块数 19前15年 20后10年 21 2025-03-31之前 22 2025-04-01之后
 * 23阳光房10°型号 24块数 25前15年 26后10年 27 2025-03-31之前 28 2025-04-01之后
 *
 * 河南特点：
 * 1. 唯一一个"按年档位"和"按自然日期档位"并存的项目：
 *    斜屋面 59 户和阳光房5° 25 户走前15/后10；
 *    阳光房10° 38 户的前15/后10 两列全空，单价 10 和 15 全在日期列上，
 *    两批农户零重叠。四列逐列判断、非空即落一条租金，不做"必须成对"校验
 * 2. 斜屋面的型号取全局第 11 列，阳光房5°/10° 各有独立型号列
 * 3. 唯一一个有"实际发电日期"的项目
 * 4. 客户姓名大量是村委会、小学、财政所等单位名，不做姓名格式校验
 * 5. 存在一格两个户号 4105339297086/4105341527689，字符串原样存，不拆
 * 6. 并网日期和实际发电日期的格式都是 yyyy"年"m"月"d"日"，走原始数值取
 *
 * @Version: V1.0
 */
@Component
public class HenanParser implements RentExcelParser {

    @Override
    public String regionCode() {
        return "HENAN";
    }

    @Override
    public String sheetName() {
        return "河南漯河项目总台账";
    }

    @Override
    public int minColumns() {
        return 29;
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
                .setTotalCapacity(row.decimal(7))
                .setTotalBlocks(row.blocks(8))
                .setGridDate(row.date(9))
                .setActualGenDate(row.date(10));

        // 斜屋面的型号取全局第 11 列
        dto.addInstall(buildInstall("HEN_SLOPE", row.flatStr(11), row, 12, 13, 14, 15, 16));
        dto.addInstall(buildInstall("HEN_SUN5", row.flatStr(17), row, 18, 19, 20, 21, 22));
        dto.addInstall(buildInstall("HEN_SUN10", row.flatStr(23), row, 24, 25, 26, 27, 28));
        return dto;
    }

    private ImportRowDTO.InstallDTO buildInstall(String typeCode, String model, ExcelRow row,
                                                 int blockIdx, int y15Idx, int y10Idx, int befIdx, int aftIdx) {
        return new ImportRowDTO.InstallDTO()
                .setInstallTypeCode(typeCode)
                .setModuleModel(model)
                .setBlockCount(row.blocks(blockIdx))
                // 按年档位
                .addRent("HEN_Y1_15", row.price(y15Idx))
                .addRent("HEN_Y16_25", row.price(y10Idx))
                // 按自然日期档位，2025-04-01 是全体农户统一的调价时点
                .addRent("HEN_D_BEF", row.price(befIdx))
                .addRent("HEN_D_AFT", row.price(aftIdx));
    }
}
