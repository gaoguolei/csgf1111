package org.jeecg.modules.pcms.rent.service;

import java.util.HashMap;
import java.util.Map;

import org.jeecg.modules.pcms.common.sys.entity.PvTenant;
import org.jeecg.modules.pcms.common.sys.mapper.PvTenantMapper;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 农户归属租户的解析。需求第14条：租金数据按租户隔离。
 *
 * 农户必须带 tenant_id 落库，否则普通用户（rel_tenant_ids 非空的）查不到自己导入的数据，
 * 只有管理员看得到——这种"数据明明导进去了却查不到"的问题最难排查，所以在写入端就卡住。
 *
 * 【为什么按名称而不是按 id 映射】
 * sys_tenant.id 在测试库和生产库不一样（测试库的天津是 102，生产库可能是别的值），
 * 写死 id 会让同一份代码换个环境就填错。租户名称是两边一致的业务标识，按名称查 id。
 *
 * 【TJLF 的特殊处理】
 * 大区与租户基本一一对应，只有 TJLF 例外：天津和廊坊是这个大区下的两个独立租户，
 * 靠项目编码前缀区分——TJ_01~TJ_05 是天津，LF_01 是廊坊。
 * 不能按 t_project.region_name 判断，因为这两个项目共用"天津廊坊项目"这个名字。
 *
 * @Version: V1.0
 */
@Component
public class RentTenantResolver {

    @Autowired
    private PvTenantMapper pvTenantMapper;

    /**
     * 租户字典：名称 -> id。sys_tenant 只有十几行，批量导入时读一次即可，
     * 不要在几万行的循环里逐行查库。
     */
    public Map<String, Integer> loadTenantIds() {
        Map<String, Integer> map = new HashMap<>();
        for (PvTenant t : pvTenantMapper.selectList(null)) {
            if (t.getName() != null) {
                map.put(t.getName().trim(), t.getId());
            }
        }
        return map;
    }

    /** 单条解析。内部现查租户表，适合只写一条农户的场景。 */
    public Integer resolve(String regionCode, TProject project) {
        return resolve(regionCode, project, loadTenantIds());
    }

    /**
     * 用调用方传入的租户字典解析，解析不到直接抛异常。
     *
     * 这里不返回 null 让调用方自己兜底：tenant_id 为空的农户在列表里是"隐形"的，
     * 与其静默写进去等用户发现少数据，不如在导入时就把问题指出来。
     */
    public Integer resolve(String regionCode, TProject project, Map<String, Integer> tenantIds) {
        if (project == null) {
            throw new IllegalArgumentException("农户没有所属项目，无法确定租户归属");
        }
        String tenantName = tenantNameOf(regionCode, project);
        if (tenantName == null) {
            throw new IllegalArgumentException("大区[" + regionCode + "]还没有配置对应的租户，"
                    + "请在 RentTenantResolver 里补上该大区的映射规则");
        }
        Integer tenantId = tenantIds == null ? null : tenantIds.get(tenantName);
        if (tenantId == null) {
            throw new IllegalArgumentException("大区[" + regionCode + "]项目[" + project.getProjectName()
                    + "]对应的租户[" + tenantName + "]在 sys_tenant 中不存在，"
                    + "无法确定数据归属，请先补建租户");
        }
        return tenantId;
    }

    /** 大区 + 项目 -> 租户名称。解析不出来返回 null，由 resolve 报错 */
    public String tenantNameOf(String regionCode, TProject project) {
        if (regionCode == null) {
            return null;
        }
        switch (regionCode.toUpperCase()) {
            case "BJ":
                return "北京项目";
            case "XZ":
                return "徐州项目";
            case "HAIN":
                return "海南项目";
            case "HENAN":
                return "河南项目";
            case "TJLF": {
                String code = project == null || project.getProjectCode() == null
                        ? "" : project.getProjectCode().trim();
                return code.toUpperCase().startsWith("LF") ? "廊坊项目" : "天津项目";
            }
            default:
                return null;
        }
    }

    /**
     * 租户ID -> 大区。上面的 tenantNameOf 反过来。
     *
     * 给"前端直接传租户ID"的场景用：农户详情和列表的房型/档位下拉只跟大区有关，
     * 知道租户就够了 —— 前端顶部租户栏给的就是 sys_tenant.id。
     * 天津和廊坊是两个租户但同属 TJLF 大区，选项本来也一样，所以不区分。
     *
     * @return 大区编码；这个租户ID不认识时返回 null
     */
    public String regionOfTenant(Integer tenantId) {
        if (tenantId == null) {
            return null;
        }
        String tenantName = null;
        for (Map.Entry<String, Integer> e : loadTenantIds().entrySet()) {
            if (tenantId.equals(e.getValue())) {
                tenantName = e.getKey();
                break;
            }
        }
        if (tenantName == null) {
            return null;
        }
        switch (tenantName) {
            case "北京项目":
                return "BJ";
            case "徐州项目":
                return "XZ";
            case "海南项目":
            case "海南二期项目":
                return "HAIN";
            case "河南项目":
                return "HENAN";
            case "天津项目":
            case "廊坊项目":
                // 两个租户同属 TJLF 大区，房型和档位本来就是同一套
                return "TJLF";
            default:
                return null;
        }
    }
}
