package org.jeecg.modules.pcms.rent.util;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 租金模块的"大区"口径。
 *
 * 【为什么需要这个类】
 * 数据库里天津和廊坊共用一个大区编码 TJLF（t_project.region_code='TJLF'），
 * 但业务上它们是大区级别的两个独立单元——租户分开、下拉框要分开、数据要分开。
 * 于是对外（下拉框、筛选条件）把它们拆成两个编码：
 *
 *   TJLF → 天津（t_project.project_code 以 TJ_ 开头）
 *   LF   → 廊坊（t_project.project_code 以 LF_ 开头）
 *
 * 【为什么不直接改库里的 region_code】
 * t_install_type 和 t_rent_price_type 这两张字典表也是按 region_code 分的，
 * 天津和廊坊共用同一套安装房型和租金档位。改 region_code 就得把这两套字典
 * 复制一份，牵连太大。所以选择在查询层做映射，库里的数据不动。
 *
 * 【兼容字典编码】
 * sys_dict 里已经有一套大区字典（BEIJ/TIANJ/LF/XUZ/HAIN/HEN），
 * 前端如果拿字典编码来筛，这里也一并认，免得两边对不上。
 *
 * @Version: V1.0
 */
@Component
public class RentRegionResolver {

    /** 天津。库里就是 TJLF，只是语义上收窄成"天津"，不再包含廊坊 */
    public static final String TIANJIN = "TJLF";
    /** 廊坊。库里没有这个编码，是拆出来的对外编码 */
    public static final String LANGFANG = "LF";

    private static final String PROJECT_CODE_TJ = "TJ\\_%";
    private static final String PROJECT_CODE_LF = "LF\\_%";

    @Autowired
    private ITProjectService projectService;

    /**
     * 把前端传的大区编码换成该项目列表。
     *
     * 认三种写法：
     *   1. 库里的大区编码 BJ / TJLF / XZ / HAIN / HENAN（TJLF 只算天津）
     *   2. 拆出来的廊坊编码 LF
     *   3. sys_dict 里的大区字典编码 BEIJ / TIANJ / LF / XUZ / HAIN / HEN
     *
     * 空列表表示该大区下没有项目，调用方应当直接返回空结果。
     */
    public List<Long> projectIds(String regionCode) {
        String code = normalize(regionCode);
        if (code == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<TProject> w = new LambdaQueryWrapper<>();
        if (LANGFANG.equals(code)) {
            // 廊坊：靠项目编码前缀认
            w.likeRight(TProject::getProjectCode, "LF_");
        } else if (TIANJIN.equals(code)) {
            // 天津：TJLF 大区里排除掉廊坊
            w.eq(TProject::getRegionCode, "TJLF")
                    .likeRight(TProject::getProjectCode, "TJ_");
        } else {
            w.eq(TProject::getRegionCode, code);
        }
        List<Long> ids = new ArrayList<>();
        for (TProject p : projectService.list(w)) {
            ids.add(p.getId());
        }
        return ids;
    }

    /** 归一化大区编码：去掉空白，把字典编码折算成库里的编码 */
    public String normalize(String regionCode) {
        if (regionCode == null || regionCode.trim().isEmpty()) {
            return null;
        }
        String code = regionCode.trim().toUpperCase();
        switch (code) {
            case "BEIJ":
                return "BJ";
            case "TIANJ":
                return TIANJIN;
            case "XUZ":
                return "XZ";
            case "HEN":
                return "HENAN";
            default:
                return code;
        }
    }

    /** 天津的项目（下拉框和筛选都要按这个口径） */
    public boolean isLangfang(String regionCode) {
        return LANGFANG.equals(normalize(regionCode));
    }

    /** 这个项目是不是廊坊（TJLF 大区里 project_code 以 LF_ 开头的那个） */
    public boolean isLangfangProject(TProject p) {
        return p != null && p.getProjectCode() != null
                && p.getProjectCode().trim().toUpperCase().startsWith("LF_");
    }

    /** 这个项目是不是天津（TJLF 大区里 project_code 以 TJ_ 开头的那些） */
    public boolean isTianjinProject(TProject p) {
        return p != null && "TJLF".equalsIgnoreCase(p.getRegionCode())
                && p.getProjectCode() != null
                && p.getProjectCode().trim().toUpperCase().startsWith("TJ_");
    }

    /**
     * 项目在对外口径里属于哪个大区。
     * 库里 TJLF 同时装着天津和廊坊，这里拆成两个编码，下拉框才能分开显示。
     */
    public String regionCodeOf(TProject p) {
        if (p == null) {
            return null;
        }
        return isLangfangProject(p) ? LANGFANG : p.getRegionCode();
    }

    /**
     * 对外显示的大区名。天津和廊坊不显示库里那个合并后的"天津廊坊项目"。
     * 其余大区沿用库里的 region_name。
     */
    public String regionNameOf(TProject p) {
        if (p == null) {
            return null;
        }
        if (isLangfangProject(p)) {
            return "廊坊";
        }
        if (isTianjinProject(p)) {
            return "天津";
        }
        return p.getRegionName();
    }

    /** 天津/廊坊这两个编码之外的大区，直接按 region_code 过滤即可 */
    public boolean isPlainRegion(String regionCode) {
        String code = normalize(regionCode);
        return code != null && !LANGFANG.equals(code) && !TIANJIN.equals(code);
    }
}
