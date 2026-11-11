package org.jeecg.modules.pcms.energyjob;


import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.project.service.IDeviceFaultService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 定时任务
 */
@Slf4j
@Component
public class DeviceFaultJob{


    @Autowired
    private IDeviceFaultService deviceFaultService;
    @Autowired
    public RedisTemplate redisTemplate;


    /***
     * 定时任务导入未处理的故障信息
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    // 锁的 Key 名
    private static final String LOCK_KEY = "lock:job:DeviceFaultJob";

    @Scheduled(cron = "22 20 7 * * ?")
    public void execute() {
        // 1. 尝试加锁，有效期设置为 55 秒（略小于执行周期，防止死锁）
        log.info("---------------------------------------------------设备工单----------------------------------------------------");
        Boolean getLock = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(55));

        if (Boolean.TRUE.equals(getLock)) {
            try {
                log.info("--- [获取锁成功] 告警数据开始执行 ---");
                deviceFaultService.insert();
            } catch (Exception e) {
                log.error("--- 任务执行异常 ---", e);
            } finally {
                // 2. 任务结束手动释放锁，以便下次执行
                redisTemplate.delete(LOCK_KEY);
                log.info("--- [释放锁成功] 任务处理完毕 ---");
            }
        } else {
            // 如果没抢到锁，说明其他实例正在执行，直接跳过
            log.debug("--- [跳过] 其他实例正在处理任务 ---");
        }
    }

}
