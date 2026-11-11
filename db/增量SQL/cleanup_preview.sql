-- 预览：哪些台账会被作废（每组保留一条）
-- 保留优先级（拼成字符串比较，越小的越优先保留）：
--   1. 已支付 / 已对账（钱已经发生过）
--   2. 有附件或纠纷记录（业务记录不能变成孤儿）
--   3. id 最小的（最早生成的那条，即原始台账）
SELECT l.station_id, l.rent_year, l.period_no, l.id, l.farmer_name,
       CASE WHEN l.pay_status = 1 OR l.recon_status > 0 THEN 'Y' ELSE '' END AS settled,
       CASE WHEN (SELECT COUNT(*) FROM t_rent_attach a WHERE a.ledger_id = l.id)
               + (SELECT COUNT(*) FROM t_rent_dispute_record d WHERE d.ledger_id = l.id) > 0
            THEN 'Y' ELSE '' END AS has_record,
       l.audit_status AS aud, l.pay_status AS pay, l.recon_status AS rec,
       l.period_payable AS payable,
       (SELECT COUNT(*) FROM t_rent_ledger_month m WHERE m.ledger_id = l.id AND m.gen_days IS NOT NULL) AS gen_months,
       DATE_FORMAT(l.create_time, '%m-%d %H:%i') AS created
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
  )
ORDER BY l.station_id, l.rent_year, l.period_no, l.id;
