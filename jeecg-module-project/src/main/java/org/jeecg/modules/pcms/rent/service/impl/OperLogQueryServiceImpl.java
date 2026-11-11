package org.jeecg.modules.pcms.rent.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.rent.dto.OperLogVO;
import org.jeecg.modules.pcms.rent.entity.TInstallInfo;
import org.jeecg.modules.pcms.rent.entity.TInstallType;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TProject;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.entity.TRentPriceType;
import org.jeecg.modules.pcms.rent.service.IOperLogQueryService;
import org.jeecg.modules.pcms.rent.service.ITInstallInfoService;
import org.jeecg.modules.pcms.rent.service.ITInstallTypeService;
import org.jeecg.modules.pcms.rent.service.ITOperLogService;
import org.jeecg.modules.pcms.rent.service.ITProjectService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.service.ITRentPriceTypeService;
import org.jeecg.modules.pcms.rent.util.OperLogFormatter;
import org.jeecg.modules.pcms.rent.util.RentRegionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志查询实现。
 *
 * 日志按"业务类型 + 业务主键"存，同一个页面上要看的往往横跨几种业务
 * （农户详情页要同时看农户、安装信息、租金台账的改动），
 * 所以这里先把相关的业务主键收齐，再一次查出来按时间倒序给前端。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class OperLogQueryServiceImpl implements IOperLogQueryService {

    /** 一个农户的安装信息、台账再多也不会到这个量级，兜底防止 IN 列表爆掉 */
    private static final int MAX_IN_SIZE = 2000;

    @Autowired
    private ITOperLogService operLogService;
    @Autowired
    private ITInstallInfoService installInfoService;
    @Autowired
    private ITRentLedgerService ledgerService;

    // 下面几个是日志摘要里外键 ID 翻名称用的字典表。
    // 都是小字典（项目、安装房型、租金档位），整表读一次比逐条按 ID 查更省事，
    // 也和 FarmerStationQueryServiceImpl 里 loadProjectMap 的取法保持一致。
    @Autowired
    private ITProjectService projectService;
    @Autowired
    private ITInstallTypeService installTypeService;
    @Autowired
    private ITRentPriceTypeService rentPriceTypeService;
    @Autowired
    private RentRegionResolver regionResolver;

    @Override
    public IPage<OperLogVO> pageList(String bizType, Long bizId, String operUser, int pageNo, int pageSize) {
        LambdaQueryWrapper<TOperLog> w = new LambdaQueryWrapper<>();
        w.eq(notBlank(bizType), TOperLog::getBizType, bizType);
        w.eq(bizId != null, TOperLog::getBizId, bizId);
        w.like(notBlank(operUser), TOperLog::getOperUser, operUser);
        w.orderByDesc(TOperLog::getOperTime).orderByDesc(TOperLog::getId);

        Page<TOperLog> page = new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        return toVOPage(operLogService.page(page, w));
    }

    @Override
    public IPage<OperLogVO> pageByStation(Long stationId, int pageNo, int pageSize) {
        if (stationId == null) {
            return new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        }
        List<Long> installIds = installIdsOfStation(stationId);
        List<Long> ledgerIds = new ArrayList<>();
        for (TRentLedger l : ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .select(TRentLedger::getId)
                .eq(TRentLedger::getStationId, stationId)
                .last("limit " + MAX_IN_SIZE))) {
            ledgerIds.add(l.getId());
        }

        LambdaQueryWrapper<TOperLog> w = new LambdaQueryWrapper<>();
        w.and(q -> {
            q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_FARMER).eq(TOperLog::getBizId, stationId));
            q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_LEDGER).eq(TOperLog::getBizId, stationId));
            if (!installIds.isEmpty()) {
                q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_INSTALL).in(TOperLog::getBizId, installIds));
                // 租金单价日志的业务主键是安装信息ID，不加这条租金改动在页面上就看不到
                q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_RENT).in(TOperLog::getBizId, installIds));
            }
            if (!ledgerIds.isEmpty()) {
                q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_LEDGER).in(TOperLog::getBizId, ledgerIds));
                q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_ATTACH).in(TOperLog::getBizId, ledgerIds));
            }
        });
        w.orderByDesc(TOperLog::getOperTime).orderByDesc(TOperLog::getId);

        Page<TOperLog> page = new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        return toVOPage(operLogService.page(page, w));
    }

    @Override
    public IPage<OperLogVO> pageByLedger(Long ledgerId, int pageNo, int pageSize) {
        if (ledgerId == null) {
            return new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        }
        // 租金单价日志挂在安装信息ID下，台账页要看到它得先绕到该农户的安装信息
        TRentLedger ledger = ledgerService.getById(ledgerId);
        List<Long> installIds = ledger == null
                ? Collections.emptyList() : installIdsOfStation(ledger.getStationId());

        LambdaQueryWrapper<TOperLog> w = new LambdaQueryWrapper<>();
        w.and(q -> {
            q.or(sub -> sub.eq(TOperLog::getBizId, ledgerId)
                    .in(TOperLog::getBizType, TOperLog.BIZ_LEDGER, TOperLog.BIZ_ATTACH));
            if (!installIds.isEmpty()) {
                q.or(sub -> sub.eq(TOperLog::getBizType, TOperLog.BIZ_RENT).in(TOperLog::getBizId, installIds));
            }
        });
        w.orderByDesc(TOperLog::getOperTime).orderByDesc(TOperLog::getId);

        Page<TOperLog> page = new Page<>(Math.max(pageNo, 1), normalizeSize(pageSize));
        return toVOPage(operLogService.page(page, w));
    }

    // ------------------------------------------------------------------

    /** 该农户下所有安装信息的ID，租金单价日志按它挂 */
    private List<Long> installIdsOfStation(Long stationId) {
        List<Long> installIds = new ArrayList<>();
        if (stationId == null) {
            return installIds;
        }
        for (TInstallInfo i : installInfoService.list(new LambdaQueryWrapper<TInstallInfo>()
                .select(TInstallInfo::getId)
                .eq(TInstallInfo::getStationId, stationId)
                .last("limit " + MAX_IN_SIZE))) {
            installIds.add(i.getId());
        }
        return installIds;
    }

    /** 整页转换前先把字典查好，避免每条日志各查一次库（N+1） */
    private IPage<OperLogVO> toVOPage(IPage<TOperLog> page) {
        OperLogFormatter.ValueResolver resolver = buildResolver();
        return page.convert(e -> toVO(e, resolver));
    }

    private OperLogVO toVO(TOperLog e, OperLogFormatter.ValueResolver resolver) {
        return new OperLogVO()
                .setId(e.getId())
                .setOperUser(e.getOperUser())
                .setOperTime(e.getOperTime())
                .setAction(e.getAction())
                .setActionName(OperLogFormatter.actionName(e.getAction()))
                .setBizType(e.getBizType())
                .setBizId(e.getBizId())
                .setSummary(OperLogFormatter.summary(e, resolver))
                .setDetail(e.getOperContent())
                .setIp(e.getIp());
    }

    /**
     * 建「外键 ID -> 名称」翻译器。
     *
     * 日志摘要里几个字段存的是 ID 或编码（所属项目、安装房型、租金档位、所属大区），
     * 直接展示出来运营看不懂（"所属项目=6"），这里统一翻成名称。
     */
    private OperLogFormatter.ValueResolver buildResolver() {
        Map<Long, String> projectNames = new HashMap<>();
        Map<String, String> regionNames = new HashMap<>();
        for (TProject p : projectService.list()) {
            projectNames.put(p.getId(), p.getProjectName());
            // 天津和廊坊在库里共用 region_code=TJLF，regionCodeOf 已按项目编码前缀拆开
            String code = regionResolver.regionCodeOf(p);
            String name = regionResolver.regionNameOf(p);
            if (code != null && !code.trim().isEmpty() && name != null && !name.trim().isEmpty()) {
                regionNames.putIfAbsent(code.trim().toUpperCase(), name);
            }
        }

        Map<Long, String> installTypeNames = new HashMap<>();
        for (TInstallType t : installTypeService.list()) {
            installTypeNames.put(t.getId(), t.getTypeName());
        }

        Map<Long, String> priceTypeNames = new HashMap<>();
        for (TRentPriceType t : rentPriceTypeService.list()) {
            priceTypeNames.put(t.getId(), t.getTypeName());
        }

        return (field, value) -> {
            switch (field) {
                case "projectId":
                    return projectNames.get(asLong(value));
                case "installTypeId":
                    return installTypeNames.get(asLong(value));
                case "priceTypeId":
                    return priceTypeNames.get(asLong(value));
                case "regionCode":
                    return regionNames.get(String.valueOf(value).trim().toUpperCase());
                default:
                    return null;
            }
        };
    }

    /** JSON 里的数字可能是 Integer / Long / String，统一取成 Long；取不到返回 null */
    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeSize(int pageSize) {
        if (pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
