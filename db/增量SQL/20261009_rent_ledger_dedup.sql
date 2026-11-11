-- ============================================================
-- 清理"重新生成"产生的重复租金台账
-- ============================================================
-- 背景：旧版 regenerate 是"追加新行 + 人工作废"，同一电站同一期会堆出多条有效台账。
--       新版已改成"原地更新不新增行"，但历史遗留的这些需要清一次。
--
-- 处理方式：**只作废，不删除**（void_flag=1）。作废是可逆的，
--          要恢复把 void_flag 改回 0 即可。
--
-- 每组保留一条，保留优先级：
--   1. 已支付 / 已对账（钱已经发生过）
--   2. 有附件或纠纷记录（业务记录不能变成孤儿）
--   3. id 最小（最早生成的原始台账）
--
-- 执行顺序：① 备份 → ② 作废 → ③ 重排累计链 → ④ 自查
-- ============================================================


-- ------------------------------------------------------------
-- ① 备份整个台账表（强烈建议，出问题能回滚）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_rent_ledger_bak_20261009;
CREATE TABLE t_rent_ledger_bak_20261009 AS SELECT * FROM t_rent_ledger;
-- 台账月明细也备一份，作废判断要对照它
DROP TABLE IF EXISTS t_rent_ledger_month_bak_20261009;
CREATE TABLE t_rent_ledger_month_bak_20261009 AS SELECT * FROM t_rent_ledger_month;


-- ------------------------------------------------------------
-- ② 作废重复台账
--    MySQL 5.7 不允许在 UPDATE 的子查询里直接读被更新的表，
--    所以先把要作废的 id 落进临时表，再按 id 更新。
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_void_ids;
CREATE TEMPORARY TABLE tmp_void_ids (id BIGINT PRIMARY KEY);

INSERT INTO tmp_void_ids (id)
SELECT l.id
FROM t_rent_ledger l
WHERE l.del_flag = 0 AND l.void_flag = 0
  AND EXISTS (
    SELECT 1 FROM t_rent_ledger b
    WHERE b.station_id = l.station_id AND b.rent_year = l.rent_year
      AND b.period_no = l.period_no AND b.del_flag = 0 AND b.void_flag = 0 AND b.id <> l.id
      AND CONCAT(
            CASE WHEN b.pay_status = 1 OR b.recon_status > 0 THEN 0 ELSE 1 END,
            CASE WHEN (SELECT COUNT(*) FROM t_rent_attach a WHERE a.ledger_id = b.id)
                    + (SELECT COUNT(*) FROM t_rent_dispute_record d WHERE d.ledger_id = b.id) > 0
                 THEN 0 ELSE 1 END,
            LPAD(b.id, 12, '0')
          ) < CONCAT(
            CASE WHEN l.pay_status = 1 OR l.recon_status > 0 THEN 0 ELSE 1 END,
            CASE WHEN (SELECT COUNT(*) FROM t_rent_attach a WHERE a.ledger_id = l.id)
                    + (SELECT COUNT(*) FROM t_rent_dispute_record d WHERE d.ledger_id = l.id) > 0
                 THEN 0 ELSE 1 END,
            LPAD(l.id, 12, '0')
          )
  );

UPDATE t_rent_ledger l
    JOIN tmp_void_ids t ON t.id = l.id
SET l.void_flag = 1,
    l.void_remark = '重新生成产生的重复台账，脚本自动作废，保留同期最早的一条',
    l.update_time = NOW();

DROP TEMPORARY TABLE tmp_void_ids;


-- ------------------------------------------------------------
-- ③ 重排累计链
--    作废行不再参与累计，之前算的累计值要按现存的有效台账重算一遍。
--    口径见 20261009_rent_amount_new_rule.sql（需求第6条新口径）。
-- ------------------------------------------------------------
UPDATE t_rent_ledger l
    JOIN (
        SELECT l1.id,
               (SELECT COALESCE(SUM(COALESCE(l2.period_paid, COALESCE(l2.period_payable, 0))), 0)
                FROM t_rent_ledger l2
                WHERE l2.station_id = l1.station_id
                  AND l2.del_flag = 0
                  AND l2.void_flag = 0
                  AND (l2.rent_year < l1.rent_year
                       OR (l2.rent_year = l1.rent_year AND l2.period_no < l1.period_no))
               ) AS prev_paid
        FROM t_rent_ledger l1
        WHERE l1.del_flag = 0 AND l1.void_flag = 0
    ) p ON p.id = l.id
SET l.prev_accum_paid = p.prev_paid,
    l.accum_paid      = p.prev_paid + COALESCE(l.period_paid, COALESCE(l.period_payable, 0)),
    l.accum_payable   = p.prev_paid + COALESCE(l.period_payable, 0),
    l.diff_amount     = COALESCE(l.period_paid, COALESCE(l.period_payable, 0))
                        - COALESCE(l.period_payable, 0)
WHERE l.del_flag = 0 AND l.void_flag = 0;


