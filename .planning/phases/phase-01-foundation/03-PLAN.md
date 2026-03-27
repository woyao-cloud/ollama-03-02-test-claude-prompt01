---
phase: 1
plan: 03
title: 用户管理模块
requirements_addressed: [USER-01, USER-02, USER-03, USER-04, USER-08, ROLE-06, ROLE-07, AUDIT-01, PERF-01, PERF-02, PERF-05, PERF-06, SEC-03, SEC-04, SEC-07]
depends_on: [01, 02]
wave: 3
autonomous: false
---

# Plan 1.3: 用户管理模块

## Objective

实现完整的用户管理功能，包括用户CRUD API、用户查询与分页、用户状态管理、用户角色分配，以及前端用户管理页面。

**Purpose:** 用户管理是系统的核心功能，为管理员提供完整的用户生命周期管理能力。

**Output:**
- User CRUD REST API
- 用户查询与分页 (支持动态条件)
- 用户状态管理 (激活/禁用/锁定)
- 用户角色分配
- 前端用户管理页面 (列表/创建/编辑)

---

## Context

### 用户状态流转
```
PENDING (待激活) ──► ACTIVE (正常)
    │                    │
    │                    ▼
    │               INACTIVE (禁用)
    │                    │
    │                    ▼
    └─────────────► LOCKED (锁定)
                           │
                           ▼ (30分钟后)
                        ACTIVE (自动解锁)
```

### API 设计规范
```
GET    /api/v1/users          # 查询用户列表 (分页)
GET    /api/v1/users/{id}     # 查询单个用户
POST   /api/v1/users          # 创建用户
PUT    /api/v1/users/{id}     # 更新用户
PATCH  /api/v1/users/{id}/status  # 更新用户状态
DELETE /api/v1/users/{id}     # 删除用户 (软删除)
POST   /api/v1/users/{id}/roles    # 分配角色
GET    /api/v1/users/{id}/profile  # 获取个人资料
PUT    /api/v1/users/{id}/profile  # 更新个人资料
```

### 性能要求
- 用户列表查询 < 200ms
- 支持大数据量分页 (游标分页)
- 数据库查询使用索引优化

---

## Tasks

### Task 1: 实现用户管理服务

**描述:** 创建UserService，实现用户CRUD业务逻辑

**文件:**
- `backend/src/main/java/com/usermanagement/service/UserService.java`
- `backend/src/main/java/com/usermanagement/service/UserServiceImpl.java`
- `backend/src/main/java/com/usermanagement/service/dto/UserDTO.java`
- `backend/src/main/java/com/usermanagement/service/dto/CreateUserRequest.java`
- `backend/src/main/java/com/usermanagement/service/dto/UpdateUserRequest.java`

**依赖:** Plan 01, Plan 02 完成

**验收标准:**
- UserService.java 接口定义:
  - `UserDTO createUser(CreateUserRequest request)`: 创建用户
  - `UserDTO updateUser(UUID id, UpdateUserRequest request)`: 更新用户
  - `void deleteUser(UUID id)`: 删除用户 (软删除)
  - `UserDTO getUserById(UUID id)`: 根据ID查询
  - `Page<UserDTO> getUsers(UserQueryRequest query)`: 分页查询
  - `void updateStatus(UUID id, UserStatus status)`: 更新状态
  - `void assignRoles(UUID id, List<UUID> roleIds)`: 分配角色
  - `UserDTO getProfile(UUID id)`: 获取个人资料
  - `UserDTO updateProfile(UUID id, UpdateProfileRequest request)`: 更新个人资料

- UserServiceImpl.java 实现要求:
  - 创建用户时验证邮箱唯一性
  - 密码使用BCrypt加密 (strength=12)
  - 更新时记录审计日志 (AOP拦截)
  - 删除用户时注销其所有会话
  - 实现乐观锁 (@Version字段)

- DTO类要求:
  - 使用Bean Validation注解
  - 敏感字段脱敏 (密码不返回)
  - 使用MapStruct或手动转换

---

### Task 2: 实现用户查询与分页

**描述:** 实现动态条件查询和高效分页

**文件:**
- `backend/src/main/java/com/usermanagement/service/dto/UserQueryRequest.java`
- `backend/src/main/java/com/usermanagement/repository/UserRepository.java` (增强)
- `backend/src/main/java/com/usermanagement/repository/spec/UserSpecification.java`

**依赖:** Task 1 完成

