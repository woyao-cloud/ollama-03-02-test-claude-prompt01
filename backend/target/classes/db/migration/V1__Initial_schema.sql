-- V1__Initial_schema.sql
-- 用户角色权限管理系统 - 初始数据库结构
-- PostgreSQL 15
-- 创建日期: 2026-03-24

BEGIN;

-- =============================================
-- 扩展插件
-- =============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- 1. departments 部门表 (先创建，users表依赖)
-- =============================================
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    parent_id UUID REFERENCES departments(id),
    manager_id UUID,  -- 部门负责人ID，应用层维护，避免循环依赖
    level INT NOT NULL CHECK (level BETWEEN 1 AND 5),
    path VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_departments_code UNIQUE (code),
    CONSTRAINT chk_departments_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_no_self_parent CHECK (parent_id IS NULL OR parent_id != id),
    CONSTRAINT chk_departments_level CHECK (level >= 1 AND level <= 5)
);

-- 部门表注释
COMMENT ON TABLE departments IS '部门表 - 使用Materialized Path存储树形结构';
COMMENT ON COLUMN departments.id IS '部门ID';
COMMENT ON COLUMN departments.name IS '部门名称';
COMMENT ON COLUMN departments.code IS '部门编码，如 DEPT-001';
COMMENT ON COLUMN departments.parent_id IS '父部门ID';
COMMENT ON COLUMN departments.manager_id IS '部门负责人ID';
COMMENT ON COLUMN departments.level IS '层级：1=公司，2=一级部门，3=二级部门...';
COMMENT ON COLUMN departments.path IS 'Materialized Path，如 /1/2/5';
COMMENT ON COLUMN departments.sort_order IS '排序号（越大越靠前）';
COMMENT ON COLUMN departments.status IS '状态：ACTIVE/INACTIVE';
COMMENT ON COLUMN departments.deleted_at IS '删除时间（软删除）';
COMMENT ON COLUMN departments.version IS '乐观锁版本号';

-- 部门表索引
CREATE UNIQUE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_departments_path ON departments(path);
CREATE INDEX idx_departments_path_pattern ON departments USING btree (path varchar_pattern_ops);
CREATE INDEX idx_departments_parent ON departments(parent_id);
CREATE INDEX idx_departments_level ON departments(level);
CREATE INDEX idx_departments_level_status ON departments(level, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_departments_parent_sort ON departments(parent_id, sort_order DESC);

-- =============================================
-- 2. users 用户表
-- =============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    department_id UUID REFERENCES departments(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'LOCKED')),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_phone_format CHECK (phone IS NULL OR phone ~ '^1[3-9]\d{9}$')
);

-- 用户表注释
COMMENT ON TABLE users IS '用户表 - 存储系统用户基本信息和认证状态';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.email IS '邮箱地址，全局唯一';
COMMENT ON COLUMN users.password_hash IS 'BCrypt加密后的密码';
COMMENT ON COLUMN users.first_name IS '名';
COMMENT ON COLUMN users.last_name IS '姓';
COMMENT ON COLUMN users.phone IS '手机号（11位）';
COMMENT ON COLUMN users.department_id IS '所属部门ID';
COMMENT ON COLUMN users.status IS '状态：ACTIVE/INACTIVE/PENDING/LOCKED';
COMMENT ON COLUMN users.failed_login_attempts IS '连续登录失败次数';
COMMENT ON COLUMN users.locked_until IS '锁定截止时间';
COMMENT ON COLUMN users.deleted_at IS '删除时间（软删除）';
COMMENT ON COLUMN users.version IS '乐观锁版本号';

-- 用户表索引
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_active ON users(id) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX idx_users_dept_status ON users(department_id, status) WHERE deleted_at IS NULL;

-- =============================================
-- 3. roles 角色表
-- =============================================
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    data_scope VARCHAR(20) NOT NULL DEFAULT 'ALL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT uq_roles_code UNIQUE (code),
    CONSTRAINT chk_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_roles_data_scope CHECK (data_scope IN ('ALL', 'DEPT', 'SELF', 'CUSTOM'))
);

-- 角色表注释
COMMENT ON TABLE roles IS '角色表 - 定义系统角色和数据权限范围';
COMMENT ON COLUMN roles.code IS '角色编码，如 ROLE_ADMIN';
COMMENT ON COLUMN roles.data_scope IS '数据权限范围：ALL/DEPT/SELF/CUSTOM';
COMMENT ON COLUMN roles.is_system IS '是否系统预设角色（不可删除）';

