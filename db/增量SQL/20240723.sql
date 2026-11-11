UPDATE pv_project_info pi
SET pi.create_time = (
    SELECT b.min_create_time
    FROM (
        SELECT project_id, MIN(create_time) AS min_create_time
        FROM pv_operate_log
        GROUP BY project_id
    ) b
    WHERE pi.id = b.project_id
)
WHERE pi.create_time IS NULL;