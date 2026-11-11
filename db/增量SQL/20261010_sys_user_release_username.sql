-- ============================================================
-- 让"已删除账号"腾出 username，同名账号才能重新注册
-- ============================================================
-- 背景：
--   sys_user 上有唯一索引 uniq_sys_user_username(username)，**不含 del_flag**，
--   而删除走的是逻辑删除（只把 del_flag 置 1）。username 原样保留 → 一直占着这个
--   索引 → 同名账号永远注册不了（数据库报 Duplicate entry）。
--
--   前端的"账号唯一性检查"因为带 del_flag=0 查询，反而提示"可以使用"，很难排查。
--
-- 处理：
--   把已删除账号的 username 改成 原名_del_主键id。
--   主键 id 是 MyBatis-Plus 的雪花ID，天然唯一 → 新名字绝不会和任何账号冲突
--   （包括运营直接建的"张三01"这种名字）。前面保留原名便于反查。
--
-- 说明：代码侧 SysUserServiceImpl.deleteUser 已同步改为这套逻辑，
--       本脚本只处理改造之前就已经删掉的那批存量账号。
--
-- 执行顺序：① 备份 → ② 改名 → ③ 清关联 → ④ 自查
-- ============================================================


-- ------------------------------------------------------------
-- ① 备份
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_bak_20261010;
CREATE TABLE sys_user_bak_20261010 AS SELECT * FROM sys_user;


-- ------------------------------------------------------------
-- ② 已删除账号改名，腾出 username
--    LEFT(...) 截断保证总长不超过 varchar(100)：100 - 5(_del_) - id长度
-- ------------------------------------------------------------
UPDATE sys_user
SET username = CONCAT(LEFT(username, 100 - 5 - LENGTH(id)), '_del_', id)
WHERE del_flag = 1
  AND username NOT LIKE '%\_del\_%';


-- ------------------------------------------------------------
-- ③ 清理已删账号残留的角色/部门绑定
--    （原 deleteUser 只调了 removeById，没清关联表，攒下了一批脏数据）
-- ------------------------------------------------------------
DELETE ur FROM sys_user_role ur
    JOIN sys_user u ON u.id = ur.user_id
WHERE u.del_flag = 1;

DELETE ud FROM sys_user_depart ud
    JOIN sys_user u ON u.id = ud.user_id
WHERE u.del_flag = 1;


-- ------------------------------------------------------------
-- ④ 自查
-- ------------------------------------------------------------

-- 4.1 还有没有已删账号占着名字（期望 0）
SELECT COUNT(*) AS still_occupied
FROM sys_user WHERE del_flag = 1 AND username NOT LIKE '%\_del\_%';

-- 4.2 还有没有悬空的角色/部门绑定（期望 0 和 0）
SELECT (SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id WHERE u.del_flag = 1) AS orphan_roles,
       (SELECT COUNT(*) FROM sys_user_depart ud JOIN sys_user u ON u.id = ud.user_id WHERE u.del_flag = 1) AS orphan_departs;

-- 4.3 看一下改完的样子
SELECT username, del_flag FROM sys_user WHERE del_flag = 1 ORDER BY username;


-- ------------------------------------------------------------
-- ⑤ 回滚（万一要撤销）
-- ------------------------------------------------------------
-- UPDATE sys_user u JOIN sys_user_bak_20261010 b ON b.id = u.id
-- SET u.username = b.username;
