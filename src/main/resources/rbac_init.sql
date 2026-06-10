-- ============================================================
-- ERP-Lite RBAC 动态权限系统 初始化 SQL
-- 请在 MySQL 中选中 erp_lite 数据库后执行本脚本
-- ============================================================

-- 先删除旧表（顺序：先删关联表，再删主表，避免依赖冲突）
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;

-- 1. 角色表
CREATE TABLE sys_role (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_key  VARCHAR(50) NOT NULL UNIQUE COMMENT '角色唯一标识',
    role_name VARCHAR(50) NOT NULL COMMENT '可读名称'
);

-- 2. 菜单表（menu_key 与前端 currentMenu 值保持完全一致）
CREATE TABLE sys_menu (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_key  VARCHAR(50) NOT NULL UNIQUE COMMENT '菜单标识',
    menu_name VARCHAR(50) NOT NULL COMMENT '可读名称'
);

-- 3. 角色-菜单关联表
CREATE TABLE sys_role_menu (
    role_id  BIGINT NOT NULL,
    menu_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- ============================================================
-- 初始化基础数据
-- ============================================================

-- 角色初始数据（与 sys_user 表的 role 字段一一对应）
INSERT INTO sys_role (role_key, role_name) VALUES
('ROLE_ADMIN',    '系统管理员'),
('ROLE_FINANCE',  '财务专员'),
('ROLE_PURCHASE', '采购/商务专员');

-- 菜单初始数据（共 9 个可配置菜单）
INSERT INTO sys_menu (menu_key, menu_name) VALUES
('partner',  '往来单位'),
('goods',    '商品管理'),
('inbound',  '采购入库'),
('outbound', '销售出库'),
('approve',  '单据审批'),
('history',  '历史单据'),
('stock',    '库存台账'),
('finance',  '财务看板'),
('report',   '报表中心');

-- ============================================================
-- 为各角色配置默认菜单权限
-- ============================================================

-- 财务专员默认权限：历史单据、库存台账、财务看板、报表中心
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_key = 'ROLE_FINANCE'
AND m.menu_key IN ('history', 'stock', 'finance', 'report');

-- 采购/商务专员默认权限：往来单位、商品管理、采购入库、销售出库
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_key = 'ROLE_PURCHASE'
AND m.menu_key IN ('partner', 'goods', 'inbound', 'outbound');
