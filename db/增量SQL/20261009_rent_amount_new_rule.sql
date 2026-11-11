-- 需求第6条新口径：累计金额字段重新计算（存量数据回填）
--
-- 新口径：
--   累计应付金额2 = 截止上期累计实付 + 本期应付     （原来基数用的是"累计应付"）
--   累计实付金额   = 截止上期累计实付 + 本期实付
--   超/欠付金额    = 累计实付金额 - 累计应付金额2  = 本期实付 - 本期应付
--
-- 说明：
--   1. 本期实付为空时按本期应付看待，与代码里的取法一致。
--   2. 同一电站同一期可能有多条台账（反复重新生成产生），它们共用"上期的累计实付"，
--      与 recalcChain() 的分组逻辑一致。
--   3. 只处理未删除、未作废的台账。
--
-- ⚠️ 执行前先备份 t_rent_ledger。执行后用下面的自查 SQL 抽几条核对。

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

-- 自查：抽一个有多期的电站看累计链是否连贯
-- SELECT station_id, rent_year, period_no, period_payable, period_paid,
--        prev_accum_paid, accum_payable, accum_paid, diff_amount
-- FROM t_rent_ledger
-- WHERE station_id = <某个电站ID> AND del_flag = 0 AND void_flag = 0
-- ORDER BY rent_year, period_no, id;

-- 自查：超/欠付应当恒等于 本期实付 - 本期应付
-- SELECT COUNT(*) AS bad_rows FROM t_rent_ledger
-- WHERE del_flag = 0 AND void_flag = 0
--   AND diff_amount <> (COALESCE(period_paid, COALESCE(period_payable,0)) - COALESCE(period_payable,0));
