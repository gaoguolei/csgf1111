package org.jeecg.modules.pcms.rent.service;

import java.util.List;

import org.jeecg.modules.pcms.rent.dto.RentPriceTypeFormDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeSaveDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeVO;

/**
 * 租金档位字典的维护。需求：按项目管理租金类型。
 *
 * 几条规则：
 * 1. 新增必须成组，合计从第1年连续覆盖到第25年（通常两条：1-3年 + 4-25年）
 * 2. 修改和删除前先查有没有农户在用——被 t_rent 引用了就不许动，避免把在用的档位改坏
 * 3. 档位是字典数据，删除是物理删除（这张表没有 del_flag）
 *
 * @Version: V1.0
 */
public interface IRentPriceTypeBizService {

    /** 某个大区下的全部租金档位，带"被多少户在用" */
    List<RentPriceTypeVO> listByRegion(String regionCode);

    /** 成组新增档位，合计必须覆盖 1-25 年 */
    void addTypes(RentPriceTypeSaveDTO form);

    /** 修改一个档位。已被引用时拒绝 */
    void editType(RentPriceTypeFormDTO form);

    /** 删除一个档位。已被引用时拒绝 */
    void deleteType(Long id);
}