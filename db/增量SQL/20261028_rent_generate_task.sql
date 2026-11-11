-- 年度租金台账生成任务
--
-- /rent/ledger/generate 要遍历全部电站，几万个电站跑几分钟，
-- nginx 60 秒就 504 了。改成"提交任务立即返回 → 后台线程跑 → 前端轮询状态"，
-- 任务状态落库而不是放内存：服务重启后还能查，多实例部署也不会查不到。

CREATE TABLE IF NOT EXISTS `t_rent_generate_task` (
  `id`             varchar(36)  NOT NULL                COMMENT '任务ID，UUID',
  `status`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待执行 RUNNING执行中 DONE完成 FAILED失败',
  `rent_year`      int(11)      NOT NULL DEFAULT 0      COMMENT '生成年度',
  `total_stations` int(11)      NOT NULL DEFAULT 0      COMMENT '参与生成的电站总数，进度分母',
  `progress`       int(11)      NOT NULL DEFAULT 0      COMMENT '已处理电站数，前端用它算进度',
  `created_count`  int(11)      NOT NULL DEFAULT 0      COMMENT '新生成的台账条数',
  `skipped_count`  int(11)      NOT NULL DEFAULT 0      COMMENT '跳过的条数',
  `fail_count`     int(11)      NOT NULL DEFAULT 0      COMMENT '失败的电站数',
  `period_range`   varchar(100) DEFAULT NULL            COMMENT '本次覆盖的时间段，如 2026-01-01 ~ 2026-12-31',
  `detail_json`    longtext     DEFAULT NULL            COMMENT '跳过/失败原因明细，结构 {"skip":[],"fail":[]}',
  `error_msg`      varchar(500) DEFAULT NULL            COMMENT '任务整体失败的原因',
  `cost_ms`        bigint(20)   NOT NULL DEFAULT 0      COMMENT '耗时毫秒',
  `create_by`      varchar(50)  DEFAULT NULL            COMMENT '发起人',
  `create_time`    datetime     DEFAULT NULL            COMMENT '发起时间',
  `finish_time`    datetime     DEFAULT NULL            COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_rent_generate_task_status` (`status`),
  KEY `idx_rent_generate_task_create` (`create_by`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租金台账生成任务';
