package org.jeecg.modules.system.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.entity.SysRole;
import org.jeecg.modules.system.entity.SysUserRole;
import org.jeecg.modules.system.service.ISysRoleService;
import org.jeecg.modules.system.service.ISysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 固定角色护栏：业务专员、财务专员、公司领导、运维。
 *
 * 【规则】（项目经理 2026-11 定的）
 *   1. 其他角色不能把用户改成这四个角色 —— 不能再往这些角色里加人；
 *   2. 已经是这四个角色之一的用户，也不能被改成其他角色；
 *   3. 这四个角色本身不允许删除、不允许改 role_code。
 * 也就是说：这四个角色的成员关系一经建立就固定，只能由 DBA 直接改库。
 *
 * 【为什么按 role_code 认角色而不是写死 id】
 * sys_role.id 在测试库和生产库不一样（运维的 id 就已经错了位），
 * 写死 id 换个环境就会锁错角色或者锁不住。role_code 两边一致。
 *
 * 【为什么不放在 @RequiresRoles 上】
 * 这是"改谁的什么"的业务规则，不是"谁能进这个方法"，
 * 所以放在保存动作里校验，所有入口（新增、编辑、角色侧加人/删人）都要过一遍。
 *
 * @Version: V1.0
 */
@Component
public class FixedRoleGuard {

    /** 固定角色的 role_code */
    private static final Set<String> FIXED_ROLE_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("businessSpecialist", "financeSpecialist", "companyLeader", "Y001")));

    /** 报错文案里的角色名，和上面一一对应 */
    private static final String FIXED_ROLE_NAMES = "业务专员/财务专员/公司领导/运维";

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysUserRoleService userRoleService;

    /** 当前环境里这四个固定角色的 id 集合 */
    public Set<String> fixedRoleIds() {
        List<SysRole> roles = roleService.list(new QueryWrapper<SysRole>()
                .in("role_code", FIXED_ROLE_CODES));
        Set<String> ids = new HashSet<>();
        if (roles != null) {
            for (SysRole r : roles) {
                if (r.getId() != null) {
                    ids.add(r.getId());
                }
            }
        }
        return ids;
    }

    public boolean isFixedRoleId(String roleId) {
        return roleId != null && fixedRoleIds().contains(roleId.trim());
    }

    /**
     * 保存用户角色前校验。
     *
     * @param userId  被编辑的用户，新增时传 null
     * @param roleIds 本次要保存的角色ID集合
     */
    public void checkRoleAssignment(String userId, Set<String> roleIds) {
        Set<String> fixed = fixedRoleIds();
        if (fixed.isEmpty()) {
            // 字典里没有这几个角色（比如新环境还没初始化），不拦
            return;
        }
        Set<String> requested = roleIds == null ? new HashSet<>() : roleIds;
        Set<String> current = userId == null ? new HashSet<>() : currentRoleIds(userId);

        boolean wasFixed = current.stream().anyMatch(fixed::contains);
        boolean willBeFixed = requested.stream().anyMatch(fixed::contains);

        if (wasFixed) {
            // 角色没动就放行（改手机号、姓名这些照常能改）
            if (!current.equals(requested)) {
                throw new JeecgBootException("该用户是" + FIXED_ROLE_NAMES + "之一，角色固定，不能改成其他角色");
            }
            return;
        }
        if (willBeFixed) {
            throw new JeecgBootException(FIXED_ROLE_NAMES + "是固定角色，不能分配给其他用户");
        }
    }

    /** 逗号串版本，控制器拿到前端参数直接传 */
    public void checkRoleAssignment(String userId, String csvRoleIds) {
        checkRoleAssignment(userId, split(csvRoleIds));
    }

    /** 固定角色不允许删除 */
    public void checkDeletable(String roleId) {
        if (isFixedRoleId(roleId)) {
            throw new JeecgBootException(FIXED_ROLE_NAMES + "是固定角色，不能删除");
        }
    }

    /** 固定角色不允许改 role_code */
    public void checkCodeUnchanged(String roleId, String newCode) {
        if (!isFixedRoleId(roleId) || newCode == null) {
            return;
        }
        List<SysRole> roles = roleService.list(new QueryWrapper<SysRole>().eq("id", roleId));
        SysRole old = roles.isEmpty() ? null : roles.get(0);
        if (old != null && old.getRoleCode() != null && !old.getRoleCode().equals(newCode)) {
            throw new JeecgBootException(FIXED_ROLE_NAMES + "是固定角色，不能修改角色编码");
        }
    }

    private Set<String> currentRoleIds(String userId) {
        Set<String> ids = new HashSet<>();
        List<SysUserRole> list = userRoleService.list(new QueryWrapper<SysUserRole>().eq("user_id", userId));
        if (list != null) {
            for (SysUserRole ur : list) {
                if (ur.getRoleId() != null) {
                    ids.add(ur.getRoleId());
                }
            }
        }
        return ids;
    }

    private Set<String> split(String csv) {
        Set<String> ids = new HashSet<>();
        if (csv == null) {
            return ids;
        }
        for (String s : csv.split(",")) {
            if (s != null && !s.trim().isEmpty()) {
                ids.add(s.trim());
            }
        }
        return ids;
    }
}
