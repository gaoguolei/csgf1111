package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.bpm.BpmProjectBeiJNodeGenerator;
import org.jeecg.modules.pcms.bpm.BpmProjectNodeGenerator;
import org.jeecg.modules.pcms.common.DicProjectBeijBpmEnum;
import org.jeecg.modules.pcms.common.DictProjectBpmEnum;
import org.jeecg.modules.pcms.common.DictYesNoEnum;
import org.jeecg.modules.pcms.project.entity.PvProjectBpm;
import org.jeecg.modules.pcms.project.mapper.PvProjectBpmMapper;
import org.jeecg.modules.pcms.project.service.IPvProjectBpmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description: pv_project_bpm
 * @Author: jun
 * @Date: 2023-03-07
 * @Version: V1.0
 */
@Service
public class PvProjectBpmServiceImpl extends ServiceImpl<PvProjectBpmMapper, PvProjectBpm> implements IPvProjectBpmService {

    @Autowired
    private PvProjectBpmMapper pvProjectBpmMapper;

    @Override
    public List<PvProjectBpm> selectByMainId(Integer mainId) {
        return pvProjectBpmMapper.selectByMainId(mainId);
    }

    @Override
    public List<PvProjectBpm> selectByProjectId(Integer projectId,String projectStatus){
        QueryWrapper<PvProjectBpm> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id",projectId);
        queryWrapper.eq("bpm_node_id", BpmProjectNodeGenerator.getProjectBpmNode(projectStatus).getCode());
        queryWrapper.orderByDesc("id");
        queryWrapper.last("limit 1");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<PvProjectBpm> selectByProjectIdBeiJ(Integer projectId, String projectStatus) {
        if (StringUtils.isNotBlank(projectStatus)) {
            DicProjectBeijBpmEnum projectBpmNode = BpmProjectBeiJNodeGenerator.getProjectBpmNode(projectStatus);
            if (projectBpmNode != null) {
                QueryWrapper<PvProjectBpm> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("project_id",projectId);
                queryWrapper.eq("bpm_node_id", projectBpmNode.getCode());
                queryWrapper.orderByDesc("id");
                queryWrapper.last("limit 1");
                return baseMapper.selectList(queryWrapper);
            }
        }
        List<PvProjectBpm> pvProjectBpms = new ArrayList<>();
        return pvProjectBpms;
    }

    @Override
    public PvProjectBpm selectByProjectBpm(PvProjectBpm pvProjectBpm) {
        return pvProjectBpmMapper.selectByProjectBpm(pvProjectBpm);
    }

    @Override
    public PvProjectBpm startProjectBpm(Integer projectId) {
        PvProjectBpm pvProjectBpm = new PvProjectBpm();
        pvProjectBpm.setProjectId(projectId);
        pvProjectBpm.setBpmNodeId(DictProjectBpmEnum.DATA_ENTRY.getCode());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
        baseMapper.insert(pvProjectBpm);
        return pvProjectBpm;
    }

    @Override
    public PvProjectBpm startProjectBpmBeiJ(Integer projectId) {
        PvProjectBpm pvProjectBpm = new PvProjectBpm();
        pvProjectBpm.setProjectId(projectId);
        pvProjectBpm.setBpmNodeId(DicProjectBeijBpmEnum.DATA_ENTRY.getCode());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
        baseMapper.insert(pvProjectBpm);
        return pvProjectBpm;
    }

    @Override
    public int insertProjectBpm(Integer projectId, DictProjectBpmEnum dictProjectBpmEnum) {
        PvProjectBpm pvProjectBpm = new PvProjectBpm();
        pvProjectBpm.setProjectId(projectId);
        pvProjectBpm.setBpmNodeId(dictProjectBpmEnum.getCode());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
        return baseMapper.insert(pvProjectBpm);
    }

    @Override
    public int insertProjectBpmBeiJ(Integer projectId, DicProjectBeijBpmEnum dictProjectBpmEnum) {
        PvProjectBpm pvProjectBpm = new PvProjectBpm();
        pvProjectBpm.setProjectId(projectId);
        pvProjectBpm.setBpmNodeId(dictProjectBpmEnum.getCode());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.NO.getCode());
        return baseMapper.insert(pvProjectBpm);
    }

    @Override
    public int commit(Integer projectId, DictProjectBpmEnum dictProjectBpmEnum) {
        QueryWrapper<PvProjectBpm> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id",projectId);
        queryWrapper.eq("bpm_node_id",dictProjectBpmEnum.getCode());
//        queryWrapper.eq("is_committed", DictYesNoEnum.NO.getCode());
        PvProjectBpm pvProjectBpm = baseMapper.selectOne(queryWrapper);
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectBpm.setApprover(loginUser.getUsername());
        pvProjectBpm.setApprovalTime(new Date());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.YES.getCode());
        baseMapper.updateById(pvProjectBpm);
        return 0;
    }

    @Override
    public int commitBeiJ(Integer projectId, DicProjectBeijBpmEnum dictProjectBpmEnum) {
        QueryWrapper<PvProjectBpm> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id",projectId);
        queryWrapper.eq("bpm_node_id",dictProjectBpmEnum.getCode());
//        queryWrapper.eq("is_committed", DictYesNoEnum.NO.getCode());
        PvProjectBpm pvProjectBpm = baseMapper.selectOne(queryWrapper);
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        pvProjectBpm.setApprover(loginUser.getUsername());
        pvProjectBpm.setApprovalTime(new Date());
        pvProjectBpm.setIsCommitted(DictYesNoEnum.YES.getCode());
        baseMapper.updateById(pvProjectBpm);
        return 0;
    }
}
