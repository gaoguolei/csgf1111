package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.pcms.project.entity.SysAssembly;
import org.jeecg.modules.pcms.project.vo.SysAssemblyVO;
import org.jeecg.modules.pcms.project.vo.SysInverterVO;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/8 上午8:10
 * @Version 1.5
 */
public interface ISysAssemblyService extends IService<SysAssembly> {

    boolean addSysInverterVOS(@Param("sysInverterVOS") List<SysAssemblyVO> sysAssemblyVOS);


    List<SysAssemblyVO> getSysInverterVOS(@Param("projectId") Integer projectId);

    boolean deleteByProjectId(Integer projectId);
}
