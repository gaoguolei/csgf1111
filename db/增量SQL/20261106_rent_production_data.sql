-- ============================================================
-- 生产环境租金模块 —— 基础数据上库脚本
-- 生成时间：2026-11-06
-- ============================================================
-- 【本脚本解决什么】
--   租金模块的 15 张 t_* 表里，只有 3 张是"配置数据"，生产必须提前灌好，
--   否则页面下拉框是空的、农户导入会失败。其余 12 张是业务数据，生产从 0 开始。
--
--   配置数据（本脚本负责）
--     t_project          12 行  项目档案，农户必须挂在项目上
--     t_install_type     16 行  安装房型，导入接口按 type_code 匹配，缺一行导入就报错
--     t_rent_price_type  14 行  租金档位，生成台账时按它切分年份
--
--   业务数据（不要导，生产从 0 开始）
--     t_farmer_station / t_install_info / t_rent / t_oper_log / t_farmer_change_request
--     t_rent_ledger / t_rent_ledger_month / t_rent_attach / t_rent_audit_record
--     t_rent_dispute_record / t_rent_generate_task / t_rent_reconcile_task
--
-- 【数据来源】
--   取自测试库导出（C:\Users\32598\Desktop\gf测试环境数据），
--   与 20261009_rent_module_deploy.sql 里的字典初始化数据逐行比对，42 行完全一致。
--
-- 【生产现状补充（2026-11-06 核对）】
--   生产库这三张表已经有数据（t_project 12 行 / t_install_type 16 行 /
--   t_rent_price_type 14 行），本脚本的 INSERT IGNORE 都是空操作，
--   实际价值只剩两件：清理测试遗留房型、跑末尾的自查。
--   如果生产是全新的空库，本脚本才真正起灌数作用。
--   注意：生产的 t_project.region_name 是「北京 / 天津廊坊 / 徐州 / 海南 / 河南」，
--   与测试库的「北京项目 / 天津廊坊项目 / …」不同，这是历史数据，保留即可，
--   代码只在展示时用它，判断租户归属走的是 region_code。
--
-- 【执行顺序】
--   1) 20261009_rent_module_deploy.sql   建表 + 菜单 + 定时任务（已执行过可跳过）
--   2) 20261018_production_deploy.sql    生产补丁（已执行过可跳过）
--   3) 20261021 / 20261022 / 20261028 / 20261102_* 其余增量
--   4) 本脚本                            基础数据 + 清理测试遗留
--
-- 【幂等性】全部 INSERT IGNORE，可重复执行。
-- ============================================================

SET NAMES utf8mb4;

-- ############################################################
-- 零、前置检查：sys_tenant 必须有这 6 个租户
--     代码 RentTenantResolver 按「租户名称」反查 sys_tenant.id 写进
--     t_farmer_station.tenant_id，名称对不上导入直接抛异常：
--     「大区[xx]对应的租户[xx]在 sys_tenant 中不存在」
--     下面这条查询必须返回 6 行，少了先补建租户再往下执行。
-- ############################################################

-- SELECT id, name FROM `sys_tenant`
--  WHERE name IN ('北京项目', '天津项目', '廊坊项目', '徐州项目', '海南项目', '河南项目')
--  ORDER BY name;
-- 需要时（id 取测试库同款）：
-- INSERT IGNORE INTO `sys_tenant`
--     (`id`, `name`, `area_project_code`, `area_address`, `create_by`, `create_time`, `status`)
-- VALUES (118337537, '徐州项目', 'MG202406176', '江苏徐州', 'admin', NOW(), 1);

-- ############################################################
-- 一、t_project 项目档案（12 行）
--     ⚠️ id 被农户/租金数据引用，务必原样，不要改 id
-- ############################################################

INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'BJ','北京项目','BJ_01','北京市户用分布式光伏项目一期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:36');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2,'BJ','北京项目','BJ_02','北京二期27MWp分布式光伏项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:37');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3,'BJ','北京项目','BJ_03','北京分布式光伏项目三期（23.23MW）',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:38');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4,'TJLF','天津廊坊项目','TJ_01','天津一期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:39');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5,'TJLF','天津廊坊项目','TJ_02','天津二期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:41');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6,'TJLF','天津廊坊项目','TJ_03','天津三期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:41');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7,'TJLF','天津廊坊项目','TJ_04','天津四期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:42');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8,'TJLF','天津廊坊项目','TJ_05','天津五期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:42');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9,'TJLF','天津廊坊项目','LF_01','廊坊',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:43');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10,'XZ','徐州项目','XZ_01','徐州项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:44');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11,'HAIN','海南项目','HN_01','海南项目',25,1,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-18 14:56:09');
INSERT IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12,'HENAN','河南项目','HEN_01','河南项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:45');

