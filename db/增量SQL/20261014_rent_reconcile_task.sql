-- 回单对账异步任务表
--
-- 背景：客户一次传一个几十页的 PDF，拆页 + 逐页 OCR 要跑几分钟，
-- 同步接口必然超时。改成"提交任务 → 后台跑 → 前端轮询状态"。
--
-- 为什么落库而不是放内存 Map：
--   1. 服务重启后任务记录还在，前端刷新页面还能查到结果
--   2. 多实例部署时，轮询请求可能落到别的实例上，内存里查不到
--
-- 结果明细（items）直接存 JSON：一次任务几十到几千条，
-- 单独建子表查询上没优势，反而多一次联表。

CREATE TABLE IF NOT EXISTS `t_rent_reconcile_task` (
  `id`           varchar(36)  NOT NULL                COMMENT '任务ID，UUID',
  `status`       varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待执行 RUNNING执行中 DONE完成 FAILED失败',
  `file_count`   int(11)      NOT NULL DEFAULT 0      COMMENT '上传的文件数',
  `total_pages`  int(11)      NOT NULL DEFAULT 0      COMMENT '拆出来的总页数',
  `progress`     int(11)      NOT NULL DEFAULT 0      COMMENT '已处理页数，前端用它显示进度',
  `total`        int(11)      NOT NULL DEFAULT 0      COMMENT '识别出的回单/清单行总条数',
  `ok_count`     int(11)      NOT NULL DEFAULT 0      COMMENT '对账一致条数',
  `error_count`  int(11)      NOT NULL DEFAULT 0      COMMENT '对账异常条数',
  `fail_count`   int(11)      NOT NULL DEFAULT 0      COMMENT '未能处理条数',
  `result_json`  longtext     DEFAULT NULL            COMMENT '各条明细，JSON 数组',
  `error_msg`    varchar(500) DEFAULT NULL            COMMENT '任务整体失败的原因',
  `ledger_id`    bigint(20)   DEFAULT NULL            COMMENT '详情页传的台账ID，列表页批量为空',
  `create_by`    varchar(50)  DEFAULT NULL            COMMENT '发起人',
  `create_time`  datetime     DEFAULT NULL            COMMENT '发起时间',
  `finish_time`  datetime     DEFAULT NULL            COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_rent_reconcile_task_create` (`create_by`, `create_time`),
  KEY `idx_rent_reconcile_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租金回单对账任务';