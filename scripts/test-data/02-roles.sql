-- =============================================
-- 测试数据: 角色数据
-- 用于本地开发和测试环境
-- 执行顺序: 02
-- =============================================

BEGIN;

-- 清空现有角色数据
TRUNCATE TABLE roles CASCADE;

-- =============================================
-- 插入系统预设角色
-- =============================================

INSERT INTO roles (id, name, code, description, data_scope, status, is_system, created_at, updated_at, version)
VALUES
    -- 系统管理角色
    (gen_random_uuid(), '超级管理员', 'ROLE_SUPER_ADMIN', '系统超级管理员，拥有所有权限', 'ALL', 'ACTIVE', TRUE, NOW(), NOW(), 0),
    (gen_random_uuid(), '系统管理员', 'ROLE_SYSTEM_ADMIN', '系统管理员，负责系统配置和用户管理', 'ALL', 'ACTIVE', TRUE, NOW(), NOW(), 0),

    -- 业务管理角色
    (gen_random_uuid(), '部门经理', 'ROLE_DEPT_MANAGER', '部门负责人，管理部门成员和数据', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '项目经理', 'ROLE_PROJECT_MANAGER', '项目负责人，管理项目相关资源', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),

    -- 技术角色
    (gen_random_uuid(), '技术负责人', 'ROLE_TECH_LEAD', '技术团队负责人，管理技术团队', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '开发工程师', 'ROLE_DEVELOPER', '普通开发人员', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '测试工程师', 'ROLE_TESTER', '测试人员，负责质量保证', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '运维工程师', 'ROLE_DEVOPS', '运维人员，负责系统部署和维护', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),

    -- 产品角色
    (gen_random_uuid(), '产品经理', 'ROLE_PM', '产品经理，负责产品规划', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '产品设计师', 'ROLE_DESIGNER', 'UI/UX设计师', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),

    -- 运营销售角色
    (gen_random_uuid(), '运营专员', 'ROLE_OPERATOR', '运营人员，负责用户运营', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '销售经理', 'ROLE_SALES_MANAGER', '销售团队负责人', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '销售代表', 'ROLE_SALES_REP', '销售人员', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),

    -- 职能角色
    (gen_random_uuid(), 'HR专员', 'ROLE_HR', '人力资源专员', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '财务专员', 'ROLE_FINANCE', '财务人员', 'DEPT', 'ACTIVE', FALSE, NOW(), NOW(), 0),

    -- 普通用户
    (gen_random_uuid(), '普通用户', 'ROLE_USER', '普通系统用户，基础权限', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0),
    (gen_random_uuid(), '访客', 'ROLE_GUEST', '访客用户，只读权限', 'SELF', 'ACTIVE', FALSE, NOW(), NOW(), 0);

COMMIT;

-- 验证数据
SELECT '角色数据插入完成' as status,
       COUNT(*) as total_roles,
       COUNT(*) FILTER (WHERE is_system = TRUE) as system_roles,
       COUNT(*) FILTER (WHERE is_system = FALSE) as custom_roles,
       COUNT(*) FILTER (WHERE data_scope = 'ALL') as all_scope,
       COUNT(*) FILTER (WHERE data_scope = 'DEPT') as dept_scope,
       COUNT(*) FILTER (WHERE data_scope = 'SELF') as self_scope
FROM roles;
