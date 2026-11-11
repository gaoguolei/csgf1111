ALTER TABLE pv_project_info ADD COLUMN `inst_cmpt_brand` VARCHAR(255) DEFAULT NULL COMMENT '组件品牌(安装)';

ALTER TABLE pv_project_info ADD COLUMN `inst_cmpt_model` VARCHAR(255) DEFAULT NULL COMMENT '组件型号(安装)';

update pv_project_remark set param_group_id = 61 ,param_itme_id = 364 where param_group_id = 59 and param_itme_id = 222;

update pv_project_remark set param_group_id = 61 ,param_itme_id = 365 where param_group_id = 59 and param_itme_id = 223;

update pv_project_remark set param_group_id = 61 ,param_itme_id = 366 where param_group_id = 59 and param_itme_id = 224;

ALTER TABLE pv_project_info ADD COLUMN `inst_assembly_brand` VARCHAR(255) DEFAULT NULL COMMENT '组件品牌(施工)';

ALTER TABLE pv_project_info ADD COLUMN `inst_assembly_spec` VARCHAR(255) DEFAULT NULL COMMENT '组件规格(施工)';

ALTER TABLE sys_inverter_wire_mppt ADD COLUMN `capacity` VARCHAR(255) DEFAULT NULL COMMENT '安装容量';

ALTER TABLE sys_inverter_wire_mppt ADD COLUMN `angle` VARCHAR(255) DEFAULT NULL COMMENT '安装角度';

ALTER TABLE sys_inverter_wire_mppt ADD COLUMN `voltage` VARCHAR(255) DEFAULT NULL COMMENT '组串电压';
