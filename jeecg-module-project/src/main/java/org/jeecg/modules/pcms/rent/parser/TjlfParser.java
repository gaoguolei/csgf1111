package org.jeecg.modules.pcms.rent.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.springframework.stereotype.Component;

/**
 * 天津、廊坊导入解析器  sheet: 总表 (2)   实测 2063 行 × 29 列
 *
 * 列索引（0基）：
 * 0序号 1发电户号 2编码 3所属项目 4客户姓名 5银行卡姓名 6银行卡号 7身份证号 8安装地址 9并网日期
 * 10安装总块数 11安装总容量(W) 12业务 13型号
 * 14阳光房块数 15南北坡块数 16其他15度角块数
 * 17 1-5年阳光房  18 1-5年南北坡  19 1-5年其他15度角
 * 20 6-25年阳光房 21 6-25年南北坡 22 6-25年其他15度角
 *
 * 天津特点：
 * 1. 安装总容量单位是 W（45块×550W=24750），其余项目是 kW，这里必须 ÷1000
 * 2. 唯一一个有身份证号的项目
 * 3. 多一个"业务"（业务员）字段
 * 4. 该文件还有"表格修改记录"和全是 #REF! 的"HB"两个 sheet，必须按名字定位
 * 5. 块数三列集中在 14-16，单价六列集中在 17-22，与其他项目的"按房型分组"排法不同
 * 6. 并网日期单元格格式是 yyyy/mm/dd，与其他四个项目又不一样，统一走原始数值取
 *
 * @Version: V1.0
 */
@Component
public class TjlfParser implements RentExcelParser {

    private static final BigDecimal W_TO_KW = new BigDecimal("1000");

    @Override
    public String regionCode() {
        return "TJLF";
    }

    @Override
    public String sheetName() {
        return "总表 (2)";
    }

    @Override
    public int minColumns() {
        return 23;
    }

    @Override
    public ImportRowDTO parse(ExcelRow row, int rowNo) {
        String powerUserNo = row.str(1);
        if (powerUserNo == null) {
            return null;
        }
        String model = row.flatStr(13);

        // 源文件单位是 W，统一换算成 kW 入库
        BigDecimal capacityW = row.decimal(11);
        BigDecimal capacityKw = capacityW == null ? null
                : capacityW.divide(W_TO_KW, 4, RoundingMode.HALF_UP);

        ImportRowDTO dto = new ImportRowDTO()
                .setRowNo(rowNo)
                .setPowerUserNo(powerUserNo)
                .setStationCode(row.str(2))
                .setProjectName(row.str(3))
                .setFarmerName(row.str(4))
                .setBankCardName(row.str(5))
                .setBankCardNo(row.str(6))
                .setIdCard(row.str(7))
                .setInstallAddr(row.flatStr(8))
                .setGridDate(row.date(9))
                .setTotalBlocks(row.blocks(10))
                .setTotalCapacity(capacityKw)
                .setSalesman(row.str(12));

        dto.addInstall(buildInstall("TJ_SUNROOM", model, row, 14, 17, 20));
        dto.addInstall(buildInstall("TJ_SN", model, row, 15, 18, 21));
        dto.addInstall(buildInstall("TJ_15DEG", model, row, 16, 19, 22));
        return dto;
    }

    private ImportRowDTO.InstallDTO buildInstall(String typeCode, String model, ExcelRow row,
                                                 int blockIdx, int firstIdx, int laterIdx) {
        return new ImportRowDTO.InstallDTO()
                .setInstallTypeCode(typeCode)
                .setModuleModel(model)
                .setBlockCount(row.blocks(blockIdx))
                .addRent("TJ_Y1_5", row.price(firstIdx))
                .addRent("TJ_Y6_25", row.price(laterIdx));
    }
}
