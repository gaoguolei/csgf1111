-- ============================================================
-- 光伏租金模块 — 生产环境部署脚本
-- 生成时间：2026-10-09
-- ============================================================
-- 使用说明：
--   本脚本包含租金模块(t_rent_* / t_farmer_station / t_install_* / t_project / t_oper_log)
--   的全部建表语句 + 字典初始化数据 + 菜单权限 + 定时任务。
--
--   执行顺序：从头到尾一次执行即可。
--   脚本已做幂等处理（CREATE TABLE IF NOT EXISTS + INSERT IGNORE），
--   重复执行不会报错、也不会产生重复数据。
--
--   执行方式：
--     mysql -h <生产库地址> -u <账号> -p <库名> < 20261009_rent_module_deploy.sql
--   或在 Navicat / DBeaver 里整段执行。
--
--   ⚠️ 前置条件：生产库必须已经跑过 JeecgBoot 基础库脚本，
--      即 sys_permission / sys_role_permission / sys_quartz_job /
--      sys_tenant / sys_role 这几张基础表已存在（本模块的菜单和定时任务要写进这些表）。
--
--   ⚠️ 业务数据（农户/安装/台账）未包含在内，生产环境从 0 开始。
--      如需从测试库迁移业务数据，请另行导出。
--
--   ⚠️ 定时任务 sys_quartz_job 的 status = -1（停用），
--      上线后需在「系统管理 → 定时任务」里确认 cron 并启用。
--
--   ⚠️ 菜单权限的 role_id 取自测试库（admin / manager / Leader）。
--      若生产环境角色 id 不同，需按实际角色 id 调整第三节的 role_id。
-- ============================================================

