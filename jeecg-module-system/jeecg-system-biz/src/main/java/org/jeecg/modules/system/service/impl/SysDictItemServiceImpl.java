package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.SysDict;
import org.jeecg.modules.system.entity.SysDictItem;
import org.jeecg.modules.system.mapper.SysDictItemMapper;
import org.jeecg.modules.system.mapper.SysDictMapper;
import org.jeecg.modules.system.service.ISysDictItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @Author zhangweijian
 * @since 2018-12-28
 */
@Service
public class SysDictItemServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements ISysDictItemService {

    @Autowired
    private SysDictItemMapper sysDictItemMapper;
    @Autowired
    private SysDictMapper sysDictMapper;

    @Override
    public List<SysDictItem> selectItemsByMainId(String mainId) {
        return sysDictItemMapper.selectItemsByMainId(mainId);
    }

    @Override
    public Result getProjectStatus(Integer tenantId) {
        //北京项目返回北京字典
        if(tenantId== -2103508991){
            QueryWrapper<SysDict> sysDictQueryWrapper = new QueryWrapper<>();
            sysDictQueryWrapper.eq("dict_code","dict_project_beij_bpm");
            SysDict sysDict = sysDictMapper.selectOne(sysDictQueryWrapper);
            String id = sysDict.getId();
            QueryWrapper<SysDictItem> sysDictItemQueryWrapper = new QueryWrapper<>();
            sysDictItemQueryWrapper.eq("dict_id",id);
            List<SysDictItem> sysDictItems = sysDictItemMapper.selectList(sysDictItemQueryWrapper);
            return Result.ok(sysDictItems);
        }else {
            QueryWrapper<SysDictItem> sysDictItemQueryWrapper = new QueryWrapper<>();
            sysDictItemQueryWrapper.eq("dict_id","1632647956767432706");
            List<SysDictItem> sysDictItems = sysDictItemMapper.selectList(sysDictItemQueryWrapper);
            return Result.ok(sysDictItems);
        }

    }
}
