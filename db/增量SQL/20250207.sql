CREATE TABLE `pv_project_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `password_key` varchar(255) DEFAULT NULL COMMENT '密钥',
  `project_id` bigint(20) DEFAULT NULL COMMENT '关联项目',
  `tenant_id` int(11) DEFAULT NULL COMMENT '所属租户',
  `key_type` varchar(2) DEFAULT NULL COMMENT '密钥类型,1:git密码',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `row_status` char(1) DEFAULT '1' COMMENT '行状态：{正常：1，Normal；逻辑删除：8，Delete}',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COMMENT='密钥';

ALTER TABLE pv_project_info MODIFY COLUMN id_number VARCHAR(255);
ALTER TABLE pv_project_info MODIFY COLUMN customer_mobile VARCHAR(255);
ALTER TABLE pv_project_info MODIFY COLUMN con_bank_card_holders VARCHAR(255);
ALTER TABLE pv_project_info MODIFY COLUMN customer_name VARCHAR(255);