package org.jeecg.modules.pcms.rent.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.entity.TFarmerChangeRequest;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TInstallInfo;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.entity.TRent;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.service.IFarmerAuditService;
import org.jeecg.modules.pcms.rent.service.IFarmerStationBizService;
import org.jeecg.modules.pcms.rent.service.ITFarmerChangeRequestService;
import org.jeecg.modules.pcms.rent.service.ITFarmerStationService;
import org.jeecg.modules.pcms.rent.service.ITInstallInfoService;
import org.jeecg.modules.pcms.rent.service.ITRentLedgerService;
import org.jeecg.modules.pcms.rent.service.ITRentService;
import org.jeecg.modules.pcms.rent.security.RentRoleGuard;
import org.jeecg.modules.pcms.rent.util.OperLogHelper;
import org.jeecg.modules.pcms.rent.util.RentPeriodUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 农户信息变更审核实现。需求 28~33。
 *
 * 【为什么修改是"立即生效 + 快照回滚"而不是"暂存等审核"】
 *   1. 审核弹窗要展示"改前/改后"，存整份快照最直观；
 *   2. 待审核期间数据要能被审核人看到、被驳回后能继续改，暂存反而多一层状态；
 *   3. 回滚用快照整体覆盖，不用处理字段级的增删差异。
 *
 * 【锁定边界】
 *   某期只要还没结束（periodEnd >= 提交日）就锁 —— 提交当天的期也算"提交之后"。
 *   已结束的期（比如 9 月底就结束的第 3 期）不受影响，可以正常操作。
 *
 * @Version: V1.0
 */
@Slf4j
@Service
public class FarmerAuditServiceImpl implements IFarmerAuditService {

    @Autowired
    private ITFarmerChangeRequestService requestService;
    @Autowired
    private ITFarmerStationService farmerStationService;
    @Autowired
    private ITInstallInfoService installInfoService;
    @Autowired
    private ITRentService rentService;
    @Autowired
    private ITRentLedgerService ledgerService;
    @Autowired
    private RentRoleGuard roleGuard;
    @Autowired
    private OperLogHelper operLog;

    /** 农户服务反过来依赖本类做审核提交，这里加 @Lazy 打断循环依赖 */
    @Lazy
    @Autowired
    private IFarmerStationBizService farmerStationBizService;

    @Override
    public boolean isPending(Long stationId) {
        return pendingOf(stationId) != null;
    }

    @Override
    public TFarmerChangeRequest pendingOf(Long stationId) {
        if (stationId == null) {
            return null;
        }
        return requestService.getOne(new LambdaQueryWrapper<TFarmerChangeRequest>()
                .eq(TFarmerChangeRequest::getStationId, stationId)
                .eq(TFarmerChangeRequest::getStatus, TFarmerChangeRequest.STATUS_PENDING)
                .orderByDesc(TFarmerChangeRequest::getId)
                .last("limit 1"), false);
    }

