package org.jeecg.modules.pcms.rent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 五个导入接口共用的中间模型。
 * 每个 Parser 只负责把自己那张 Excel 的列布局翻译成本对象，
 * 之后的落库逻辑五个项目完全一致。
 *
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class ImportRowDTO {

    /** Excel 行号，从 1 开始（不含表头），用于错误定位 */
    private int rowNo;

    // ---------- 农户电站信息 ----------
    private String farmerName;
    private String idCard;
    private String bankCardNo;
    private String bankCardName;
    private String installAddr;
    /** 发电户号原样值，重复时由 RentImportService 统一加 -1/-2 后缀 */
    private String powerUserNo;
    /** Excel 中"所属项目"原值，用于匹配 t_project.project_name */
    private String projectName;
    private String stationCode;
    private String salesman;
    private Date gridDate;
    private Date actualGenDate;
    /** 安装总容量，Parser 负责换算成 kW（天津源文件是 W） */
    private BigDecimal totalCapacity;
    private Integer totalBlocks;

    // ---------- 安装信息 + 租金 ----------
    private List<InstallDTO> installs = new ArrayList<>();

    public ImportRowDTO addInstall(InstallDTO install) {
        if (install != null && install.getBlockCount() != null && install.getBlockCount() > 0) {
            this.installs.add(install);
        }
        return this;
    }

    /**
     * 一个安装类型一条
     */
    @Data
    @Accessors(chain = true)
    public static class InstallDTO {
        /** 安装类型编码，对应 t_install_type.type_code */
        private String installTypeCode;
        /** 组件型号，字符串原样存 */
        private String moduleModel;
        private Integer blockCount;
        private List<RentDTO> rents = new ArrayList<>();

        public InstallDTO addRent(String priceTypeCode, BigDecimal unitPrice) {
            if (unitPrice != null) {
                this.rents.add(new RentDTO().setPriceTypeCode(priceTypeCode).setUnitPrice(unitPrice));
            }
            return this;
        }
    }

    /**
     * 一个租金档位一条
     */
    @Data
    @Accessors(chain = true)
    public static class RentDTO {
        /** 档位编码，对应 t_rent_price_type.type_code */
        private String priceTypeCode;
        private BigDecimal unitPrice;
    }
}
