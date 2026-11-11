-- 需求第21条：修改租金单价时，当月按旧价、次月1号起按新价。
-- t_rent 增加单价生效区间字段，并调整唯一键以容纳"同一档位不同时间段不同单价"。
--
-- 原唯一键 uk_install_price(install_id, price_type_id, del_time) 意味着
-- 同一安装信息下同一档位只能有一条未删除记录，无法表达"旧价保留到本月底"。
-- 改成加入 effect_date_from 后，旧价(effect_date_to=月底)和新价(effect_date_from=次月1号)
-- 才能共存。effect_date_from 为 NULL 的记录在 MySQL 唯一索引中允许重复，
-- 首次新增的档位（effect_date_from=NULL）不会与改价产生的新记录冲突。

ALTER TABLE `t_rent`
    ADD COLUMN `effect_date_from` date DEFAULT NULL COMMENT '单价生效起日，NULL=从并网日或档位起始日起' AFTER `amount`,
    ADD COLUMN `effect_date_to` date DEFAULT NULL COMMENT '单价生效止日，NULL=到租期结束' AFTER `effect_date_from`;

ALTER TABLE `t_rent` DROP INDEX `uk_install_price`;
ALTER TABLE `t_rent` ADD UNIQUE KEY `uk_install_price` (`install_id`,`price_type_id`,`del_time`,`effect_date_from`);
