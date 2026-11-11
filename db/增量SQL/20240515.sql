CREATE TABLE `pv_tj_project_install` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint DEFAULT NULL COMMENT '项目id',
  `house_type` varchar(255) DEFAULT NULL COMMENT '房屋类型',
  `inst_angle` varchar(255) DEFAULT NULL COMMENT '安装角度',
  `inst_cmpt_number` int DEFAULT NULL COMMENT '组件安装数量（块）',
  `inst_cmpt_capacity` decimal(10,2) DEFAULT NULL COMMENT '组件安装容量（瓦）',
  `inst_cmpt_arrange` varchar(255) DEFAULT NULL COMMENT '组件排布方式',
  `cmpt_model` varchar(255) DEFAULT NULL COMMENT '组件型号',
  `cmpt_spec` varchar(255) DEFAULT NULL COMMENT '组件规格',
  `file_id` varchar(255) DEFAULT NULL COMMENT '踏勘图集合',
  `remakes_id` varchar(255) DEFAULT NULL COMMENT '安装备注id集合',
  `create_date` date DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='天津项目安装模块';

ALTER TABLE pv_project_info ADD COLUMN `total_capacity` decimal(10,2) DEFAULT NULL COMMENT '组件总容量';


ALTER TABLE sys_user ADD COLUMN `grade` varchar(120) DEFAULT NULL COMMENT '角色级别';

ALTER TABLE sys_user ADD COLUMN `create_id` varchar(120) DEFAULT NULL COMMENT '创建人id';

insert into sys_role (id, role_name, role_code, description, create_by, create_time, update_by, update_time) values ('1791047313644818434', '一级商', 'firstSeller', '一级商', 'admin', str_to_date('2024-05-16 18:05:30', '%Y-%m-%d %T'), null, null);
insert into sys_role (id, role_name, role_code, description, create_by, create_time, update_by, update_time) values ('1791047421807529985', '二级商', 'secondSeller', '二级商', 'admin', str_to_date('2024-05-16 18:05:56', '%Y-%m-%d %T'), null, null);


--已经执行过
CREATE INDEX index_project_id ON pv_operate_log (project_id);








