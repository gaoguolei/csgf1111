package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.entity.SysTenant;
import org.jeecg.modules.pcms.project.vo.ChartVo;
import org.jeecg.modules.pcms.project.vo.ProjectDataVo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Description: pv_project_info
 * @Author: jun
 * @Date:   2023-03-07
 * @Version: V1.0
 */
public interface PvProjectInfoMapper extends BaseMapper<PvProjectInfo> {

    /**
     * 通过id查询已经逻辑删除的数据（日志专用）
     * @param id
     * @return
     */
    public PvProjectInfo getLogicalDeletedById(@Param("id") Integer id);

    /**
     * 获取已过期的完结项目清单
     * @param expirationDays
     * @return
     */
    @InterceptorIgnore(tenantLine = "true")
    public List<PvProjectInfo> getExpirationList(@Param("expirationDays") Integer expirationDays);

    //累计签约户数
    Integer getHomePageData(PvProjectInfo pvProjectInfo,@Param("bpmNodeId")Integer bpmNodeId);

    //累计签约容量
    BigDecimal getHomePageDataLoad(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);

    BigDecimal getHomePageDataLoadFact(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);

    //今日签约户数
    Integer getHomePageDatatToday(PvProjectInfo pvProjectInfo,@Param("bpmNodeId")Integer bpmNodeId);

    //今日累计签约容量
    BigDecimal getHomePageDataLoadToday(PvProjectInfo pvProjectInfo,@Param("bpmNodeId")Integer bpmNodeId);

    //实际安装容量
    BigDecimal getHomePageDataLoadTodayFact(PvProjectInfo pvProjectInfo,@Param("bpmNodeId")Integer bpmNodeId);



    BigDecimal getHomeDataLoad(PvProjectInfo pvProjectInfo,@Param("bpmNodeId")Integer bpmNodeId);

    List<PvProjectInfo> getHomeDataCount(@Param("pvProjectInfo")PvProjectInfo pvProjectInfo);

    IPage<PvProjectInfo> getLedger(@Param("page") Page page, @Param("pvProjectInfo")PvProjectInfo pvProjectInfo);
    IPage<PvProjectInfo> getLedgerNoSeller(@Param("page") Page page, @Param("pvProjectInfo")PvProjectInfo pvProjectInfo);

    IPage<PvProjectInfo> getRecycle(@Param("page")Page page, @Param("pvProjectInfo")PvProjectInfo pvProjectInfo);
    IPage<PvProjectInfo> getRecycleNoSeller(@Param("page")Page page, @Param("pvProjectInfo")PvProjectInfo pvProjectInfo);


    @Update("update pv_project_info set project_status = #{rejectionStatus} ,rejection_type='1' where id=#{projectId}")
    Boolean updateStatusById(@Param("rejectionStatus")String rejectionStatus,@Param("projectId")Integer projectId);


    @Update("update pv_project_info set project_status = #{rejectionStatus} ,rejection_type='' where id=#{projectId}")
    Boolean updateBefore(@Param("rejectionStatus")String rejectionStatus,@Param("projectId")Integer projectId);


    @Update("update pv_project_info set inst_location = #{instLocation}, inst_longitude=#{instLongitude},inst_latitude=#{instLatitude}  where id=#{id}")
    Boolean updateAreaAddress(@Param("instLocation")String instLocation, @Param("instLongitude")String instLongitude,
                              @Param("instLatitude")String instLatitude,@Param("id")Integer id);



    @Update("update pv_project_info set del_flag=0,update_time=#{updateTime} where id=#{projectId}")
    Boolean updateByProjectId(@Param("projectId")Integer projectId,@Param("updateTime") Date updateTime);



    @Select("select * from sys_tenant where status='1' order by create_time asc")
    List<SysTenant> getRenterId();


    @Update("update pv_project_info set del_flag=1,update_time=#{updateTime} where id=#{projectId}")
    Boolean restoreByprojectId(@Param("projectId")Integer projectId,@Param("updateTime") Date updateTime);

    @Select("select * from pv_project_info where id = #{projectId}")
    PvProjectInfo getByProjectId(@Param("projectId")Integer projectId);


    /**
     * 累计签约户数和容量
     * @param pvProjectInfo
     * @param bpmNodeId
     * @return
     */

    ProjectDataVo getLoad(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);

    ProjectDataVo getLoadFact(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);

    /**
     * 本周签约户数和容量
     * @param pvProjectInfo
     * @param bpmNodeId
     * @return
     */
    ProjectDataVo getWeek(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);
    ProjectDataVo getWeekFact(PvProjectInfo pvProjectInfo, @Param("bpmNodeId")Integer bpmNodeId);


    @Update("update pv_project_info set is_upload = #{isUpload} where id=#{projectId}")
    public void updateIsUpload(@Param("isUpload")String isUpload,@Param("projectId")Integer projectId);

    //获取最近 6个

    @Select("select count(*) as signCount ,sum(inst_cmpt_capacity) as capacity from pv_project_info WHERE DATE_FORMAT(create_time, '%Y-%m') = #{date}")
    public ChartVo getChart(String date);

    @Update({
            "<script>",
            "UPDATE pv_project_info",
            "SET project_type = #{typeId}",
            "WHERE id IN",
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    public void updateBatchMain(@Param("ids") List<Integer> ids, @Param("typeId") String typeId);

    public void updateProjectInfo(PvProjectInfo pvProjectInfo);

    void updateBuildAfter(@Param("projectInfoId") Integer projectInfoId, @Param("buildAfter") String buildAfter);

    List<PvProjectInfo> selectProjectInfoList();

    @Select("select * from pv_project_info where id = #{projectId}")
    PvProjectInfo getById(Integer projectInfoId);

    @Select("select * from pv_project_info where tenant_id = #{renterId} and del_flag = 0")
    List<PvProjectInfo> getByRenterId(Integer renterId);

    void updateByIdNew(@Param("p") PvProjectInfo pvProjectInfo);

    void updateCustomerName(@Param("decrypt") String decrypt, @Param("projectInfoId") Integer projectInfoId);

}
