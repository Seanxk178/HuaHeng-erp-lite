SET NAMES utf8mb4;

-- 1. 往来单位增加字段
ALTER TABLE `base_partner` 
ADD COLUMN `address` VARCHAR(255) DEFAULT NULL COMMENT '地址' AFTER `phone`,
ADD COLUMN `main_product` VARCHAR(255) DEFAULT NULL COMMENT '主营产品' AFTER `address`;

-- 2. 新增仓库表
CREATE TABLE `base_warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  `code` VARCHAR(50) NOT NULL COMMENT '仓库编号',
  `name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库信息表';

-- 插入默认仓库（避免原有单据找不到仓库）
INSERT INTO `base_warehouse` (`id`, `code`, `name`) VALUES (1, 'WH001', '默认总仓');

-- 将现有库存、单据归属到默认仓库
UPDATE `inv_stock` SET `warehouse_id` = 1 WHERE `warehouse_id` IS NULL;
UPDATE `doc_order` SET `warehouse_id` = 1 WHERE `warehouse_id` IS NULL;

-- 3. 新增数据修改与操作审核任务表
CREATE TABLE `sys_audit_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `module` VARCHAR(50) NOT NULL COMMENT '业务模块(如 GOODS, PARTNER, ORDER)',
  `entity_id` BIGINT DEFAULT NULL COMMENT '目标记录ID(如被修改的商品ID，新增时为NULL)',
  `type` VARCHAR(20) NOT NULL COMMENT '操作类型(UPDATE, DELETE, REVERT, CREATE)',
  `original_data` JSON DEFAULT NULL COMMENT '修改前数据快照/或撤销操作的原单快照',
  `new_data` JSON DEFAULT NULL COMMENT '修改后期望的数据',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) NOT NULL COMMENT '申请人姓名',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审核人ID(若已审核)',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名',
  `status` INT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统全局审核任务表';
