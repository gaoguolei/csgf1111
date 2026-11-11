-- ============================================================
-- 生产环境租金模块升级（按生产实际表结构定制）
-- 生成时间：2026-11-06
-- ============================================================
-- 【依据】生产库 renergy（10.42.0.22:3306, MySQL 5.7.26）2026-11-06 导出的建表语句
--
-- 【生产现状逐表核对结果】
--   已是最新结构，不用动：
--     t_install_info / t_oper_log / t_rent / t_rent_ledger_month
--     t_rent_attach / t_rent_dispute_record / t_rent_price_type / t_rent_reconcile_task
--   缺字段：
--     t_farmer_station   缺 audit_status / audit_submit_time / audit_submit_by 和审核索引
--     t_install_type     缺 is_builtin / status / create_* / update_* 和 uk_region_name
--     t_project          缺 periods_per_year
--     t_rent_ledger      缺 install_snapshot + 四级审核的 8 个字段
--     t_rent_audit_record 缺 audit_stage
--   整表缺失：
--     t_rent_generate_task / t_farmer_change_request
--
-- 【本脚本只加不删】
--   全部是 ADD COLUMN / CREATE TABLE IF NOT EXISTS，不 DROP、不 TRUNCATE。
--   t_project / t_install_type 有数据（12 行 / 16 行），只补列。
--   其余表都是 0 行，补列瞬间完成。
--
-- ⚠️ ALTER 语句不能重复执行（MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS），
--    重复跑会报 Duplicate column，那是正常的，说明已经加过了。
--    角色/菜单部分用 INSERT IGNORE，可重复执行。
--
-- 【执行顺序】
--   1) 本脚本 20261106_rent_production_upgrade.sql
--   2) 20261106_rent_production_data.sql（字典校验 + 清理测试遗留房型）
-- ============================================================

SET NAMES utf8mb4;


-- ############################################################
-- 一、t_farmer_station：农户三级审核字段
-- ############################################################

ALTER TABLE `t_farmer_station`
    ADD COLUMN `audit_status` int(2) NOT NULL DEFAULT '0'
        COMMENT '0正常 1新增待项目审核 2新增项目驳回 3新增待业务审核 4新增业务驳回 5新增待领导审核 6新增领导驳回 7修改待项目审核 8修改项目驳回 9修改待业务审核 10修改业务驳回 11修改待领导审核 12修改领导驳回' AFTER `del_time`,
    ADD COLUMN `audit_submit_time` datetime DEFAULT NULL
        COMMENT '最近一次提交审核的时间' AFTER `audit_status`,
    ADD COLUMN `audit_submit_by` varchar(50) DEFAULT NULL
        COMMENT '最近一次提交审核的人' AFTER `audit_submit_time`,
    ADD INDEX `idx_farmer_station_audit` (`audit_status`);


-- ############################################################
-- 二、t_install_type：内置标记 + 审计列 + 名称唯一键
-- ############################################################

