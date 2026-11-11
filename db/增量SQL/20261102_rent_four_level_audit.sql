-- ============================================================
-- 需求 36（改版）：四级审核 + 农户三级审核 + 角色权限调整
-- 生成时间：2026-11-02
-- ============================================================
-- 本脚本替代同日的两级审核方案，执行顺序：
--   1) 20261102_rent_roles_and_audit.sql（已执行过则跳过）
--   2) 本脚本
--
-- 内容：
--   一、租金台账：新增项目/领导审核字段，audit_status 改为四级编码
--   二、审核记录：audit_stage 扩展为 PROJECT/BUSINESS/FINANCE/LEADER
--   三、农户：audit_status 扩展为三级审核的 12 个状态
--   四、农户变更申请：新增 audit_stage 和三级审核人/时间
--   五、新增「公司领导」角色，按角色重新授权租金菜单
--
-- ⚠️ ALTER 语句不能重复执行（MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS）。
-- ⚠️ 角色/菜单部分用 INSERT IGNORE / 按 role_code 删除，可重复执行。
-- ============================================================

SET NAMES utf8mb4;

-- ############################################################
-- 一、租金台账：四级审核字段
--     business_audit_by/time、finance_audit_by/time 由上一个脚本创建
-- ############################################################

ALTER TABLE `t_rent_ledger`
    ADD COLUMN `project_audit_by` varchar(64) DEFAULT NULL COMMENT '项目审核人（第一级）' AFTER `submit_by`,
    ADD COLUMN `project_audit_time` datetime DEFAULT NULL COMMENT '项目审核时间' AFTER `project_audit_by`,
    ADD COLUMN `leader_audit_by` varchar(64) DEFAULT NULL COMMENT '领导审核人（第四级）' AFTER `finance_audit_time`,
    ADD COLUMN `leader_audit_time` datetime DEFAULT NULL COMMENT '领导审核时间' AFTER `leader_audit_by`;

-- 旧的两级状态映射到新的四级编码：
--   旧 1 待财务审核      -> 5 待财务审核
--   旧 2 业务通过(待支付) -> 9 领导审核通过(待支付)
--   旧 3 业务驳回        -> 4 业务审核驳回
--   旧 4 待业务审核      -> 3 待业务审核
--   旧 5 财务驳回        -> 6 财务审核驳回
-- 用一条 CASE 完成，避免逐条 UPDATE 时值互相覆盖。
UPDATE `t_rent_ledger`
SET `audit_status` = CASE `audit_status`
                         WHEN 1 THEN 5
                         WHEN 2 THEN 9
                         WHEN 3 THEN 4
                         WHEN 4 THEN 3
                         WHEN 5 THEN 6
                         ELSE `audit_status`
    END
WHERE `audit_status` BETWEEN 1 AND 5;

ALTER TABLE `t_rent_ledger`
    MODIFY COLUMN `audit_status` int(1) DEFAULT '0'
        COMMENT '审核状态 0待提交 1待项目审核 2项目审核驳回 3待业务审核 4业务审核驳回 5待财务审核 6财务审核驳回 7待领导审核 8领导审核驳回 9领导审核通过(待支付)';

-- ############################################################
-- 二、审核记录阶段扩展
--    旧数据的第一级（FINANCE）按位置迁移到 PROJECT，业务级保持不动；
--    历史记录里第一级实际是财务专员审的，位置对齐后才能和新流程拼成一条链。
-- ############################################################

UPDATE `t_rent_audit_record` SET `audit_stage` = 'PROJECT' WHERE `audit_stage` = 'FINANCE';

ALTER TABLE `t_rent_audit_record`
    MODIFY COLUMN `audit_stage` varchar(16) NOT NULL DEFAULT 'PROJECT'
        COMMENT 'PROJECT项目审核 BUSINESS业务审核 FINANCE财务审核 LEADER领导审核';

-- ############################################################
-- 三、农户状态：三级审核
--    新增：1待项目 2项目驳回 3待业务 4业务驳回 5待领导 6领导驳回
--    修改：7待项目 8项目驳回 9待业务 10业务驳回 11待领导 12领导驳回
--
--    旧值映射：
--      旧 1 新增待审核 -> 1 新增待项目审核
--      旧 2 修改待审核 -> 7 修改待项目审核
--      旧 3 新增驳回   -> 2 新增项目审核驳回
--      旧 4 修改驳回   -> 8 修改项目审核驳回
-- ############################################################

