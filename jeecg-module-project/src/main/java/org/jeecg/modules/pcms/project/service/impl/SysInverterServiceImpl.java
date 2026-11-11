package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.project.entity.SysInverter;
import org.jeecg.modules.pcms.project.entity.SysInverterWire;
import org.jeecg.modules.pcms.project.entity.SysInverterWireMppt;
import org.jeecg.modules.pcms.project.mapper.SysInverterMapper;
import org.jeecg.modules.pcms.project.mapper.SysInverterWireMapper;
import org.jeecg.modules.pcms.project.mapper.SysInverterWireMpptMapper;
import org.jeecg.modules.pcms.project.service.ISysInverterService;
import org.jeecg.modules.pcms.project.vo.SysInverterVO;
import org.jeecg.modules.pcms.project.vo.SysInverterWireMpptVO;
import org.jeecg.modules.pcms.project.vo.SysInverterWireVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/7 下午1:48
 * @Version 1.5
 */
@Service
public class SysInverterServiceImpl extends ServiceImpl<SysInverterMapper, SysInverter> implements ISysInverterService {


    @Autowired
    private SysInverterMapper sysInverterMapper;

    @Autowired
    private SysInverterWireMapper sysInverterWireMapper;

    @Autowired
    private SysInverterWireMpptMapper sysInverterWireMpptMapper;

    @Override
    @Transactional
    public boolean addSysInverterVOS(List<SysInverterVO> sysInverterVOS) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();
        try {
            if (sysInverterVOS != null && sysInverterVOS.size() > 0) {
                for (int i = 0; i < sysInverterVOS.size(); i++) {
                    SysInverterVO sysInverterVO = sysInverterVOS.get(i);
                    SysInverter sysInverter = new SysInverter();
                    sysInverter.setCreateBy(username);
                    sysInverter.setCreateTime(new Date());
                    BeanUtils.copyProperties(sysInverterVO, sysInverter);
                    //返回逆变器的id
                    sysInverterMapper.insert(sysInverter);
                    Integer id = sysInverter.getId();
                    List<SysInverterWireVO> sysInverterWireVOS = sysInverterVO.getSysInverterWireVOS();
                    if (sysInverterWireVOS != null) {
                        for (int j = 0; j < sysInverterWireVOS.size(); j++) {
                            SysInverterWireVO sysInverterWireVO = sysInverterWireVOS.get(j);
                            SysInverterWire sysInverterWire = new SysInverterWire();
                            BeanUtils.copyProperties(sysInverterWireVO, sysInverterWire);
                            //返回接线信息id
                            sysInverterWire.setInverterId(id);
                            sysInverterWireMapper.insert(sysInverterWire);
                            Integer id1 = sysInverterWire.getId();
                            List<SysInverterWireMpptVO> sysInverterWireMpptVOS = sysInverterWireVO.getSysInverterWireMpptVOS();
                            if (sysInverterWireMpptVOS != null && sysInverterWireMpptVOS.size() > 0) {
                                for (int k = 0; k < sysInverterWireMpptVOS.size(); k++) {
                                    SysInverterWireMpptVO sysInverterWireMpptVO = sysInverterWireMpptVOS.get(k);
                                    SysInverterWireMppt sysInverterWireMppt = new SysInverterWireMppt();
                                    BeanUtils.copyProperties(sysInverterWireMpptVO, sysInverterWireMppt);
                                    sysInverterWireMppt.setInverterWireId(id1);
                                    sysInverterWireMpptMapper.insert(sysInverterWireMppt);
                                }
                            }
                        }
                    }
                }

            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<SysInverterVO> getSysInverterVOS(Integer projectId) {
        try {
            QueryWrapper<SysInverter> sysInverterQueryWrapper = new QueryWrapper<>();
            sysInverterQueryWrapper.eq("project_id",projectId);
            List<SysInverter> sysInverters = sysInverterMapper.selectList(sysInverterQueryWrapper);
            List<SysInverterVO> sysInverterVOS = new ArrayList<>();
            for (int i = 0; i < sysInverters.size(); i++) {
                SysInverterVO sysInverterVO = new SysInverterVO();
                SysInverter sysInverter = sysInverters.get(i);
                //作为接线信息的id继续查询
                Integer id = sysInverter.getId();
                QueryWrapper<SysInverterWire> sysInverterWireQueryWrapper = new QueryWrapper<>();
                sysInverterWireQueryWrapper.eq("inverter_id",id);
                List<SysInverterWireVO> sysInverterWireVOS = new ArrayList<>();
                List<SysInverterWire> sysInverterWires = sysInverterWireMapper.selectList(sysInverterWireQueryWrapper);
                for (int j = 0; j < sysInverterWires.size(); j++) {
                    SysInverterWireVO sysInverterWireVO = new SysInverterWireVO();
                    SysInverterWire sysInverterWire = sysInverterWires.get(j);
                    //作为MPPTid继续查询
                    Integer id1 = sysInverterWires.get(j).getId();
                    QueryWrapper<SysInverterWireMppt> sysInverterWireMpptQueryWrapper = new QueryWrapper<>();
                    sysInverterWireMpptQueryWrapper.eq("inverter_wire_id",id1);
                    List<SysInverterWireMppt> sysInverterWireMppts = sysInverterWireMpptMapper.selectList(sysInverterWireMpptQueryWrapper);
                    ArrayList<SysInverterWireMpptVO> sysInverterWireMpptVOS = new ArrayList<>();
                    for (int k = 0; k < sysInverterWireMppts.size(); k++) {
                        SysInverterWireMppt sysInverterWireMppt = sysInverterWireMppts.get(k);
                        SysInverterWireMpptVO sysInverterWireMpptVO = new SysInverterWireMpptVO();
                        BeanUtils.copyProperties(sysInverterWireMppt,sysInverterWireMpptVO);
                        sysInverterWireMpptVOS.add(sysInverterWireMpptVO);
                    }
                    BeanUtils.copyProperties(sysInverterWire,sysInverterWireVO);
                    sysInverterWireVO.setSysInverterWireMpptVOS(sysInverterWireMpptVOS);
                    sysInverterWireVOS.add(sysInverterWireVO);
                }
                BeanUtils.copyProperties(sysInverter,sysInverterVO);
                sysInverterVO.setSysInverterWireVOS(sysInverterWireVOS);
                sysInverterVOS.add(sysInverterVO);
            }
            return sysInverterVOS;
        }catch (Exception e){
            return new ArrayList<>();
        }
    }


    @Override
    public boolean deleteByProjectId(Integer projectId) {
        sysInverterMapper.deleteByProjectId(projectId);
        return true;
    }
}