**验收标准:**
- UserQueryRequest.java:
  - 支持查询条件: email, firstName, lastName, phone, status, departmentId
  - 支持排序: createdAt, updatedAt, email, lastName
  - 支持分页: page, size
- UserRepository.java 增强:
  - 使用JPA Specification实现动态查询
  - 添加自定义查询方法支持模糊搜索
- UserSpecification.java:
  - 使用Criteria API构建动态查询
  - 支持多条件组合 (AND/OR)
  - 软删除过滤 (deleted_at IS NULL)

**查询示例:**
```java
public static Specification<User> withQuery(UserQueryRequest query) {
    return (root, criteriaQuery, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        // 软删除过滤
        predicates.add(cb.isNull(root.get("deletedAt")));

        // 动态条件
        if (query.getEmail() != null) {
            predicates.add(cb.like(root.get("email"), "%" + query.getEmail() + "%"));
        }
        if (query.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), query.getStatus()));
        }
        // ... 其他条件

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

---

### Task 3: 实现用户管理 REST API

**描述:** 创建UserController，暴露RESTful API

**文件:**
- `backend/src/main/java/com/usermanagement/web/controller/UserController.java`
- `backend/src/main/java/com/usermanagement/web/dto/ApiResponse.java`
- `backend/src/main/java/com/usermanagement/web/dto/PageResponse.java`

**依赖:** Task 1, Task 2 完成

**验收标准:**
- UserController.java:
  - `@RestController` + `@RequestMapping("/api/v1/users")`
  - `@PreAuthorize` 注解控制权限
  - 统一的响应格式封装

- API端点实现:
  - `GET /api/v1/users`: 查询用户列表，返回分页数据
  - `GET /api/v1/users/{id}`: 查询单个用户详情
  - `POST /api/v1/users`: 创建用户，返回201
  - `PUT /api/v1/users/{id}`: 更新用户，返回200
  - `PATCH /api/v1/users/{id}/status`: 更新状态，返回200
  - `DELETE /api/v1/users/{id}`: 删除用户，返回204
  - `POST /api/v1/users/{id}/roles`: 分配角色，返回200
  - `GET /api/v1/users/{id}/profile`: 获取个人资料
  - `PUT /api/v1/users/{id}/profile`: 更新个人资料

- 响应格式:
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-03-24T10:00:00Z"
}
```

- 分页响应:
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### Task 4: 实现角色分配功能

**描述:** 实现用户角色分配和查询

**文件:**
- `backend/src/main/java/com/usermanagement/service/UserRoleService.java`
- `backend/src/main/java/com/usermanagement/service/UserRoleServiceImpl.java`
- `backend/src/main/java/com/usermanagement/web/dto/AssignRolesRequest.java`

**依赖:** Task 1 完成

**验收标准:**
- UserRoleService.java:
  - `void assignRoles(UUID userId, List<UUID> roleIds)`: 分配角色 (覆盖式)
  - `void addRole(UUID userId, UUID roleId)`: 添加单个角色
  - `void removeRole(UUID userId, UUID roleId)`: 移除角色
  - `List<RoleDTO> getUserRoles(UUID userId)`: 获取用户角色列表
  - `List<String> getUserPermissions(UUID userId)`: 获取用户权限代码列表
- 角色变更时:
  - 记录审计日志 (ROLE_ASSIGN)
  - 清除用户权限缓存 (Redis)
  - 可选: 强制用户重新登录

---

### Task 5: 配置审计日志 AOP 拦截

**描述:** 配置AOP拦截器，自动记录用户相关操作的审计日志

**文件:**
- `backend/src/main/java/com/usermanagement/audit/AuditAspect.java`
- `backend/src/main/java/com/usermanagement/audit/AuditAnnotation.java`
- `backend/src/main/java/com/usermanagement/service/AuditLogService.java`

**依赖:** Plan 01 (AuditLog实体), Task 3 完成

**验收标准:**
- AuditAnnotation.java:
  - 自定义注解 `@Audit(operation, resourceType)`
  - 支持标注在Controller方法上
- AuditAspect.java:
  - 拦截带@Audit注解的方法
  - 收集操作信息: 用户ID、IP、User-Agent、操作类型、资源类型
  - 记录操作前后数据变化 (JSON格式)
  - 异步保存审计日志
- 需要记录的操作:
  - USER_CREATE, USER_UPDATE, USER_DELETE
  - USER_STATUS_CHANGE
  - ROLE_ASSIGN

