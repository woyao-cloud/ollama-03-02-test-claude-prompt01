-- =============================================
-- 测试数据: 部门数据
-- 用于本地开发和测试环境
-- 执行顺序: 01 (最先执行)
-- =============================================

BEGIN;

-- 清空现有测试数据 (保留已有关联数据的处理)
TRUNCATE TABLE departments CASCADE;

-- 重置序列
ALTER SEQUENCE IF EXISTS departments_id_seq RESTART WITH 1;

-- =============================================
-- 插入部门数据
-- 层级结构: 总公司 -> 一级部门 -> 二级部门
-- =============================================

-- 总公司 (Level 1)
INSERT INTO departments (id, name, code, parent_id, manager_id, level, path, sort_order, description, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), '科技有限公司', 'DEPT-COMPANY', NULL, NULL, 1, '/1', 1, '总公司', 'ACTIVE', NOW(), NOW(), 0);

-- 获取总公司ID用于子部门
DO $$
DECLARE
    company_id UUID;
BEGIN
    SELECT id INTO company_id FROM departments WHERE code = 'DEPT-COMPANY';

    -- 一级部门 (Level 2)
    INSERT INTO departments (id, name, code, parent_id, manager_id, level, path, sort_order, description, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '技术研发中心', 'DEPT-TECH', company_id, NULL, 2, '/1/2', 1, '负责产品研发和技术架构', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '产品运营中心', 'DEPT-PRODUCT', company_id, NULL, 2, '/1/3', 2, '负责产品规划和运营管理', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '市场销售中心', 'DEPT-SALES', company_id, NULL, 2, '/1/4', 3, '负责市场推广和销售业务', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '人力资源部', 'DEPT-HR', company_id, NULL, 2, '/1/5', 4, '负责人事招聘和员工管理', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '财务行政部', 'DEPT-FINANCE', company_id, NULL, 2, '/1/6', 5, '负责财务和行政管理', 'ACTIVE', NOW(), NOW(), 0);

END $$;

-- 获取一级部门ID用于二级部门
DO $$
DECLARE
    tech_id UUID;
    product_id UUID;
    sales_id UUID;
    hr_id UUID;
    finance_id UUID;
BEGIN
    SELECT id INTO tech_id FROM departments WHERE code = 'DEPT-TECH';
    SELECT id INTO product_id FROM departments WHERE code = 'DEPT-PRODUCT';
    SELECT id INTO sales_id FROM departments WHERE code = 'DEPT-SALES';
    SELECT id INTO hr_id FROM departments WHERE code = 'DEPT-HR';
    SELECT id INTO finance_id FROM departments WHERE code = 'DEPT-FINANCE';

    -- 技术研发中心下属部门 (Level 3)
    INSERT INTO departments (id, name, code, parent_id, manager_id, level, path, sort_order, description, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '前端开发部', 'DEPT-FE', tech_id, NULL, 3, '/1/2/7', 1, '负责前端应用开发', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '后端开发部', 'DEPT-BE', tech_id, NULL, 3, '/1/2/8', 2, '负责后端服务开发', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '测试质量部', 'DEPT-QA', tech_id, NULL, 3, '/1/2/9', 3, '负责软件测试和质量保障', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '运维安全部', 'DEPT-OPS', tech_id, NULL, 3, '/1/2/10', 4, '负责系统运维和信息安全', 'ACTIVE', NOW(), NOW(), 0);

    -- 产品运营中心下属部门 (Level 3)
    INSERT INTO departments (id, name, code, parent_id, manager_id, level, path, sort_order, description, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '产品设计部', 'DEPT-UIUX', product_id, NULL, 3, '/1/3/11', 1, '负责产品UI/UX设计', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '产品策划部', 'DEPT-PM', product_id, NULL, 3, '/1/3/12', 2, '负责产品规划和需求分析', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '用户运营部', 'DEPT-OPERATION', product_id, NULL, 3, '/1/3/13', 3, '负责用户增长和运营活动', 'ACTIVE', NOW(), NOW(), 0);

    -- 市场销售中心下属部门 (Level 3)
    INSERT INTO departments (id, name, code, parent_id, manager_id, level, path, sort_order, description, status, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), '华北销售部', 'DEPT-SALES-N', sales_id, NULL, 3, '/1/4/14', 1, '负责华北区域销售', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '华南销售部', 'DEPT-SALES-S', sales_id, NULL, 3, '/1/4/15', 2, '负责华南区域销售', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '华东销售部', 'DEPT-SALES-E', sales_id, NULL, 3, '/1/4/16', 3, '负责华东区域销售', 'ACTIVE', NOW(), NOW(), 0),
        (gen_random_uuid(), '市场策划部', 'DEPT-MARKETING', sales_id, NULL, 3, '/1/4/17', 4, '负责市场推广和品牌建设', 'ACTIVE', NOW(), NOW(), 0);

END $$;

-- 更新路径字段为实际ID路径
UPDATE departments SET path = '/1' WHERE code = 'DEPT-COMPANY';

UPDATE departments d1
SET path = '/1/' || (SELECT id::text FROM departments WHERE code = 'DEPT-COMPANY')
WHERE d1.code IN ('DEPT-TECH', 'DEPT-PRODUCT', 'DEPT-SALES', 'DEPT-HR', 'DEPT-FINANCE');

COMMIT;

-- 验证数据
SELECT '部门数据插入完成' as status,
       COUNT(*) as total_departments,
       COUNT(*) FILTER (WHERE level = 1) as level1_count,
       COUNT(*) FILTER (WHERE level = 2) as level2_count,
       COUNT(*) FILTER (WHERE level = 3) as level3_count
FROM departments;
