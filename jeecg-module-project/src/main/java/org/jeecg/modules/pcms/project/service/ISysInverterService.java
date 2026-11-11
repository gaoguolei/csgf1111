package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.pcms.project.entity.SysInverter;
import org.jeecg.modules.pcms.project.vo.SysInverterVO;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/7 下午1:42
 * @Version 1.5
 */
public interface ISysInverterService  extends IService<SysInverter> {


    boolean addSysInverterVOS(@Param("sysInverterVOS") List<SysInverterVO> sysInverterVOS);

    List<SysInverterVO> getSysInverterVOS(@Param("projectId") Integer projectId);


    boolean deleteByProjectId(@Param("projectId")Integer projectId);
}
