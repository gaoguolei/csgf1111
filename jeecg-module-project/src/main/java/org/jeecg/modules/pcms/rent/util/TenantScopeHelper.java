package org.jeecg.modules.pcms.rent.util;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 租户数据范围的统一入口。需求第14条：租金数据按租户隔离。
 *
 * 【规则】与项目模块（ApiTaskListController）保持同一套惯例：
 *   rel_tenant_ids 有值  → 普通用户 → 强制只查自己租户的数据，前端传什么都不认
 *   rel_tenant_ids 为空  → 租户管理员 / 超级管理员 → 按前端顶部租户下拉传的 renterId 过滤；
 *                          renterId 也没传时才不过滤（看全部）
 *   业务专员 / 财务专员 / 公司领导 → 跳过 rel_tenant_ids 的强制绑定，其余同上：
 *                         顶部下拉选了哪个租户就看哪个，不选看全部
 *
 * 【为什么看 renterId 而不是看"是不是管理员"】
 * 管理员也有"切到某个租户下看"的场景，切了就该只看那个租户。
 * 只按角色判断会让顶部租户下拉对租金模块失效——切了跟没切一样。
 *
 * 【为什么要抽成公用类】
 * 数据隔离最怕的不是漏做，而是"有的接口做了、有的忘了"，导致
 * 天津的用户在农户列表看不到北京的数据、换个页签却看到了。
 * 所有查询都走 apply()，就不会有漏网的。
 *
 * @Version: V1.0
 */
@Component
public class TenantScopeHelper {

    /** 前端顶部租户下拉传过来的参数名，与项目模块保持一致 */
    private static final String TENANT_PARAM = "renterId";

    @Autowired
    private RentRoleGuard roleGuard;

    /**
     * 当前用户能看到的租户 id 列表。
     * null 表示"不过滤"，空列表表示"什么都看不到"。
     */
    public List<Integer> allowedTenantIds() {
        LoginUser user = currentUser();
        if (user == null) {
            // 定时任务等无登录态的场景：不隔离，按系统内部调用处理
            return null;
        }
        // 业务/财务/公司领导是集团职能角色，绑定哪个租户都不收窄，
        // 但顶部租户下拉选了谁还是要按谁筛——否则选了「海南项目」还是出全部数据
        boolean crossTenant = roleGuard.isCrossTenantRole();
        String relTenantIds = user.getRelTenantIds();
        if (!crossTenant && relTenantIds != null && !relTenantIds.trim().isEmpty()) {
            // 普通用户：强制自己的租户
            return parse(relTenantIds);
        }
        // 租户管理员 / 超管：跟随前端顶部租户下拉
        String picked = requestParam(TENANT_PARAM);
        if (picked == null || picked.trim().isEmpty()) {
            // 没传就不过滤，看全部（需求14：管理员可以查看所有数据）
            return null;
        }
        return parse(picked);
    }

    /**
     * 给查询条件追加租户过滤。
     *
     * @param tenantColumn 实体上的租户列，如 TFarmerStation::getTenantId
     */
    public <T> void apply(LambdaQueryWrapper<T> w, SFunction<T, ?> tenantColumn) {
        List<Integer> ids = allowedTenantIds();
        if (ids == null) {
            return;
        }
        if (ids.isEmpty()) {
            // 传了租户但一个都没解析出来：宁可查不到，也不能把别人的数据放出去
            w.eq(tenantColumn, -1);
            return;
        }
        w.in(tenantColumn, ids.toArray());
    }

    /** 单条数据是否在当前用户的租户范围内，用于详情/其他操作的越权校验 */
    public boolean canAccess(Integer tenantId) {
        List<Integer> ids = allowedTenantIds();
        return ids == null || (tenantId != null && ids.contains(tenantId));
    }

    /** 逗号分隔的租户 id 串解析成列表，配错的单个值忽略 */
    private List<Integer> parse(String csv) {
        List<Integer> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.valueOf(s));
            } catch (NumberFormatException e) {
                // 配错的租户 id 直接忽略，不让它把整条查询带崩
            }
        }
        return ids;
    }

    /** 读当前请求的查询参数，非 Web 线程返回 null */
    private String requestParam(String name) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest().getParameter(name);
        } catch (Exception e) {
            return null;
        }
    }

    private LoginUser currentUser() {
        try {
            if (SecurityUtils.getSubject() == null) {
                return null;
            }
            Object principal = SecurityUtils.getSubject().getPrincipal();
            return principal instanceof LoginUser ? (LoginUser) principal : null;
        } catch (Exception e) {
            // 非 Shiro 环境（如定时任务）忽略
            return null;
        }
    }
}
