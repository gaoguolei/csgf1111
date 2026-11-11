package org.jeecg.modules.pcms.rent.security;

import java.util.Collections;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 租金模块的角色校验。
 *
 * 【为什么放在服务层】
 * 前端菜单只能挡住"看不见"，直接调接口照样能改数据。角色校验放在 Service
 * 实现里，Controller、短信/API 入口、批量调用都会走到同一道闸。
 *
 * 【角色划分】
 *   管理员 admin                 ：农户维护 + 租金生成/修改/提交 + 项目审核
 *   项目经理 manager              ：同上
 *   业务专员 businessSpecialist    ：租金/农户业务审核、支付、对账；租金操作全放开
 *   财务专员 financeSpecialist     ：只做财务审核，不能生成/修改租金，也不维护农户
 *   公司领导 companyLeader         ：领导审核；租金操作全放开
 *
 * 【跨租户】
 *   业务专员、财务专员、公司领导是集团职能角色，数据范围不按租户隔离
 *   （见 TenantScopeHelper），账号绑定哪个租户都能访问全部大区。
 *   管理员、项目经理仍按各自租户隔离。
 *
 * 【没有登录主体时放行】
 * 定时任务（每年生成台账）、异步线程里 Shiro 拿不到 principal，这类内部调用
 * 不该被拦，否则定时任务会直接失败。
 *
 * @Version: V2.0
 */
@Component
public class RentRoleGuard {

    /** 系统内置管理员 */
    public static final String ROLE_ADMIN = "admin";
    /** 项目经理 */
    public static final String ROLE_PM = "manager";
    /**
     * 「项目经理」角色在线上库里实际的 role_code。
     * 数据库里角色名和 code 是错位的：「管理员」用了 manager，「项目经理」用的是 Leader。
     * 两个都当项目经理认，否则真实的项目经理账号一律被判无权。
     */
    public static final String ROLE_PM_LEGACY = "Leader";
    /** 业务专员 */
    public static final String ROLE_BUSINESS = "businessSpecialist";
    /** 财务专员 */
    public static final String ROLE_FINANCE = "financeSpecialist";
    /** 公司领导 */
    public static final String ROLE_LEADER = "companyLeader";

    @Autowired
    private ISysUserService sysUserService;

    /** 当前线程有没有登录用户。定时任务、异步线程没有，返回 false */
    public boolean hasLoginUser() {
        return loginUser() != null;
    }

    /** 当前登录用户的角色编码集合，取不到时返回空集合 */
    public Set<String> currentRoles() {
        LoginUser user = loginUser();
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Collections.emptySet();
        }
        try {
            Set<String> roles = sysUserService.getUserRolesSet(user.getUsername());
            return roles == null ? Collections.emptySet() : roles;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /** 管理员或项目经理 */
    public boolean isAdminOrPm() {
        return isAdminOrPm(currentRoles());
    }

    private boolean isAdminOrPm(Set<String> roles) {
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_PM) || roles.contains(ROLE_PM_LEGACY);
    }

    /** 超级管理员：所有角色判断一律放行，不受审核阶段和专员角色限制 */
    private boolean isSuperAdmin(Set<String> roles) {
        return roles.contains(ROLE_ADMIN);
    }

    /** 业务专员 */
    public boolean isBusiness() {
        return currentRoles().contains(ROLE_BUSINESS);
    }

    /** 财务专员 */
    public boolean isFinance() {
        return currentRoles().contains(ROLE_FINANCE);
    }

    /** 公司领导 */
    public boolean isLeader() {
        return currentRoles().contains(ROLE_LEADER);
    }

    /**
     * 跨租户角色：业务专员、财务专员、公司领导是集团层面的职能角色，
     * 工作范围天然覆盖所有大区，所以账号绑定的租户不限制数据范围
     * （如北京的业务员可以处理天津的租金）；顶部租户下拉仍可用来收窄。
     * 管理员/项目经理照旧按绑定租户隔离。
     */
    public boolean isCrossTenantRole() {
        Set<String> roles = currentRoles();
        return roles.contains(ROLE_BUSINESS)
                || roles.contains(ROLE_FINANCE)
                || roles.contains(ROLE_LEADER);
    }

