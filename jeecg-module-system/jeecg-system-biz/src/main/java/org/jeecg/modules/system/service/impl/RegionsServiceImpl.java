package org.jeecg.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.SysTenant;
import org.jeecg.modules.system.mapper.RegionsMapper;
import org.jeecg.modules.system.mapper.SysTenantMapper;
import org.jeecg.modules.system.mapper.SysUserRoleMapper;
import org.jeecg.modules.system.model.Regions;
import org.jeecg.modules.system.service.IRegionsService;
import org.jeecg.modules.system.vo.ParamVO;
import org.jeecg.modules.system.vo.RegionsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gongzhou
 * @Date 2023/3/5 上午9:39
 * @Version 1.5
 */
@Service
public class RegionsServiceImpl extends ServiceImpl<RegionsMapper, Regions> implements IRegionsService {

    @Autowired
    private RegionsMapper regionsMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysTenantMapper sysTenantMapper;


    @Override
    public Result add(RegionsVO regionsVO) {
        Regions regions = new Regions();
        BeanUtil.copyProperties(regionsVO, regions);
        regionsMapper.insert(regions);
        Integer regionId = regionsMapper.getLastId();
        String regionPath = "";
        regions = getModel(regionId);
        if (regions.getParentId() != null && regions.getParentId() != 0) {
            Regions p = getModel(regions.getParentId());
            if (p == null) {
                return new Result(500, "当前地区父地区id无效");
            }
            regionPath = p.getRegionPath() + regionId + ",";
        } else {
            regionPath = "," + regionId + ",";
        }
        //对地区级别进行处理
        String subreg = regionPath.substring(0, regionPath.length() - 1);
        subreg = subreg.substring(1);
        String[] regs = subreg.split(",");
        regions.setRegionGrade(regs.length);
        regions.setRegionPath(regionPath);
        //修改地区
        return this.edit(regions, regionId);
    }

    @Override
    public Result edit(Regions regions, Integer id) {
        Regions parentRegion = this.getModel(regions.getParentId());
        if (!regions.getParentId().equals(0) && parentRegion == null) {
//            return Result.error(500,"当前地区父地区id无效");
        }
        //修改地区信息
        regions.setId(id);
        int i = regionsMapper.updateById(regions);
        //清除地区缓存
        if (i > 0) {
            return Result.ok();
        } else {
            return Result.error(500, "修改失败");
        }

    }

    @Override
    public Result delete(Integer id) {
        Regions regions = this.getModel(id);
        if (regions == null) {
            return Result.error(500, "该地区不存在");
        }
        //需要判断该地区是否在使用
        Integer count = regionsMapper.selectUseRegions(id);
        Integer tenant = regionsMapper.selectUseRegionsTenant(id);
        if (count > 0 || tenant > 0) {
            return Result.error(500, "该地区正在被使用，不可删除！");
        }
        Integer integer = regionsMapper.deleteAndPath(id);
        //清除缓存
        if (integer > 0) {
            return Result.ok();
        } else {
            return Result.error(500, "修改失败");
        }
    }

    @Override
    public Regions getModel(Integer id) {
        return this.regionsMapper.selectById(id);
    }

    @Override
    public Result getRegionsChildren(ParamVO paramVO, Integer regionId) {
//        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
//
//        String userId = sysUser.getId();
//        QueryWrapper<SysUserRole> userRoleQueryWrapper = new QueryWrapper<>();
//        userRoleQueryWrapper.eq("user_id", userId);
//        SysUserRole only = sysUserRoleMapper.selectOne(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, userId));
//        String roleId = only.getRoleId();

//        String relTenantIds = sysUser.getRelTenantIds();
        String renterId = paramVO.getRenterId();

//        if (StringUtils.isNotBlank(relTenantIds)) {
        //通过当前项目
        SysTenant sysTenant = sysTenantMapper.selectById(renterId);
        String areaAddress = sysTenant.getAreaAddress();
        areaAddress = areaAddress.substring(1, areaAddress.length());
        String[] regionPathArray = areaAddress.split(",");
        String parentId = regionPathArray[0];
//        Regions region = this.getModel(Integer.valueOf(parentId));
        Regions region = this.getModel(regionId);
        if (region == null) {
            Result.error("500", "该地区不存在");
        }
        Integer depth = 0;
        if (!regionId.equals(0)) {
            depth = region.getRegionGrade();
        }
        //因为需要查找下一级，所以需要将深度+1
        depth = depth + 1;
        List<Regions> regions = null;
        // 如果为空的话需要重数据库中查出数据 然后放入缓存
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> columnMap = new HashMap<String, Object>();
            columnMap.put("region_grade", i);
            List<Regions> rgs = regionsMapper.selectByMap(columnMap);
//            if (sysUser.getRelTenantIds().equals("102")) {
            rgs.stream().forEach(j -> {
                String regionPath = j.getRegionPath();
                String substring = regionPath.substring(1, regionPath.length() - 1);
                String[] split = substring.replace("，", ",").split(",");
                String first = split[0]; // 获取最后一个元素
                j.setTempId(first);
            });
            List<Regions> collect = rgs.stream().filter(k -> k.getTempId().equals(parentId)).collect(Collectors.toList());
            if (i == depth) {
                regions = collect;
            }
        }
        List<Regions> rgs = new ArrayList<>();
        if (regions != null) {
            for (int i = 0; i < regions.size(); i++) {
                if (regions.get(i).getParentId().equals(regionId)) {
                    rgs.add(regions.get(i));
                }
            }
        }