UPDATE `t_farmer_station`
SET `audit_status` = CASE `audit_status`
                         WHEN 1 THEN 1
                         WHEN 2 THEN 7
                         WHEN 3 THEN 2
                         WHEN 4 THEN 8
                         ELSE `audit_status`
    END
WHERE `audit_status` BETWEEN 1 AND 4;

ALTER TABLE `t_farmer_station`
    MODIFY COLUMN `audit_status` int(2) DEFAULT '0'
        COMMENT '审核状态 0正常 1新增待项目审核 2新增项目驳回 3新增待业务审核 4新增业务驳回 5新增待领导审核 6新增领导驳回 7修改待项目审核 8修改项目驳回 9修改待业务审核 10修改业务驳回 11修改待领导审核 12修改领导驳回';

-- ############################################################
-- 四、农户变更申请：三级审核阶段与审核人
-- ############################################################

ALTER TABLE `t_farmer_change_request`
    ADD COLUMN `audit_stage` varchar(16) NOT NULL DEFAULT 'PROJECT'
        COMMENT '当前审核阶段 PROJECT项目审核 BUSINESS业务审核 LEADER领导审核' AFTER `status`,
    ADD COLUMN `project_audit_by` varchar(64) DEFAULT NULL COMMENT '项目审核人' AFTER `audit_remark`,
    ADD COLUMN `project_audit_time` datetime DEFAULT NULL COMMENT '项目审核时间' AFTER `project_audit_by`,
    ADD COLUMN `business_audit_by` varchar(64) DEFAULT NULL COMMENT '业务审核人' AFTER `project_audit_time`,
    ADD COLUMN `business_audit_time` datetime DEFAULT NULL COMMENT '业务审核时间' AFTER `business_audit_by`,
    ADD COLUMN `leader_audit_by` varchar(64) DEFAULT NULL COMMENT '领导审核人' AFTER `business_audit_time`,
    ADD COLUMN `leader_audit_time` datetime DEFAULT NULL COMMENT '领导审核时间' AFTER `leader_audit_by`;

-- 升级前待审核的申请统一回到项目审核（站内状态已同步改成"待项目审核"）
UPDATE `t_farmer_change_request`
SET `audit_stage` = 'PROJECT'
WHERE `status` = 'PENDING';

-- ############################################################
-- 五、角色与菜单
--    管理员 admin / 项目经理 manager / 业务专员 businessSpecialist /
--    财务专员 financeSpecialist 由上一个脚本创建；本脚本新增公司领导 companyLeader。
--    菜单可见：
--      全部 5 条：admin / manager / businessSpecialist / companyLeader
--      3 条（无农户）：financeSpecialist
--      其他角色：一律撤销
-- ############################################################

INSERT IGNORE INTO `sys_role`
    (`id`, `role_name`, `role_code`, `description`, `create_by`, `create_time`)
VALUES
    ('1900000000000000003', '公司领导', 'companyLeader',
     '租金和农户信息最后一级审核', 'admin', NOW());

-- 全部租金菜单：管理员、项目经理、业务专员、公司领导
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2086670953071960066', '2086715467086098434',
                          '2087786048837607425', '2087786299975753730')
WHERE r.role_code IN ('admin', 'manager', 'businessSpecialist', 'companyLeader');

-- 财务专员：租金管理 + 租金台账 + 台账详情（没有农户菜单）
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2087786048837607425', '2087786299975753730')
WHERE r.role_code = 'financeSpecialist';

-- 其它角色撤销租金菜单
DELETE rp
FROM `sys_role_permission` rp
WHERE rp.permission_id IN ('2086670609055145985', '2086670953071960066', '2086715467086098434',
                           '2087786048837607425', '2087786299975753730')
  AND rp.role_id NOT IN (SELECT id
                         FROM `sys_role`
                         WHERE role_code IN ('admin', 'manager', 'businessSpecialist',
                                             'financeSpecialist', 'companyLeader'));

-- ============================================================
-- 执行后检查：
--   SELECT role_name, role_code FROM sys_role WHERE role_code IN
--     ('admin','manager','businessSpecialist','financeSpecialist','companyLeader');
--   SELECT r.role_code, p.name FROM sys_role_permission rp
--     JOIN sys_role r ON r.id = rp.role_id
--     JOIN sys_permission p ON p.id = rp.permission_id
--    WHERE p.id IN ('2086670609055145985','2086670953071960066','2086715467086098434',
--                   '2087786048837607425','2087786299975753730')
--    ORDER BY r.role_code, p.name;
-- 预期：4 个角色各 5 条菜单，financeSpecialist 只有 3 条，其他角色 0 条。
-- ============================================================