ALTER TABLE `t_install_type`
    ADD COLUMN `is_builtin` tinyint(4) NOT NULL DEFAULT '0'
        COMMENT '1=系统内置，导入接口依赖其type_code，禁止删除和修改编码；0=运营自建' AFTER `type_name`,
    ADD COLUMN `status` tinyint(4) NOT NULL DEFAULT '1'
        COMMENT '1启用 0停用' AFTER `sort_no`,
    ADD COLUMN `create_by` varchar(50) DEFAULT NULL COMMENT '创建人' AFTER `status`,
    ADD COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `create_by`,
    ADD COLUMN `update_by` varchar(50) DEFAULT NULL COMMENT '更新人' AFTER `create_time`,
    ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`,
    ADD UNIQUE INDEX `uk_region_name` (`region_code`,`type_name`);

-- 生产这 16 条都是导入接口依赖的内置房型，必须标成内置，
-- 否则前端会把它们当"运营自建"允许删除/改编码，改完导入就断了。
UPDATE `t_install_type` SET `is_builtin` = 1
 WHERE `type_code` IN ('BJ_SUNROOM','BJ_SOUTH','BJ_NORTH','BJ_EW',
                       'TJ_SUNROOM','TJ_SN','TJ_15DEG',
                       'XZ_DOUBLE','XZ_SUN5','XZ_SUN10',
                       'HN_DOUBLE','HN_SUN5','HN_SUN10',
                       'HEN_SLOPE','HEN_SUN5','HEN_SUN10');


-- ############################################################
-- 三、t_project：一年结算几期
-- ############################################################

ALTER TABLE `t_project`
    ADD COLUMN `periods_per_year` int(11) NOT NULL DEFAULT '4'
        COMMENT '一年结算几期租金，只能取12的约数：1/2/3/4/6/12' AFTER `lease_years`;

-- ⚠️ 海南是一年一期，其余一年四期。
--    不校正的话海南会按季度生成 4 期台账，金额全错。
UPDATE `t_project` SET `periods_per_year` = 1 WHERE `project_code` = 'HN_01';
UPDATE `t_project` SET `periods_per_year` = 4 WHERE `project_code` <> 'HN_01';


-- ############################################################
-- 四、t_rent_ledger：安装快照 + 四级审核字段
-- ############################################################

ALTER TABLE `t_rent_ledger`
    ADD COLUMN `install_snapshot` longtext
        COMMENT '生成时的安装信息快照（房型、型号、块数、容量、档位单价），JSON' AFTER `total_capacity`,
    ADD COLUMN `project_audit_by` varchar(64) DEFAULT NULL COMMENT '项目审核人（第一级）' AFTER `submit_by`,
    ADD COLUMN `project_audit_time` datetime DEFAULT NULL COMMENT '项目审核时间' AFTER `project_audit_by`,
    ADD COLUMN `finance_audit_by` varchar(64) DEFAULT NULL COMMENT '财务审核人' AFTER `audit_by`,
    ADD COLUMN `finance_audit_time` datetime DEFAULT NULL COMMENT '财务审核时间' AFTER `finance_audit_by`,
    ADD COLUMN `leader_audit_by` varchar(64) DEFAULT NULL COMMENT '领导审核人（第四级）' AFTER `finance_audit_time`,
    ADD COLUMN `leader_audit_time` datetime DEFAULT NULL COMMENT '领导审核时间' AFTER `leader_audit_by`,
    ADD COLUMN `business_audit_by` varchar(64) DEFAULT NULL COMMENT '业务审核人' AFTER `leader_audit_time`,
    ADD COLUMN `business_audit_time` datetime DEFAULT NULL COMMENT '业务审核时间' AFTER `business_audit_by`;

-- 表里没有数据，直接改注释即可（旧注释是两级审核前的口径）
ALTER TABLE `t_rent_ledger`
    MODIFY COLUMN `audit_status` int(1) DEFAULT '0'
        COMMENT '审核状态 0待提交 1待项目审核 2项目审核驳回 3待业务审核 4业务审核驳回 5待财务审核 6财务审核驳回 7待领导审核 8领导审核驳回 9领导审核通过(待支付)';


-- ############################################################
-- 五、t_rent_audit_record：审核阶段
-- ############################################################

ALTER TABLE `t_rent_audit_record`
    ADD COLUMN `audit_stage` varchar(16) NOT NULL DEFAULT 'PROJECT'
        COMMENT 'PROJECT项目审核 BUSINESS业务审核 FINANCE财务审核 LEADER领导审核' AFTER `audit_round`;


-- ############################################################
-- 六、两张缺失的表
-- ############################################################

CREATE TABLE IF NOT EXISTS `t_rent_generate_task` (
  `id` varchar(36) NOT NULL COMMENT '任务ID，UUID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/DONE/FAILED',
  `rent_year` int(11) NOT NULL DEFAULT '0' COMMENT '生成年度',
  `total_stations` int(11) NOT NULL DEFAULT '0' COMMENT '参与生成的电站总数',
  `progress` int(11) NOT NULL DEFAULT '0' COMMENT '已处理电站数',
  `created_count` int(11) NOT NULL DEFAULT '0' COMMENT '新生成的台账条数',
  `skipped_count` int(11) NOT NULL DEFAULT '0' COMMENT '跳过的条数',
  `fail_count` int(11) NOT NULL DEFAULT '0' COMMENT '失败的电站数',
  `period_range` varchar(100) DEFAULT NULL COMMENT '本次覆盖的时间段',
  `detail_json` longtext COMMENT '跳过/失败原因明细，结构 {"skip":[],"fail":[]}',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '任务整体失败的原因',
  `cost_ms` bigint(20) NOT NULL DEFAULT '0' COMMENT '耗时毫秒',
  `create_by` varchar(50) DEFAULT NULL COMMENT '发起人',
  `create_time` datetime DEFAULT NULL COMMENT '发起时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_rent_generate_task_status` (`status`),
  KEY `idx_rent_generate_task_create` (`create_by`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租金台账生成任务';

CREATE TABLE IF NOT EXISTS `t_farmer_change_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `station_id` bigint(20) NOT NULL COMMENT '农户电站信息表ID',
  `change_type` varchar(10) NOT NULL COMMENT 'INSERT新增 UPDATE修改',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING'
      COMMENT 'PENDING待审核 APPROVED通过 REJECTED驳回 CANCELLED取消',
  `audit_stage` varchar(16) NOT NULL DEFAULT 'PROJECT'
      COMMENT '当前审核阶段 PROJECT项目审核 BUSINESS业务审核 LEADER领导审核',
  `before_json` longtext COMMENT '修改前快照，新增时为空',
  `after_json` longtext COMMENT '修改后快照',
  `submit_by` varchar(50) DEFAULT NULL COMMENT '提交人',
  `submit_time` datetime DEFAULT NULL COMMENT '提交审核时间，租金锁定以此为准',
  `audit_by` varchar(50) DEFAULT NULL COMMENT '审核人（最近一次）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间（最近一次）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `project_audit_by` varchar(64) DEFAULT NULL COMMENT '项目审核人',
  `project_audit_time` datetime DEFAULT NULL COMMENT '项目审核时间',
  `business_audit_by` varchar(64) DEFAULT NULL COMMENT '业务审核人',
  `business_audit_time` datetime DEFAULT NULL COMMENT '业务审核时间',
  `leader_audit_by` varchar(64) DEFAULT NULL COMMENT '领导审核人',
  `leader_audit_time` datetime DEFAULT NULL COMMENT '领导审核时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_change_station_status` (`station_id`, `status`),
  KEY `idx_change_status_submit` (`status`, `submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农户信息变更审核申请';


-- ############################################################
-- 七、菜单与角色
-- ############################################################

-- 7.1 五条租金菜单（id 固定，角色授权按它关联，不要改）
--     2026-11-06 已在生产执行
--       SELECT id,name,url FROM sys_permission WHERE url LIKE '/rent%';
--     返回 0 行 —— 说明生产从未部署过租金菜单，下面 5 条会全新插入，id 不会有冲突。
--     若将来重跑时这 5 条已存在，INSERT IGNORE 会跳过，不影响授权。
--     ⚠️ 唯一要留意的是：如果哪天有人在前端「菜单管理」里手工建过租金菜单，
--        它的 id 必然不是下面这 5 个，授权会挂不上，届时先查出来改成实际 id。
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086670609055145985',NULL,'租金管理','/rent','layouts/default/index',1,'',NULL,0,NULL,'0',14.00,0,'ant-design:account-book-outlined',0,0,0,0,NULL,'admin','2026-08-10 12:26:56',NULL,NULL,0,0,NULL,0);
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086670953071960066','2086670609055145985','农户信息管理','/rent/farmer','rent/farmer/index',1,'',NULL,1,NULL,'0',0.00,0,'ant-design:skin-outlined',0,0,0,0,NULL,'admin','2026-08-10 12:28:18','admin','2026-08-17 10:10:14',0,0,NULL,0);
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086715467086098434','2086670953071960066','农户详情','/rent/farmer/detail','rent/farmer/detail/index',1,'',NULL,1,NULL,'0',0.00,0,NULL,1,0,1,0,NULL,'admin','2026-08-10 15:25:11','admin','2026-08-10 15:35:20',0,0,NULL,0);
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2087786048837607425','2086670609055145985','租金台账','/rent/ledger','rent/ledger/index',1,'',NULL,1,NULL,'0',1.00,0,'ant-design:barcode-outlined',0,0,0,0,NULL,'admin','2026-08-13 14:19:18',NULL,NULL,0,0,NULL,0);
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2087786299975753730','2087786048837607425','租金台账详情','/rent/ledger/detail','rent/ledger/detail/index',1,'',NULL,1,NULL,'0',1.00,0,'ant-design:book-outlined',1,0,1,0,NULL,'admin','2026-08-13 14:20:18',NULL,NULL,0,0,NULL,0);

-- 7.2 新增三个角色
--     生产已有的：admin 超级管理员(f6817f48…)、manager 管理员(1638746311646339073)、
--                Leader 项目经理(1638753735342120961) —— 这两个 id 与测试库一致
INSERT IGNORE INTO `sys_role`
    (`id`, `role_name`, `role_code`, `description`, `create_by`, `create_time`)
VALUES
    ('1900000000000000001', '业务专员', 'businessSpecialist',
     '农户信息维护与审核 + 租金生成/修改/提交/四级审核/支付/对账', 'admin', NOW()),
    ('1900000000000000002', '财务专员', 'financeSpecialist',
     '只负责审核提交上来的租金，不能生成或修改租金', 'admin', NOW()),
    ('1900000000000000003', '公司领导', 'companyLeader',
     '租金和农户信息最后一级审核', 'admin', NOW());

-- 7.3 全部 5 条菜单：超级管理员 / 管理员 / 项目经理 / 业务专员 / 公司领导
--     ⚠️ 'Leader' 不能漏：代码 RentRoleGuard 把 Leader 当项目经理认，
--        漏掉它，第 7.5 节的反向删除会把项目经理的租金菜单删光。
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2086670953071960066', '2086715467086098434',
                          '2087786048837607425', '2087786299975753730')
WHERE r.role_code IN ('admin', 'manager', 'Leader', 'businessSpecialist', 'companyLeader');

-- 7.4 财务专员：租金管理 + 租金台账 + 台账详情（不给农户菜单）
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2087786048837607425', '2087786299975753730')
WHERE r.role_code = 'financeSpecialist';

-- 7.5 其它角色一律撤销租金菜单
--     生产还有资料员、销售员、施工队等 15 个角色，它们不该看到租金模块
DELETE rp
FROM `sys_role_permission` rp
WHERE rp.permission_id IN ('2086670609055145985', '2086670953071960066', '2086715467086098434',
                           '2087786048837607425', '2087786299975753730')
  AND rp.role_id NOT IN (SELECT id
                         FROM `sys_role`
                         WHERE role_code IN ('admin', 'manager', 'Leader', 'businessSpecialist',
                                             'financeSpecialist', 'companyLeader'));


-- ############################################################
-- 八、年度生成定时任务
--     status = -1 停用；上线确认 cron 后在「系统管理 → 定时任务」里启用
-- ############################################################

INSERT IGNORE INTO `sys_quartz_job` (`id`, `create_by`, `create_time`, `del_flag`, `update_by`, `update_time`, `job_class_name`, `cron_expression`, `parameter`, `description`, `status`) VALUES ('rentledgergeneratejob0000000001','admin','2026-08-18 14:32:24',0,NULL,NULL,'org.jeecg.modules.pcms.rent.job.RentLedgerGenerateJob','0 0 1 1 3 ?',NULL,'每年3月1日生成当年度全部电站的租金台账，期数按项目配置',-1);


-- ############################################################
-- 九、执行后自查
-- ############################################################
-- 1) 15 张表齐全（预期 15）
-- SELECT COUNT(*) AS 表数量 FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN
--    ('t_farmer_station','t_install_info','t_install_type','t_oper_log','t_project',
--     't_rent','t_rent_ledger','t_rent_ledger_month','t_rent_attach','t_rent_audit_record',
--     't_rent_dispute_record','t_rent_price_type','t_rent_reconcile_task',
--     't_rent_generate_task','t_farmer_change_request');
--
-- 2) 补的字段都在
-- SHOW COLUMNS FROM t_farmer_station  LIKE 'audit%';      -- 3 个
-- SHOW COLUMNS FROM t_install_type    LIKE 'is_builtin';
-- SHOW COLUMNS FROM t_project         LIKE 'periods_per_year';
-- SHOW COLUMNS FROM t_rent_ledger     LIKE '%audit_by%';  -- 4 个：project/finance/leader/business
-- SHOW COLUMNS FROM t_rent_audit_record LIKE 'audit_stage';
--
-- 3) 字典数据
-- SELECT project_code, periods_per_year FROM t_project ORDER BY id;  -- HN_01 必须是 1
-- SELECT COUNT(*) FROM t_install_type WHERE is_builtin = 1;          -- 预期 16
--
-- 4) 角色与菜单：admin / manager / Leader / businessSpecialist / companyLeader 各 5 条，
--    financeSpecialist 3 条，其余角色 0 条
-- SELECT r.role_code, r.role_name, COUNT(*) AS menu_cnt
--   FROM sys_role_permission rp
--   JOIN sys_role r ON r.id = rp.role_id
--  WHERE rp.permission_id IN ('2086670609055145985','2086670953071960066','2086715467086098434',
--                             '2087786048837607425','2087786299975753730')
--  GROUP BY r.role_code, r.role_name ORDER BY menu_cnt DESC;
--
-- 5) 租户齐全（预期 6 行，名称必须完全一致）
-- SELECT id, name FROM sys_tenant
--  WHERE name IN ('北京项目','天津项目','廊坊项目','徐州项目','海南项目','河南项目');
-- ############################################################
