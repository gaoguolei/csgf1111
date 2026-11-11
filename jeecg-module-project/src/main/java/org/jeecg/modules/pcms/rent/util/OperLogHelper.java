package org.jeecg.modules.pcms.rent.util;

import java.util.Date;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.pcms.rent.entity.TOperLog;
import org.jeecg.modules.pcms.rent.service.ITOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志记录。需求稿第 1 条要求 t_oper_log 记录增删改等所有操作。
 *
 * 记日志失败绝不能影响主业务，所有方法内部都吞掉异常只打 warn。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class OperLogHelper {

    @Autowired
    private ITOperLogService operLogService;

    /**
     * 当前线程的操作人。
     *
     * 异步任务跑在没有请求上下文的线程里，currentUser() 只能返回 system，
     * 台账的 create_by/update_by 就会丢掉真正的操作人。发起任务时用 callAs 把用户名带进来，
     * 后台线程里所有 currentUser() 就都按这个人算。用完必须清掉，否则线程池复用时会串人。
     */
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    /** 以指定操作人身份执行一段逻辑，结束后恢复本线程原来的身份 */
    public <T> T callAs(String user, Supplier<T> task) {
        String old = CURRENT_USER.get();
        if (user != null && !user.trim().isEmpty()) {
            CURRENT_USER.set(user);
        }
        try {
            return task.get();
        } finally {
            if (old == null) {
                CURRENT_USER.remove();
            } else {
                CURRENT_USER.set(old);
            }
        }
    }

    /**
     * @param action  TOperLog.ACTION_*
     * @param bizType TOperLog.BIZ_*
     * @param bizId   业务主键
     * @param before  改前对象，新增时传 null
     * @param after   改后对象，删除时传 null
     */
    public void record(String action, String bizType, Long bizId, Object before, Object after) {
        try {
            JSONObject content = new JSONObject();
            if (before != null) {
                content.put("before", JSON.toJSON(before));
            }
            if (after != null) {
                content.put("after", JSON.toJSON(after));
            }
            operLogService.save(new TOperLog()
                    .setOperUser(currentUser())
                    .setOperTime(new Date())
                    .setAction(action)
                    .setBizType(bizType)
                    .setBizId(bizId)
                    .setOperContent(content.toJSONString())
                    .setIp(currentIp()));
        } catch (Exception e) {
            log.warn("写操作日志失败 action={} bizType={} bizId={}", action, bizType, bizId, e);
        }
    }

    /**
     * 直接记一段文本，导入汇总用
     */
    public void recordText(String action, String bizType, Long bizId, String text) {
        recordText(currentUser(), action, bizType, bizId, text);
    }

    /**
     * 指定操作人记一段文本。
     *
     * 异步任务（回单对账）跑在后台线程里，那里没有请求上下文，
     * currentUser() 只会返回 "system"，操作人就丢了 —— 财务操作要留痕，
     * 所以发起任务时把用户名带进来，一路传到底。
     */
    public void recordText(String user, String action, String bizType, Long bizId, String text) {
        try {
            operLogService.save(new TOperLog()
                    .setOperUser(user == null || user.trim().isEmpty() ? "system" : user)
                    .setOperTime(new Date())
                    .setAction(action)
                    .setBizType(bizType)
                    .setBizId(bizId)
                    .setOperContent(text)
                    .setIp(currentIp()));
        } catch (Exception e) {
            log.warn("写操作日志失败 action={} bizType={}", action, bizType, e);
        }
    }

    public String currentUser() {
        String override = CURRENT_USER.get();
        if (override != null) {
            return override;
        }
        try {
            HttpServletRequest req = request();
            String user = req == null ? null : JwtUtil.getUserNameByToken(req);
            return user == null ? "system" : user;
        } catch (Exception e) {
            return "system";
        }
    }

    private String currentIp() {
        try {
            HttpServletRequest req = request();
            return req == null ? null : oConvertUtils.getIpAddrByRequest(req);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest request() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
