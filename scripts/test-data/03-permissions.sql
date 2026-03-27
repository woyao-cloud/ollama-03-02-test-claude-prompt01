-- =============================================
-- 测试数据: 权限数据
-- 用于本地开发和测试环境
-- 执行顺序: 03
-- =============================================

BEGIN;

-- 清空现有权限数据
TRUNCATE TABLE permissions CASCADE;

-- =============================================
-- 插入菜单权限
-- =============================================

-- 1. 系统管理菜单
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '系统管理', 'menu:system', 'MENU', 'system', NULL, NULL, 'Settings', '/system', 1, 'ACTIVE', NOW(), NOW(), 0);

DO $$
DECLARE
    system_menu_id UUID;
BEGIN
    SELECT id INTO system_menu_id FROM permissions WHERE code = 'menu:system';

    INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '用户管理', 'menu:users', 'MENU', 'user', NULL, system_menu_id, 'Users', '/system/users', 1, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '角色管理', 'menu:roles', 'MENU', 'role', NULL, system_menu_id, 'Shield', '/system/roles', 2, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '权限管理', 'menu:permissions', 'MENU', 'permission', NULL, system_menu_id, 'Key', '/system/permissions', 3, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '部门管理', 'menu:departments', 'MENU', 'department', NULL, system_menu_id, 'Building', '/system/departments', 4, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '系统配置', 'menu:config', 'MENU', 'config', NULL, system_menu_id, 'Sliders', '/system/config', 5, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '审计日志', 'menu:audit', 'MENU', 'audit', NULL, system_menu_id, 'FileText', '/system/audit', 6, 'ACTIVE', NOW(), NOW(), 0);
END $$;

-- 2. 业务管理菜单
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '业务管理', 'menu:business', 'MENU', 'business', NULL, NULL, 'Briefcase', '/business', 2, 'ACTIVE', NOW(), NOW(), 0);

DO $$
DECLARE
    business_menu_id UUID;
BEGIN
    SELECT id INTO business_menu_id FROM permissions WHERE code = 'menu:business';

    INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '客户管理', 'menu:customers', 'MENU', 'customer', NULL, business_menu_id, 'UserCircle', '/business/customers', 1, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '项目管理', 'menu:projects', 'MENU', 'project', NULL, business_menu_id, 'FolderKanban', '/business/projects', 2, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '订单管理', 'menu:orders', 'MENU', 'order', NULL, business_menu_id, 'ShoppingCart', '/business/orders', 3, 'ACTIVE', NOW(), NOW(), 0);
END $$;

-- 3. 数据分析菜单
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '数据分析', 'menu:analytics', 'MENU', 'analytics', NULL, NULL, 'BarChart', '/analytics', 3, 'ACTIVE', NOW(), NOW(), 0);

DO $$
DECLARE
    analytics_menu_id UUID;
BEGIN
    SELECT id INTO analytics_menu_id FROM permissions WHERE code = 'menu:analytics';

    INSERT INTO permissions (id, name, code, type, resource, action, parent_id, icon, route, sort_order, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '用户统计', 'menu:user-stats', 'MENU', 'user-stats', NULL, analytics_menu_id, 'PieChart', '/analytics/users', 1, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '销售报表', 'menu:sales-report', 'MENU', 'sales-report', NULL, analytics_menu_id, 'TrendingUp', '/analytics/sales', 2, 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '系统监控', 'menu:monitoring', 'MENU', 'monitoring', NULL, analytics_menu_id, 'Activity', '/analytics/monitoring', 3, 'ACTIVE', NOW(), NOW(), 0);
END $$;

-- =============================================
-- 插入操作权限 (API 接口权限)
-- =============================================

-- 用户管理操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '用户查看', 'user:read', 'ACTION', 'user', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '用户创建', 'user:create', 'ACTION', 'user', 'create', NULL, 2, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '用户更新', 'user:update', 'ACTION', 'user', 'update', NULL, 3, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '用户删除', 'user:delete', 'ACTION', 'user', 'delete', NULL, 4, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '用户导出', 'user:export', 'ACTION', 'user', 'export', NULL, 5, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '重置密码', 'user:reset-password', 'ACTION', 'user', 'reset-password', NULL, 6, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '分配角色', 'user:assign-role', 'ACTION', 'user', 'assign-role', NULL, 7, 'ACTIVE', NOW(), NOW(), 0);

-- 角色管理操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '角色查看', 'role:read', 'ACTION', 'role', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '角色创建', 'role:create', 'ACTION', 'role', 'create', NULL, 2, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '角色更新', 'role:update', 'ACTION', 'role', 'update', NULL, 3, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '角色删除', 'role:delete', 'ACTION', 'role', 'delete', NULL, 4, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '分配权限', 'role:assign-permission', 'ACTION', 'role', 'assign-permission', NULL, 5, 'ACTIVE', NOW(), NOW(), 0);

-- 权限管理操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '权限查看', 'permission:read', 'ACTION', 'permission', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '权限创建', 'permission:create', 'ACTION', 'permission', 'create', NULL, 2, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '权限更新', 'permission:update', 'ACTION', 'permission', 'update', NULL, 3, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '权限删除', 'permission:delete', 'ACTION', 'permission', 'delete', NULL, 4, 'ACTIVE', NOW(), NOW(), 0);

-- 部门管理操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '部门查看', 'department:read', 'ACTION', 'department', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '部门创建', 'department:create', 'ACTION', 'department', 'create', NULL, 2, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '部门更新', 'department:update', 'ACTION', 'department', 'update', NULL, 3, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '部门删除', 'department:delete', 'ACTION', 'department', 'delete', NULL, 4, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '调整部门', 'department:move', 'ACTION', 'department', 'move', NULL, 5, 'ACTIVE', NOW(), NOW(), 0);

-- 系统配置操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '配置查看', 'config:read', 'ACTION', 'config', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '配置修改', 'config:update', 'ACTION', 'config', 'update', NULL, 2, 'ACTIVE', NOW(), NOW(), 0);

-- 审计日志操作权限
INSERT INTO permissions (id, name, code, type, resource, action, parent_id, sort_order, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '日志查看', 'audit:read', 'ACTION', 'audit', 'read', NULL, 1, 'ACTIVE', NOW(), NOW(), 0),
    (gen_random_uuid(), '日志导出', 'audit:export', 'ACTION', 'audit', 'export', NULL, 2, 'ACTIVE', NOW(), NOW(), 0);

COMMIT;

-- 验证数据
SELECT '权限数据插入完成' as status,
       COUNT(*) as total_permissions,
       COUNT(*) FILTER (WHERE type = 'MENU') as menu_count,
       COUNT(*) FILTER (WHERE type = 'ACTION') as action_count,
       COUNT(*) FILTER (WHERE type = 'FIELD') as field_count,
       COUNT(*) FILTER (WHERE type = 'DATA') as data_count
FROM permissions;