        return Result.ok(rgs);
    }


    @Override
    public Result getRegions() {
        //封装数据
        QueryWrapper<Regions> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("parent_id", 0);
        queryWrapper.eq("local_name", "天津");
        List<Regions> one = regionsMapper.selectList(queryWrapper);
        List<Regions> collect = one.stream().filter(t -> t.getParentId() == 0).map(
                m -> {
                    m.setChildList(getChildren(m, one));
                    return m;
                }
        ).collect(Collectors.toList());
        return Result.ok(collect);
    }


    public static List<Regions> getChildren(Regions root, List<Regions> all) {
        List<Regions> children = all.stream().filter(t -> {
            return Objects.equals(t.getParentId(), root.getId());
        }).map(
                m -> {
                    m.setChildList(getChildren(m, all));
                    return m;
                }
        ).collect(Collectors.toList());
        return children;
    }


    @Override
    public Result deleteTest(Integer id) {
        return Result.ok(regionsMapper.deleteById(id));
    }

    @Override
    public Result getRegionsByTenantId(Integer regionId) {
        Regions region = this.getModel(regionId);
        if (region == null && regionId != 0) {
            Result.error("500", "该地区不存在");
        }
        Integer depth = 0;
        if (!regionId.equals(0)) {
            depth = region.getRegionGrade();
        }
        //因为需要查找下一级，所以需要将深度+1
        depth = depth + 1;
        List<Regions> regions = null;
        // 如果为空的话需要重数据库中查出数据 然后放入缓存
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> columnMap = new HashMap<String, Object>();
            columnMap.put("region_grade", i);
            List<Regions> rgs = regionsMapper.selectByMap(columnMap);
            if (i == depth) {
                regions = rgs;
            }
        }
        List<Regions> rgs = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getParentId().equals(regionId)) {
                rgs.add(regions.get(i));
            }
        }
        return Result.ok(rgs);
    }

    @Override
    public Result getRegionsByRenterId(String renterId) {
        if (StringUtils.isNotBlank(renterId)) {
            SysTenant sysTenant = sysTenantMapper.selectById(renterId);
            String areaAddress = sysTenant.getAreaAddress();
            areaAddress = areaAddress.substring(1, areaAddress.length());
            String[] regionPathArray = areaAddress.split(",");
            String id = regionPathArray[0];
            Regions oneById = this.getOneById(Long.valueOf(id));
            List<Regions> regions = new ArrayList<>();
            regions.add(oneById);
            return Result.ok(regions);
        } else {
            //封装数据
            QueryWrapper<Regions> queryWrapper = new QueryWrapper<>();
            List<Regions> one = regionsMapper.selectList(queryWrapper);
            List<Regions> collect = one.stream().filter(t -> t.getParentId() == 0).map(
                    m -> {
                        m.setChildList(getChildren(m, one));
                        return m;
                    }
            ).collect(Collectors.toList());
            return Result.ok(collect);
        }
    }

    public Regions getOneById(Long id) {
        Regions dict = regionsMapper.selectById(id);
        if (dict != null) {
            Integer parentId = dict.getId();
            QueryWrapper<Regions> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parentId);
            List<Regions> dictList = regionsMapper.selectList(queryWrapper);
            //儿子塞进去
            dict.setChildList(dictList);
            //递归
            getTree(dictList);
        }
        return dict;
    }

    private void getTree(List<Regions> dictList) {
        List<Regions> retList = new ArrayList<>();
        for (Regions dict : dictList) {
            QueryWrapper<Regions> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", dict.getId());
            retList = regionsMapper.selectList(queryWrapper);
            //size>0有儿子
            if (retList.size() > 0) {
                //儿子塞进去
                dict.setChildList(retList);
                getTree(retList);
            }
        }
    }

}