-- ############################################################
-- 一、建表（12 张）
-- ############################################################

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `t_farmer_station` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `farmer_name` varchar(100) NOT NULL COMMENT '农户名称/客户姓名，可能是村委会、小学、财政所等单位',
  `id_card` varchar(32) DEFAULT NULL COMMENT '农户身份证号，仅天津/廊坊有',
  `bank_card_no` varchar(40) DEFAULT NULL COMMENT '银行卡号，北京没有',
  `bank_card_name` varchar(100) DEFAULT NULL COMMENT '银行卡姓名，北京没有',
  `install_addr` varchar(255) DEFAULT NULL COMMENT '电站安装地址',
  `power_user_no` varchar(255) NOT NULL COMMENT '发电户号，纯字符串原样存：允许一格多号（河南最长98字符）和重复时的 -1/-2 后缀；系统不解析不拆分',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID -> t_project.id',
  `region_code` varchar(20) NOT NULL COMMENT '大区编码，冗余，避免列表联表',
  `station_code` varchar(64) DEFAULT NULL COMMENT '编号：北京为"编号"GJ202405069643，天津为"编码"WEQY，徐州/海南/河南无',
  `salesman` varchar(50) DEFAULT NULL COMMENT '业务员，仅天津/廊坊"业务"列',
  `grid_date` date DEFAULT NULL COMMENT '并网日期',
  `actual_gen_date` date DEFAULT NULL COMMENT '实际发电日期，仅河南有',
  `rent_deadline` date DEFAULT NULL COMMENT '租金截止日期 = 并网日期 + t_project.lease_years，落库时算好',
  `total_capacity` decimal(12,4) DEFAULT NULL COMMENT '安装总容量，统一kW；天津源文件是W，导入时除以1000',
  `total_blocks` int(11) DEFAULT NULL COMMENT '安装总块数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0正常 1删除，逻辑删除',
  `del_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间，未删除固定为1970-01-01；参与唯一键，避免同一户号反复删除新增时唯一键冲突',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `project_code` varchar(255) DEFAULT NULL COMMENT '项目编号（预留字段）',
  `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID，用于数据隔离',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_project_user` (`project_id`,`power_user_no`,`del_time`) USING BTREE COMMENT '同项目下户号唯一，导入去重依据',
  KEY `idx_name` (`farmer_name`) USING BTREE,
  KEY `idx_power_user_no` (`power_user_no`) USING BTREE,
  KEY `idx_grid_date` (`grid_date`) USING BTREE,
  KEY `idx_rent_deadline` (`rent_deadline`) USING BTREE,
  KEY `idx_region` (`region_code`) USING BTREE,
  KEY `idx_project` (`project_id`) USING BTREE,
  KEY `idx_addr` (`install_addr`(64)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4271 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='农户电站信息表';
CREATE TABLE IF NOT EXISTS `t_install_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `station_id` bigint(20) NOT NULL COMMENT '农户电站信息表ID -> t_farmer_station.id',
  `farmer_name` varchar(100) NOT NULL COMMENT '农户姓名',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID',
  `power_user_no` varchar(255) NOT NULL COMMENT '发电户号，冗余',
  `install_type_id` bigint(20) NOT NULL COMMENT '安装类型ID -> t_install_type.id',
  `module_model` varchar(100) DEFAULT NULL COMMENT '组件型号w，字符串原样存：存在 550+580 / 630*50 635*269 / 630W50块 换行 635W8块 / 632.551020408163 等值',
  `is_multi_model` tinyint(4) NOT NULL DEFAULT '0' COMMENT '1=该行型号为多型号混写，待人工拆分',
  `block_count` int(11) NOT NULL DEFAULT '0' COMMENT '安装块数',
  `capacity_kw` decimal(12,4) DEFAULT NULL COMMENT '该类型容量kW，型号×块数；多型号行留空',
  `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0正常 1删除',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_station` (`station_id`) USING BTREE,
  KEY `idx_project_type` (`project_id`,`install_type_id`) USING BTREE,
  KEY `idx_multi_model` (`is_multi_model`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7956 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='农户安装信息表';
CREATE TABLE IF NOT EXISTS `t_install_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_code` varchar(20) NOT NULL COMMENT '所属大区，决定该类型在哪些项目下可选',
  `type_code` varchar(40) NOT NULL COMMENT '安装类型编码',
  `type_name` varchar(60) NOT NULL COMMENT '安装类型名称',
  `is_builtin` tinyint(4) NOT NULL DEFAULT '0' COMMENT '1=系统内置，导入接口依赖其type_code，禁止删除和修改编码；0=运营自建',
  `sort_no` int(11) NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1启用 0停用。停用后不出现在新增下拉里，但历史数据仍能正常显示',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_region_type` (`region_code`,`type_code`) USING BTREE,
  UNIQUE KEY `uk_region_name` (`region_code`,`type_name`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='安装类型字典，各项目安装类型不同';
CREATE TABLE IF NOT EXISTS `t_oper_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `oper_user` varchar(50) NOT NULL COMMENT '操作用户',
  `oper_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `action` varchar(20) NOT NULL COMMENT '动作：INSERT/UPDATE/DELETE/AUDIT/IMPORT/EXPORT',
  `biz_type` varchar(40) DEFAULT NULL COMMENT '业务类型：农户信息/安装信息/租金/导入',
  `biz_id` bigint(20) DEFAULT NULL COMMENT '业务主键',
  `oper_content` text COMMENT '操作内容，改前改后JSON',
  `ip` varchar(64) DEFAULT NULL COMMENT '操作IP',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_time` (`oper_time`) USING BTREE,
  KEY `idx_biz` (`biz_type`,`biz_id`) USING BTREE,
  KEY `idx_user` (`oper_user`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=33682 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='操作日志记录表';
CREATE TABLE IF NOT EXISTS `t_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_code` varchar(20) NOT NULL COMMENT '大区编码，对应5个导入接口：BJ/TJLF/XZ/HAIN/HENAN',
  `region_name` varchar(50) NOT NULL COMMENT '大区名称',
  `project_code` varchar(50) NOT NULL COMMENT '项目编码',
  `project_name` varchar(100) NOT NULL COMMENT '项目全称，取Excel中"所属项目"原值',
  `lease_years` int(11) NOT NULL DEFAULT '25' COMMENT '租期年限，用于算租金截止日期',
  `periods_per_year` int(11) NOT NULL DEFAULT '4' COMMENT '一年结算几期租金，只能取12的约数：1/2/3/4/6/12',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_project_code` (`project_code`) USING BTREE,
  KEY `idx_region` (`region_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='项目档案表';
CREATE TABLE IF NOT EXISTS `t_rent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `station_id` bigint(20) NOT NULL COMMENT '农户电站信息表ID -> t_farmer_station.id',
  `install_id` bigint(20) NOT NULL COMMENT '安装信息表ID -> t_install_info.id',
  `farmer_name` varchar(100) NOT NULL COMMENT '农户姓名，冗余',
  `power_user_no` varchar(255) NOT NULL COMMENT '发电户号，冗余',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID，冗余',
  `price_type_id` bigint(20) NOT NULL COMMENT '租金单价类型ID -> t_rent_price_type.id',
  `unit_price` decimal(12,4) NOT NULL COMMENT '租金单价，元/块/年',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '金额 = 单价 × 块数，元/年',
  `effect_date_from` date DEFAULT NULL COMMENT '单价生效起日，NULL=从并网日或档位起始日起',
  `effect_date_to` date DEFAULT NULL COMMENT '单价生效止日，NULL=到租期结束',
  `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0正常 1删除',
  `del_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间，未删除固定为1970-01-01，参与唯一键',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_install_price` (`install_id`,`price_type_id`,`del_time`,`effect_date_from`),
  KEY `idx_station` (`station_id`) USING BTREE,
  KEY `idx_project` (`project_id`) USING BTREE,
  KEY `idx_price_type` (`price_type_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16080 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='租金表';
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
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COMMENT='租金附件';
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
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COMMENT='租金审核记录';
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='租金纠纷暂停支付记录';
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
) ENGINE=InnoDB AUTO_INCREMENT=33292 DEFAULT CHARSET=utf8mb4 COMMENT='租金台账';
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
) ENGINE=InnoDB AUTO_INCREMENT=102213 DEFAULT CHARSET=utf8mb4 COMMENT='租金台账月明细';
CREATE TABLE IF NOT EXISTS `t_rent_price_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_code` varchar(20) NOT NULL COMMENT '所属大区',
  `type_code` varchar(40) NOT NULL COMMENT '档位编码',
  `type_name` varchar(60) NOT NULL COMMENT '档位名称',
  `split_mode` tinyint(4) NOT NULL DEFAULT '1' COMMENT '切分方式：1=按年，相对每户并网日；2=按自然日期，全体农户统一时点',
  `start_year` int(11) DEFAULT NULL COMMENT 'split_mode=1时必填：起始年，相对并网日期，含',
  `end_year` int(11) DEFAULT NULL COMMENT 'split_mode=1时必填：结束年，含',
  `effect_date_from` date DEFAULT NULL COMMENT 'split_mode=2时用：生效起日，NULL=从并网日起',
  `effect_date_to` date DEFAULT NULL COMMENT 'split_mode=2时用：生效止日，NULL=到租期结束',
  `sort_no` int(11) NOT NULL DEFAULT '0' COMMENT '排序号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_region_price_type` (`region_code`,`type_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='租金单价档位字典';

-- ############################################################
-- 二、字典初始化数据
--    t_project(项目) / t_install_type(安装房型) / t_rent_price_type(租金档位)
--    ⚠️ 这三张表的 id 被业务数据引用，务必原样导入，不要改动 id
-- ############################################################

INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'BJ','北京项目','BJ_01','北京市户用分布式光伏项目一期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:36');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2,'BJ','北京项目','BJ_02','北京二期27MWp分布式光伏项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:37');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3,'BJ','北京项目','BJ_03','北京分布式光伏项目三期（23.23MW）',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:38');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4,'TJLF','天津廊坊项目','TJ_01','天津一期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:39');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5,'TJLF','天津廊坊项目','TJ_02','天津二期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:41');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6,'TJLF','天津廊坊项目','TJ_03','天津三期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:41');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7,'TJLF','天津廊坊项目','TJ_04','天津四期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:42');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8,'TJLF','天津廊坊项目','TJ_05','天津五期',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:42');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9,'TJLF','天津廊坊项目','LF_01','廊坊',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:43');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10,'XZ','徐州项目','XZ_01','徐州项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:44');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11,'HAIN','海南项目','HN_01','海南项目',25,1,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-18 14:56:09');
INSERT  IGNORE INTO `t_project` (`id`, `region_code`, `region_name`, `project_code`, `project_name`, `lease_years`, `periods_per_year`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12,'HENAN','河南项目','HEN_01','河南项目',25,4,1,NULL,'2026-07-28 10:28:38',NULL,'2026-08-10 14:31:45');

INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'BJ','BJ_SUNROOM','阳光房',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2,'BJ','BJ_SOUTH','南坡',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3,'BJ','BJ_NORTH','北坡',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4,'BJ','BJ_EW','东西坡',1,4,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5,'TJLF','TJ_SUNROOM','阳光房',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6,'TJLF','TJ_SN','南北坡',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7,'TJLF','TJ_15DEG','其他15度角',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8,'XZ','XZ_DOUBLE','双坡/庭院',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9,'XZ','XZ_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10,'XZ','XZ_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11,'HAIN','HN_DOUBLE','双坡/庭院',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12,'HAIN','HN_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (13,'HAIN','HN_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (14,'HENAN','HEN_SLOPE','斜屋面',1,1,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (15,'HENAN','HEN_SUN5','阳光房5°',1,2,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
INSERT  IGNORE INTO `t_install_type` (`id`, `region_code`, `type_code`, `type_name`, `is_builtin`, `sort_no`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (16,'HENAN','HEN_SUN10','阳光房10°',1,3,1,NULL,'2026-08-10 16:13:48',NULL,'2026-08-10 16:13:57');
-- 49~55 是测试同事手工建着玩的自定义房型（「你好」「14」「哈哈」等），
-- 不带到生产。生产只需要上面 16 条内置房型。


INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (1,'BJ','BJ_Y1_5','前5年',1,1,5,NULL,NULL,1);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (2,'BJ','BJ_Y6_25','后20年',1,6,25,NULL,NULL,2);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (3,'TJLF','TJ_Y1_5','1-5年',1,1,5,NULL,NULL,1);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (4,'TJLF','TJ_Y6_25','6-25年',1,6,25,NULL,NULL,2);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (5,'XZ','XZ_Y1_10','前10年',1,1,10,NULL,NULL,1);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (6,'XZ','XZ_Y11_25','后15年',1,11,25,NULL,NULL,2);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (7,'HAIN','HN_Y1_10','前10年',1,1,10,NULL,NULL,1);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (8,'HAIN','HN_Y11_25','后15年',1,11,25,NULL,NULL,2);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (9,'HAIN','HN_Y1_8','前8年',1,1,8,NULL,NULL,3);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (10,'HAIN','HN_Y9_25','后17年',1,9,25,NULL,NULL,4);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (11,'HENAN','HEN_Y1_15','前15年',1,1,15,NULL,NULL,1);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (12,'HENAN','HEN_Y16_25','后10年',1,16,25,NULL,NULL,2);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (13,'HENAN','HEN_D_BEF','2025年3月31日之前',2,NULL,NULL,NULL,'2025-03-31',3);
INSERT  IGNORE INTO `t_rent_price_type` (`id`, `region_code`, `type_code`, `type_name`, `split_mode`, `start_year`, `end_year`, `effect_date_from`, `effect_date_to`, `sort_no`) VALUES (14,'HENAN','HEN_D_AFT','2025年4月1日之后',2,NULL,NULL,'2025-04-01',NULL,4);

-- ############################################################
-- 三、菜单与权限
--    sys_permission      租金管理菜单(5条)
--    sys_role_permission 已授权角色(admin / manager / Leader)
--    ⚠️ 若生产环境的角色 id 与测试库不同，需要按实际角色 id 调整
--       sys_role_permission 里的 role_id 字段
-- ############################################################

INSERT  IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086670609055145985',NULL,'租金管理','/rent','layouts/default/index',1,'',NULL,0,NULL,'0',14.00,0,'ant-design:account-book-outlined',0,0,0,0,NULL,'admin','2026-08-10 12:26:56',NULL,NULL,0,0,NULL,0);
INSERT  IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086670953071960066','2086670609055145985','农户信息管理','/rent/farmer','rent/farmer/index',1,'',NULL,1,NULL,'0',0.00,0,'ant-design:skin-outlined',0,0,0,0,NULL,'admin','2026-08-10 12:28:18','admin','2026-08-17 10:10:14',0,0,NULL,0);
INSERT  IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2086715467086098434','2086670953071960066','农户详情','/rent/farmer/detail','rent/farmer/detail/index',1,'',NULL,1,NULL,'0',0.00,0,NULL,1,0,1,0,NULL,'admin','2026-08-10 15:25:11','admin','2026-08-10 15:35:20',0,0,NULL,0);
INSERT  IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2087786048837607425','2086670609055145985','租金台账','/rent/ledger','rent/ledger/index',1,'',NULL,1,NULL,'0',1.00,0,'ant-design:barcode-outlined',0,0,0,0,NULL,'admin','2026-08-13 14:19:18',NULL,NULL,0,0,NULL,0);
INSERT  IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`) VALUES ('2087786299975753730','2087786048837607425','租金台账详情','/rent/ledger/detail','rent/ledger/detail/index',1,'',NULL,1,NULL,'0',1.00,0,'ant-design:book-outlined',1,0,1,0,NULL,'admin','2026-08-13 14:20:18',NULL,NULL,0,0,NULL,0);

INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2086671093069438978','f6817f48af4fb3af11b9e8bf182f618b','2086670609055145985',NULL,'2026-08-10 12:28:52','10.42.0.21');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2086671093082021889','f6817f48af4fb3af11b9e8bf182f618b','2086670953071960066',NULL,'2026-08-10 12:28:52','10.42.0.21');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2086724915930263554','f6817f48af4fb3af11b9e8bf182f618b','2086715467086098434',NULL,'2026-08-10 16:02:44','10.42.0.21');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2087786372478492673','f6817f48af4fb3af11b9e8bf182f618b','2087786048837607425',NULL,'2026-08-13 14:20:35','10.42.0.21');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2087786372486881282','f6817f48af4fb3af11b9e8bf182f618b','2087786299975753730',NULL,'2026-08-13 14:20:35','10.42.0.21');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377077221662721','1638746311646339073','2086670609055145985',NULL,'2026-10-09 10:00:42','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377077221662722','1638746311646339073','2086670953071960066',NULL,'2026-10-09 10:00:42','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377077221662723','1638746311646339073','2086715467086098434',NULL,'2026-10-09 10:00:42','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377077221662724','1638746311646339073','2087786048837607425',NULL,'2026-10-09 10:00:42','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377077221662725','1638746311646339073','2087786299975753730',NULL,'2026-10-09 10:00:42','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377193475186690','1638753735342120961','2086670609055145985',NULL,'2026-10-09 10:01:09','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377193475186691','1638753735342120961','2086670953071960066',NULL,'2026-10-09 10:01:09','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377193475186692','1638753735342120961','2086715467086098434',NULL,'2026-10-09 10:01:09','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377193475186693','1638753735342120961','2087786048837607425',NULL,'2026-10-09 10:01:09','192.168.3.74');
INSERT  IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`) VALUES ('2108377193475186694','1638753735342120961','2087786299975753730',NULL,'2026-10-09 10:01:09','192.168.3.74');

-- ############################################################
-- 四、定时任务（年度租金台账生成）
--    ⚠️ status = -1 表示停用。确认 cron_expression 后手动启用。
-- ############################################################

INSERT  IGNORE INTO `sys_quartz_job` (`id`, `create_by`, `create_time`, `del_flag`, `update_by`, `update_time`, `job_class_name`, `cron_expression`, `parameter`, `description`, `status`) VALUES ('rentledgergeneratejob0000000001','admin','2026-08-18 14:32:24',0,NULL,NULL,'org.jeecg.modules.pcms.rent.job.RentLedgerGenerateJob','0 0 1 1 3 ?',NULL,'每年3月1日生成当年度全部电站的租金台账，期数按项目配置',-1);

