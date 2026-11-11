CREATE TABLE `pv_project_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` int(11) DEFAULT NULL,
  `type_value` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` tinyint(1) DEFAULT '0',
  `tj_type` char(2) DEFAULT '1' COMMENT '天津查询参数',
  `hain_type` char(2) DEFAULT '1' COMMENT '海南查询参数',
  `hain2_type` char(2) DEFAULT '1' COMMENT '海南二期查询参数',
  `hen_type` char(2) DEFAULT '1' COMMENT '河南查询参数',
  `bj_type` char(2) DEFAULT '1' COMMENT '北京查询参数',
  `xzh_type` char(2) DEFAULT '1' COMMENT '徐州查询参数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4;

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (1, 102, '自然人', str_to_date('2024-06-04 11:41:55', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:47', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (2, 102, '法人', str_to_date('2024-06-04 11:41:59', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:45', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (3, 102, '法人二期', str_to_date('2024-06-04 11:42:00', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:43', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (4, 102, '法人三期', str_to_date('2024-06-04 11:42:01', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:38', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (5, 102, '法人四期', str_to_date('2024-06-04 11:42:03', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:34', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (6, 102, '法人五期', str_to_date('2024-06-04 11:42:04', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:36', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (7, 102, '法人六期', str_to_date('2024-06-04 11:42:05', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:31', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (8, 102, '法人七期', str_to_date('2024-06-04 11:42:07', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:29', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (9, 102, '法人八期', str_to_date('2024-06-04 11:42:08', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:25', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (10, 102, '法人九期', str_to_date('2024-06-04 11:42:13', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:16', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

insert into pv_project_type (id, tenant_id, type_value, create_time, update_time, del_flag, tj_type, hain_type, hain2_type, hen_type, bj_type, xzh_type) values (11, 102, '法人十期', str_to_date('2024-06-04 11:42:11', '%Y-%m-%d %T'), str_to_date('2024-06-04 11:42:15', '%Y-%m-%d %T'), 0, '1', '1', '1', '1', '1', '1');

