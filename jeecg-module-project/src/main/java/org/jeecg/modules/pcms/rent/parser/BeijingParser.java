package org.jeecg.modules.pcms.rent.parser;

import org.jeecg.modules.pcms.rent.dto.ImportRowDTO;
import org.jeecg.modules.pcms.rent.util.ExcelRow;
import org.springframework.stereotype.Component;

/**
 * 北京导入解析器  sheet: 租金支付   实测 1422 行 × 29 列
 *
 * 列索引（0基）：
 * 0序号 1发电户号 2编号 3姓名 4所属项目 5安装地址 6安装总容量(kW) 7安装总块数 8并网时间 9组件型号
 * 10阳光房块数 11阳光房前5年 12阳光房后20年
 * 13南坡块数   14南坡前5年   15南坡后20年
 * 16北坡块数   17北坡前5年   18北坡后20年
 * 19东西坡块数 20东西坡前5年 21东西坡后20年
 * 22前5年每年租金 23后20年每年租金  —— 这两列按项目经理决策忽略，不入库不校验
 *
 * 北京特点：
 * 1. 四种坡向，"东西坡"是北京独有
 * 2. 无身份证、无银行卡号、无银行卡姓名
 * 3. 组件型号只有一列，四种坡向共用
 * 4. 安装总容量已是 kW，不换算
 * 5. 并网时间单元格格式是 yyyy"年"m"月"d"日"，显示成"2024年10月17日"，
 *    所以必须走 ExcelRow 的原始数值取日期，不能解析显示字符串
 *
 * @Version: V1.0
 */
@Component
public class BeijingParser implements RentExcelParser {

    @Override
    public String regionCode() {
        return "BJ";
    }

    @Override
    public String sheetName() {
        return "租金支付";
    }

    @Override
    public int minColumns() {
        return 24;
    }

    @Override
    public ImportRowDTO parse(ExcelRow row, int rowNo) {
        String powerUserNo = row.str(1);
        if (powerUserNo == null) {
            return null;
        }
        // 四种坡向共用第 9 列的组件型号
        String model = row.flatStr(9);

        ImportRowDTO dto = new ImportRowDTO()
                .setRowNo(rowNo)
                .setPowerUserNo(powerUserNo)
                .setStationCode(row.str(2))
                .setFarmerName(row.str(3))
                .setProjectName(row.str(4))
                .setInstallAddr(row.flatStr(5))
                .setTotalCapacity(row.decimal(6))
                .setTotalBlocks(row.blocks(7))
                .setGridDate(row.date(8));

        dto.addInstall(buildInstall("BJ_SUNROOM", model, row, 10, 11, 12));
        dto.addInstall(buildInstall("BJ_SOUTH", model, row, 13, 14, 15));
        dto.addInstall(buildInstall("BJ_NORTH", model, row, 16, 17, 18));
        dto.addInstall(buildInstall("BJ_EW", model, row, 19, 20, 21));
        return dto;
    }

    private ImportRowDTO.InstallDTO buildInstall(String typeCode, String model, ExcelRow row,
                                                 int blockIdx, int firstIdx, int laterIdx) {
        return new ImportRowDTO.InstallDTO()
                .setInstallTypeCode(typeCode)
                .setModuleModel(model)
                .setBlockCount(row.blocks(blockIdx))
                .addRent("BJ_Y1_5", row.price(firstIdx))
                .addRent("BJ_Y6_25", row.price(laterIdx));
    }
}
