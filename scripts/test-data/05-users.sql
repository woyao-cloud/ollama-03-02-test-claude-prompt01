-- =============================================
-- 测试数据: 用户数据
-- 用于本地开发和测试环境
-- 执行顺序: 05 (在部门和角色之后执行)
-- =============================================

BEGIN;

-- 清空现有用户数据 (保留系统用户)
DELETE FROM users WHERE email NOT LIKE '%admin%';

-- =============================================
-- 辅助函数: 生成BCrypt密码哈希 (密码: Test@123)
-- 实际值: $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ...
-- =============================================

-- 密码都是 'Test@123' 的BCrypt哈希
-- 使用固定哈希值以确保一致性

-- =============================================
-- 插入测试用户数据
-- =============================================

-- 超级管理员
INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, last_login_at, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'superadmin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '超级', '管理员', '13800000001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=superadmin', NULL, 'ACTIVE', TRUE, 0, NOW() - INTERVAL '1 day', NOW(), NOW(), 0);

-- 系统管理员
INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, last_login_at, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'admin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '系统', '管理员', '13800000002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', NULL, 'ACTIVE', TRUE, 0, NOW() - INTERVAL '2 days', NOW(), NOW(), 0);

-- 获取部门ID用于分配用户
DO $$
DECLARE
    dept_tech UUID;
    dept_fe UUID;
    dept_be UUID;
    dept_qa UUID;
    dept_ops UUID;
    dept_product UUID;
    dept_uiux UUID;
    dept_pm UUID;
    dept_sales UUID;
    dept_hr UUID;
    dept_finance UUID;