-- ############################################################
-- 二、t_install_type 安装房型（16 行，仅内置）
--     is_builtin=1，导入接口按 type_code 匹配，删改会导致导入失败
--     测试库里多出来的 7 条 CUSTOM_HENAN_* 是测试同事手工建着玩的
--     （名称是「你好」「14」「49」「哈哈」），不要带到生产
-- ############################################################

INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'BJ','BJ_SUNROOM','阳光房',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2,'BJ','BJ_SOUTH','南坡',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3,'BJ','BJ_NORTH','北坡',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4,'BJ','BJ_EW','东西坡',1,4,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5,'TJLF','TJ_SUNROOM','阳光房',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6,'TJLF','TJ_SN','南北坡',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7,'TJLF','TJ_15DEG','其他15度角',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8,'XZ','XZ_DOUBLE','双坡/庭院',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9,'XZ','XZ_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10,'XZ','XZ_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11,'HAIN','HN_DOUBLE','双坡/庭院',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12,'HAIN','HN_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (13,'HAIN','HN_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (14,'HENAN','HEN_SLOPE','斜屋面',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (15,'HENAN','HEN_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (16,'HENAN','HEN_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');

-- ############################################################
-- 三、t_rent_price_type 租金档位（14 行）
--     split_mode=1 按年切（相对每户并网日），split_mode=2 按自然日期切
-- ############################################################

INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (1,'BJ','BJ_Y1_5','前5年',1,1,5,NULL,NULL,1);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (2,'BJ','BJ_Y6_25','后20年',1,6,25,NULL,NULL,2);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (3,'TJLF','TJ_Y1_5','1-5年',1,1,5,NULL,NULL,1);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (4,'TJLF','TJ_Y6_25','6-25年',1,6,25,NULL,NULL,2);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (5,'XZ','XZ_Y1_10','前10年',1,1,10,NULL,NULL,1);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (6,'XZ','XZ_Y11_25','后15年',1,11,25,NULL,NULL,2);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (7,'HAIN','HN_Y1_10','前10年',1,1,10,NULL,NULL,1);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (8,'HAIN','HN_Y11_25','后15年',1,11,25,NULL,NULL,2);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (9,'HAIN','HN_Y1_8','前8年',1,1,8,NULL,NULL,3);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (10,'HAIN','HN_Y9_25','后17年',1,9,25,NULL,NULL,4);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (11,'HENAN','HEN_Y1_15','前15年',1,1,15,NULL,NULL,1);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (12,'HENAN','HEN_Y16_25','后10年',1,16,25,NULL,NULL,2);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (13,'HENAN','HEN_D_BEF','2025年3月31日之前',2,NULL,NULL,NULL,'2025-03-31',3);
INSERT IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (14,'HENAN','HEN_D_AFT','2025年4月1日之后',2,NULL,NULL,'2025-04-01',NULL,4);

-- ############################################################
-- 四、清理测试遗留的自定义房型
--     如果生产已经跑过 20261009_rent_module_deploy.sql 的老版本，
--     这 7 条测试房型已经写进去了，这里删掉。
--     先确认没有业务数据引用（正常生产库里应该是 0）：
--       SELECT COUNT(*) FROM t_install_info WHERE install_type_id BETWEEN 49 AND 55;
-- ############################################################

DELETE FROM `t_install_type`
 WHERE `is_builtin` = 0
   AND `type_code` LIKE 'CUSTOM\_HENAN\_%'
   AND `id` BETWEEN 49 AND 55;

-- ############################################################
-- 五、执行后自查
-- ############################################################
-- 1) 三张字典表的行数：12 / 16 / 14
-- SELECT 't_project' AS t, COUNT(*) AS n FROM t_project
-- UNION ALL SELECT 't_install_type', COUNT(*) FROM t_install_type
-- UNION ALL SELECT 't_rent_price_type', COUNT(*) FROM t_rent_price_type;
--
-- 2) 不能有测试遗留房型
-- SELECT * FROM t_install_type WHERE type_code LIKE 'CUSTOM\_HENAN\_%';
--
-- 3) 租户齐全（必须 6 行）
-- SELECT id, name FROM sys_tenant
--  WHERE name IN ('北京项目','天津项目','廊坊项目','徐州项目','海南项目','河南项目');
--
-- 4) 角色与菜单（5 个角色，4 个全量 5 条 + 财务 3 条）
-- SELECT r.role_code, COUNT(*) AS menu_cnt
--   FROM sys_role_permission rp
--   JOIN sys_role r ON r.id = rp.role_id
--  WHERE rp.permission_id IN ('2086670609055145985','2086670953071960066','2086715467086098434',
--                             '2087786048837607425','2087786299975753730')
--  GROUP BY r.role_code;
--
-- 5) 定时任务已启用（上线后手工把 status 改成 1）
-- SELECT job_class_name, cron_expression, status FROM sys_quartz_job
--  WHERE job_class_name LIKE '%RentLedgerGenerateJob%';
-- ############################################################
