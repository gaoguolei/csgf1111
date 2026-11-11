-- 需求第14条：租金数据按租户隔离
-- t_farmer_station / t_rent_ledger 增加 tenant_id 字段，
-- 与 sys_tenant.id 对应。普通用户只看自己租户的数据，
-- 管理员（rel_tenant_ids 为空）看全部。
--
-- ⚠️ 如果是全新部署（生产库还没有这两张表），不需要执行本脚本，
--    请直接执行 20261009_rent_module_deploy.sql（建表语句已含 tenant_id）。

ALTER TABLE `t_farmer_station`
    ADD COLUMN `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID，用于数据隔离' AFTER `del_time`,
    ADD INDEX `idx_farmer_station_tenant` (`tenant_id`);

ALTER TABLE `t_rent_ledger`
    ADD COLUMN `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID，关联sys_tenant.id' AFTER `del_flag`,
    ADD INDEX `idx_rent_ledger_tenant` (`tenant_id`);

-- 存量数据回填。
-- 注意：不能按 region_name 匹配 sys_tenant.name——天津和廊坊在 t_project 里
-- 共用一个 region_name「天津廊坊项目」，而 sys_tenant 里是分开的两个租户，
-- 按名称匹配会让这 2000 多行全部落空。
-- 正确的区分依据是 t_project.project_code 前缀：TJ_*=天津，LF_*=廊坊。
UPDATE `t_farmer_station` s
    JOIN `t_project` p ON p.id = s.project_id
    JOIN `sys_tenant` t ON t.name = p.region_name
SET s.tenant_id = t.id
WHERE s.tenant_id IS NULL
  AND p.project_code NOT LIKE 'TJ\_%'
  AND p.project_code NOT LIKE 'LF\_%';

-- 天津：project_code 以 TJ_ 开头
UPDATE `t_farmer_station` s
    JOIN `t_project` p ON p.id = s.project_id
SET s.tenant_id = (SELECT t.id FROM `sys_tenant` t WHERE t.name = '天津项目' LIMIT 1)
WHERE s.tenant_id IS NULL AND p.project_code LIKE 'TJ\_%';

-- 廊坊：project_code 以 LF_ 开头
UPDATE `t_farmer_station` s
    JOIN `t_project` p ON p.id = s.project_id
SET s.tenant_id = (SELECT t.id FROM `sys_tenant` t WHERE t.name = '廊坊项目' LIMIT 1)
WHERE s.tenant_id IS NULL AND p.project_code LIKE 'LF\_%';

-- 台账的租户跟它所属电站保持一致。
--
-- ⚠️⚠️ 这一段绝对不能漏！
-- 台账列表是按 tenant_id 过滤的，管理员在顶部切到某个租户后执行的是
--   WHERE tenant_id = <该租户id>
-- 如果台账这一列还是 NULL，切换租户后页面直接一条都查不出来，
-- 看起来就像"数据被删了"。本次就踩过这个坑：农户表回填了、台账表漏了，
-- 结果天津（7938 条台账）在列表里全不见了。
UPDATE `t_rent_ledger` l
    JOIN `t_farmer_station` s ON s.id = l.station_id
SET l.tenant_id = s.tenant_id
WHERE l.tenant_id IS NULL;

-- ============ 执行后自查，两条都必须为 0 ============
-- 农户没归属租户的行数
-- SELECT COUNT(*) AS 农户未归属 FROM t_farmer_station WHERE tenant_id IS NULL AND del_flag = 0;
-- 台账没归属租户的行数（不为 0 就说明上面那段没执行成功）
-- SELECT COUNT(*) AS 台账未归属 FROM t_rent_ledger WHERE tenant_id IS NULL;
-- 各租户的数据量分布，用来确认天津/廊坊是分开的
-- SELECT tenant_id, COUNT(*) FROM t_rent_ledger GROUP BY tenant_id;
