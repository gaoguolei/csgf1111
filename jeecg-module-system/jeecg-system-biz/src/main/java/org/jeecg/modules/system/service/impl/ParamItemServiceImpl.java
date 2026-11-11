package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.mapper.ParamGroupMapper;
import org.jeecg.modules.system.mapper.ParamItemMapper;
import org.jeecg.modules.system.model.ParamGroup;
import org.jeecg.modules.system.model.ParamItem;
import org.jeecg.modules.system.service.IParamItemService;
import org.jeecg.modules.system.vo.ParamVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/3/6 下午4:17
 * @Version 1.5
 */
@Service
public class ParamItemServiceImpl extends ServiceImpl<ParamItemMapper, ParamItem> implements IParamItemService {
    @Autowired
    private ParamItemMapper paramItemMapper;
    @Autowired
    private ParamGroupMapper paramGroupMapper;

    @Override
    public Result add(ParamItem paramItem) {
        // 查询参数组
        ParamGroup paramGroup = paramGroupMapper.getParamGroupById(Integer.valueOf(paramItem.getParamGroupId()));
        if (paramGroup == null) {
            return Result.error(500, "所属参数组不存在");
        }
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
        paramItem.setCreateTime(createTime);
        if (StringUtils.isNotBlank(username)) {
            paramItem.setCreateBy(username);
        }
        //租户id
        if (StringUtils.isNotBlank(relTenantIds)) {
            paramItem.setTenantId(Integer.valueOf(relTenantIds));
        }
        if (StringUtils.isNotBlank(orgCode)) {
            paramItem.setSysOrgCode(orgCode);
        }
        paramItem.setTenantId(paramItem.getTenantId());
        // 选择项
//        if(paramItem.getWidgetType().equals("2") 2){
//            if(!StringUtil.notEmpty(parameters.getOptions()) ){
//                throw new ServiceException(GoodsErrorCode.E303.code(), "选择项类型必须填写选择内容");
//            }
//        }
        ParamItem paramtmp = paramItemMapper.getParamGroupByGroupId(Integer.valueOf(paramItem.getParamGroupId()));
        if (paramtmp == null) {
            paramItem.setSortNo(1);
        } else {
            paramItem.setSortNo(paramtmp.getSortNo() + 1);
        }
        int insert = paramItemMapper.insert(paramItem);
        if (insert > 0) {
            return Result.ok();
        } else {
            return Result.error(500, "添加失败");
        }
    }

    @Override
    public Result edit(ParamItem paramItem) {
        Integer id = paramItem.getId();
        ParamItem parameters = paramItemMapper.selectById(id);
        if (parameters == null) {
            return Result.error(500, "参数不存在");
        }
        parameters.setTenantId(paramItem.getTenantId());
        // 选择项
//        if(parameters.getParamType() == 2){
//            if(!StringUtil.notEmpty(parameters.getOptions()) ){
//                throw new ServiceException(GoodsErrorCode.E303.code(), "选择项类型必须填写选择内容");
//            }
//        }
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = sysUser.getUsername();
        String orgCode = sysUser.getOrgCode();
        String relTenantIds = sysUser.getRelTenantIds();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date updateTime = null;
        try {
            updateTime = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        paramItem.setUpdateTime(updateTime);
        if (StringUtils.isNotBlank(username)) {
            paramItem.setUpdateBy(username);
        }
        //租户id
        if (StringUtils.isNotBlank(relTenantIds)) {
            paramItem.setTenantId(Integer.valueOf(relTenantIds));
        }
        if (StringUtils.isNotBlank(orgCode)) {
            paramItem.setSysOrgCode(orgCode);
        }

        BeanUtils.copyProperties(paramItem, parameters);

        int i = paramItemMapper.updateById(parameters);
        if (i > 0) {
            return Result.ok();
        } else {
            return Result.error(500, "修改失败");
        }

    }

    @Override
    public Result delete(Integer id) {
        int i = paramItemMapper.deleteById(id);
        if (i > 0) {
            return Result.ok();
        } else {
            return Result.error(500, "删除失败");
        }
    }

    @Override
    public Result getParamItem(Integer id) {
        ParamItem paramItem = paramItemMapper.selectById(id);
        if (paramItem != null) {
            return Result.ok(paramItem);
        } else {
            return Result.error(500, "未找到参数项");
        }
    }

    @Override
    public Result paramSort(ParamVO paramVO) {
        Integer id = paramVO.getId();
        String sortType = paramVO.getSortType();
        ParamItem paramItem = paramItemMapper.selectById(id);
        Integer sortNo = paramItem.getSortNo();
        Integer paramGroupId = paramItem.getParamGroupId();
        if (paramItem == null) {
            return Result.error(500, "要移动的参数不存在");
        }
        try {
            if ("up".equals(sortType)) {
                ParamItem paramItemBySortNoUp = paramItemMapper.getParamItemBySortNoUp(sortNo, paramGroupId);
                if (paramItemBySortNoUp != null) {
                    paramItemMapper.updateSort(paramItemBySortNoUp.getSortNo(), paramItem.getId());
                    paramItemMapper.updateSort(paramItem.getSortNo(), paramItemBySortNoUp.getId());
                }
            } else if ("down".equals(sortType)) {
                ParamItem paramItemBySortNoDown = paramItemMapper.getParamItemBySortNoDown(sortNo, paramGroupId);
                if (paramItemBySortNoDown != null) {
                    paramItemMapper.updateSort(paramItemBySortNoDown.getSortNo(), paramItem.getId());
                    paramItemMapper.updateSort(paramItem.getSortNo(), paramItemBySortNoDown.getId());
                }
            }
        }catch (Exception e){
            return Result.error(500,e.getMessage());
        }
        return Result.ok();
    }
}
