package org.jeecg.modules.pcms.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.project.entity.*;
import org.jeecg.modules.pcms.project.vo.*;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: pv_project_info
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface IPvProjectInfoService extends IService<PvProjectInfo> {

	/**
	 * 添加一对多
	 *
	 * @param pvProjectInfo
	 * @param pvProjectBpmList
	 * @param pvProjectFileList
	 * @param pvProjectRemarkList
	 */
	public void saveMain(PvProjectInfo pvProjectInfo,List<PvProjectBpm> pvProjectBpmList,List<PvProjectFile> pvProjectFileList,List<PvProjectRemark> pvProjectRemarkList) ;
	
	/**
	 * 修改一对多
	 *
   * @param pvProjectInfo
   * @param pvProjectBpmList
   * @param pvProjectFileList
   * @param pvProjectRemarkList
	 */
	public void updateMain(PvProjectInfo pvProjectInfo,List<PvProjectBpm> pvProjectBpmList,List<PvProjectFile> pvProjectFileList,List<PvProjectRemark> pvProjectRemarkList);

	public void keepProjectInfo(PvProjectInfo pvProjectInfo,List<PvProjectBpm> pvProjectBpmList,List<PvProjectFile> pvProjectFileList,List<PvProjectRemark> pvProjectRemarkList);


	/**
	 * 归档员修改数据
	 * @param pvProjectInfo
	 * @param pvProjectBpmList
	 * @param pvProjectFileList
	 * @param pvProjectRemarkList
	 */
	public void updateMainFile(PvProjectInfo pvProjectInfo,List<PvProjectBpm> pvProjectBpmList,List<PvProjectFile> pvProjectFileList,List<PvProjectRemark> pvProjectRemarkList);

	/**
	 * 删除一对多
	 *
	 * @param id
	 */
	public void delMain (Integer id);
	
	/**
	 * 批量删除一对多
	 *
	 * @param idList
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);

	/**
	 * 设置项目状态
	 * @param projectId
	 * @param status
	 */
	public void setProjectStatus(Integer projectId,String status);

	public void setProjectBeiJStatus(Integer projectId,String status);

	/**
	 * 通过id查询已经逻辑删除的数据（日志专用）
	 * @param id
	 * @return
	 */
	public PvProjectInfo getLogicalDeletedById(Integer id);

	/**
	 * 删除已完成项目备份的原图
	 */
	public void deleteExpiredImages();



	List<Object> getTaskStatus(String taskId);

	public List<ProjectExportVO>getList(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo);

	public List<ProjectExportVOXuZ>getListXuZ(String taskId,HttpServletRequest request,PvProjectInfo pvProjectInfo);


	public List<ProjectExportVOBeij>getListBeiJ(String taskId,HttpServletRequest request, PvProjectInfo pvProjectInfo);

	public List<ProjectExportVOHaiN>getListHaiN(String taskId,HttpServletRequest request, PvProjectInfo pvProjectInfo);

	public List<ProjectExportVOHeN>getListHeN(String taskId,HttpServletRequest request, PvProjectInfo pvProjectInfo);

	public List<ProjectExportVOLF>getListLF(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo);


	HomePageData getHomePageData(PvProjectInfo pvProjectInfo);


	List<LocationVO> getHomeDataCount(PvProjectInfo pvProjectInfo);

	/**
	 * 台账
	 */
	IPage<PvProjectInfo> getLedger(Page<PvProjectInfo> page, PvProjectInfo pvProjectInfo);
	IPage<PvProjectInfo> getRecycle(Page<PvProjectInfo> page, PvProjectInfo pvProjectInfo);


	Result updateAreaAddress(PvProjectInfo pvProjectInfo);

	Result getRenterId();


	boolean updateByProjectId(List<Integer> projectIds);


	PvProjectInfo getByProjectId(Integer projectId);


	Result homePageCount(PvProjectInfo pvProjectInfo);

	Result homePageCountNew(PvProjectInfo pvProjectInfo);


	public void updateIsUpload(String isUpload,Integer projectId);

    public void updateBatchMain(List<Integer> ids, String typeId);

	public Result<String> addProjectType(Integer tenantId,String typeValue);

	public Result<String> updateProjectType(ProjectTypeVo projectTypeVo);

	public IPage<PvProjectType> pagePt(Page<PvProjectType> page,Integer tenantId);

	public Result<String> delProjectType(List<String> ids,Integer tenantId);

	public PvProjectType selectType(Integer typeId,Integer tenantId);

	public Result<String> enableProjectType(List<String> ids, Integer tenantId);

	public List<PvProjectType> selectProjectType(Integer tenantId);

	public List<ProjectExportVOHaiN2> getListHaiN2(String taskId, HttpServletRequest request, PvProjectInfo pvProjectInfo);

	List<PvProjectInfo> selectProjectInfoList();

    List<PvProjectInfo> getByRenterId(String renterId);

    void updateByIdNew(PvProjectInfo pvProjectInfo);

	void updateCustomerName(String decrypt, Integer projectInfoId);

    void addLogToOtherFile(PvProjectFileOther pvProjectFileOther, LoginUser loginUser, String type);

}
