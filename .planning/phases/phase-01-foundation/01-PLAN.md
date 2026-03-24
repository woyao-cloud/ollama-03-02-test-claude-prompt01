---
phase: 1
plan: 01
title: 数据库设计与 Flyway 迁移
requirements_addressed: [USER-01, USER-02, USER-03, USER-04, ROLE-01, ROLE-02, ROLE-06, ROLE-07, PERM-01, PERM-02, PERM-05, PERM-06, PERM-07, AUDIT-01, AUDIT-02, AUDIT-04, AUDIT-06]
depends_on: []
wave: 1
autonomous: false
---

# Plan 1.1: 数据库设计与 Flyway 迁移

## Objective

建立全栈用户管理系统的数据基础，创建完整的JPA实体类和Flyway迁移脚本，实现PostgreSQL/H2双数据库支持，为后续业务功能开发提供可靠的数据层。

**Purpose:** 数据层是所有业务功能的基础，必须先于业务代码完成，确保数据模型设计正确、约束完整。

**Output:**
- JPA实体类 (User, Role, Permission, UserRole, RolePermission, AuditLog)
- Flyway迁移脚本 (V1__init.sql)
- 双数据库配置 (PostgreSQL生产/H2开发测试)
- 基础索引和约束

---

## Context

### 技术约束
- 数据库: PostgreSQL 15 (生产) / H2 (开发测试)
- ORM: Spring Data JPA 3.5
- 迁移工具: Flyway 10.x
- 主键: UUID v4
- 软删除: deleted_at字段

### 数据模型关系
```
User (用户) ──────── UserRole ──────── Role (角色)
                                            │
                                            RolePermission
                                            │
                                       Permission (权限)
```

### 核心表设计参考
- 用户表: 存储用户基本信息、状态、登录信息
- 角色表: 角色定义，支持数据权限范围
- 权限表: 四级权限模型 (MENU/ACTION/FIELD/DATA)
- 审计日志表: 分区表设计，按月分区

---

## Tasks

### Task 1: 创建 JPA 实体类 - 用户与角色

**描述:** 创建 User、Role、Permission 三个核心JPA实体类

**文件:**
- `backend/src/main/java/com/usermanagement/domain/User.java`
- `backend/src/main/java/com/usermanagement/domain/Role.java`
- `backend/src/main/java/com/usermanagement/domain/Permission.java`

**依赖:** 无

**验收标准:**
- User.java 包含必需字段: id(UUID), email, password_hash, first_name, last_name, phone, status, failed_login_attempts, locked_until, last_login_at, created_at, updated_at, deleted_at
- Role.java 包含必需字段: id(UUID), name, code(唯一), description, data_scope, status, created_at, updated_at, deleted_at
- Permission.java 包含必需字段: id(UUID), name, code(唯一), type(MENU/ACTION/FIELD/DATA), resource, action, parent_id, sort_order, status
- 所有实体使用 `@Entity` 和 `@Table` 注解
- 字段使用 JPA 注解正确映射
- 启用 JPA Auditing (created_at/updated_at自动更新)

---

### Task 2: 创建 JPA 实体类 - 关联实体与审计日志

**描述:** 创建 UserRole、RolePermission 关联实体和 AuditLog 审计日志实体

**文件:**
- `backend/src/main/java/com/usermanagement/domain/UserRole.java`
- `backend/src/main/java/com/usermanagement/domain/RolePermission.java`
- `backend/src/main/java/com/usermanagement/domain/AuditLog.java`
- `backend/src/main/java/com/usermanagement/domain/BaseEntity.java` (公共基类)

**依赖:** Task 1 完成

**验收标准:**
- UserRole.java: 复合主键 (user_id, role_id), 使用 `@ManyToOne` 关联 User 和 Role
- RolePermission.java: 复合主键 (role_id, permission_id), 使用 `@ManyToOne` 关联 Role 和 Permission
- AuditLog.java: 包含 id, user_id, username, operation, resource_type, resource_id, old_value(JSONB), new_value(JSONB), client_ip, user_agent, success, error_message, execution_time_ms, created_at
- BaseEntity.java: 提取公共字段 (id, created_at, updated_at, deleted_at)
- 所有关联实体配置级联关系和懒加载

---

### Task 3: 创建 Flyway 迁移脚本

**描述:** 编写初始数据库迁移脚本 V1__init.sql，包含所有表创建、索引、约束和外键

**文件:**
- `backend/src/main/resources/db/migration/V1__init.sql`

**依赖:** Task 1, Task 2 完成

**验收标准:**
- 创建 users 表，包含所有字段、CHECK约束、索引
- 创建 roles 表，code字段唯一约束
- 创建 permissions 表，code字段唯一约束
- 创建 user_roles 关联表，复合主键，外键约束
- 创建 role_permissions 关联表，复合主键，外键约束
- 创建 audit_log 表，包含 JSONB 字段，按 created_at 分区
- 创建所有必要索引 (email, status, code, type等)
- 插入初始数据: 超级管理员角色、基础权限

