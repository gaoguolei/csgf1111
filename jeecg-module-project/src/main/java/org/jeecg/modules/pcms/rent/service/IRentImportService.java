package org.jeecg.modules.pcms.rent.service;

import org.jeecg.modules.pcms.rent.dto.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金台账导入。五个项目共用，靠 regionCode 选择对应的 Parser。
 *
 * @Version: V1.0
 */
public interface IRentImportService {

    /**
     * @param file       上传的 Excel，.xls / .xlsx 均可
     * @param regionCode BJ / TJLF / XZ / HAIN / HENAN
     */
    ImportResultVO importExcel(MultipartFile file, String regionCode);
}
