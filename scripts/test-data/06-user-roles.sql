-- =============================================
-- 测试数据: 用户角色关联数据
-- 用于本地开发和测试环境
-- 执行顺序: 06 (最后执行)
-- =============================================

BEGIN;

-- 清空现有关联数据
TRUNCATE TABLE user_roles CASCADE;

-- =============================================
-- 分配用户角色
-- =============================================

-- 超级管理员 -> 超级管理员角色
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'superadmin@test.com' AND r.code = 'ROLE_SUPER_ADMIN';

-- 系统管理员 -> 系统管理员角色
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'admin@test.com' AND r.code = 'ROLE_SYSTEM_ADMIN';

-- 技术总监 -> 部门经理 + 技术负责人
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'tech.lead@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_TECH_LEAD');

-- 前端开发 -> 开发工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'fe.dev%@test.com' AND r.code = 'ROLE_DEVELOPER';

-- 后端开发 -> 开发工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'be.dev%@test.com' AND r.code = 'ROLE_DEVELOPER';

-- 测试负责人 -> 部门经理 + 测试工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'qa.lead@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_TESTER');

-- 测试工程师 -> 测试工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'qa.tester%@test.com' AND r.code = 'ROLE_TESTER';

-- 运维负责人 -> 部门经理 + 运维工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'ops.lead@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_DEVOPS');

-- 运维工程师 -> 运维工程师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'ops.dev1@test.com' AND r.code = 'ROLE_DEVOPS';

-- 产品总监 -> 部门经理 + 产品经理
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'product.lead@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_PM');

-- 设计师 -> 产品设计师
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'ui.designer%@test.com' AND r.code = 'ROLE_DESIGNER';

-- 产品经理 -> 产品经理
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'pm%@test.com' AND r.code = 'ROLE_PM';

-- 运营专员 -> 运营专员
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'ops.user%@test.com' AND r.code = 'ROLE_OPERATOR';

-- 销售总监 -> 部门经理 + 销售经理
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'sales.lead@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_SALES_MANAGER');

-- 销售代表 -> 销售代表
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email LIKE 'sales.%@test.com' AND u.email != 'sales.lead@test.com' AND r.code = 'ROLE_SALES_REP';

-- HR经理 -> 部门经理 + HR专员
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'hr.manager@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_HR');

-- HR专员 -> HR专员
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'hr.specialist@test.com' AND r.code = 'ROLE_HR';

-- 财务经理 -> 部门经理 + 财务专员
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'finance.manager@test.com' AND r.code IN ('ROLE_DEPT_MANAGER', 'ROLE_FINANCE');

-- 财务专员 -> 财务专员
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'finance.specialist@test.com' AND r.code = 'ROLE_FINANCE';

-- 待定用户 -> 普通用户
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'pending.user@test.com' AND r.code = 'ROLE_USER';

-- 锁定用户 -> 普通用户
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'locked.user@test.com' AND r.code = 'ROLE_USER';

-- 禁用用户 -> 访客
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = 'inactive.user@test.com' AND r.code = 'ROLE_GUEST';

COMMIT;

-- 验证数据
SELECT '用户角色关联数据插入完成' as status,
       u.email as user_email,
       STRING_AGG(r.name, ', ') as roles
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email IN ('superadmin@test.com', 'admin@test.com', 'tech.lead@test.com', 'qa.lead@test.com')
GROUP BY u.id, u.email
ORDER BY u.email;
