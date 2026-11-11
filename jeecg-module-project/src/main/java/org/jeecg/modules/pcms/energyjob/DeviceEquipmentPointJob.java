package org.jeecg.modules.pcms.energyjob;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.pcms.project.service.IDeviceEquipmentPointService;
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
public class DeviceEquipmentPointJob{

    @Autowired
    private IDeviceEquipmentPointService iDeviceEquipmentPointService;

    @Autowired
    public RedisTemplate redisTemplate;

    private static final String LOCK_KEY = "lock:job:DeviceEquipmentPointJob";

    @Scheduled(cron = "4 16 7,11,14,18 * * ?")
    public void execute() throws JobExecutionException {
        // 1. 尝试加锁，有效期设置为 55 秒（略小于执行周期，防止死锁）
        log.info("-------------------------------------------------阳光云逆变器实时测点数据定时任务------------------------------------------");
        Boolean getLock = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(55));
        if (Boolean.TRUE.equals(getLock)) {
            try {
            } catch (Exception e) {
                log.error("--- 任务执行异常 ---", e);
            } finally {
                // 2. 任务结束手动释放锁，以便下次执行
                iDeviceEquipmentPointService.insert();
                redisTemplate.delete(LOCK_KEY);
                log.info("--- [释放锁成功] 任务处理完毕 ---");
            }
        } else {
            // 如果没抢到锁，说明其他实例正在执行，直接跳过
            log.debug("--- [跳过] 其他实例正在处理任务 ---");
        }
    }
}
