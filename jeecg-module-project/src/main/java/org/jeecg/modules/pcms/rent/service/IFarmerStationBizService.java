package org.jeecg.modules.pcms.rent.service;

import java.util.List;

import org.jeecg.modules.pcms.rent.dto.DictOptionVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationDetailVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationFormDTO;
import org.jeecg.modules.pcms.rent.dto.InstallFormDTO;

/**
 * 农户信息的增删改与详情。对应需求稿第 4、5 条。
 *
 * @Version: V1.0
 */
public interface IFarmerStationBizService {

    /** 农户详情：主表 + 安装信息 + 每条安装下的租金档位 */
    FarmerStationDetailVO detail(Long id);

    /**
     * 某农户的安装信息（含每家装下的各档位单价）。
     * 农户详情和租金台账详情共用一份组装逻辑，避免两处的安装信息结构不一致。
     */
    List<FarmerStationDetailVO.InstallVO> installsOf(Long stationId);

    /** 新增农户，返回新ID */
    Long addFarmer(FarmerStationFormDTO form);

    /** 编辑农户 */
    void editFarmer(FarmerStationFormDTO form);

    /** 删除农户，逻辑删除并级联删除其安装信息和租金 */
    void deleteFarmer(Long id);

    /** 批量删除农户 */
    void deleteFarmerBatch(List<Long> ids);

    /** 新增或编辑一条安装信息，租金档位一并整体替换。返回安装信息ID */
    Long saveInstall(InstallFormDTO form);

    /** 删除一条安装信息及其下全部租金 */
    void deleteInstall(Long installId);

    /**
     * 某农户可选的安装房型。
     *
     * 选项按【大区】决定，所以 stationId 和 projectId 传一个就够：
     * 农户详情页传 stationId；新建农户时农户还没建出来，只能传 projectId。
     */
    List<DictOptionVO> installTypeOptions(Long stationId, Long projectId);

    /**
     * 某农户可选的租金档位。stationId 和 projectId 传一个即可，说明同上
     */
    List<DictOptionVO> priceTypeOptions(Long stationId, Long projectId);
}
