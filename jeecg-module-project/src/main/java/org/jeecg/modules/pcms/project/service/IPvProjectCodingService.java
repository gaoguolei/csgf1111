package org.jeecg.modules.pcms.project.service;

import org.jeecg.modules.pcms.project.entity.PvProjectCoding;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: pv_project_coding
 * @Author: jun
 * @Date:   2023-03-10
 * @Version: V1.0
 */
public interface IPvProjectCodingService extends IService<PvProjectCoding> {

    /**
     * 生成客户项目编号
     * @param areaAddress
     * @return
     */
    public String generateProjectCode(String areaAddress,String projectName);

}
