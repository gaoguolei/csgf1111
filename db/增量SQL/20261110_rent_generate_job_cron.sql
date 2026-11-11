-- 年度租金台账生成任务：执行时间统一为每年 3 月 1 日凌晨 1 点
--
-- 之前三处说法不一致：业务流程图写 3 月 1 日、Job 注释写 3 月 31 日、
-- 库里配的是 1 月 1 日（0 0 1 1 1 ?）。按业务流程图统一成 3 月 1 日。
--
-- 执行后注意两点：
--   1. quartz 里已注册的触发器还是旧时间，要在「定时任务」页面把该任务重新保存一次，
--      或者重启服务，新 cron 才会生效；
--   2. 这条任务的 status 是 -1（停止），如果之前没手动启用过，它压根不会跑。

UPDATE `sys_quartz_job`
   SET `cron_expression` = '0 0 1 1 3 ?',
       `description`     = '每年3月1日生成当年度全部电站的租金台账，期数按项目配置',
       `update_time`     = NOW()
 WHERE `job_class_name` = 'org.jeecg.modules.pcms.rent.job.RentLedgerGenerateJob';
