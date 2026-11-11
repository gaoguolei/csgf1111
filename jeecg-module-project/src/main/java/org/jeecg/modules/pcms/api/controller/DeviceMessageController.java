package org.jeecg.modules.pcms.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.energy.entity.DeviceMessage;
import org.jeecg.modules.pcms.energy.vo.DevicePageVo;
import org.jeecg.modules.pcms.project.service.IDeviceMessageService;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "操作消息模块")
@RestController
@RequestMapping(value = "project/deviceMessage")
public class DeviceMessageController {

    @Autowired
    private IDeviceMessageService iDeviceMessageService;


    @Autowired
    private ISysUserService iSysUserService;


    @GetMapping(value = "/pageList")
    public Result<?> queryPageList(DevicePageVo devicePageVo) {
        QueryWrapper<DeviceMessage> dispatchQueryWrapper = new QueryWrapper<>();

        //按照创建时间倒序
        dispatchQueryWrapper.orderByDesc("create_time");
        // 动态分页参数
        IPage<DeviceMessage> pageData = new Page<>(
                devicePageVo.getPageNo(),
                devicePageVo.getPageSize()
        );
        IPage<DeviceMessage> page = iDeviceMessageService.page(pageData, dispatchQueryWrapper);

        if(page != null ){
            List<DeviceMessage> records = page.getRecords();
            if(records != null && records.size()>0){
                for (int i = 0; i < records.size(); i++) {
                    String userId = records.get(i).getUserId();
                    SysUser byId = iSysUserService.getById(userId);
                    if(byId != null){
                        records.get(i).setUserName(byId.getUsername());
                    }
                }
            }
            page.setRecords(records);
        }

        return Result.ok(page);

    }
}