-- 角色表索引
CREATE UNIQUE INDEX idx_roles_name ON roles(name);
CREATE UNIQUE INDEX idx_roles_code ON roles(code);
CREATE INDEX idx_roles_status ON roles(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_roles_data_scope ON roles(data_scope);

-- =============================================
-- 4. permissions 权限表
-- =============================================
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50),
    parent_id UUID REFERENCES permissions(id),
    icon VARCHAR(100),
    route VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,  -- 软删除字段
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_permissions_code UNIQUE (code),
    CONSTRAINT chk_permissions_type CHECK (type IN ('MENU', 'ACTION', 'FIELD', 'DATA')),
    CONSTRAINT chk_permissions_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 权限表注释
COMMENT ON TABLE permissions IS '权限表 - 支持四级权限模型（菜单/操作/字段/数据）';
COMMENT ON COLUMN permissions.code IS '权限编码，如 user:create';
COMMENT ON COLUMN permissions.type IS '权限类型：MENU/ACTION/FIELD/DATA';
COMMENT ON COLUMN permissions.resource IS '资源类型，如 user';
COMMENT ON COLUMN permissions.action IS '操作类型，如 create/read/update/delete';
COMMENT ON COLUMN permissions.parent_id IS '父权限ID（用于菜单层级）';
COMMENT ON COLUMN permissions.route IS '前端路由（仅MENU类型）';
COMMENT ON COLUMN permissions.deleted_at IS '删除时间（软删除）';

-- 权限表索引
CREATE UNIQUE INDEX idx_permissions_code ON permissions(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_type ON permissions(type);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_type_resource ON permissions(type, resource);
CREATE INDEX idx_permissions_parent ON permissions(parent_id);
CREATE INDEX idx_permissions_status ON permissions(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_deleted ON permissions(deleted_at) WHERE deleted_at IS NOT NULL;

-- =============================================
-- 5. role_permissions 角色权限关联表
-- =============================================
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, permission_id)
);

-- 角色权限关联表注释
COMMENT ON TABLE role_permissions IS '角色权限关联表 - 多对多关系';

-- 角色权限关联表索引
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- =============================================
-- 6. user_roles 用户角色关联表
-- =============================================
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id)
);

-- 用户角色关联表注释
COMMENT ON TABLE user_roles IS '用户角色关联表 - 多对多关系';

-- 用户角色关联表索引
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- =============================================
-- 7. user_sessions 用户会话表
-- =============================================
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_token VARCHAR(2048) NOT NULL,
    refresh_token VARCHAR(2048) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    client_ip VARCHAR(45),
    user_agent TEXT,
    device_info VARCHAR(200),
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_sessions_access_token UNIQUE (access_token),
    CONSTRAINT uq_sessions_refresh_token UNIQUE (refresh_token)
);

-- 会话表注释
COMMENT ON TABLE user_sessions IS '用户会话表 - 支持分布式会话管理';
COMMENT ON COLUMN user_sessions.access_token IS 'Access Token';
COMMENT ON COLUMN user_sessions.refresh_token IS 'Refresh Token';
COMMENT ON COLUMN user_sessions.expires_at IS 'Access Token过期时间';
COMMENT ON COLUMN user_sessions.is_valid IS '是否有效（登出时设为FALSE）';
COMMENT ON COLUMN user_sessions.updated_at IS '更新时间';
COMMENT ON COLUMN user_sessions.deleted_at IS '删除时间（软删除）';
COMMENT ON COLUMN user_sessions.version IS '乐观锁版本号';

-- 会话表索引
CREATE UNIQUE INDEX idx_sessions_access_token ON user_sessions(access_token);
CREATE UNIQUE INDEX idx_sessions_refresh_token ON user_sessions(refresh_token);
CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires ON user_sessions(expires_at);
CREATE INDEX idx_sessions_user_valid ON user_sessions(user_id, is_valid) WHERE is_valid = TRUE;
CREATE INDEX idx_sessions_created ON user_sessions(created_at);

-- =============================================
-- 8. 自动更新 updated_at 的触发器函数
-- 必须在所有触发器之前定义
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 用户会话表触发器
CREATE TRIGGER update_user_sessions_updated_at
    BEFORE UPDATE ON user_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 8. audit_logs 审计日志表（分区表）
