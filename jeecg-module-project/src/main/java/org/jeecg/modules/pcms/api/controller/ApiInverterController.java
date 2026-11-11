package org.jeecg.modules.pcms.api.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.project.service.ISysInverterService;
import org.jeecg.modules.pcms.project.service.ISysInverterWireMpptService;
import org.jeecg.modules.pcms.project.service.ISysInverterWireService;
import org.jeecg.modules.pcms.project.vo.SysInverterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/7/7 上午10:07
 * @Version 1.5
 */

@Api(tags = "项目逆变器")
@RestController
@RequestMapping("/project/inverter")
@Slf4j
public class ApiInverterController {



    @Autowired
    private ISysInverterService iSysInverterService;

    @Autowired
    private ISysInverterWireService iSysInverterWireService;


    @Autowired
    private ISysInverterWireMpptService iSysInverterWireMpptService;


    @ApiImplicitParam(name = "tenant-id", value = "Authorization", required = true, dataType = "int", paramType = "header")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody List<SysInverterVO> sysInverterVOS) {
        boolean b = iSysInverterService.addSysInverterVOS(sysInverterVOS);
        if(b){
            return Result.OK("添加成功！");
        }
       else {
            return Result.error("添加失败！");
        }
    }

}
