-- ============================================================
-- 需求 36：租金模块角色权限 + 财务/业务两级审核
-- 生成时间：2026-11-02
-- ============================================================
-- 内容：
--   一、租金台账 / 审核记录加字段（两级审核）
--   二、新增「业务专员」「财务专员」两个角色
--   三、租金菜单按角色授权（其他角色一律隐藏）
--
-- 执行顺序：从头到尾一次执行。
-- ⚠️ 本脚本的 ALTER 语句不能重复执行（MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS），
--    重复执行会报 Duplicate column。角色/菜单部分用 INSERT IGNORE / 按 id 删除，可重复执行。
--
-- ⚠️ 角色 id 取固定值，便于授权；生产库若已有同 id 角色请先确认。
-- ============================================================

SET NAMES utf8mb4;

-- ############################################################
-- 一、两级审核字段
-- ############################################################

-- 财务、业务两级的审核人/时间分别留一份；audit_by/audit_time 继续表示"最近一次审核"
ALTER TABLE `t_rent_ledger`
    ADD COLUMN `finance_audit_by` varchar(64) DEFAULT NULL COMMENT '财务审核人' AFTER `audit_by`,
    ADD COLUMN `finance_audit_time` datetime DEFAULT NULL COMMENT '财务审核时间' AFTER `finance_audit_by`,
    ADD COLUMN `business_audit_by` varchar(64) DEFAULT NULL COMMENT '业务审核人' AFTER `finance_audit_time`,
    ADD COLUMN `business_audit_time` datetime DEFAULT NULL COMMENT '业务审核时间' AFTER `business_audit_by`;

-- 审核状态扩展：保留 0/1/2/3 原语义，新增 4/5。
-- 历史数据不需要改值：1 现在显示"待财务审核"，2 显示"业务审核通过"，3 显示"业务审核驳回"。
ALTER TABLE `t_rent_ledger`
    MODIFY COLUMN `audit_status` int(1) DEFAULT '0'
        COMMENT '审核状态 0待提交 1待财务审核 2业务审核通过 3业务审核驳回 4待业务审核 5财务审核驳回';

-- 审核记录区分财务/业务阶段。历史记录都是原来的单级审核，统一按业务审核展示。
ALTER TABLE `t_rent_audit_record`
    ADD COLUMN `audit_stage` varchar(16) NOT NULL DEFAULT 'BUSINESS'
        COMMENT 'FINANCE财务审核 BUSINESS业务审核' AFTER `audit_round`;

-- ############################################################
-- 二、新增角色
--    管理员 = admin，项目经理 = manager（系统已有，不新增）
-- ############################################################

INSERT IGNORE INTO `sys_role`
    (`id`, `role_name`, `role_code`, `description`, `create_by`, `create_time`)
VALUES
    ('1900000000000000001', '业务专员', 'businessSpecialist',
     '农户信息维护与审核 + 租金生成/修改/提交/两级审核/支付/对账', 'admin', NOW()),
    ('1900000000000000002', '财务专员', 'financeSpecialist',
     '只负责审核管理员/项目经理提交的租金信息，不能生成或修改租金', 'admin', NOW());

-- ############################################################
-- 三、菜单授权
--    租金菜单 5 条：
--      2086670609055145985 租金管理
--      2086670953071960066 农户信息管理
--      2086715467086098434 农户详情
--      2087786048837607425 租金台账
--      2087786299975753730 租金台账详情
-- ############################################################

-- 业务专员：全部 5 条（拥有所有权限）
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2086670953071960066', '2086715467086098434',
                          '2087786048837607425', '2087786299975753730')
WHERE r.role_code = 'businessSpecialist';

-- 财务专员：租金管理 + 租金台账 + 台账详情（不给农户菜单，只能看要审的租金）
INSERT IGNORE INTO `sys_role_permission`
    (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT MD5(CONCAT(r.id, ':', p.id)), r.id, p.id, NULL, NOW(), NULL
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.id IN ('2086670609055145985', '2087786048837607425', '2087786299975753730')
WHERE r.role_code = 'financeSpecialist';

-- 其它角色（当前是 Leader 等）撤销租金菜单：需求36只允许
-- 管理员 / 项目经理 / 业务专员 / 财务专员 / 公司领导 看到租金模块
-- 注意：companyLeader 由 20261102_rent_four_level_audit.sql 创建并授权。
--       这里必须把它列进白名单，否则先跑四级脚本、再重跑本脚本时会把公司领导的菜单删掉。
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
--     ('admin','manager','businessSpecialist','financeSpecialist');
--   SELECT r.role_code, p.name FROM sys_role_permission rp
--     JOIN sys_role r ON r.id = rp.role_id
--     JOIN sys_permission p ON p.id = rp.permission_id
--    WHERE p.id IN ('2086670609055145985','2086670953071960066','2086715467086098434',
--                   '2087786048837607425','2087786299975753730')
--    ORDER BY r.role_code, p.name;
-- 预期只剩 admin / manager / businessSpecialist / financeSpecialist 四个角色，
-- 其中 financeSpecialist 没有「农户信息管理」「农户详情」。
-- ============================================================
