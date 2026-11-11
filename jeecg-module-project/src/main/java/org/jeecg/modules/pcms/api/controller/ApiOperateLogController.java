package org.jeecg.modules.pcms.api.controller;

import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.ApiImplicitParam;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.pcms.project.entity.PvOperateLog;
import org.jeecg.modules.pcms.project.service.IPvOperateLogService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @Description: pv_operate_log
 * @Author: jun
 * @Date:   2023-03-11
 * @Version: V1.0
 */
@Api(tags="光伏-操作日志")
@RestController
@RequestMapping("/project/operateLog")
@Slf4j
public class ApiOperateLogController extends JeecgController<PvOperateLog, IPvOperateLogService> {
	@Autowired
	private IPvOperateLogService pvOperateLogService;
	
	/**
	 * 分页列表查询
	 *
	 * @param pvOperateLog
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "pv_operate_log-分页列表查询")
	@ApiOperation(value="操作日志-分页列表查询", notes="操作日志-分页列表查询")
	@ApiImplicitParam(name = "tenant-id", value = "Authorization",required = true, dataType = "int",paramType="header")
	@GetMapping(value = "/list")
	public Result<IPage<PvOperateLog>> queryPageList(PvOperateLog pvOperateLog,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<PvOperateLog> queryWrapper = QueryGenerator.initQueryWrapper(pvOperateLog, req.getParameterMap());
		Page<PvOperateLog> page = new Page<PvOperateLog>(pageNo, pageSize);
		IPage<PvOperateLog> pageList = pvOperateLogService.page(page, queryWrapper);
		return Result.OK(pageList);
	}


    /**
    * 导出excel
    *
    * @param request
    * @param pvOperateLog
    */
    //@RequiresPermissions("project:pv_operate_log:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, PvOperateLog pvOperateLog) {
        return super.exportXls(request, pvOperateLog, PvOperateLog.class, "pv_operate_log");
    }


}
