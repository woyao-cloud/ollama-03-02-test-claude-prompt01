-- =============================================
-- 测试数据: 角色权限关联数据
-- 用于本地开发和测试环境
-- 执行顺序: 04 (在角色和权限之后执行)
-- =============================================

BEGIN;

-- 清空现有关联数据
TRUNCATE TABLE role_permissions CASCADE;

-- =============================================
-- 超级管理员角色 - 拥有所有权限
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_SUPER_ADMIN';

-- =============================================
-- 系统管理员角色 - 拥有系统管理相关权限
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_SYSTEM_ADMIN'
  AND p.code IN (
    -- 系统管理菜单
    'menu:system', 'menu:users', 'menu:roles', 'menu:permissions', 'menu:departments', 'menu:config', 'menu:audit',
    -- 用户管理权限
    'user:read', 'user:create', 'user:update', 'user:delete', 'user:export', 'user:reset-password', 'user:assign-role',
    -- 角色管理权限
    'role:read', 'role:create', 'role:update', 'role:delete', 'role:assign-permission',
    -- 权限管理权限
    'permission:read', 'permission:create', 'permission:update', 'permission:delete',
    -- 部门管理权限
    'department:read', 'department:create', 'department:update', 'department:delete', 'department:move',
    -- 系统配置权限
    'config:read', 'config:update',
    -- 审计日志权限
    'audit:read', 'audit:export'
  );

-- =============================================
-- 部门经理角色 - 管理部门相关权限
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_DEPT_MANAGER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users', 'menu:departments',
    -- 用户管理 (只读+部分操作)
    'user:read', 'user:export',
    -- 部门管理 (完整权限)
    'department:read', 'department:create', 'department:update', 'department:move',
    -- 业务管理菜单
    'menu:business', 'menu:customers', 'menu:projects', 'menu:orders',
    -- 数据分析菜单
    'menu:analytics', 'menu:user-stats', 'menu:sales-report', 'menu:monitoring',
    -- 审计日志
    'audit:read'
  );

-- =============================================
-- 项目经理角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_PROJECT_MANAGER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 业务管理菜单
    'menu:business', 'menu:customers', 'menu:projects', 'menu:orders',
    -- 数据分析菜单
    'menu:analytics', 'menu:user-stats', 'menu:sales-report', 'menu:monitoring',
    -- 审计日志
    'audit:read'
  );

-- =============================================
-- 技术负责人角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_TECH_LEAD'
  AND p.code IN (
    -- 系统管理菜单
    'menu:system', 'menu:users', 'menu:config', 'menu:audit',
    -- 用户管理
    'user:read', 'user:export',
    -- 系统配置
    'config:read', 'config:update',
    -- 审计日志
    'audit:read', 'audit:export',
    -- 业务管理
    'menu:business', 'menu:projects',
    -- 数据分析
    'menu:analytics', 'menu:monitoring'
  );

-- =============================================
-- 开发工程师角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_DEVELOPER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 审计日志 (只读)
    'audit:read',
    -- 业务管理
    'menu:business', 'menu:projects',
    -- 数据分析
    'menu:analytics'
  );

-- =============================================
-- 测试工程师角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_TESTER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 审计日志 (只读)
    'audit:read',
    -- 业务管理
    'menu:business', 'menu:projects', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:monitoring'
  );

-- =============================================
-- 运维工程师角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_DEVOPS'
  AND p.code IN (
    -- 系统管理菜单
    'menu:system', 'menu:users', 'menu:config', 'menu:audit',
    -- 用户管理 (只读)
    'user:read', 'user:export',
    -- 系统配置
    'config:read', 'config:update',
    -- 审计日志
    'audit:read', 'audit:export',
    -- 数据分析
    'menu:analytics', 'menu:monitoring'
  );

-- =============================================
-- 产品经理角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_PM'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 业务管理 (完整权限)
    'menu:business', 'menu:customers', 'menu:projects', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:user-stats', 'menu:sales-report'
  );

-- =============================================
-- 产品设计师角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_DESIGNER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system',
    -- 业务管理 (只读)
    'menu:business', 'menu:customers', 'menu:projects',
    -- 数据分析
    'menu:analytics', 'menu:user-stats'
  );

-- =============================================
-- 运营专员角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_OPERATOR'
  AND p.code IN (
    -- 业务管理
    'menu:business', 'menu:customers', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:user-stats', 'menu:sales-report'
  );

-- =============================================
-- 销售经理角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_SALES_MANAGER'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 业务管理
    'menu:business', 'menu:customers', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:sales-report'
  );

-- =============================================
-- 销售代表角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_SALES_REP'
  AND p.code IN (
    -- 业务管理
    'menu:business', 'menu:customers', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:sales-report'
  );

-- =============================================
-- HR专员角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_HR'
  AND p.code IN (
    -- 系统管理菜单
    'menu:system', 'menu:users', 'menu:departments',
    -- 用户管理
    'user:read', 'user:create', 'user:update', 'user:export',
    -- 部门管理
    'department:read', 'department:create', 'department:update'
  );

-- =============================================
-- 财务专员角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_FINANCE'
  AND p.code IN (
    -- 系统管理菜单 (只读)
    'menu:system', 'menu:users',
    -- 用户管理 (只读)
    'user:read',
    -- 业务管理
    'menu:business', 'menu:orders',
    -- 数据分析
    'menu:analytics', 'menu:sales-report'
  );

-- =============================================
-- 普通用户角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_USER'
  AND p.code IN (
    -- 只读菜单
    'menu:system', 'menu:users',
    -- 只读权限
    'user:read'
  );

-- =============================================
-- 访客角色
-- =============================================

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    r.id as role_id,
    p.id as permission_id,
    NOW() as created_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_GUEST'
  AND p.code IN (
    -- 仅系统管理菜单
    'menu:system'
  );

COMMIT;

-- 验证数据
SELECT '角色权限关联数据插入完成' as status,
       r.name as role_name,
       COUNT(rp.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
GROUP BY r.id, r.name
ORDER BY permission_count DESC;
