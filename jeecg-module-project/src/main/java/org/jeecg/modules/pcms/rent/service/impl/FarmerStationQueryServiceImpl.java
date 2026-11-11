package org.jeecg.modules.pcms.rent.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.FarmerStationListVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationQueryDTO;
import org.jeecg.modules.pcms.rent.dto.ProjectOptionVO;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.service.IFarmerStationQueryService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.RentTenantResolver;
import org.jeecg.modules.pcms.rent.util.RentQueryParamUtil;
import org.jeecg.modules.pcms.rent.util.RentRegionResolver;
import org.jeecg.modules.pcms.rent.util.TenantScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 农户信息管理查询实现。
 *
 * 列表要展示的 8 个字段全部在 t_farmer_station 单表里，所以不联表；
 * 项目名称从 t_project 一次性读进内存补上——那张表只有 12 行。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class FarmerStationQueryServiceImpl implements IFarmerStationQueryService {

    /** 导入时生成的占位值，列表上要给运营打标提示补录 */
    private static final String NO_USER_NO_PREFIX = "待补户号-";
    private static final String NO_NAME = "待补姓名";

    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private TenantScopeHelper tenantScope;
    @Autowired
    private RentTenantResolver tenantResolver;
    @Autowired
    private RentRegionResolver regionResolver;

    @Override
    public IPage<FarmerStationListVO> pageList(FarmerStationQueryDTO query, int pageNo, int pageSize) {
        LambdaQueryWrapper<TFarmerStation> w = buildWrapper(
                query == null ? new FarmerStationQueryDTO() : query);

        Page<TFarmerStation> page = new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        IPage<TFarmerStation> data = farmerStationService.page(page, w);

        Map<Long, TProject> projectMap = loadProjectMap();
        return data.convert(e -> toVO(e, projectMap));
    }

    @Override
    public List<TFarmerStation> listForExport(FarmerStationQueryDTO query) {
        return farmerStationService.list(buildWrapper(
                query == null ? new FarmerStationQueryDTO() : query));
    }

    @Override
    public List<TFarmerStation> listByIdsForExport(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<>();
        w.eq(TFarmerStation::getDelFlag, 0);
        // 勾选导出同样要过租户隔离：拿着别的租户的ID来查也查不出来
        tenantScope.apply(w, TFarmerStation::getTenantId);
        w.in(TFarmerStation::getId, ids);
        w.orderByAsc(TFarmerStation::getId);
        return farmerStationService.list(w);
    }

    /**
     * 按查询条件拼 where。列表和导出共用，两边筛出来的结果永远一致。
     */
    private LambdaQueryWrapper<TFarmerStation> buildWrapper(FarmerStationQueryDTO query) {
        LambdaQueryWrapper<TFarmerStation> w = new LambdaQueryWrapper<>();
        w.eq(TFarmerStation::getDelFlag, 0);

        // 需求第14条：数据隔离。管理员（rel_tenant_ids 为空）看全部，
        // 普通用户只看自己租户的农户——天津的账号不该看到北京的数据
        tenantScope.apply(w, TFarmerStation::getTenantId);

        // 需求稿第 4 条：姓名、发电户号、安装地址为模糊查询
        w.like(notBlank(query.getFarmerName()), TFarmerStation::getFarmerName, trim(query.getFarmerName()));
        w.like(notBlank(query.getInstallAddr()), TFarmerStation::getInstallAddr, trim(query.getInstallAddr()));
        w.like(notBlank(query.getStationCode()), TFarmerStation::getStationCode, trim(query.getStationCode()));
        // 户号必须用全模糊：河南有一格写多个户号的，前缀匹配会漏掉后面几个；
        // 重复户号的 1230000-1/-2/-3 也要能用 1230000 一次查全
        w.like(notBlank(query.getPowerUserNo()), TFarmerStation::getPowerUserNo, trim(query.getPowerUserNo()));

        // 项目：选了具体项目就按项目，只选了大区就按大区
        w.eq(query.getProjectId() != null, TFarmerStation::getProjectId, query.getProjectId());
        // 需求29/30：状态栏筛选。auditStatus 可以传单个值，也可以传逗号分隔的多个值（如 2,4,6,8）；
        // 也可以只传聚合口径 auditStatusGroup=PENDING/REJECTED，把三级和新增/修改合并掉
        List<Integer> auditStatuses = RentQueryParamUtil.parseIntList(query.getAuditStatus());
        if (!auditStatuses.isEmpty()) {
            w.in(TFarmerStation::getAuditStatus, auditStatuses);
        } else if (notBlank(query.getAuditStatusGroup())) {
            applyAuditStatusGroup(w, query.getAuditStatusGroup().trim());
        }
        if (query.getProjectId() == null && notBlank(query.getRegionCode())) {
            // 天津和廊坊共用库里的 region_code=TJLF，不能直接拿 region_code 比
            //（选廊坊会比较不出任何数据）。统一折算成项目ID再筛，
            // 口径与下拉框、台账列表保持一致
            List<Long> projectIds = regionResolver.projectIds(query.getRegionCode());
            if (projectIds.isEmpty()) {
                // 该大区下没有项目，返回空结果，不要退化成全表查询
                w.eq(TFarmerStation::getId, -1L);
            } else {
                w.in(TFarmerStation::getProjectId, projectIds);
            }
        }

        // 并网日期区间，两端都含
        w.ge(query.getGridDateBegin() != null, TFarmerStation::getGridDate, query.getGridDateBegin());
        w.le(query.getGridDateEnd() != null, TFarmerStation::getGridDate, query.getGridDateEnd());

        applySort(w, query);
        return w;
    }

    /**
     * 聚合状态筛选：PENDING 把所有待审（新增/修改 × 项目/业务/领导）合成一档，
     * REJECTED 把所有驳回合成一档。只用于筛选，列表返回的 auditStatusName 仍是明细。
     */
    private void applyAuditStatusGroup(LambdaQueryWrapper<TFarmerStation> w, String group) {
        if ("PENDING".equalsIgnoreCase(group)) {
            w.in(TFarmerStation::getAuditStatus, TFarmerStation.PENDING_AUDIT_VALUES);
        } else if ("REJECTED".equalsIgnoreCase(group)) {
            w.in(TFarmerStation::getAuditStatus, TFarmerStation.REJECTED_AUDIT_VALUES);
        }
    }

    @Override
    public List<ProjectOptionVO> projectOptions() {
        List<TProject> projects = projectService.list(new LambdaQueryWrapper<TProject>()
                .eq(TProject::getStatus, 1)
                .orderByAsc(TProject::getRegionCode)
                .orderByAsc(TProject::getProjectCode));

        // 需求第14条：下拉框也要按租户过滤。不过滤的话天津的账号能选到北京的项目，
        // 选完查出来是空的，运营会以为是数据丢了
        projects = filterByTenant(projects);

        Map<String, ProjectOptionVO> grouped = new LinkedHashMap<>();
        for (TProject p : projects) {
            // 天津和廊坊在库里共用 region_code=TJLF，但业务上是两个大区。
            // 这里按项目编码前缀拆开，下拉框才显示成「天津」「廊坊」两项而不是「天津廊坊」
            String key = regionResolver.regionCodeOf(p);
            ProjectOptionVO group = grouped.computeIfAbsent(key,
                    k -> new ProjectOptionVO()
                            .setRegionCode(k)
                            .setRegionName(regionResolver.regionNameOf(p)));
            group.getChildren().add(new ProjectOptionVO.ProjectItem()
                    .setId(p.getId())
                    .setProjectCode(p.getProjectCode())
                    .setProjectName(p.getProjectName()));
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * 按当前用户的租户范围过滤项目。管理员（rel_tenant_ids 为空）返回全部。
     *
     * t_project 上没有 tenant_id 列，项目归属租户是靠"大区 + 项目编码"推出来的
     * （天津和廊坊同属 TJLF 大区，靠 project_code 前缀区分）。
     * 这里复用 RentTenantResolver 的同一套映射规则，保证"下拉框能选到的项目"
     * 和"农户落库时归到哪个租户"用的是同一个判断，不会出现选得到但查不出数据的情况。
     */
    private List<TProject> filterByTenant(List<TProject> projects) {
        List<Integer> allowed = tenantScope.allowedTenantIds();
        if (allowed == null) {
            return projects;
        }
        Map<String, Integer> tenantIdByName = tenantResolver.loadTenantIds();
        List<TProject> kept = new ArrayList<>();
        for (TProject p : projects) {
            String tenantName = tenantResolver.tenantNameOf(p.getRegionCode(), p);
            Integer tenantId = tenantName == null ? null : tenantIdByName.get(tenantName);
            if (tenantId != null && allowed.contains(tenantId)) {
                kept.add(p);
            }
        }
        return kept;
    }

    // ------------------------------------------------------------------

    private FarmerStationListVO toVO(TFarmerStation e, Map<Long, TProject> projectMap) {
        FarmerStationListVO vo = new FarmerStationListVO()
                .setId(e.getId())
                .setStationCode(e.getStationCode())
                .setFarmerName(e.getFarmerName())
                .setInstallAddr(e.getInstallAddr())
                .setTotalCapacity(e.getTotalCapacity())
                .setPowerUserNo(e.getPowerUserNo())
                .setGridDate(e.getGridDate())
                .setRentDeadline(e.getRentDeadline())
                .setTotalBlocks(e.getTotalBlocks())
                .setProjectId(e.getProjectId())
                .setRegionCode(e.getRegionCode());

        TProject p = projectMap.get(e.getProjectId());
        if (p != null) {
            vo.setProjectName(p.getProjectName());
            // 天津和廊坊共用库里的 region_code=TJLF、region_name=天津廊坊，
            // 列表上要按业务口径拆开显示
            vo.setRegionCode(regionResolver.regionCodeOf(p));
            vo.setRegionName(regionResolver.regionNameOf(p));
        }

        // 导入时靠占位值兜住的行，列表上标出来提醒运营补录
        boolean placeholder = (e.getPowerUserNo() != null && e.getPowerUserNo().startsWith(NO_USER_NO_PREFIX))
                || NO_NAME.equals(e.getFarmerName())
                || e.getGridDate() == null;
        vo.setNeedCheck(placeholder ? 1 : 0);
        // 需求29：列表和导出都要显示审核状态
        vo.setAuditStatus(e.getAuditStatus())
                .setAuditStatusName(TFarmerStation.auditStatusName(e.getAuditStatus()))
                .setAuditSubmitTime(e.getAuditSubmitTime());
        return vo;
    }

    private Map<Long, TProject> loadProjectMap() {
        Map<Long, TProject> map = new HashMap<>();
        for (TProject p : projectService.list()) {
            map.put(p.getId(), p);
        }
        return map;
    }

    /**
     * 排序全部走 Lambda 列引用，不拼任何字符串 SQL，前端传什么都注入不进来
     */
    private void applySort(LambdaQueryWrapper<TFarmerStation> w, FarmerStationQueryDTO query) {
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        String field = query.getSortField() == null ? "" : query.getSortField();
        switch (field) {
            case "gridDate":
                w.orderBy(true, asc, TFarmerStation::getGridDate);
                break;
            case "rentDeadline":
                w.orderBy(true, asc, TFarmerStation::getRentDeadline);
                break;
            case "totalCapacity":
                w.orderBy(true, asc, TFarmerStation::getTotalCapacity);
                break;
            case "totalBlocks":
                w.orderBy(true, asc, TFarmerStation::getTotalBlocks);
                break;
            case "createTime":
                w.orderBy(true, asc, TFarmerStation::getCreateTime);
                break;
            case "id":
                w.orderBy(true, asc, TFarmerStation::getId);
                break;
            default:
                // 默认按创建时间倒序
                w.orderByDesc(TFarmerStation::getCreateTime);
                break;
        }
        // 同值时再按 id 兜底，保证翻页顺序稳定不重不漏
        w.orderByDesc(TFarmerStation::getId);
    }

    private int normalizeSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        // 挡住 pageSize=99999 这种把整表拉出来的请求
        return Math.min(pageSize, 500);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