-- ------------------------------------------------------------
-- ④ 自查
-- ------------------------------------------------------------

-- 4.1 同一电站同一期是否还有多条有效台账（期望：0 行）
SELECT station_id, rent_year, period_no, COUNT(*) AS cnt
FROM t_rent_ledger
WHERE del_flag = 0 AND void_flag = 0
GROUP BY station_id, rent_year, period_no
HAVING COUNT(*) > 1;

-- 4.2 超/欠付是否恒等于 本期实付 - 本期应付（期望：0）
SELECT COUNT(*) AS bad_diff_rows
FROM t_rent_ledger
WHERE del_flag = 0 AND void_flag = 0
  AND diff_amount <> (COALESCE(period_paid, COALESCE(period_payable, 0)) - COALESCE(period_payable, 0));

-- 4.3 本次作废了多少条
SELECT COUNT(*) AS voided_rows
FROM t_rent_ledger
WHERE void_flag = 1 AND void_remark LIKE '重新生成产生的重复台账%';


-- ------------------------------------------------------------
-- ⑤ 【按需】把被作废行上录的发电天数补回保留行
--
-- 背景：被作废的那些行是 bug 版重新生成产生的，但运营后来在上面录过实际发电天数，
--       作废后这些录入值就成了孤儿。下面两段二选一执行。
-- ------------------------------------------------------------

-- 先看看有哪些值会丢（只查询）
SELECT keep.station_id, keep.rent_year, keep.period_no,
       keep.id AS keep_id, keep_m.ym, keep_m.gen_days AS keep_gen_days,
       lose.id AS void_id, lose_m.gen_days AS void_gen_days
FROM t_rent_ledger keep
    JOIN t_rent_ledger lose
        ON lose.station_id = keep.station_id AND lose.rent_year = keep.rent_year
       AND lose.period_no = keep.period_no AND lose.void_flag = 1
       AND lose.void_remark LIKE '重新生成产生的重复台账%'
    JOIN t_rent_ledger_month keep_m ON keep_m.ledger_id = keep.id
    JOIN t_rent_ledger_month lose_m ON lose_m.ledger_id = lose.id AND lose_m.ym = keep_m.ym
WHERE keep.del_flag = 0 AND keep.void_flag = 0
  AND lose_m.gen_days IS NOT NULL
  AND (keep_m.gen_days IS NULL OR keep_m.gen_days <> lose_m.gen_days)
ORDER BY keep.station_id, keep.rent_year, keep.period_no, keep_m.ym;

-- 方案A（保守）：只补保留行没有值的月份，保留行已有的值不动
-- UPDATE t_rent_ledger_month km
--     JOIN t_rent_ledger keep ON keep.id = km.ledger_id AND keep.void_flag = 0 AND keep.del_flag = 0
--     JOIN t_rent_ledger lose ON lose.station_id = keep.station_id AND lose.rent_year = keep.rent_year
--                            AND lose.period_no = keep.period_no AND lose.void_flag = 1
--                            AND lose.void_remark LIKE '重新生成产生的重复台账%'
--     JOIN t_rent_ledger_month lm ON lm.ledger_id = lose.id AND lm.ym = km.ym
-- SET km.gen_days = lm.gen_days,
--     km.amount   = km.theory_amount / km.rent_days * lm.gen_days
-- WHERE km.gen_days IS NULL AND lm.gen_days IS NOT NULL AND km.rent_days > 0;

-- 方案B（激进）：被作废行的值覆盖保留行，取运营最后一次录入的口径
-- UPDATE t_rent_ledger_month km
--     JOIN t_rent_ledger keep ON keep.id = km.ledger_id AND keep.void_flag = 0 AND keep.del_flag = 0
--     JOIN t_rent_ledger lose ON lose.station_id = keep.station_id AND lose.rent_year = keep.rent_year
--                            AND lose.period_no = keep.period_no AND lose.void_flag = 1
--                            AND lose.void_remark LIKE '重新生成产生的重复台账%'
--     JOIN t_rent_ledger_month lm ON lm.ledger_id = lose.id AND lm.ym = km.ym
-- SET km.gen_days = lm.gen_days,
--     km.amount   = km.theory_amount / km.rent_days * lm.gen_days
-- WHERE lm.gen_days IS NOT NULL AND km.rent_days > 0;

-- 补完发电天数后，台账上的"本期应付/累计"要跟着重算 —— 在页面上对这几户点一次
-- 「重新生成」即可（新版是原地更新，会带上修好的发电天数）


-- ------------------------------------------------------------
-- 回滚（万一要撤销）
-- ------------------------------------------------------------
-- UPDATE t_rent_ledger
-- SET void_flag = 0, void_remark = NULL
-- WHERE void_flag = 1 AND void_remark LIKE '重新生成产生的重复台账%';
-- 或整表恢复：
-- UPDATE t_rent_ledger l JOIN t_rent_ledger_bak_20261009 b ON b.id = l.id
-- SET l.void_flag = b.void_flag, l.void_remark = b.void_remark;
