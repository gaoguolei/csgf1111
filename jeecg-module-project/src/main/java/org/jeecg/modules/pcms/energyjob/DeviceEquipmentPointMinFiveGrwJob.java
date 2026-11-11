package org.jeecg.modules.pcms.energyjob;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointMinFiveGrwService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Slf4j
@Component
public class DeviceEquipmentPointMinFiveGrwJob{


    @Autowired
    private IDeviceEquipmentPointMinFiveGrwService iDeviceEquipmentPointMinFiveGrwService;

    @Autowired
    public RedisTemplate redisTemplate;

    private static final String LOCK_KEY = "lock:job:DeviceEquipmentPointMinFiveGrwJob";

    @Scheduled(cron = "2 12 7 * * ?")
    public void execute() throws JobExecutionException {
        // 1. 尝试加锁，有效期设置为 55 秒（略小于执行周期，防止死锁）
        log.info("-------------------------------------------------古瑞瓦特设备5分钟定时任务开始------------------------------------------");
        Boolean getLock = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(55));
        if (Boolean.TRUE.equals(getLock)) {
            try {
            } catch (Exception e) {
                log.error("--- 任务执行异常 ---", e);
            } finally {
                // 2. 任务结束手动释放锁，以便下次执行
                iDeviceEquipmentPointMinFiveGrwService.insert();
                redisTemplate.delete(LOCK_KEY);
                log.info("--- [释放锁成功] 任务处理完毕 ---");
            }
        } else {
            // 如果没抢到锁，说明其他实例正在执行，直接跳过
            log.debug("--- [跳过] 其他实例正在处理任务 ---");
        }
    }
}
