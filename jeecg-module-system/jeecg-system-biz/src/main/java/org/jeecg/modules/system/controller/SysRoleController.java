package org.jeecg.modules.system.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xkcoding.http.support.Http;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.model.TreeModel;
import org.jeecg.modules.system.service.*;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @Author scott
 * @since 2018-12-19
 */
@RestController
@RequestMapping("/sys/role")
@Slf4j
public class SysRoleController {
	@Autowired
	private ISysRoleService sysRoleService;
	
	@Autowired
	private ISysPermissionDataRuleService sysPermissionDataRuleService;
	
	@Autowired
	private ISysRolePermissionService sysRolePermissionService;
	
	@Autowired
	private ISysPermissionService sysPermissionService;

	@Autowired
	private ISysUserService sysUserService;


	@Autowired
	private ISysUserRoleService userRoleService;

	/** 业务专员/财务专员/公司领导/运维 是固定角色，不能删除、不能改编码 */
	@Autowired
	private org.jeecg.modules.system.security.FixedRoleGuard fixedRoleGuard;

	/**
	  * 分页列表查询
	 * @param role
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Result<IPage<SysRole>> queryPageList(SysRole role,
									  @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
									  @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
									  HttpServletRequest req) {
		Result<IPage<SysRole>> result = new Result<IPage<SysRole>>();
		QueryWrapper<SysRole> queryWrapper = QueryGenerator.initQueryWrapper(role, req.getParameterMap());
		//获取当前登录用户如果非超级管理员不显示超级管理员角色
		//获取当前登录用户
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		String id = loginUser.getId();
		//获取当前用户角色，超级管理员角色id作比较。如果不是超级管理员的角色就不展示超级管理员的用户
		QueryWrapper<SysUserRole> sysUserRoleQueryWrapper = new QueryWrapper<>();
		sysUserRoleQueryWrapper.eq("user_id",id);
		SysUserRole one = userRoleService.getOne(sysUserRoleQueryWrapper);
		String roleId = one.getRoleId();
//		if(!roleId.equals("f6817f48af4fb3af11b9e8bf182f618b")){
//			//如果是项目经理和平台管理员 就去掉二级商
//			if(roleId.equals("1638746311646339073") || roleId.equals("1638753735342120961")){
//				queryWrapper.ne("id","1791047421807529985");
//			}
//			queryWrapper.ne("id","f6817f48af4fb3af11b9e8bf182f618b");
//			queryWrapper.ne("role_code","444645");
//		}
		Page<SysRole> page = new Page<SysRole>(pageNo, pageSize);
		IPage<SysRole> pageList = sysRoleService.page(page, queryWrapper);
		result.setSuccess(true);
		result.setResult(pageList);
		return result;
	}

	/**
	 * 查找北京租户项目下的一级商用户
	 * @return
	 */
	@RequestMapping(value = "/getFirstSellerList", method = RequestMethod.GET)
	public Result<List<SysUser>> queryPageList(@RequestParam(name="renterId",required=true) String renterId) {
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		String id = loginUser.getId();
		SysUser sysUser = sysUserService.getById(id);
		QueryWrapper<SysUserRole> sysUserRoleQueryWrapper = new QueryWrapper<>();
		sysUserRoleQueryWrapper.eq("user_id",id);
		SysUserRole role = userRoleService.getOne(sysUserRoleQueryWrapper);
		String roleId = role.getRoleId();
		//先查询一级商用户，再根据项目id过滤
		if(roleId.equals("1791047313644818434")){
			List<SysUser> userIdList = new ArrayList<>();
			userIdList.add(sysUser);
		}
		QueryWrapper<SysUserRole> sysUserQueryWrapper = new QueryWrapper<>();
		sysUserQueryWrapper.eq("role_id","1791047313644818434");
		List<SysUserRole> list = userRoleService.list(sysUserQueryWrapper);
		List<SysUser> userIdList = new ArrayList<>();
		if(list != null && list.size()>0) {
			for (int i = 0; i < list.size(); i++) {
				String userId = list.get(i).getUserId();
				SysUser byId = sysUserService.getById(userId);
				if (byId != null) {
					userIdList.add(byId);
				}
			}
		}
		if(userIdList!= null && userIdList.size()>0){
			List<SysUser> collect = userIdList.stream().filter(i -> i.getRelTenantIds().equals("-2103508991")).collect(Collectors.toList());
			return Result.ok(collect);
		}
		return Result.ok();
	}
	