    @Override
    public TFarmerChangeRequest getById(Long requestId) {
        if (requestId == null) {
            throw new JeecgBootException("缺少审核申请ID");
        }
        TFarmerChangeRequest req = requestService.getById(requestId);
        if (req == null) {
            throw new JeecgBootException("审核申请不存在");
        }
        return req;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitChange(Long stationId, String changeType, String beforeJson, String afterJson) {
        if (stationId == null) {
            throw new JeecgBootException("缺少农户ID");
        }
        // 上一次变更还没审完，不允许再叠加一次
        if (isPending(stationId)) {
            throw new JeecgBootException("该农户还有一次变更正在审核中，请等审核结果或取消后再提交");
        }
        Date now = new Date();
        String user = operLog.currentUser();
        boolean insert = TFarmerChangeRequest.TYPE_INSERT.equals(changeType);

        requestService.save(new TFarmerChangeRequest()
                .setStationId(stationId)
                .setChangeType(changeType)
                .setStatus(TFarmerChangeRequest.STATUS_PENDING)
                .setAuditStage(TFarmerChangeRequest.STAGE_PROJECT)
                .setBeforeJson(insert ? null : beforeJson)
                .setAfterJson(afterJson)
                .setSubmitBy(user)
                // 这个时间就是租金锁定的分界线，必须用提交时刻
                .setSubmitTime(now)
                .setCreateTime(now).setUpdateTime(now));

        farmerStationService.updateById(new TFarmerStation()
                .setId(stationId)
                .setAuditStatus(insert
                        ? TFarmerStation.AUDIT_INSERT_PENDING_PROJECT
                        : TFarmerStation.AUDIT_UPDATE_PENDING_PROJECT)
                .setAuditSubmitTime(now)
                .setAuditSubmitBy(user));

        operLog.recordText(TOperLog.ACTION_SUBMIT, TOperLog.BIZ_FARMER, stationId,
                "提交" + TFarmerChangeRequest.typeName(changeType) + "审核（提交项目审核）");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean mergePendingSnapshot(Long stationId, String afterJson) {
        TFarmerChangeRequest pending = pendingOf(stationId);
        if (pending == null) {
            return false;
        }
        requestService.updateById(new TFarmerChangeRequest()
                .setId(pending.getId())
                .setAfterJson(afterJson)
                .setUpdateTime(new Date()));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long requestId, String remark) {
        TFarmerChangeRequest req = getPending(requestId);
        String stage = stageOf(req);
        roleGuard.requireFarmerAudit(stage, TFarmerChangeRequest.stageName(stage) + "通过农户信息");
        Date now = new Date();
        String user = operLog.currentUser();
        boolean insert = TFarmerChangeRequest.TYPE_INSERT.equals(req.getChangeType());
        String note = blank(remark) ? null : remark.trim();

        TFarmerChangeRequest update = new TFarmerChangeRequest()
                .setId(req.getId())
                .setAuditBy(user).setAuditTime(now)
                .setAuditRemark(note)
                .setUpdateTime(now);
        if (TFarmerChangeRequest.STAGE_PROJECT.equals(stage)) {
            update.setAuditStage(TFarmerChangeRequest.STAGE_BUSINESS)
                    .setProjectAuditBy(user).setProjectAuditTime(now);
        } else if (TFarmerChangeRequest.STAGE_BUSINESS.equals(stage)) {
            update.setAuditStage(TFarmerChangeRequest.STAGE_LEADER)
                    .setBusinessAuditBy(user).setBusinessAuditTime(now);
        } else {
            // 领导审核通过：申请完结，农户恢复正常，租金解锁
            update.setStatus(TFarmerChangeRequest.STATUS_APPROVED)
                    .setLeaderAuditBy(user).setLeaderAuditTime(now);
        }
        requestService.updateById(update);

        if (TFarmerChangeRequest.STAGE_LEADER.equals(stage)) {
            farmerStationService.updateById(new TFarmerStation()
                    .setId(req.getStationId())
                    .setAuditStatus(TFarmerStation.AUDIT_NORMAL));
            // 把这次变更同步到还没提交的租金台账上（只刷快照，不重算金额）
            syncPendingLedgers(req);
        } else {
            farmerStationService.updateById(new TFarmerStation()
                    .setId(req.getStationId())
                    .setAuditStatus(TFarmerStation.pendingStatusFor(insert, stage)));
        }

        operLog.recordText(TOperLog.ACTION_APPROVE, TOperLog.BIZ_FARMER, req.getStationId(),
                TFarmerChangeRequest.stageName(stage) + "通过"
                        + TFarmerChangeRequest.typeName(req.getChangeType())
                        + (note == null ? "" : "，备注：" + note));
    }

    /**
     * 审核通过后，把农户的最新信息同步到还没提交的租金台账上。
     *
     * 只刷新持久化快照，不重算金额 —— 安装信息/单价变了之后，
     * 前端提示用户，再由前端调 /rent/ledger/regenerate 重算。
     *
     * 同步范围：同一农户、未删除、未作废、未纠纷暂停、非历史年度，
     *          且审核状态为待提交或已驳回的台账。
     * 已经在流程中的（审核中/审核通过/待支付）以及已支付、已对账的一律不动。
     */
    private void syncPendingLedgers(TFarmerChangeRequest req) {
        if (!TFarmerChangeRequest.TYPE_UPDATE.equals(req.getChangeType())
                || !changedForLedger(req.getBeforeJson(), req.getAfterJson())) {
            return;
        }
        TFarmerStation station = farmerStationService.getById(req.getStationId());
        if (station == null) {
            return;
        }
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        List<TRentLedger> rows = ledgerService.list(new LambdaQueryWrapper<TRentLedger>()
                .eq(TRentLedger::getStationId, req.getStationId())
                .eq(TRentLedger::getDelFlag, 0)
                .eq(TRentLedger::getVoidFlag, 0)
                .eq(TRentLedger::getDisputeFlag, 0)
                .ge(TRentLedger::getRentYear, thisYear)
                .and(w -> w.isNull(TRentLedger::getAuditStatus)
                        .or().in(TRentLedger::getAuditStatus,
                                TRentLedger.AUDIT_DRAFT, TRentLedger.AUDIT_PROJECT_REJECTED,
                                TRentLedger.AUDIT_BUSINESS_REJECTED,
                                TRentLedger.AUDIT_FINANCE_REJECTED,
                                TRentLedger.AUDIT_LEADER_REJECTED)));
        if (rows.isEmpty()) {
            return;
        }

        String installSnapshot = JSON.toJSONString(
                farmerStationBizService.installsOf(station.getId()));
        Date now = new Date();
        String user = operLog.currentUser();
        List<Long> ids = new ArrayList<>();
        for (TRentLedger l : rows) {
            ids.add(l.getId());
        }
        // 用 UpdateWrapper 而不是 updateById：农户把某个字段改空时，
        // updateById 会忽略 null，台账上还会留着旧值
        ledgerService.update(new LambdaUpdateWrapper<TRentLedger>()
                .in(TRentLedger::getId, ids)
                .set(TRentLedger::getFarmerName, station.getFarmerName())
                .set(TRentLedger::getPowerUserNo, station.getPowerUserNo())
                .set(TRentLedger::getInstallAddr, station.getInstallAddr())
                .set(TRentLedger::getIdCard, station.getIdCard())
                .set(TRentLedger::getBankCardNo, station.getBankCardNo())
                .set(TRentLedger::getBankCardName, station.getBankCardName())
                .set(TRentLedger::getProjectId, station.getProjectId())
                .set(TRentLedger::getTenantId, station.getTenantId())
                .set(TRentLedger::getTotalBlocks, station.getTotalBlocks())
                .set(TRentLedger::getTotalCapacity, station.getTotalCapacity())
                .set(TRentLedger::getInstallSnapshot, installSnapshot)
                .set(TRentLedger::getUpdateBy, user)
                .set(TRentLedger::getUpdateTime, now));

        // 并网日期变了，并网剩余天数/月数要按每期自己的期末重算
        for (TRentLedger l : rows) {
            if (!Objects.equals(l.getGridDate(), station.getGridDate())) {
                ledgerService.update(new LambdaUpdateWrapper<TRentLedger>()
                        .eq(TRentLedger::getId, l.getId())
                        .set(TRentLedger::getGridDate, station.getGridDate())
                        .set(TRentLedger::getGridMonthRemainDays,
                                RentPeriodUtil.gridMonthRemainDays(station.getGridDate()))
                        .set(TRentLedger::getGridMonths,
                                RentPeriodUtil.gridMonths(station.getGridDate(), l.getPeriodEnd())));
            }
        }
        log.info("农户 {} 审核通过，同步了 {} 条未提交租金台账的农户快照",
                station.getId(), rows.size());
    }

    /**
     * 本次农户变更有没有动到台账快照上的信息。
     * 只改了备注、业务员这类不参与租金展示的字段时，不必刷新台账。
     */
    private boolean changedForLedger(String beforeJson, String afterJson) {
        if (beforeJson == null || afterJson == null) {
            return false;
        }
        JSONObject b = JSON.parseObject(beforeJson);
        JSONObject a = JSON.parseObject(afterJson);
        TFarmerStation bs = b.getObject("station", TFarmerStation.class);
        TFarmerStation as = a.getObject("station", TFarmerStation.class);
        if (bs == null || as == null) {
            return false;
        }
        if (!Objects.equals(bs.getFarmerName(), as.getFarmerName())
                || !Objects.equals(bs.getPowerUserNo(), as.getPowerUserNo())
                || !Objects.equals(bs.getInstallAddr(), as.getInstallAddr())
                || !Objects.equals(bs.getIdCard(), as.getIdCard())
                || !Objects.equals(bs.getBankCardNo(), as.getBankCardNo())
                || !Objects.equals(bs.getBankCardName(), as.getBankCardName())
                || !Objects.equals(bs.getProjectId(), as.getProjectId())
                || !Objects.equals(bs.getTotalBlocks(), as.getTotalBlocks())
                || !Objects.equals(bs.getTotalCapacity(), as.getTotalCapacity())
                || !Objects.equals(bs.getGridDate(), as.getGridDate())) {
            return true;
        }
        return !jsonEquals(b.get("installs"), a.get("installs"))
                || !jsonEquals(b.get("rents"), a.get("rents"));
    }

    private boolean jsonEquals(Object x, Object y) {
        return JSON.toJSONString(x).equals(JSON.toJSONString(y));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, String remark) {
        if (blank(remark)) {
            throw new JeecgBootException("请填写驳回原因");
        }
        TFarmerChangeRequest req = getPending(requestId);
        String stage = stageOf(req);
        roleGuard.requireFarmerAudit(stage, TFarmerChangeRequest.stageName(stage) + "驳回农户信息");
        Date now = new Date();
        String user = operLog.currentUser();
        boolean insert = TFarmerChangeRequest.TYPE_INSERT.equals(req.getChangeType());

        // 修改类申请：回滚到修改前；新增类申请没有"修改前"，农户保留但不可用
        if (!insert) {
            restore(req.getStationId(), req.getBeforeJson());
        }

        TFarmerChangeRequest update = new TFarmerChangeRequest()
                .setId(req.getId())
                .setStatus(TFarmerChangeRequest.STATUS_REJECTED)
                .setAuditBy(user).setAuditTime(now)
                .setAuditRemark(remark.trim())
                .setUpdateTime(now);
        if (TFarmerChangeRequest.STAGE_PROJECT.equals(stage)) {
            update.setProjectAuditBy(user).setProjectAuditTime(now);
        } else if (TFarmerChangeRequest.STAGE_BUSINESS.equals(stage)) {
            update.setBusinessAuditBy(user).setBusinessAuditTime(now);
        } else {
            update.setLeaderAuditBy(user).setLeaderAuditTime(now);
        }
        requestService.updateById(update);

        farmerStationService.updateById(new TFarmerStation()
                .setId(req.getStationId())
                .setAuditStatus(TFarmerStation.rejectedStatusFor(insert, stage)));

        operLog.recordText(TOperLog.ACTION_REJECT, TOperLog.BIZ_FARMER, req.getStationId(),
                TFarmerChangeRequest.stageName(stage) + "驳回"
                        + TFarmerChangeRequest.typeName(req.getChangeType())
                        + "，原因：" + remark.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long requestId) {
        roleGuard.requireFarmerOps("取消农户信息修改");
        TFarmerChangeRequest req = getPending(requestId);
        Date now = new Date();
        boolean insert = TFarmerChangeRequest.TYPE_INSERT.equals(req.getChangeType());
        if (!insert) {
            restore(req.getStationId(), req.getBeforeJson());
        }

        requestService.updateById(new TFarmerChangeRequest()
                .setId(req.getId())
                .setStatus(TFarmerChangeRequest.STATUS_CANCELLED)
                .setAuditBy(operLog.currentUser()).setAuditTime(now)
                .setUpdateTime(now));
        farmerStationService.updateById(new TFarmerStation()
                .setId(req.getStationId())
                .setAuditStatus(TFarmerStation.AUDIT_NORMAL));

        operLog.recordText(TOperLog.ACTION_UPDATE, TOperLog.BIZ_FARMER, req.getStationId(),
                "取消" + TFarmerChangeRequest.typeName(req.getChangeType()) + "审核，数据已回滚");
    }

    @Override
    public boolean periodLocked(Long stationId, Date periodEnd) {
        if (stationId == null) {
            return false;
        }
        TFarmerStation s = farmerStationService.getById(stationId);
        if (s == null) {
            return false;
        }
        Integer status = s.getAuditStatus();
        if (status == null || status == TFarmerStation.AUDIT_NORMAL) {
            return false;
        }
        // 修改被驳回（任何一级）→ 数据已回滚到修改前，不锁
        if (status == TFarmerStation.AUDIT_UPDATE_PROJECT_REJECTED
                || status == TFarmerStation.AUDIT_UPDATE_BUSINESS_REJECTED
                || status == TFarmerStation.AUDIT_UPDATE_LEADER_REJECTED) {
            return false;
        }
        // 新增被驳回（任何一级）→ 这个农户整体不能生成租金，所有期都锁
        if (status == TFarmerStation.AUDIT_INSERT_PROJECT_REJECTED
                || status == TFarmerStation.AUDIT_INSERT_BUSINESS_REJECTED
                || status == TFarmerStation.AUDIT_INSERT_LEADER_REJECTED) {
            return true;
        }
        // 新增/修改待审核（待项目/待业务/待领导）：按提交时间判断
        Date submit = s.getAuditSubmitTime();
        if (submit == null) {
            return true; // 状态是待审核却没有提交时间，保守锁住
        }
        if (periodEnd == null) {
            return true;
        }
        // 该期还没结束（截止日 >= 提交日）→ 算"提交之后" → 锁
        return !RentPeriodUtil.truncate(periodEnd).before(RentPeriodUtil.truncate(submit));
    }

    // ------------------------------------------------------------------

    @Override
    public String snapshot(Long stationId) {
        TFarmerStation station = farmerStationService.getById(stationId);
        if (station == null) {
            throw new JeecgBootException("农户不存在");
        }
        List<TInstallInfo> installs = installInfoService.list(
                new LambdaQueryWrapper<TInstallInfo>()
                        .eq(TInstallInfo::getStationId, stationId)
                        .eq(TInstallInfo::getDelFlag, 0)
                        .orderByAsc(TInstallInfo::getId));
        List<TRent> rents = new ArrayList<>();
        if (!installs.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (TInstallInfo i : installs) {
                ids.add(i.getId());
            }
            rents = rentService.list(new LambdaQueryWrapper<TRent>()
                    .in(TRent::getInstallId, ids)
                    .eq(TRent::getDelFlag, 0)
                    .orderByAsc(TRent::getId));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("station", station);
        m.put("installs", installs);
        m.put("rents", rents);
        return JSON.toJSONString(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long stationId, String json) {
        if (stationId == null || json == null || json.isEmpty()) {
            return;
        }
        JSONObject obj = JSON.parseObject(json);
        TFarmerStation station = obj.getObject("station", TFarmerStation.class);
        List<TInstallInfo> installs = new ArrayList<>();
        if (obj.getJSONArray("installs") != null) {
            installs = obj.getJSONArray("installs").toJavaList(TInstallInfo.class);
        }
        List<TRent> rents = new ArrayList<>();
        if (obj.getJSONArray("rents") != null) {
            rents = obj.getJSONArray("rents").toJavaList(TRent.class);
        }

        // 安装信息和租金整组覆盖：先删当前的，再按快照原样插回（id 保留）
        installInfoService.remove(new LambdaQueryWrapper<TInstallInfo>()
                .eq(TInstallInfo::getStationId, stationId));
        rentService.remove(new LambdaQueryWrapper<TRent>()
                .eq(TRent::getStationId, stationId));
        for (TInstallInfo i : installs) {
            installInfoService.save(i);
        }
        for (TRent r : rents) {
            rentService.save(r);
        }
        if (station != null) {
            farmerStationService.updateById(station);
        }
        log.info("农户 {} 数据已回滚到变更前的快照（安装信息 {} 条，租金 {} 条）",
                stationId, installs.size(), rents.size());
    }

    private TFarmerChangeRequest getPending(Long requestId) {
        TFarmerChangeRequest req = getById(requestId);
        if (!req.pending()) {
            throw new JeecgBootException("这次变更已经处理过了（"
                    + TFarmerChangeRequest.statusName(req.getStatus()) + "），不能重复操作");
        }
        return req;
    }

    /**
     * 当前该由谁审。老数据没有阶段，按业务审核处理（升级前就是业务专员在审）。
     */
    private String stageOf(TFarmerChangeRequest req) {
        String stage = req.getAuditStage();
        if (TFarmerChangeRequest.STAGE_PROJECT.equals(stage)
                || TFarmerChangeRequest.STAGE_BUSINESS.equals(stage)
                || TFarmerChangeRequest.STAGE_LEADER.equals(stage)) {
            return stage;
        }
        return TFarmerChangeRequest.STAGE_BUSINESS;
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