**AOP示例:**
```java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(auditAnnotation)")
    public Object audit(ProceedingJoinPoint point, Audit auditAnnotation) throws Throwable {
        // 记录操作前数据
        Object result = point.proceed();
        // 记录操作后数据，保存审计日志
        return result;
    }
}
```

---

### Task 6: 创建前端用户管理页面

**描述:** 创建Next.js用户管理页面，包括列表、创建、编辑功能

**文件:**
- `frontend/src/app/users/page.tsx` (用户列表页)
- `frontend/src/app/users/create/page.tsx` (创建用户页)
- `frontend/src/app/users/[id]/edit/page.tsx` (编辑用户页)
- `frontend/src/components/users/UserTable.tsx`
- `frontend/src/components/users/UserForm.tsx`
- `frontend/src/components/users/UserFilter.tsx`
- `frontend/src/lib/api/users.ts` (用户API客户端)

**依赖:** Task 3 完成 (API已就绪)

**验收标准:**
- 用户列表页:
  - 表格展示用户数据 (姓名、邮箱、部门、状态、角色)
  - 分页组件
  - 搜索和筛选功能
  - 操作按钮: 编辑、删除、分配角色
- 用户表单:
  - 创建/编辑共用组件
  - 字段: 姓名、邮箱、手机号、部门选择、角色多选
  - 表单验证 (使用Zod + React Hook Form)
- API客户端:
  - 封装axios调用
  - 错误处理
  - Token自动附加

---

## Verification

### 自动化验证

```bash
# 1. 单元测试
./mvnw test -Dtest="UserServiceTest,UserControllerTest"

# 2. 集成测试
./mvnw test -Dtest="UserIntegrationTest"

# 3. API测试
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {token}"
```

### API 测试用例

```bash
# 创建用户
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "张",
    "lastName": "三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "departmentId": "dept-uuid",
    "roleIds": ["role-uuid-1"]
  }'

# 查询用户列表
curl "http://localhost:8080/api/v1/users?page=0&size=20&status=ACTIVE" \
  -H "Authorization: Bearer {token}"

# 更新用户状态
curl -X PATCH http://localhost:8080/api/v1/users/{id}/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

### 手动验证清单

- [ ] 可以创建新用户
- [ ] 邮箱重复时返回错误
- [ ] 可以分页查询用户列表
- [ ] 可以更新用户信息
- [ ] 可以软删除用户
- [ ] 可以分配角色给用户
- [ ] 操作记录审计日志
- [ ] 前端页面功能完整

---

## Success Criteria

1. **CRUD完整**: 用户的增删改查功能全部可用
2. **分页查询**: 支持动态条件查询和分页
3. **状态管理**: 用户状态流转正常 (激活/禁用/锁定)
4. **角色分配**: 可以为用户分配多个角色
5. **审计记录**: 用户操作被正确记录到审计日志
6. **前端功能**: 用户管理页面可以正常使用

---

## must_haves

### truths
- 管理员可以创建、查询、更新、删除用户
- 用户邮箱必须全局唯一
- 用户状态支持 ACTIVE/INACTIVE/PENDING/LOCKED
- 可以为用户分配多个角色
- 用户删除为软删除，保留审计数据

### artifacts
- path: "backend/src/main/java/com/usermanagement/service/UserService.java"
  provides: "用户服务接口"
  min_lines: 40
- path: "backend/src/main/java/com/usermanagement/web/controller/UserController.java"
  provides: "用户API控制器"
  min_lines: 100
- path: "backend/src/main/java/com/usermanagement/audit/AuditAspect.java"
  provides: "审计日志AOP拦截器"
  min_lines: 80
- path: "frontend/src/app/users/page.tsx"
  provides: "前端用户列表页面"
  min_lines: 100

### key_links
- from: "UserController.java"
  to: "UserService.java"
  via: "依赖注入"
- from: "UserService.java"
  to: "UserRepository.java"
  via: "数据访问"
- from: "AuditAspect.java"
  to: "AuditLogService.java"
  via: "审计日志记录"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 邮箱重复冲突 | 中 | 中 | 数据库唯一约束，应用层双重检查 |
| 分页性能问题 | 中 | 中 | 使用索引，限制最大页大小 |
| 并发更新冲突 | 低 | 中 | 乐观锁机制 (@Version) |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/03-SUMMARY.md`