    /**
     * 农户维护：新增、修改、删除、安装信息、导入。
     * 需求：只有项目经理、管理员可以录入或修改农户信息。
     */
    public void requireFarmerOps(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!isAdminOrPm(roles)) {
            throw denied(action, "管理员或项目经理");
        }
    }

    /**
     * 农户审核：项目审核由管理员/项目经理，业务审核由业务专员，领导审核由公司领导。
     */
    public void requireFarmerAudit(String stage, String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (STAGE_PROJECT.equals(stage)) {
            if (!isAdminOrPm(roles)) {
                throw denied(action, "管理员或项目经理");
            }
            return;
        }
        if (STAGE_BUSINESS.equals(stage)) {
            if (!roles.contains(ROLE_BUSINESS)) {
                throw denied(action, "业务专员");
            }
            return;
        }
        if (!roles.contains(ROLE_LEADER)) {
            throw denied(action, "公司领导");
        }
    }

    /** 审核阶段编码，与审核记录里的 audit_stage 保持一致 */
    public static final String STAGE_PROJECT = "PROJECT";
    public static final String STAGE_BUSINESS = "BUSINESS";
    public static final String STAGE_FINANCE = "FINANCE";
    public static final String STAGE_LEADER = "LEADER";

    /**
     * 租金生成、重算、修改、作废、纠纷标记、提交审核、档位维护。
     * 业务专员、公司领导按"拥有所有权限"同样放开；财务专员不参与这些操作。
     */
    public void requireLedgerOps(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!isAdminOrPm(roles)
                && !roles.contains(ROLE_BUSINESS) && !roles.contains(ROLE_LEADER)) {
            throw denied(action, "管理员、项目经理、业务专员或公司领导");
        }
    }

    /** 项目审核：只有管理员、项目经理，也就是提交人自己做第一级审核 */
    public void requireProjectAudit(String action) {
        if (!hasLoginUser()) {
            return;
        }
        if (!isAdminOrPm()) {
            throw denied(action, "管理员或项目经理");
        }
    }

    /** 业务审核：只有业务专员 */
    public void requireBusinessAudit(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!roles.contains(ROLE_BUSINESS)) {
            throw denied(action, "业务专员");
        }
    }

    /** 财务审核：只有财务专员 */
    public void requireFinanceAudit(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!roles.contains(ROLE_FINANCE)) {
            throw denied(action, "财务专员");
        }
    }

    /** 领导审核：只有公司领导 */
    public void requireLeaderAudit(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!roles.contains(ROLE_LEADER)) {
            throw denied(action, "公司领导");
        }
    }

    /** 标记支付、对账：只有业务专员 */
    public void requirePaymentOps(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (isSuperAdmin(roles)) {
            return;
        }
        if (!roles.contains(ROLE_BUSINESS)) {
            throw denied(action, "业务专员");
        }
    }

    /** 审核过程材料：四级审核人都能传 */
    public void requireAuditOps(String action) {
        if (!hasLoginUser()) {
            return;
        }
        Set<String> roles = currentRoles();
        if (!isAdminOrPm(roles)
                && !roles.contains(ROLE_BUSINESS)
                && !roles.contains(ROLE_FINANCE) && !roles.contains(ROLE_LEADER)) {
            throw denied(action, "审核人");
        }
    }

    /** 当前用户是否为租金模块的只读角色（财务专员），前端按钮显隐也能用 */
    public boolean isFinanceOnly() {
        Set<String> roles = currentRoles();
        return roles.contains(ROLE_FINANCE)
                && !isAdminOrPm(roles)
                && !roles.contains(ROLE_BUSINESS)
                && !roles.contains(ROLE_LEADER);
    }

    private JeecgBootException denied(String action, String who) {
        return new JeecgBootException("当前角色无权" + (action == null ? "执行该操作" : action)
                + "，" + who + "可操作");
    }

    private LoginUser loginUser() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Object principal = subject == null ? null : subject.getPrincipal();
            return principal instanceof LoginUser ? (LoginUser) principal : null;
        } catch (Exception e) {
            // 没有 Shiro 上下文的线程（定时任务/异步任务）
            return null;
        }
    }
}