**SQL验证:**
```sql
-- 检查所有表创建
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

-- 检查约束
SELECT constraint_name, table_name FROM information_schema.table_constraints WHERE table_schema = 'public';
```

---

### Task 4: 配置双数据库支持 (PostgreSQL + H2)

**描述:** 配置Spring Boot应用支持PostgreSQL(生产)和H2(开发/测试)双数据库

**文件:**
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/main/resources/application-prod.yml`

**依赖:** Task 3 完成

**验收标准:**
- application.yml: 公共配置，默认dev profile
- application-dev.yml: H2内存数据库配置，启用H2 Console
- application-test.yml: H2内存数据库配置，用于单元测试
- application-prod.yml: PostgreSQL配置，连接池配置(HikariCP)
- 配置Flyway迁移启用
- 配置JPA属性 (ddl-auto=validate, show-sql=false)
- 配置连接池参数 (最小5, 最大20, 连接超时30s)

---

### Task 5: 创建 Repository 接口

**描述:** 为所有实体创建Spring Data JPA Repository接口

**文件:**
- `backend/src/main/java/com/usermanagement/repository/UserRepository.java`
- `backend/src/main/java/com/usermanagement/repository/RoleRepository.java`
- `backend/src/main/java/com/usermanagement/repository/PermissionRepository.java`
- `backend/src/main/java/com/usermanagement/repository/UserRoleRepository.java`
- `backend/src/main/java/com/usermanagement/repository/RolePermissionRepository.java`
- `backend/src/main/java/com/usermanagement/repository/AuditLogRepository.java`

**依赖:** Task 1, Task 2 完成

**验收标准:**
- UserRepository: 继承 JpaRepository, 添加 findByEmail, existsByEmail, findByStatus 方法
- RoleRepository: 继承 JpaRepository, 添加 findByCode, findByStatus 方法
- PermissionRepository: 继承 JpaRepository, 添加 findByCode, findByType, findByParentId 方法
- UserRoleRepository: 继承 JpaRepository, 添加 findByUserId, findByRoleId, deleteByUserId 方法
- RolePermissionRepository: 继承 JpaRepository, 添加 findByRoleId, deleteByRoleId 方法
- AuditLogRepository: 继承 JpaRepository, 添加分页查询方法

---

## Verification

### 自动化验证

```bash
# 1. 编译项目
cd backend && ./mvnw clean compile

# 2. 运行单元测试
./mvnw test -Dtest="*RepositoryTest"

# 3. 验证Flyway迁移
./mvnw flyway:info
./mvnw flyway:migrate

# 4. 检查H2 Console (开发模式)
# 访问 http://localhost:8080/h2-console
```

### 手动验证清单

- [ ] 所有实体类编译通过
- [ ] Flyway迁移成功执行
- [ ] H2数据库表结构正确
- [ ] Repository接口可以正常使用
- [ ] 初始数据插入成功

---

## Success Criteria

1. **数据库表创建:** users, roles, permissions, user_roles, role_permissions, audit_log 表全部创建成功
2. **约束完整性:** 所有唯一约束、外键约束、CHECK约束正确配置
3. **索引优化:** 查询常用字段均有索引支持
4. **双数据库:** 开发和生产环境数据库切换正常
5. **代码质量:** Repository接口提供基础CRUD和常用查询方法

---

## must_haves

### truths
- 用户表 (users) 可以正常创建和查询
- 角色表 (roles) 支持唯一code约束
- 权限表 (permissions) 支持四级权限类型
- 审计日志表 (audit_log) 支持JSONB字段和分区
- Flyway迁移可以在PostgreSQL和H2上成功执行

### artifacts
- path: "backend/src/main/java/com/usermanagement/domain/User.java"
  provides: "用户JPA实体"
  min_lines: 80
- path: "backend/src/main/java/com/usermanagement/domain/Role.java"
  provides: "角色JPA实体"
  min_lines: 60
- path: "backend/src/main/java/com/usermanagement/domain/Permission.java"
  provides: "权限JPA实体"
  min_lines: 70
- path: "backend/src/main/resources/db/migration/V1__init.sql"
  provides: "Flyway初始迁移脚本"
  min_lines: 200

### key_links
- from: "domain/*Entity.java"
  to: "db/migration/V1__init.sql"
  via: "JPA映射与DDL对应"
- from: "Repository.java"
  to: "domain/*Entity.java"
  via: "Spring Data JPA继承"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 数据库方言差异 | 中 | 中 | 使用标准SQL，H2兼容模式 |
| 外键约束导致删除失败 | 中 | 低 | 实现软删除，级联策略明确 |
| 分区表不支持H2 | 中 | 中 | H2环境使用普通表，生产用分区 |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/01-SUMMARY.md`
