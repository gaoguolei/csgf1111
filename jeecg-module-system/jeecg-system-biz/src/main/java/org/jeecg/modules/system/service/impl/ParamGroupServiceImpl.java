package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.mapper.ParamGroupMapper;
import org.jeecg.modules.system.mapper.ParamItemMapper;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.model.ParamGroup;
import org.jeecg.modules.system.model.ParamItem;
import org.jeecg.modules.system.service.IParamGroupService;
import org.jeecg.modules.system.vo.ParamGroupItemsVO;
import org.jeecg.modules.system.vo.ParamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author gongzhou
 * @Date 2023/3/6 上午10:50
 * @Version 1.5
 */
@Service
public class ParamGroupServiceImpl extends ServiceImpl<ParamGroupMapper, ParamGroup> implements IParamGroupService {

    @Autowired
    private ParamGroupMapper paramGroupMapper;

    @Autowired
    private ParamItemMapper paramItemMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public Result add(ParamGroup paramGroup) {

//        Integer tenantId = paramGroup.getTenantId();
        // 查看租户是否存在
//        ParamGroup category = categoryManager.getModel(paramGroup.getCategoryId());
//        if (category == null) {
//        }
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = sysUser.getUsername();
        String orgCode = sysUser.getOrgCode();
        String relTenantIds = sysUser.getRelTenantIds();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date createTime = null;
        try {
            createTime = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        paramGroup.setCreateTime(createTime);
        if (StringUtils.isNotBlank(username)) {
            paramGroup.setCreateBy(username);
        }

        //租户id
        if (StringUtils.isNotBlank(relTenantIds)) {
            paramGroup.setTenantId(Integer.valueOf(relTenantIds));
        }

        if (StringUtils.isNotBlank(orgCode)) {
            paramGroup.setSysOrgCode(orgCode);
        }

        ParamGroup paramGroupByProjectId = paramGroupMapper.getParamGroupByProjectId(Integer.valueOf(paramGroup.getAreaProjectId()));
        if (paramGroupByProjectId == null) {
            paramGroup.setSortNo(1);
        } else {
            paramGroup.setSortNo(paramGroupByProjectId.getSortNo() + 1);
        }

        int insert = paramGroupMapper.insert(paramGroup);
        if (insert > 0) {
            return Result.ok();
        } else {
            return Result.error("500", "添加错误");
        }
    }

    @Override
    public Result edit(ParamGroup paramGroup) {
        Integer id = paramGroup.getId();
        String groupName = paramGroup.getGroupName();
        ParamGroup group = paramGroupMapper.selectById(id);
        if (group == null) {
            return Result.error(500, "参数组不存在！");
        }
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = sysUser.getUsername();
        if (StringUtils.isNotBlank(groupName)) {
            paramGroup.setGroupName(groupName);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date updateTime = null;
        try {
            updateTime = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        paramGroup.setUpdateBy(username);
        paramGroup.setUpdateTime(updateTime);
        return Result.ok(paramGroupMapper.updateById(paramGroup));
    }

    @Transactional
    @Override
    public Result delete(Integer id) {
        //此位置需要删除属于该参数组的参数项
        paramItemMapper.deleteByGroupId(id);
        int i = paramGroupMapper.deleteById(id);
        if(i>0){
            return Result.ok(i);
        }else {
            return Result.error(500,"删除失败");
        }
    }

    @Override
    public Result getParamGroup(Integer id) {
        ParamGroup group = paramGroupMapper.selectById(id);
        if (group != null) {
            return Result.ok(group);
        } else {
            return Result.error(500, "未找到参数组");
        }
    }

    @Override
    public Result params(Integer projectId) {
        QueryWrapper<ParamGroup> sysTenantQueryWrapper = new QueryWrapper<>();
        sysTenantQueryWrapper.eq("area_project_id",projectId);
        List<ParamGroup> paramGroups = paramGroupMapper.selectList(sysTenantQueryWrapper);

        List<ParamItem> itemList = paramGroupMapper.getItemList();
        List<ParamGroupItemsVO> paramGroupItemsVOS = this.convertParamList(paramGroups, itemList);
        if (!paramGroupItemsVOS.isEmpty()) {
            return Result.ok(paramGroupItemsVOS);
        } else {
            return Result.ok(new ArrayList<>());
        }
    }

    @Override
    public Result groupSort(ParamVO paramVO) {
        Integer id = paramVO.getId();
        String sortType = paramVO.getSortType();
        ParamGroup paramGroup = paramGroupMapper.selectById(id);
        if (paramGroup == null) {
            return Result.error(500, "未找到数据");
        }
        Integer sortNo = paramGroup.getSortNo();
        Integer areaProjectId = paramGroup.getAreaProjectId();
        try {
            if ("up".equals(sortType)) {
                ParamGroup projectParam = paramGroupMapper.getParamItemBySortNoUp(sortNo, areaProjectId);
                if (projectParam != null) {
                    paramGroupMapper.updateSort(projectParam.getSortNo(), paramGroup.getId());
                    paramGroupMapper.updateSort(paramGroup.getSortNo(), projectParam.getId());
                }
            } else if ("down".equals(sortType)) {
                ParamGroup projectParam = paramGroupMapper.getParamItemBySortNoDown(sortNo, areaProjectId);
                if (projectParam != null) {
                    paramGroupMapper.updateSort(projectParam.getSortNo(), paramGroup.getId());
                    paramGroupMapper.updateSort(paramGroup.getSortNo(), projectParam.getId());
                }
            }
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
        return Result.ok();
    }

    @Override
    public Result getParams(Integer groupId) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String id = loginUser.getId();
        SysUser sysUser = sysUserMapper.selectById(id);
        String relTenantIds = sysUser.getRelTenantIds();
        QueryWrapper<ParamGroup> sysTenantQueryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(relTenantIds)){
            sysTenantQueryWrapper.eq("area_project_id",relTenantIds);
        }
        sysTenantQueryWrapper.eq("id",groupId);
        List<ParamGroup> paramGroups = paramGroupMapper.selectList(sysTenantQueryWrapper);

        List<ParamItem> itemList = paramGroupMapper.getItemList();
        List<ParamGroupItemsVO> paramGroupItemsVOS = this.convertParamList(paramGroups, itemList);
        if (!paramGroupItemsVOS.isEmpty()) {
            return Result.ok(paramGroupItemsVOS);
        } else {
            return Result.ok(new ArrayList<>());
        }
    }

    @Override
    public Result getGroup(Integer id) {
        List<ParamGroup> group = paramGroupMapper.getGroup(id);
        if(group != null ){
            return Result.ok(group);
        }
        else {
            return Result.ok("未查询到数据");
        }
    }

    private List<ParamGroupItemsVO> convertParamList(List<ParamGroup> groupList, List<ParamItem> paramList) {
        Map<Integer, List<ParamItem>> map = new HashMap<>(paramList.size());
        for (ParamItem param : paramList) {

            List<ParamItem> list = map.get(param.getParamGroupId());
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(param);
            map.put(param.getParamGroupId(), list);
        }

        List<ParamGroupItemsVO> resList = new ArrayList<>();
        for (ParamGroup group : groupList) {
            ParamGroupItemsVO groupVo = new ParamGroupItemsVO();
            groupVo.setId(group.getId());
            groupVo.setGroupName(group.getGroupName());
            groupVo.setParams(map.get(group.getId()) == null ? new ArrayList<>() : map.get(group.getId()));
            resList.add(groupVo);
        }
        return resList;
    }
}