	/**
	  *   添加
	 * @param role
	 * @return
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	//@RequiresRoles({"admin"})
	public Result<SysRole> add(@RequestBody SysRole role) {
		Result<SysRole> result = new Result<>();
		try {
			role.setCreateTime(new Date());
			sysRoleService.save(role);
			result.success("添加成功！");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.error500("操作失败");
		}
		return result;
	}


	/**
	 * 查看上级的创建人
	 * @param requestRole
	 * @return
	 */
	@RequestMapping(value = "/highUserList", method = RequestMethod.GET)
	public Result highUserList(@RequestParam(name="requestRole",required=true) String requestRole,@RequestParam(name="renterId",required=true) String renterId) {
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		String id = loginUser.getId();
		SysUser sysUser = sysUserService.getById(id);
		String username = loginUser.getUsername();
		String realname = sysUser.getRealname();
		QueryWrapper<SysUserRole> sysUserRoleQueryWrapper = new QueryWrapper<>();
		sysUserRoleQueryWrapper.eq("user_id",id);
		SysUserRole role = userRoleService.getOne(sysUserRoleQueryWrapper);
		String roleId = role.getRoleId();
		//获取二级商的角色id，用此id找到创建人，然后再找上一级创建人就可以
		QueryWrapper<SysUserRole> sysUserQueryWrapper = new QueryWrapper<>();


		//如果当前是超管，管理员，项目经理。那么就列出全平台的
		if((roleId.equals("f6817f48af4fb3af11b9e8bf182f618b") || roleId.equals("1638746311646339073") || roleId.equals("1638753735342120961"))){
			//如果传入的是销售员，列出全平台的 二级商，
			if(requestRole.equals("1632852352834883585") ){
				sysUserQueryWrapper.eq("role_id","1791047421807529985");
				List<SysUserRole> list = userRoleService.list(sysUserQueryWrapper);
				List<SysUser> userIdList = new ArrayList<>();
				if(list != null && list.size()>0) {
					for (int i = 0; i < list.size(); i++) {
						String userId = list.get(i).getUserId();
						SysUser byId = sysUserService.getById(userId);
						if (byId != null) {
							userIdList.add(byId);
						}
					}
				}
				if(userIdList != null && userIdList.size()>0) {
					//给二级商的一级商赋值
					for (int i = 0; i < userIdList.size(); i++) {
						String createBy = userIdList.get(i).getCreateBy();
						SysUser userByName = sysUserService.getUserByName(createBy);
						if(userByName != null){
							userIdList.get(i).setFirstSer(userByName.getRealname());
						}
					}
					List<SysUser> collect = userIdList.stream().filter(j -> j.getRelTenantIds().equals(renterId)).collect(Collectors.toList());
					return Result.ok(collect);
				}
			}
			//如果传入的是二级商，列出全平台的一级商
			if(requestRole.equals("1791047421807529985") ){
				sysUserQueryWrapper.eq("role_id","1791047313644818434");
				List<SysUserRole> list = userRoleService.list(sysUserQueryWrapper);
				List<SysUser> userIdList = new ArrayList<>();
				if(list != null && list.size()>0) {
					for (int i = 0; i < list.size(); i++) {
						String userId = list.get(i).getUserId();
						SysUser byId = sysUserService.getById(userId);
						if (byId != null) {
							userIdList.add(byId);
						}
					}
				}
				if(userIdList != null && userIdList.size()>0) {
					List<SysUser> collect = userIdList.stream().filter(j -> j.getRelTenantIds().equals(renterId)).collect(Collectors.toList());
					return Result.ok(collect);
				}
			}
		}
		//如果登录的是一级商
		if(roleId.equals("1791047313644818434")){
			//如果建立的是二级商，一级商就反显的是他自己
			if(requestRole.equals("1791047421807529985")){
				List<SysUser> sysUsers = new ArrayList<>();
				sysUsers.add(sysUser);
				return Result.ok(sysUsers);
			}
			//如果建立的是销售员，那二级商就只能是他自己建立的二级商
			if(requestRole.equals("1632852352834883585")){
				sysUserQueryWrapper.eq("role_id","1791047421807529985");
				List<SysUserRole> list = userRoleService.list(sysUserQueryWrapper);
				List<SysUser> userIdList = new ArrayList<>();
				if(list != null && list.size()>0) {
					for (int i = 0; i < list.size(); i++) {
						String userId = list.get(i).getUserId();
						SysUser byId = sysUserService.getById(userId);
						if (byId != null) {
							userIdList.add(byId);
						}
					}
				}
				if(userIdList != null && userIdList.size()>0){
					List<SysUser> collect = userIdList.stream().filter(j -> j.getCreateBy().equals(username)).filter(j->j.getRelTenantIds().equals(renterId)).collect(Collectors.toList());
					if(collect!= null && collect.size()>0){
						for (int i = 0; i < collect.size(); i++) {
							collect.get(i).setFirstSer(realname);
						}
					}
					return Result.ok(collect);
				}
			}
		}
		//如果当前登录用户二级商
		if(roleId.equals("1791047421807529985") ){
			//二级商只能建立销售员，这时候他的二级商就反显的是他自己
			List<SysUser> sysUsers = new ArrayList<>();
			sysUsers.add(sysUser);
			if(sysUsers!= null && sysUsers.size()>0){
				for (int i = 0; i < sysUsers.size(); i++) {
					String createBy = sysUsers.get(i).getCreateBy();
					SysUser userByName = sysUserService.getUserByName(createBy);
					if(userByName != null){
						sysUsers.get(i).setFirstSer(userByName.getRealname());
					}
				}
			}
			return Result.ok(sysUsers);
		}
		return Result.ok();

	}
	/**
	  *  编辑
	 * @param role
	 * @return
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/edit",method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<SysRole> edit(@RequestBody SysRole role) {
		Result<SysRole> result = new Result<SysRole>();
		SysRole sysrole = sysRoleService.getById(role.getId());
		if(sysrole==null) {
			result.error500("未找到对应实体");
		}else {
			role.setUpdateTime(new Date());
			// 固定角色不允许改 role_code，其他字段（名称、描述）照常可改
			fixedRoleGuard.checkCodeUnchanged(role.getId(), role.getRoleCode());
			boolean ok = sysRoleService.updateById(role);
			//TODO 返回false说明什么？
			if(ok) {
				result.success("修改成功!");
			}
		}
		
		return result;
	}
	
	/**
	  *   通过id删除
	 * @param id
	 * @return
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		fixedRoleGuard.checkDeletable(id);
		sysRoleService.deleteRole(id);
		return Result.ok("删除角色成功");
	}
	
	/**
	  *  批量删除
	 * @param ids
	 * @return
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/deleteBatch", method = RequestMethod.DELETE)
	public Result<SysRole> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		Result<SysRole> result = new Result<SysRole>();
		if(oConvertUtils.isEmpty(ids)) {
			result.error500("未选中角色！");
		}else {
			for (String roleId : ids.split(",")) {
				fixedRoleGuard.checkDeletable(roleId.trim());
			}
			sysRoleService.deleteBatchRole(ids.split(","));
			result.success("删除角色成功!");
		}
		return result;
	}
	
	/**
	  * 通过id查询
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/queryById", method = RequestMethod.GET)
	public Result<SysRole> queryById(@RequestParam(name="id",required=true) String id) {
		Result<SysRole> result = new Result<SysRole>();
		SysRole sysrole = sysRoleService.getById(id);
		if(sysrole==null) {
			result.error500("未找到对应实体");
		}else {
			result.setResult(sysrole);
			result.setSuccess(true);
		}
		return result;
	}

	/**
	 * 查询所有角色
	 * @param renterId
	 * @return
	 */
	@RequestMapping(value = "/queryall", method = RequestMethod.GET)
	public Result<List<SysRole>> queryall(@RequestParam(name="renterId",required=true) String renterId) {
		Result<List<SysRole>> result = new Result<>();
		//获取当前登录用户
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		String id = loginUser.getId();
		//获取当前用户角色，超级管理员角色id作比较。如果不是超级管理员的角色就不展示超级管理员的用户
		QueryWrapper<SysUserRole> sysUserRoleQueryWrapper = new QueryWrapper<>();
		sysUserRoleQueryWrapper.eq("user_id",id);
		SysUserRole one = userRoleService.getOne(sysUserRoleQueryWrapper);
		String roleId = one.getRoleId();
		//天津项目
		//如果当前进来的一级商 那就只给他显示二级商 二级商就只给他创建销售员
		//一级商只能创建并查看二级商
		if(renterId.equals("102") || renterId.equals("118337537") ||renterId.equals("-1759715327")){
			if(roleId.equals("1791047313644818434")){
				QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
				objectQueryWrapper.in("id","1791047421807529985","1632852352834883585");
				List<SysRole> list = sysRoleService.list(objectQueryWrapper);
				if(list==null||list.size()<=0) {
					result.error500("未找到角色信息");
				}else {
					result.setResult(list);
					result.setSuccess(true);
				}
				return result;
			}
			//二级商 只能创建并查看销售员
			if(roleId.equals("1791047421807529985")){
				QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
				objectQueryWrapper.eq("id","1632852352834883585");
				List<SysRole> list = sysRoleService.list(objectQueryWrapper);
				if(list==null||list.size()<=0) {
					result.error500("未找到角色信息");
				}else {
					result.setResult(list);
					result.setSuccess(true);
				}
				return result;
			}
		}
		//北京项目
		if(renterId.equals("-2103508991")){
			//一级商可以建立以下 	资料员，巡检员
			if(roleId.equals("1791047313644818434")){
				QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
				objectQueryWrapper.in("id","1791047421807529985","1632852352834883585","1632852792637018113","1632852197175873538");
				List<SysRole> list = sysRoleService.list(objectQueryWrapper);
				if(list==null||list.size()<=0) {
					result.error500("未找到角色信息");
				}else {
					result.setResult(list);
					result.setSuccess(true);
				}
				return result;
			}
			//二级商 只能创建 施工管理员、施工队，并网员  资料员
			if(roleId.equals("1791047421807529985")){
				QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
				if(!roleId.equals("f6817f48af4fb3af11b9e8bf182f618b")){
					objectQueryWrapper.ne("id","f6817f48af4fb3af11b9e8bf182f618b");
				}
				objectQueryWrapper.in("id","1632852352834883585","1632852551321931777","1632852669798436865","1632853052226686977","1632852197175873538");
				List<SysRole> list = sysRoleService.list(objectQueryWrapper);
				if(list==null||list.size()<=0) {
					result.error500("未找到角色信息");
				}else {
					result.setResult(list);
					result.setSuccess(true);
				}
				return result;
			}
		}
		//如果其他的项目暂时不让创建一级商二级商
		if(!(renterId.equals("102")|| renterId.equals("118337537") || renterId.equals("-2103508991") || renterId.equals("-1759715327"))){
			QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
			objectQueryWrapper.notIn("id","1791047313644818434","1791047421807529985");
			if(!roleId.equals("f6817f48af4fb3af11b9e8bf182f618b")){
				if(roleId.equals("1638753735342120961")){
					objectQueryWrapper.ne("id","1638746311646339073");
				}
				objectQueryWrapper.ne("id","f6817f48af4fb3af11b9e8bf182f618b");
			}
			List<SysRole> list = sysRoleService.list(objectQueryWrapper);
			if(list==null||list.size()<=0) {
				result.error500("未找到角色信息");
			}else {
				result.setResult(list);
				result.setSuccess(true);
			}
			return result;
		}
		//如果不是超管 就排除超管的自己的角色，

//			objectQueryWrapper.ne("role_code","444645");
//			if(roleId.equals("1638746311646339073") || roleId.equals("1638753735342120961")){
//				objectQueryWrapper.ne("id","1791047421807529985");
//			}
		//不管是哪个租户，如果当前不是管理员，那就要踢出超级管理员
		QueryWrapper<SysRole> objectQueryWrapper = new QueryWrapper<>();
		if(!roleId.equals("f6817f48af4fb3af11b9e8bf182f618b")){
			objectQueryWrapper.ne("id","f6817f48af4fb3af11b9e8bf182f618b");
		}
		//不管是哪个租户如果当前是项目经理，他不能建立超管和管理员

		List<SysRole> list = sysRoleService.list(objectQueryWrapper);

		if(list==null||list.size()<=0) {
			result.error500("未找到角色信息");
		}else {
			result.setResult(list);
			result.setSuccess(true);
		}
		return result;
	}
	/**
	  * 校验角色编码唯一
	 */
	@RequestMapping(value = "/checkRoleCode", method = RequestMethod.GET)
	public Result<Boolean> checkUsername(String id,String roleCode) {
		Result<Boolean> result = new Result<>();
        //如果此参数为false则程序发生异常
		result.setResult(true);
		log.info("--验证角色编码是否唯一---id:"+id+"--roleCode:"+roleCode);
		try {
			SysRole role = null;
			if(oConvertUtils.isNotEmpty(id)) {
				role = sysRoleService.getById(id);
			}
			SysRole newRole = sysRoleService.getOne(new QueryWrapper<SysRole>().lambda().eq(SysRole::getRoleCode, roleCode));
			if(newRole!=null) {
				//如果根据传入的roleCode查询到信息了，那么就需要做校验了。
				if(role==null) {
					//role为空=>新增模式=>只要roleCode存在则返回false
					result.setSuccess(false);
					result.setMessage("角色编码已存在");
					return result;
				}else if(!id.equals(newRole.getId())) {
					//否则=>编辑模式=>判断两者ID是否一致-
					result.setSuccess(false);
					result.setMessage("角色编码已存在");
					return result;
				}
			}
		} catch (Exception e) {
			result.setSuccess(false);
			result.setResult(false);
			result.setMessage(e.getMessage());
			return result;
		}
		result.setSuccess(true);
		return result;
	}

