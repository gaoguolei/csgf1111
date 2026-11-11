package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.checkerframework.checker.units.qual.A;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.project.entity.SysAssembly;
import org.jeecg.modules.pcms.project.mapper.SysAssemblyMapper;
import org.jeecg.modules.pcms.project.service.ISysAssemblyService;
import org.jeecg.modules.pcms.project.vo.SysAssemblyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/8 上午8:10
 * @Version 1.5
 */
@Service
public class SysAssemblyServiceImpl extends ServiceImpl<SysAssemblyMapper, SysAssembly> implements ISysAssemblyService {

    @Autowired
    private SysAssemblyMapper sysAssemblyMapper;

    @Override
    public boolean addSysInverterVOS(List<SysAssemblyVO> sysAssemblyVOS) {
        try {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String username = loginUser.getUsername();
            if(sysAssemblyVOS != null && sysAssemblyVOS.size()>0){
                for (int i = 0; i < sysAssemblyVOS.size(); i++) {
                    SysAssemblyVO sysAssemblyVO = sysAssemblyVOS.get(i);
                    SysAssembly sysAssembly = new SysAssembly();
                    BeanUtils.copyProperties(sysAssemblyVO,sysAssembly);
                    sysAssembly.setCreateTime(new Date());
                    sysAssembly.setCreateBy(username);
                    sysAssemblyMapper.insert(sysAssembly);
                }

            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public List<SysAssemblyVO> getSysInverterVOS(Integer projectId) {
        try {
            QueryWrapper<SysAssembly> objectQueryWrapper = new QueryWrapper<>();
            objectQueryWrapper.eq("project_id",projectId);
            List<SysAssembly> sysAssemblies = sysAssemblyMapper.selectList(objectQueryWrapper);
            ArrayList<SysAssemblyVO> sysAssemblyVOS = new ArrayList<>();
            for (int i = 0; i < sysAssemblies.size(); i++) {
                SysAssembly sysAssembly = sysAssemblies.get(i);
                SysAssemblyVO sysAssemblyVO = new SysAssemblyVO();
                BeanUtils.copyProperties(sysAssembly,sysAssemblyVO);
                sysAssemblyVOS.add(sysAssemblyVO);
            }
            return sysAssemblyVOS;
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByProjectId(Integer projectId) {
        return  sysAssemblyMapper.deleteByProjectId(projectId);
    }
}