-- =============================================

-- 创建主表（模板表）
CREATE TABLE audit_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(100),
    operation VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    old_value JSONB,
    new_value JSONB,
    description TEXT,
    client_ip VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(100),
    success BOOLEAN NOT NULL,
    error_message TEXT,
    execution_time_ms INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 审计日志表注释
COMMENT ON TABLE audit_logs IS '审计日志表 - 按月分区存储操作记录';
COMMENT ON COLUMN audit_logs.operation IS '操作类型：CREATE/UPDATE/DELETE/LOGIN/LOGOUT等';
COMMENT ON COLUMN audit_logs.resource_type IS '资源类型：USER/ROLE/PERMISSION/DEPARTMENT等';
COMMENT ON COLUMN audit_logs.old_value IS '操作前数据（JSON格式）';
COMMENT ON COLUMN audit_logs.new_value IS '操作后数据（JSON格式）';
COMMENT ON COLUMN audit_logs.execution_time_ms IS '执行耗时（毫秒）';

-- 创建分区（当前月及未来3个月）
CREATE TABLE audit_logs_2026_03 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE audit_logs_2026_04 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE audit_logs_2026_05 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE audit_logs_2026_06 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

-- 默认分区（处理未来数据）
CREATE TABLE audit_logs_default PARTITION OF audit_logs DEFAULT;

-- 在分区上创建索引
CREATE INDEX idx_audit_logs_2026_03_user ON audit_logs_2026_03(user_id);
CREATE INDEX idx_audit_logs_2026_03_time ON audit_logs_2026_03(created_at DESC);
CREATE INDEX idx_audit_logs_2026_03_resource ON audit_logs_2026_03(resource_type, resource_id);
CREATE INDEX idx_audit_logs_2026_03_operation ON audit_logs_2026_03(operation, created_at DESC);

CREATE INDEX idx_audit_logs_2026_04_user ON audit_logs_2026_04(user_id);
CREATE INDEX idx_audit_logs_2026_04_time ON audit_logs_2026_04(created_at DESC);
CREATE INDEX idx_audit_logs_2026_04_resource ON audit_logs_2026_04(resource_type, resource_id);
CREATE INDEX idx_audit_logs_2026_04_operation ON audit_logs_2026_04(operation, created_at DESC);

CREATE INDEX idx_audit_logs_2026_05_user ON audit_logs_2026_05(user_id);
CREATE INDEX idx_audit_logs_2026_05_time ON audit_logs_2026_05(created_at DESC);
CREATE INDEX idx_audit_logs_2026_05_resource ON audit_logs_2026_05(resource_type, resource_id);
CREATE INDEX idx_audit_logs_2026_05_operation ON audit_logs_2026_05(operation, created_at DESC);

CREATE INDEX idx_audit_logs_2026_06_user ON audit_logs_2026_06(user_id);
CREATE INDEX idx_audit_logs_2026_06_time ON audit_logs_2026_06(created_at DESC);
CREATE INDEX idx_audit_logs_2026_06_resource ON audit_logs_2026_06(resource_type, resource_id);
CREATE INDEX idx_audit_logs_2026_06_operation ON audit_logs_2026_06(operation, created_at DESC);

-- =============================================
-- 9. 为其他表创建 updated_at 触发器
-- （函数已在前面定义）
-- =============================================

-- 为用户表创建触发器
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为部门表创建触发器
CREATE TRIGGER trg_departments_updated_at
    BEFORE UPDATE ON departments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为角色表创建触发器
CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为权限表创建触发器
CREATE TRIGGER trg_permissions_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 10. 创建分区管理函数
-- =============================================
CREATE OR REPLACE FUNCTION create_audit_log_partition(
    p_year INT,
    p_month INT
) RETURNS TEXT AS $$
DECLARE
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
BEGIN
    partition_name := 'audit_logs_' || p_year || '_' || LPAD(p_month::TEXT, 2, '0');
    start_date := make_date(p_year, p_month, 1);
    end_date := start_date + INTERVAL '1 month';

    -- 检查分区是否已存在
    IF EXISTS (
        SELECT 1 FROM pg_tables
        WHERE tablename = partition_name
        AND schemaname = 'public'
    ) THEN
        RETURN 'Partition ' || partition_name || ' already exists';
    END IF;

    -- 创建分区
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs
         FOR VALUES FROM (%L) TO (%L)',
        partition_name, start_date, end_date
    );

    -- 在分区上创建索引
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%s_user ON %I(user_id)',
        partition_name, partition_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%s_time ON %I(created_at DESC)',
        partition_name, partition_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%s_resource ON %I(resource_type, resource_id)',
        partition_name, partition_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%s_operation ON %I(operation, created_at DESC)',
        partition_name, partition_name
    );

    RETURN 'Created partition: ' || partition_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_audit_log_partition IS '创建审计日志分区表';

