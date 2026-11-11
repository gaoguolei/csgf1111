-- 租金模块的表统一成 utf8mb4
--
-- 问题：t_farmer_station / t_install_info / t_rent 都是 utf8mb4（4 字节），
-- 但台账这几张表还停在 utf8（最多 3 字节）。农户地址里一旦出现生僻字或 emoji，
-- 生成台账时把 install_addr 复制过来就会报
--   Incorrect string value: '\xF0...' for column 'install_addr' (error 1366)
-- 整个电站的台账生成直接失败。
--
-- utf8 是 utf8mb4 的子集，转换不会丢数据，只是把每字符最多 3 字节放宽到 4 字节。

ALTER TABLE `t_rent_ledger`          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `t_rent_ledger_month`    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `t_rent_attach`          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `t_rent_audit_record`    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `t_rent_dispute_record`  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
