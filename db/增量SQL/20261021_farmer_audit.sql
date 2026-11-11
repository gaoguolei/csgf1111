-- ============================================================
-- 农户信息修改审核（需求 28~33）
-- ============================================================
-- 背景：
--   农户信息（主表 + 安装信息）是租金的计价依据。改完之后不能立刻生效到
--   正在走的租金上：提交审核时间所在的那一期及之后的租金要锁住，
--   等审核通过才放开；驳回的话数据回滚到修改前。
--
-- 设计要点：
--   1. 修改是"立即生效 + 存改前快照"，驳回/取消时按快照回滚 ——
--      这样审核弹窗能直接展示"改前/改后"，不用另外推演。
--   2. 待审核期间锁定的是【租金操作】，不是农户数据本身；
--      数据要能继续被审核人看到（以及被驳回后继续改）。
--   3. 新增农户走同一套审核：审核驳回后农户保留但不可用（不能生成租金）。
-- ============================================================

-- ------------------------------------------------------------
-- ① 农户表加审核状态
-- ------------------------------------------------------------
ALTER TABLE `t_farmer_station`
    ADD COLUMN `audit_status` tinyint(4) NOT NULL DEFAULT '0'
        COMMENT '0正常 1新增待审核 2修改待审核 3新增审核驳回 4修改审核驳回' AFTER `del_time`,
    ADD COLUMN `audit_submit_time` datetime DEFAULT NULL COMMENT '最近一次提交审核的时间' AFTER `audit_status`,
    ADD COLUMN `audit_submit_by` varchar(50) DEFAULT NULL COMMENT '最近一次提交审核的人' AFTER `audit_submit_time`;

-- 按审核状态筛选（农户列表的状态栏、导出）
ALTER TABLE `t_farmer_station`
    ADD INDEX `idx_farmer_station_audit` (`audit_status`);

-- ------------------------------------------------------------
-- ② 农户变更申请
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_farmer_change_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `station_id` bigint(20) NOT NULL COMMENT '农户电站信息表ID',
  `change_type` varchar(10) NOT NULL COMMENT 'INSERT新增 UPDATE修改',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING'
      COMMENT 'PENDING待审核 APPROVED通过 REJECTED驳回 CANCELLED取消',
  `before_json` longtext COMMENT '修改前快照（农户主表 + 安装信息 + 租金档位），新增时为空',
  `after_json` longtext COMMENT '修改后快照',
  `submit_by` varchar(50) DEFAULT NULL COMMENT '提交人',
  `submit_time` datetime DEFAULT NULL COMMENT '提交审核时间，租金锁定以此为准',
  `audit_by` varchar(50) DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_change_station_status` (`station_id`, `status`),
  KEY `idx_change_status_submit` (`status`, `submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农户信息变更审核申请';