BEGIN
    SELECT id INTO dept_tech FROM departments WHERE code = 'DEPT-TECH';
    SELECT id INTO dept_fe FROM departments WHERE code = 'DEPT-FE';
    SELECT id INTO dept_be FROM departments WHERE code = 'DEPT-BE';
    SELECT id INTO dept_qa FROM departments WHERE code = 'DEPT-QA';
    SELECT id INTO dept_ops FROM departments WHERE code = 'DEPT-OPS';
    SELECT id INTO dept_product FROM departments WHERE code = 'DEPT-PRODUCT';
    SELECT id INTO dept_uiux FROM departments WHERE code = 'DEPT-UIUX';
    SELECT id INTO dept_pm FROM departments WHERE code = 'DEPT-PM';
    SELECT id INTO dept_sales FROM departments WHERE code = 'DEPT-SALES';
    SELECT id INTO dept_hr FROM departments WHERE code = 'DEPT-HR';
    SELECT id INTO dept_finance FROM departments WHERE code = 'DEPT-FINANCE';

    -- 技术研发中心用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        -- 技术总监
        (gen_random_uuid(), 'tech.lead@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '技术', '总监', '13800100001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=techlead', dept_tech, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 前端开发工程师
        (gen_random_uuid(), 'fe.dev1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '张三', '前端', '13800100002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=fedev1', dept_fe, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'fe.dev2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '李四', '前端', '13800100003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=fedev2', dept_fe, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'fe.dev3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '王五', '前端', '13800100004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=fedev3', dept_fe, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 后端开发工程师
        (gen_random_uuid(), 'be.dev1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '赵六', '后端', '13800100005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bedev1', dept_be, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'be.dev2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '钱七', '后端', '13800100006', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bedev2', dept_be, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'be.dev3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '孙八', '后端', '13800100007', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bedev3', dept_be, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 测试工程师
        (gen_random_uuid(), 'qa.lead@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '测试', '负责人', '13800100008', 'https://api.dicebear.com/7.x/avataaars/svg?seed=qalead', dept_qa, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'qa.tester1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '周九', '测试', '13800100009', 'https://api.dicebear.com/7.x/avataaars/svg?seed=qatester1', dept_qa, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'qa.tester2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '吴十', '测试', '13800100010', 'https://api.dicebear.com/7.x/avataaars/svg?seed=qatester2', dept_qa, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 运维工程师
        (gen_random_uuid(), 'ops.lead@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '运维', '负责人', '13800100011', 'https://api.dicebear.com/7.x/avataaars/svg?seed=opslead', dept_ops, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'ops.dev1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '郑一', '运维', '13800100012', 'https://api.dicebear.com/7.x/avataaars/svg?seed=opsdev1', dept_ops, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0);

    -- 产品运营中心用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        -- 产品总监
        (gen_random_uuid(), 'product.lead@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '产品', '总监', '13800200001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=productlead', dept_product, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- UI/UX设计师
        (gen_random_uuid(), 'ui.designer1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '陈二', '设计师', '13800200002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=uidesigner1', dept_uiux, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'ui.designer2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '刘三', '设计师', '13800200003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=uidesigner2', dept_uiux, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 产品经理
        (gen_random_uuid(), 'pm1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '黄四', '产品经理', '13800200004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=pm1', dept_pm, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'pm2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '林五', '产品经理', '13800200005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=pm2', dept_pm, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 运营专员
        (gen_random_uuid(), 'ops.user1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '何六', '运营', '13800200006', 'https://api.dicebear.com/7.x/avataaars/svg?seed=opsuser1', dept_product, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'ops.user2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '郭七', '运营', '13800200007', 'https://api.dicebear.com/7.x/avataaars/svg?seed=opsuser2', dept_product, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0);

    -- 市场销售中心用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        -- 销售总监
        (gen_random_uuid(), 'sales.lead@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '销售', '总监', '13800300001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=saleslead', dept_sales, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 华北销售
        (gen_random_uuid(), 'sales.n1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '马八', '华北销售', '13800300002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=salesn1', dept_sales, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 华南销售
        (gen_random_uuid(), 'sales.s1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '罗九', '华南销售', '13800300003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=saless1', dept_sales, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        -- 华东销售
        (gen_random_uuid(), 'sales.e1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '梁十', '华东销售', '13800300004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=salese1', dept_sales, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0);

    -- 人力资源部用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), 'hr.manager@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', 'HR', '经理', '13800400001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=hrmanager', dept_hr, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'hr.specialist@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '宋一', 'HR专员', '13800400002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=hrspecialist', dept_hr, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0);

    -- 财务行政部用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), 'finance.manager@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '财务', '经理', '13800500001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=financemanager', dept_finance, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), 'finance.specialist@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '唐二', '财务专员', '13800500002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=financespecialist', dept_finance, 'ACTIVE', TRUE, 0, NOW(), NOW(), 0);

    -- 添加一些特殊状态的用户
    INSERT INTO users (id, email, password_hash, first_name, last_name, phone, avatar_url, department_id, status, email_verified, failed_login_attempts, created_at, updated_at, version)
    VALUES
        -- 待激活用户
        (gen_random_uuid(), 'pending.user@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '待定', '用户', '13800600001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=pending', dept_fe, 'PENDING', FALSE, 0, NOW(), NOW(), 0),
        -- 锁定用户
        (gen_random_uuid(), 'locked.user@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '锁定', '用户', '13800600002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=locked', dept_be, 'LOCKED', TRUE, 5, NOW() - INTERVAL '1 day', NOW(), NOW(), 0),
        -- 禁用用户
        (gen_random_uuid(), 'inactive.user@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrzQ.zJGI7C0QYxJ1n3X0uZ0X0uZ0O', '禁用', '用户', '13800600003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=inactive', dept_qa, 'INACTIVE', TRUE, 0, NOW(), NOW(), 0);

END $$;

COMMIT;

-- 验证数据
SELECT '用户数据插入完成' as status,
       COUNT(*) as total_users,
       COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_users,
       COUNT(*) FILTER (WHERE status = 'PENDING') as pending_users,
       COUNT(*) FILTER (WHERE status = 'LOCKED') as locked_users,
       COUNT(*) FILTER (WHERE status = 'INACTIVE') as inactive_users
FROM users;
