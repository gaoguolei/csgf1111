package org.jeecg.modules.pcms.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.entity.Regions;
import org.jeecg.modules.pcms.project.mapper.RegionsMapperOne;
import org.jeecg.modules.pcms.project.service.IRegionsServiceOne;
import org.jeecg.modules.pcms.project.vo.RegionsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gongzhou
 * @Date 2023/3/16 下午5:12
 * @Version 1.5
 */
@Service
public class RegionsServiceImplOne extends ServiceImpl<RegionsMapperOne, Regions> implements IRegionsServiceOne {

    @Autowired
    private RegionsMapperOne regionsMapper;


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
                return new Result(500,"当前地区父地区id无效");
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
        if(i>0){
            return Result.ok();
        }else {
            return Result.error(500,"修改失败");
        }

    }

    @Override
    public Result delete(Integer id) {
        Regions regions = this.getModel(id);
        if (regions == null) {
            Result.error(500,"该地区不存在");
        }
        Integer integer = regionsMapper.deleteAndPath(id);
        //清除缓存
        if(integer>0){
            return Result.ok();
        }else {
            return Result.error(500,"修改失败");
        }
    }

    @Override
    public Regions getModel(Integer id) {
        return this.regionsMapper.selectById(id);
    }

    @Override
    public Result getRegionsChildren(Integer regionId) {
        Regions region = this.getModel(regionId);
        if (region == null && regionId != 0) {
            Result.error("500","该地区不存在");
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
            columnMap.put("region_grade",i);
            List<Regions> rgs = regionsMapper.selectByMap(columnMap);
            if (i == depth) {
                regions = rgs;
            }
        }
        List<Regions> rgs = new ArrayList<>();
        if(regions!=null){
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
        return Result.ok(regionsMapper.deleteById(id))  ;
    }

    @Override
    public String getNameByCode(String orgCode) {
        return regionsMapper.getNameByCode(orgCode);
    }

    @Override
    public String getProjectName(String projectName) {
        String name = regionsMapper.getProjectName(projectName);
        if(StringUtils.isBlank(name)){
            return "QG";
        }
        else {
            return regionsMapper.getProjectName(projectName);
        }
    }

    @Override
    public List<String> getRoleIds(String userId) {
        return regionsMapper.getRoleIds(userId);
    }

    @Override
    public String getRoleId(String userId) {
        return regionsMapper.getRoleId(userId);
    }

    @Override
    public String getImgUrl(Long imgId) {
        return regionsMapper.getImgUrl(imgId);
    }

    @Override
    public List<Integer> getRegionsIds(String id) {
        return regionsMapper.getRegionsIds(id);
    }

    @Override
    public List<Integer> getRegionsIdsXZ(String id) {
        return regionsMapper.getRegionsIdsXZ(id);
    }
}