-- =============================================
-- 11. 插入默认数据
-- =============================================

-- 创建根部门（公司）
INSERT INTO departments (id, name, code, parent_id, level, path, sort_order, description, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '总公司',
    'DEPT-ROOT',
    NULL,
    1,
    '/00000000-0000-0000-0000-000000000001',
    0,
    '根部门，代表整个公司',
    'ACTIVE'
);

-- 创建默认角色
INSERT INTO roles (id, name, code, description, data_scope, status, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000001', '超级管理员', 'ROLE_SUPER_ADMIN', '拥有系统所有权限', 'ALL', 'ACTIVE', TRUE),
    ('00000000-0000-0000-0000-000000000002', '系统管理员', 'ROLE_ADMIN', '管理系统用户和权限', 'ALL', 'ACTIVE', TRUE),
    ('00000000-0000-0000-0000-000000000003', '部门经理', 'ROLE_DEPT_MANAGER', '管理部门成员', 'DEPT', 'ACTIVE', FALSE),
    ('00000000-0000-0000-0000-000000000004', '普通用户', 'ROLE_USER', '标准用户权限', 'SELF', 'ACTIVE', FALSE),
    ('00000000-0000-0000-0000-000000000005', '审计员', 'ROLE_AUDITOR', '查看审计日志', 'ALL', 'ACTIVE', FALSE);

-- 创建默认权限（菜单权限）
INSERT INTO permissions (id, name, code, type, resource, action, sort_order, status)
VALUES
    -- 系统管理菜单
    ('00000000-0000-0000-0000-000000000001', '系统管理', 'system:manage', 'MENU', 'system', NULL, 1, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', '用户管理', 'system:user:menu', 'MENU', 'user', NULL, 10, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003', '角色管理', 'system:role:menu', 'MENU', 'role', NULL, 20, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000004', '权限管理', 'system:permission:menu', 'MENU', 'permission', NULL, 30, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000005', '部门管理', 'system:dept:menu', 'MENU', 'department', NULL, 40, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000006', '审计日志', 'system:audit:menu', 'MENU', 'audit', NULL, 50, 'ACTIVE'),

    -- 用户操作权限
    ('00000000-0000-0000-0000-000000000007', '用户查看', 'user:read', 'ACTION', 'user', 'read', 1, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000008', '用户创建', 'user:create', 'ACTION', 'user', 'create', 2, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000009', '用户更新', 'user:update', 'ACTION', 'user', 'update', 3, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000010', '用户删除', 'user:delete', 'ACTION', 'user', 'delete', 4, 'ACTIVE'),

    -- 角色操作权限
    ('00000000-0000-0000-0000-000000000011', '角色查看', 'role:read', 'ACTION', 'role', 'read', 1, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000012', '角色创建', 'role:create', 'ACTION', 'role', 'create', 2, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000013', '角色更新', 'role:update', 'ACTION', 'role', 'update', 3, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000014', '角色删除', 'role:delete', 'ACTION', 'role', 'delete', 4, 'ACTIVE'),

    -- 部门操作权限
    ('00000000-0000-0000-0000-000000000015', '部门查看', 'department:read', 'ACTION', 'department', 'read', 1, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000016', '部门创建', 'department:create', 'ACTION', 'department', 'create', 2, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000017', '部门更新', 'department:update', 'ACTION', 'department', 'update', 3, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000018', '部门删除', 'department:delete', 'ACTION', 'department', 'delete', 4, 'ACTIVE');

-- 为超级管理员分配所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions;

-- 为系统管理员分配除系统管理外的权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions;

-- 创建分区函数已在上面定义

-- =============================================
-- 13. 提交事务
-- =============================================

COMMIT;

-- 验证数据
SELECT '数据库初始化完成' as status,
       (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public') as table_count;

COMMIT;
