-- ============================================================
-- 统一 del_time 的"未删除"占位值，修复唯一键失效的隐患
-- ============================================================
-- 背景：
--   t_farmer_station 的唯一键是 (project_id, power_user_no, del_time)
--   t_rent           的唯一键是 (install_id, price_type_id, del_time, effect_date_from)
--   两者都靠 del_time 区分"没删过"的行。
--
--   但"未删除"的占位值有两套：
--     Excel 导入 / 数据库默认值 → 1970-01-01 00:00:00
--     页面手工新增（代码里的 new Date(0L)）→ 1970-01-01 08:00:00（东八区偏移）
--
--   后果：同一个项目下同一个户号，先导入一条(00:00:00)、再手工建一条(08:00:00)，
--         唯一键不冲突 → 出现两条有效农户。
--
-- 配套代码改动：
--   FarmerStationBizServiceImpl 的 NOT_DELETED 已从 new Date(0L) 改为
--   Timestamp.valueOf(TFarmerStation.NOT_DELETED)，以后写入统一是 00:00:00。
--
-- 本脚本只处理改造之前就已经存在的那批脏数据。
-- 执行顺序：① 备份 → ② 统一 → ③ 自查
-- ============================================================


-- ------------------------------------------------------------
-- ① 备份
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_farmer_station_bak_20261010;
CREATE TABLE t_farmer_station_bak_20261010 AS SELECT * FROM t_farmer_station;
DROP TABLE IF EXISTS t_rent_bak_20261010;
CREATE TABLE t_rent_bak_20261010 AS SELECT * FROM t_rent;


-- ------------------------------------------------------------
-- ② 统一"未删除"占位值
--    只动 del_flag=0 的行：已删除的行 del_time 存的是真实删除时间，不能碰
-- ------------------------------------------------------------
UPDATE t_farmer_station
SET del_time = '1970-01-01 00:00:00'
WHERE del_flag = 0 AND del_time = '1970-01-01 08:00:00';

UPDATE t_rent
SET del_time = '1970-01-01 00:00:00'
WHERE del_flag = 0 AND del_time = '1970-01-01 08:00:00';


-- ------------------------------------------------------------
-- ③ 自查
-- ------------------------------------------------------------

-- 3.1 占位值是否已统一（期望只剩 00:00:00 一种）
SELECT 't_farmer_station' AS tbl, del_flag, del_time, COUNT(*) AS cnt
FROM t_farmer_station GROUP BY del_flag, del_time
UNION ALL
SELECT 't_rent', del_flag, del_time, COUNT(*) FROM t_rent GROUP BY del_flag, del_time;

-- 3.2 唯一键是否真的生效：同项目同户号不该有两条未删除的（期望空）
SELECT project_id, power_user_no, COUNT(*) AS cnt
FROM t_farmer_station WHERE del_flag = 0
GROUP BY project_id, power_user_no HAVING cnt > 1;

-- 3.3 租金表同样检查（期望空）
--     ⚠️ t_rent 的唯一键是 (install_id, price_type_id, del_time, effect_date_from)，
--     必须带上 effect_date_from 分组：改价会故意留下"旧价(efrom=NULL) + 新价(efrom=次月1号)"
--     两条，那是有意为之的数据，不是重复。
SELECT install_id, price_type_id, IFNULL(effect_date_from, 'NULL') AS eff_from, COUNT(*) AS cnt
FROM t_rent WHERE del_flag = 0
GROUP BY install_id, price_type_id, effect_date_from HAVING cnt > 1;


-- ------------------------------------------------------------
-- ④ 回滚（万一要撤销）
-- ------------------------------------------------------------
-- UPDATE t_farmer_station t JOIN t_farmer_station_bak_20261010 b ON b.id = t.id SET t.del_time = b.del_time;
-- UPDATE t_rent t JOIN t_rent_bak_20261010 b ON b.id = t.id SET t.del_time = b.del_time;