	/**
	 * 导出excel
	 * @param request
	 */
	@RequestMapping(value = "/exportXls")
	public ModelAndView exportXls(SysRole sysRole,HttpServletRequest request) {
		// Step.1 组装查询条件
		QueryWrapper<SysRole> queryWrapper = QueryGenerator.initQueryWrapper(sysRole, request.getParameterMap());
		//Step.2 AutoPoi 导出Excel
		ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
		List<SysRole> pageList = sysRoleService.list(queryWrapper);
		//导出文件名称
		mv.addObject(NormalExcelConstants.FILE_NAME,"角色列表");
		mv.addObject(NormalExcelConstants.CLASS,SysRole.class);
		LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		mv.addObject(NormalExcelConstants.PARAMS,new ExportParams("角色列表数据","导出人:"+user.getRealname(),"导出信息"));
		mv.addObject(NormalExcelConstants.DATA_LIST,pageList);
		return mv;
	}

	/**
	 * 通过excel导入数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/importExcel", method = RequestMethod.POST)
	public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            // 获取上传文件对象
			MultipartFile file = entity.getValue();
			ImportParams params = new ImportParams();
			params.setTitleRows(2);
			params.setHeadRows(1);
			params.setNeedSave(true);
			try {
				return sysRoleService.importExcelCheckRoleCode(file, params);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return Result.error("文件导入失败:" + e.getMessage());
			} finally {
				try {
					file.getInputStream().close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return Result.error("文件导入失败！");
	}
	
	/**
	 * 查询数据规则数据
	 */
	@GetMapping(value = "/datarule/{permissionId}/{roleId}")
	public Result<?> loadDatarule(@PathVariable("permissionId") String permissionId,@PathVariable("roleId") String roleId) {
		List<SysPermissionDataRule> list = sysPermissionDataRuleService.getPermRuleListByPermId(permissionId);
		if(list==null || list.size()==0) {
			return Result.error("未找到权限配置信息");
		}else {
			Map<String,Object> map = new HashMap(5);
			map.put("datarule", list);
			LambdaQueryWrapper<SysRolePermission> query = new LambdaQueryWrapper<SysRolePermission>()
					.eq(SysRolePermission::getPermissionId, permissionId)
					.isNotNull(SysRolePermission::getDataRuleIds)
					.eq(SysRolePermission::getRoleId,roleId);
			SysRolePermission sysRolePermission = sysRolePermissionService.getOne(query);
			if(sysRolePermission==null) {
				//return Result.error("未找到角色菜单配置信息");
			}else {
				String drChecked = sysRolePermission.getDataRuleIds();
				if(oConvertUtils.isNotEmpty(drChecked)) {
					map.put("drChecked", drChecked.endsWith(",")?drChecked.substring(0, drChecked.length()-1):drChecked);
				}
			}
			return Result.ok(map);
			//TODO 以后按钮权限的查询也走这个请求 无非在map中多加两个key
		}
	}
	
