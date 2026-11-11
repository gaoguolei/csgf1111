-- ============================================================
-- 生产环境（库名 renergy）租金模块部署补丁
-- ============================================================
-- 生成时间：2026-10-18
--
-- 【生产现状】（从生产导出的表结构得出）
--   已有 7 张表，但都是"数据隔离"和"改价次月生效"两个改造【之前】的版本：
--     · t_farmer_station 缺 tenant_id 字段
--     · t_rent 缺 effect_date_from / effect_date_to，唯一键还是旧的三列版
--     · 另外 6 张租金核心表完全不存在
--
-- 【执行顺序】就按本文件从上到下，一次跑完。
-- 【幂等性】建表是 IF NOT EXISTS；ALTER 加字段这一版只跑一次，
--          重复执行会报 Duplicate column（那是正常的，说明已经加过了）。
--
-- ⚠️ 本脚本只动结构，不灌任何业务数据。
-- ============================================================


-- ############################################################
-- 一、建 6 张缺失的表
--    t_rent_ledger 这里已经带了 tenant_id（新建的就是最新结构），
--    所以下面第二节不用再给它加
-- ############################################################


CREATE TABLE IF NOT EXISTS `t_rent_ledger` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `station_id` bigint(20) NOT NULL COMMENT '农户电站信息表ID',
  `farmer_name` varchar(100) DEFAULT NULL COMMENT '农户姓名，冗余',
  `power_user_no` varchar(100) DEFAULT NULL COMMENT '发电户号，冗余',
  `install_addr` varchar(255) DEFAULT NULL COMMENT '安装地址，冗余',
  `id_card` varchar(32) DEFAULT NULL COMMENT '身份证号，冗余',
  `bank_card_no` varchar(64) DEFAULT NULL COMMENT '银行卡号，冗余，对账用',
  `bank_card_name` varchar(100) DEFAULT NULL COMMENT '银行卡姓名，冗余，对账用',
  `project_id` bigint(20) DEFAULT NULL COMMENT '所属项目ID，冗余',
  `rent_year` int(11) NOT NULL COMMENT '租金年度',
  `period_no` int(11) NOT NULL COMMENT '期数 1-4',
  `period_name` varchar(32) DEFAULT NULL COMMENT '期数名称，如 2025年1期',
  `total_blocks` int(11) DEFAULT NULL COMMENT '安装总块数，生成时快照',
  `total_capacity` decimal(18,4) DEFAULT NULL COMMENT '安装总容量kW，生成时快照',
  `grid_date` date DEFAULT NULL COMMENT '并网日期',
  `grid_month_remain_days` int(11) DEFAULT NULL COMMENT '并网当月剩余天数，含并网当天',
  `grid_months` int(11) DEFAULT NULL COMMENT '并网月数',
  `period_begin` date DEFAULT NULL COMMENT '本期租金起算时间',
  `period_end` date DEFAULT NULL COMMENT '本期租金截止时间',
  `period_days` int(11) DEFAULT NULL COMMENT '本期理论计租天数',
  `actual_gen_days` int(11) DEFAULT NULL COMMENT '本期累计实际发电天数，由月明细汇总',
  `cycle_total_amount` decimal(18,2) DEFAULT NULL COMMENT '周期应付累计总金额，全租期理论值',
  `period_payable` decimal(18,2) DEFAULT NULL COMMENT '本期应付金额，默认按月明细汇总，支持手工调整',
  `payable_adjust_flag` int(1) NOT NULL DEFAULT '0' COMMENT '1=本期应付被人工调整过，锁住不再被月明细重算覆盖',
  `accum_payable` decimal(18,2) DEFAULT NULL COMMENT '截止本期累计应付总金额',
  `prev_accum_paid` decimal(18,2) DEFAULT NULL COMMENT '截止上期累计实付金额，不含本期',
  `period_paid` decimal(18,2) DEFAULT NULL COMMENT '本期实付金额，可手工调整',
  `accum_paid` decimal(18,2) DEFAULT NULL COMMENT '截止本期累计实付金额，含本期',
  `diff_amount` decimal(18,2) DEFAULT NULL COMMENT '超/欠付金额，正超付负欠付',
  `audit_status` int(1) DEFAULT '0' COMMENT '审核状态 0待提交 1审核中 2审核通过 3已驳回',
  `pay_status` int(1) DEFAULT '0' COMMENT '支付状态 0未支付 1已支付',
  `recon_status` int(1) DEFAULT '0' COMMENT '对账状态 0未对账 1已对账 2对账异常',
  `receipt_amount` decimal(18,2) DEFAULT NULL COMMENT '银行回单上识别出的金额',
  `recon_remark` varchar(1000) DEFAULT NULL COMMENT '对账异常说明',
  `recon_time` datetime DEFAULT NULL COMMENT '对账时间',
  `dispute_flag` int(1) DEFAULT '0' COMMENT '1=纠纷暂停支付，此状态不可提交审核',
  `dispute_remark` varchar(1000) DEFAULT NULL COMMENT '最近一次标记纠纷的原因；历次见 t_rent_dispute_record',
  `dispute_round` int(11) NOT NULL DEFAULT '0' COMMENT '第几次纠纷，从1开始，每标记一次+1',
  `dispute_time` datetime DEFAULT NULL COMMENT '标记纠纷暂停支付的时间，反复标记时记最近一次',
  `dispute_cancel_time` datetime DEFAULT NULL COMMENT '取消纠纷暂停支付的时间，反复标记时记最近一次',
  `reject_remark` varchar(1000) DEFAULT NULL COMMENT '最近一次驳回原因；历次原因见 t_rent_audit_record',
  `audit_round` int(11) NOT NULL DEFAULT '0' COMMENT '审核轮次，从1开始，每次提交审核+1',
  `submit_time` datetime DEFAULT NULL COMMENT '提交审核时间',
  `submit_by` varchar(64) DEFAULT NULL COMMENT '提交人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_by` varchar(64) DEFAULT NULL COMMENT '审核人',
  `void_flag` int(1) DEFAULT '0' COMMENT '1=已作废',
  `void_remark` varchar(1000) DEFAULT NULL COMMENT '作废原因',
  `regen_flag` int(1) DEFAULT '0' COMMENT '1=重新生成的行',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `del_flag` int(1) DEFAULT '0' COMMENT '0正常 1删除',
  `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID，关联sys_tenant.id',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ledger_station_period` (`station_id`,`rent_year`,`period_no`),
  KEY `idx_ledger_year_period` (`rent_year`,`period_no`),
  KEY `idx_ledger_farmer` (`farmer_name`),
  KEY `idx_ledger_user_no` (`power_user_no`),
  KEY `idx_ledger_audit` (`audit_status`),
  KEY `idx_ledger_project` (`project_id`),
  KEY `idx_rent_ledger_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33326 DEFAULT CHARSET=utf8mb4 COMMENT='租金台账';
CREATE TABLE IF NOT EXISTS `t_rent_ledger_month` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '台账ID',
  `ym` varchar(7) DEFAULT NULL COMMENT '年月 yyyy-MM',
  `seg_begin` date DEFAULT NULL COMMENT '该月计租起日',
  `seg_end` date DEFAULT NULL COMMENT '该月计租止日',
  `month_days` int(11) DEFAULT NULL COMMENT '当月自然天数',
  `rent_days` int(11) DEFAULT NULL COMMENT '该月理论计租天数',
  `gen_days` int(11) DEFAULT NULL COMMENT '该月实际发电天数，人工录入，空=未录入',
  `month_rent` decimal(18,2) DEFAULT NULL COMMENT '满月租金 = 年租金 ÷ 12',
  `theory_amount` decimal(18,2) DEFAULT NULL COMMENT '该月理论金额，按 rent_days 算',
  `amount` decimal(18,2) DEFAULT NULL COMMENT '该月金额，录入发电天数后按天折算',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ledger_month` (`ledger_id`,`ym`)
) ENGINE=InnoDB AUTO_INCREMENT=102621 DEFAULT CHARSET=utf8mb4 COMMENT='租金台账月明细';
CREATE TABLE IF NOT EXISTS `t_rent_attach` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '台账ID',
  `biz_type` varchar(16) DEFAULT 'ATTACH' COMMENT 'ATTACH普通附件 RECEIPT银行回单 AUDIT审核附件',
  `file_name` varchar(255) DEFAULT NULL COMMENT '用户上传时的原始文件名，列表按这个显示',
  `file_path` varchar(500) DEFAULT NULL COMMENT '服务器相对存储路径',
  `file_ext` varchar(16) DEFAULT NULL COMMENT '扩展名，小写不带点',
  `file_size` bigint(20) DEFAULT NULL COMMENT '字节数',
  `is_image` int(1) DEFAULT '0' COMMENT '1=图片，可在线预览',
  `audit_round` int(11) NOT NULL DEFAULT '0' COMMENT '上传时台账处于第几轮审核，0=升级前的历史数据',
  `del_flag` int(1) DEFAULT '0' COMMENT '0正常 1删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '上传人',
  `create_time` datetime DEFAULT NULL COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_attach_ledger` (`ledger_id`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COMMENT='租金附件';
CREATE TABLE IF NOT EXISTS `t_rent_audit_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '台账ID',
  `audit_round` int(11) NOT NULL DEFAULT '0' COMMENT '审核轮次，从1开始，每次提交审核+1',
  `action` varchar(16) DEFAULT NULL COMMENT 'SUBMIT提交 APPROVE通过 REJECT驳回',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注；驳回时是驳回原因，非必填',
  `oper_by` varchar(64) DEFAULT NULL COMMENT '操作人',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_ledger_round` (`ledger_id`,`audit_round`)
) ENGINE=InnoDB AUTO_INCREMENT=150 DEFAULT CHARSET=utf8mb4 COMMENT='租金审核记录';
CREATE TABLE IF NOT EXISTS `t_rent_dispute_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '台账ID',
  `dispute_round` int(11) NOT NULL DEFAULT '0' COMMENT '第几次纠纷，从1开始',
  `dispute_time` datetime DEFAULT NULL COMMENT '标记纠纷暂停支付的时间',
  `dispute_remark` varchar(1000) DEFAULT NULL COMMENT '标记纠纷的原因',
  `dispute_by` varchar(64) DEFAULT NULL COMMENT '标记人',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消纠纷的时间，空=还在暂停中',
  `cancel_remark` varchar(1000) DEFAULT NULL COMMENT '取消纠纷的原因',
  `cancel_by` varchar(64) DEFAULT NULL COMMENT '取消人',
  PRIMARY KEY (`id`),
  KEY `idx_dispute_ledger_round` (`ledger_id`,`dispute_round`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COMMENT='租金纠纷暂停支付记录';
CREATE TABLE IF NOT EXISTS `t_rent_reconcile_task` (
  `id` varchar(36) NOT NULL COMMENT '任务ID，UUID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待执行 RUNNING执行中 DONE完成 FAILED失败',
  `file_count` int(11) NOT NULL DEFAULT '0' COMMENT '上传的文件数',
  `total_pages` int(11) NOT NULL DEFAULT '0' COMMENT '拆出来的总页数',
  `progress` int(11) NOT NULL DEFAULT '0' COMMENT '已处理页数，前端用它显示进度',
  `total` int(11) NOT NULL DEFAULT '0' COMMENT '识别出的回单/清单行总条数',
  `ok_count` int(11) NOT NULL DEFAULT '0' COMMENT '对账一致条数',
  `error_count` int(11) NOT NULL DEFAULT '0' COMMENT '对账异常条数',
  `fail_count` int(11) NOT NULL DEFAULT '0' COMMENT '未能处理条数',
  `result_json` longtext COMMENT '各条明细，JSON 数组',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '任务整体失败的原因',
  `ledger_id` bigint(20) DEFAULT NULL COMMENT '详情页传的台账ID，列表页批量为空',
  `create_by` varchar(50) DEFAULT NULL COMMENT '发起人',
  `create_time` datetime DEFAULT NULL COMMENT '发起时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_rent_reconcile_task_create` (`create_by`,`create_time`),
  KEY `idx_rent_reconcile_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租金回单对账任务';




-- ############################################################
-- 二、给已有的 t_farmer_station 补 tenant_id（需求第14条：数据隔离）
--    生产这张表是空的，没有存量数据要回填，加字段即可
-- ############################################################

ALTER TABLE `t_farmer_station`
    ADD COLUMN `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID，用于数据隔离' AFTER `del_time`,
    ADD INDEX `idx_farmer_station_tenant` (`tenant_id`);


-- ############################################################
-- 三、给已有的 t_rent 补单价生效区间（需求第21条：改价次月生效）
--    并重建唯一键 —— 旧的 (install_id,price_type_id,del_time) 不允许
--    同一档位留两条记录，表达不了"旧价 + 新价"两个时间段
-- ############################################################

ALTER TABLE `t_rent`
    ADD COLUMN `effect_date_from` date DEFAULT NULL COMMENT '单价生效起日，NULL=从并网日或档位起始日起' AFTER `amount`,
    ADD COLUMN `effect_date_to` date DEFAULT NULL COMMENT '单价生效止日，NULL=到租期结束' AFTER `effect_date_from`;

ALTER TABLE `t_rent` DROP INDEX `uk_install_price`;
ALTER TABLE `t_rent` ADD UNIQUE KEY `uk_install_price` (`install_id`,`price_type_id`,`del_time`,`effect_date_from`);


-- ############################################################
-- 四、执行后自查
-- ############################################################
-- 1) 13 张表应该全在
-- SELECT COUNT(*) AS 表数量 FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 't\_%';
--
-- 2) 两处字段应该都有返回
-- SHOW COLUMNS FROM t_farmer_station LIKE 'tenant_id';
-- SHOW COLUMNS FROM t_rent           LIKE 'effect_date%';
--
-- 3) 唯一键应该是四列了
-- SHOW INDEX FROM t_rent WHERE Key_name = 'uk_install_price';
--
-- 4) 菜单和定时任务（生产若没有，见下一节说明）
-- SELECT COUNT(*) FROM sys_permission WHERE url LIKE '/rent%';
-- SELECT COUNT(*) FROM sys_quartz_job WHERE job_class_name LIKE '%Rent%';