	/**
	 * 保存数据规则至角色菜单关联表
	 */
	@PostMapping(value = "/datarule")
	public Result<?> saveDatarule(@RequestBody JSONObject jsonObject) {
		try {
			String permissionId = jsonObject.getString("permissionId");
			String roleId = jsonObject.getString("roleId");
			String dataRuleIds = jsonObject.getString("dataRuleIds");
			log.info("保存数据规则>>"+"菜单ID:"+permissionId+"角色ID:"+ roleId+"数据权限ID:"+dataRuleIds);
			LambdaQueryWrapper<SysRolePermission> query = new LambdaQueryWrapper<SysRolePermission>()
					.eq(SysRolePermission::getPermissionId, permissionId)
					.eq(SysRolePermission::getRoleId,roleId);
			SysRolePermission sysRolePermission = sysRolePermissionService.getOne(query);
			if(sysRolePermission==null) {
				return Result.error("请先保存角色菜单权限!");
			}else {
				sysRolePermission.setDataRuleIds(dataRuleIds);
				this.sysRolePermissionService.updateById(sysRolePermission);
			}
		} catch (Exception e) {
			log.error("SysRoleController.saveDatarule()发生异常：" + e.getMessage(),e);
			return Result.error("保存失败");
		}
		return Result.ok("保存成功!");
	}
	
	
	/**
	 * 用户角色授权功能，查询菜单权限树
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/queryTreeList", method = RequestMethod.GET)
	public Result<Map<String,Object>> queryTreeList(HttpServletRequest request) {
		Result<Map<String,Object>> result = new Result<>();
		//全部权限ids
		List<String> ids = new ArrayList<>();
		try {
			LambdaQueryWrapper<SysPermission> query = new LambdaQueryWrapper<SysPermission>();
			//获取当前登录用户如果非超级管理员不显示超级管理员角色
			LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
			String id = loginUser.getId();
			//获取当前用户角色，超级管理员角色id作比较。如果是非管理员用户，不给显示以下菜单
			QueryWrapper<SysUserRole> sysUserRoleQueryWrapper = new QueryWrapper<>();
			sysUserRoleQueryWrapper.eq("user_id",id);
			SysUserRole one = userRoleService.getOne(sysUserRoleQueryWrapper);
			String roleId = one.getRoleId();
			if(!roleId.equals("f6817f48af4fb3af11b9e8bf182f618b")){
				query.ne(SysPermission::getId,"1438108176273760258");
				query.ne(SysPermission::getId,"1455100420297859074");
				query.ne(SysPermission::getId,"1447763172274495489");
				query.ne(SysPermission::getId,"1439398677984878593");
				query.ne(SysPermission::getId,"1443390062919208961");
				query.ne(SysPermission::getId,"1438108221551271937");
				query.ne(SysPermission::getId,"1438108196993622018");
				query.ne(SysPermission::getId,"1438108187455774722");
				query.ne(SysPermission::getId,"1633461921333985282");
				query.ne(SysPermission::getId,"1170592628746878978");
				query.ne(SysPermission::getId,"5c2f42277948043026b7a14692456828");
				query.ne(SysPermission::getId,"1438469604861403137");
				query.ne(SysPermission::getId,"1438782851980210178");
				query.ne(SysPermission::getId,"1438782530717495298");
				query.ne(SysPermission::getId,"1438782641187074050");
				query.ne(SysPermission::getId,"1455735714507472898");
				query.ne(SysPermission::getId,"1280350452934307841");
				query.ne(SysPermission::getId,"1438108225451974658");
			}
			query.eq(SysPermission::getDelFlag, CommonConstant.DEL_FLAG_0);
			query.orderByAsc(SysPermission::getSortNo);
			List<SysPermission> list = sysPermissionService.list(query);
			for(SysPermission sysPer : list) {
				ids.add(sysPer.getId());
			}
			List<TreeModel> treeList = new ArrayList<>();
			getTreeModelList(treeList, list, null);
			Map<String,Object> resMap = new HashMap(5);
            //全部树节点数据
			resMap.put("treeList", treeList);
            //全部树ids
			resMap.put("ids", ids);
			result.setResult(resMap);
			result.setSuccess(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return result;
	}
	
	private void getTreeModelList(List<TreeModel> treeList,List<SysPermission> metaList,TreeModel temp) {
		for (SysPermission permission : metaList) {
			String tempPid = permission.getParentId();
			TreeModel tree = new TreeModel(permission.getId(), tempPid, permission.getName(),permission.getRuleFlag(), permission.isLeaf());
			if(temp==null && oConvertUtils.isEmpty(tempPid)) {
				treeList.add(tree);
				if(!tree.getIsLeaf()) {
					getTreeModelList(treeList, metaList, tree);
				}
			}else if(temp!=null && tempPid!=null && tempPid.equals(temp.getKey())){
				temp.getChildren().add(tree);
				if(!tree.getIsLeaf()) {
					getTreeModelList(treeList, metaList, tree);
				}
			}
			
		}
	}
	
	
}
