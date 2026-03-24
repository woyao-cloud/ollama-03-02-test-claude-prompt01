---
phase: 1
plan: 04
title: 角色权限模块
requirements_addressed: [ROLE-01, ROLE-02, ROLE-06, ROLE-07, PERM-01, PERM-02, PERM-05, PERM-06, PERM-07, AUDIT-01, PERF-04, SEC-07]
depends_on: [01, 02]
wave: 3
autonomous: false
---

# Plan 1.4: 角色权限模块

## Objective

实现角色管理和基础RBAC权限控制，包括Role CRUD API、菜单/操作权限管理、权限缓存(Redis)、角色分配功能，以及前端角色管理页面。

**Purpose:** RBAC权限模型是系统的核心安全机制，通过角色管理简化权限分配，支持菜单级和操作级权限控制。

**Output:**
- Role CRUD REST API
- Permission 管理 API
- 基础RBAC权限 (菜单 + 操作)
- 权限缓存 (Redis)
- 角色-权限分配
- 前端角色管理页面

---

## Context

### RBAC 四级权限模型 (Phase 1实现前两级)
```
Level 1: 菜单权限 (Menu Permission)
├── 控制导航菜单的可见性
├── 示例: user:menu:view, role:menu:view
└── 存储: permission.type = 'MENU'

Level 2: 操作权限 (Action Permission)
├── 控制按钮/功能的可操作性
├── 示例: user:create, user:update, user:delete, user:read
└── 存储: permission.type = 'ACTION'

Level 3-4: 字段权限/数据权限 (Phase 2实现)
```

### 权限代码规范
- 格式: `{resource}:{action}`
- 示例: `user:create`, `user:read`, `user:update`, `user:delete`
- 菜单权限: `{resource}:menu:view`

### 数据权限范围 (role.data_scope)
- `ALL`: 全部数据
- `DEPT`: 本部门及子部门数据
- `SELF`: 仅本人数据
- `CUSTOM`: 自定义 (Phase 2实现)

---

## Tasks

### Task 1: 实现角色管理服务

**描述:** 创建RoleService，实现角色CRUD和权限分配

**文件:**
- `backend/src/main/java/com/usermanagement/service/RoleService.java`
- `backend/src/main/java/com/usermanagement/service/RoleServiceImpl.java`
- `backend/src/main/java/com/usermanagement/service/dto/RoleDTO.java`
- `backend/src/main/java/com/usermanagement/service/dto/CreateRoleRequest.java`
- `backend/src/main/java/com/usermanagement/service/dto/UpdateRoleRequest.java`

**依赖:** Plan 01, Plan 02 完成

**验收标准:**
- RoleService.java 接口定义:
  - `RoleDTO createRole(CreateRoleRequest request)`: 创建角色
  - `RoleDTO updateRole(UUID id, UpdateRoleRequest request)`: 更新角色
  - `void deleteRole(UUID id)`: 删除角色
  - `RoleDTO getRoleById(UUID id)`: 根据ID查询
  - `List<RoleDTO> getAllRoles()`: 查询所有角色
  - `Page<RoleDTO> getRoles(RoleQueryRequest query)`: 分页查询
  - `void assignPermissions(UUID roleId, List<UUID> permissionIds)`: 分配权限
  - `List<PermissionDTO> getRolePermissions(UUID roleId)`: 获取角色权限
  - `void updateDataScope(UUID roleId, DataScope dataScope)`: 更新数据权限范围

- RoleServiceImpl.java 实现要求:
  - 创建角色时验证code唯一性
  - 删除角色前检查是否被用户引用
  - 权限变更时清除相关用户缓存
  - 记录角色变更审计日志

---

### Task 2: 实现权限管理服务

**描述:** 创建PermissionService，实现权限管理和树形结构

**文件:**
- `backend/src/main/java/com/usermanagement/service/PermissionService.java`
- `backend/src/main/java/com/usermanagement/service/PermissionServiceImpl.java`
- `backend/src/main/java/com/usermanagement/service/dto/PermissionDTO.java`
- `backend/src/main/java/com/usermanagement/service/dto/PermissionTreeDTO.java`

**依赖:** Task 1 完成

**验收标准:**
- PermissionService.java:
  - `PermissionDTO createPermission(CreatePermissionRequest request)`: 创建权限
  - `PermissionDTO updatePermission(UUID id, UpdatePermissionRequest request)`: 更新权限
  - `void deletePermission(UUID id)`: 删除权限
  - `PermissionDTO getPermissionById(UUID id)`: 根据ID查询
  - `List<PermissionDTO> getAllPermissions()`: 查询所有权限
  - `List<PermissionTreeDTO> getPermissionTree()`: 获取权限树
  - `List<PermissionDTO> getPermissionsByType(PermissionType type)`: 按类型查询
  - `List<PermissionDTO> getPermissionsByRoleId(UUID roleId)`: 获取角色权限

- 权限树构建:
  - 支持父子层级关系
  - 菜单权限可以包含子权限
  - 返回树形结构便于前端展示

**权限树结构示例:**
```java
public class PermissionTreeDTO {
    private UUID id;
    private String name;
    private String code;
    private PermissionType type;
    private List<PermissionTreeDTO> children;
}
```

---

### Task 3: 实现权限缓存服务

**描述:** 使用Redis缓存用户权限，提高权限校验性能

**文件:**
- `backend/src/main/java/com/usermanagement/service/PermissionCacheService.java`
- `backend/src/main/java/com/usermanagement/service/PermissionCacheServiceImpl.java`
- `backend/src/main/java/com/usermanagement/config/CacheConfig.java`

**依赖:** Task 1, Task 2 完成

**验收标准:**
- PermissionCacheService.java:
  - `void cacheUserPermissions(UUID userId, Set<String> permissions)`: 缓存用户权限
  - `Set<String> getUserPermissions(UUID userId)`: 获取缓存的用户权限
  - `void evictUserPermissions(UUID userId)`: 清除用户权限缓存
  - `void evictAllPermissions()`: 清除所有权限缓存
  - `boolean hasPermission(UUID userId, String permissionCode)`: 检查用户是否有权限

- Redis Key设计:
  - `user:permissions:{userId}` → Set<PermissionCode>
  - `user:roles:{userId}` → Set<RoleId>
  - `permission:all` → List<PermissionDTO> (所有权限缓存)

- 缓存策略:
  - TTL: 15分钟
  - 角色/权限变更时清除相关缓存
  - 使用Cache Aside模式

---

### Task 4: 实现角色权限 REST API

**描述:** 创建RoleController和PermissionController，暴露RESTful API

**文件:**
- `backend/src/main/java/com/usermanagement/web/controller/RoleController.java`
- `backend/src/main/java/com/usermanagement/web/controller/PermissionController.java`
- `backend/src/main/java/com/usermanagement/web/dto/AssignPermissionsRequest.java`

**依赖:** Task 1, Task 2, Task 3 完成

**验收标准:**
- RoleController.java:
  - `GET /api/v1/roles`: 查询角色列表
  - `GET /api/v1/roles/{id}`: 查询角色详情
  - `POST /api/v1/roles`: 创建角色
  - `PUT /api/v1/roles/{id}`: 更新角色
  - `DELETE /api/v1/roles/{id}`: 删除角色
  - `GET /api/v1/roles/{id}/permissions`: 获取角色权限
  - `POST /api/v1/roles/{id}/permissions`: 分配权限给角色
  - `PATCH /api/v1/roles/{id}/data-scope`: 更新数据权限范围

- PermissionController.java:
  - `GET /api/v1/permissions`: 查询权限列表
  - `GET /api/v1/permissions/tree`: 获取权限树
  - `GET /api/v1/permissions/{id}`: 查询权限详情
  - `POST /api/v1/permissions`: 创建权限
  - `PUT /api/v1/permissions/{id}`: 更新权限
  - `DELETE /api/v1/permissions/{id}`: 删除权限
  - `GET /api/v1/permissions/types/{type}`: 按类型查询权限

- 权限控制:
  - 使用`@PreAuthorize`注解
  - 角色管理需要 `role:*` 权限
  - 权限管理需要 `permission:*` 权限

---

### Task 5: 实现权限校验组件

**描述:** 创建权限校验工具类和注解，支持方法级权限控制

**文件:**
- `backend/src/main/java/com/usermanagement/security/PermissionChecker.java`
- `backend/src/main/java/com/usermanagement/security/annotation/RequirePermission.java`
- `backend/src/main/java/com/usermanagement/security/aspect/PermissionAspect.java`

**依赖:** Task 3 完成

**验收标准:**
- RequirePermission.java:
  - 自定义注解 `@RequirePermission(String value)`
  - 支持标注在方法上
  - 支持SpEL表达式

- PermissionChecker.java:
  - `boolean hasPermission(String permissionCode)`: 检查当前用户权限
  - `boolean hasAnyPermission(String... permissions)`: 检查是否有任一权限
  - `boolean hasAllPermissions(String... permissions)`: 检查是否有所有权限
  - `boolean hasRole(String roleCode)`: 检查当前用户角色

- PermissionAspect.java:
  - 拦截`@RequirePermission`注解
  - 调用PermissionChecker验证权限
  - 无权限时抛出AccessDeniedException

**使用示例:**
```java
@RestController
public class UserController {

    @RequirePermission("user:create")
    @PostMapping("/api/v1/users")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody CreateUserRequest request) {
        // 只有拥有 user:create 权限的用户才能访问
    }

    @RequirePermission("user:read")
    @GetMapping("/api/v1/users")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getUsers() {
        // 只有拥有 user:read 权限的用户才能访问
    }
}
```

---

### Task 6: 创建前端角色权限页面

**描述:** 创建Next.js角色管理和权限管理页面

**文件:**
- `frontend/src/app/roles/page.tsx` (角色列表页)
- `frontend/src/app/roles/create/page.tsx` (创建角色页)
- `frontend/src/app/roles/[id]/edit/page.tsx` (编辑角色页)
- `frontend/src/app/permissions/page.tsx` (权限列表页)
- `frontend/src/components/roles/RoleTable.tsx`
- `frontend/src/components/roles/RoleForm.tsx`
- `frontend/src/components/roles/PermissionSelector.tsx` (权限树选择器)
- `frontend/src/components/permissions/PermissionTree.tsx`
- `frontend/src/lib/api/roles.ts`
- `frontend/src/lib/api/permissions.ts`

**依赖:** Task 4 完成 (API已就绪)

**验收标准:**
- 角色列表页:
  - 表格展示角色数据 (名称、编码、数据权限范围、权限数量)
  - 分页组件
  - 操作按钮: 编辑、删除、分配权限
- 角色表单:
  - 字段: 角色名称、角色编码、描述、数据权限范围
  - 权限选择器 (树形结构)
- 权限管理页:
  - 树形展示所有权限
  - 支持展开/折叠
  - 创建/编辑权限
- 权限选择器组件:
  - 树形多选
  - 按类型分组显示
  - 已选项高亮

---

## Verification

### 自动化验证

```bash
# 1. 单元测试
./mvnw test -Dtest="RoleServiceTest,PermissionServiceTest,PermissionCacheTest"

# 2. 集成测试
./mvnw test -Dtest="RolePermissionIntegrationTest"

# 3. API测试
curl -X GET http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer {token}"
```

### API 测试用例

```bash
# 创建角色
curl -X POST http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "部门管理员",
    "code": "ROLE_DEPT_MANAGER",
    "description": "管理部门成员",
    "dataScope": "DEPT"
  }'

# 分配权限给角色
curl -X POST http://localhost:8080/api/v1/roles/{roleId}/permissions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionIds": ["perm-uuid-1", "perm-uuid-2"]
  }'

# 获取权限树
curl -X GET http://localhost:8080/api/v1/permissions/tree \
  -H "Authorization: Bearer {token}"
```

### 手动验证清单

- [ ] 可以创建新角色
- [ ] 角色code必须唯一
- [ ] 可以给角色分配权限
- [ ] 权限变更后缓存更新
- [ ] 权限校验注解生效
- [ ] 前端角色页面功能完整
- [ ] 前端权限树展示正确

---

## Success Criteria

1. **角色CRUD**: 角色的增删改查功能全部可用
2. **权限管理**: 权限可以创建、分配、查询
3. **权限缓存**: Redis权限缓存正常工作
4. **权限校验**: 方法级权限控制生效
5. **基础RBAC**: 菜单权限和操作权限控制有效
6. **前端功能**: 角色和权限管理页面可以正常使用

---

## must_haves

### truths
- 管理员可以创建、查询、更新、删除角色
- 角色代码必须全局唯一
- 可以给角色分配多个权限
- 支持菜单权限和操作权限
- 权限信息缓存在Redis中

### artifacts
- path: "backend/src/main/java/com/usermanagement/service/RoleService.java"
  provides: "角色服务接口"
  min_lines: 40
- path: "backend/src/main/java/com/usermanagement/service/PermissionCacheService.java"
  provides: "权限缓存服务"
  min_lines: 30
- path: "backend/src/main/java/com/usermanagement/security/PermissionChecker.java"
  provides: "权限校验组件"
  min_lines: 50
- path: "backend/src/main/java/com/usermanagement/web/controller/RoleController.java"
  provides: "角色API控制器"
  min_lines: 80
- path: "frontend/src/app/roles/page.tsx"
  provides: "前端角色管理页面"
  min_lines: 80

### key_links
- from: "RoleController.java"
  to: "RoleService.java"
  via: "依赖注入"
- from: "PermissionService.java"
  to: "PermissionCacheService.java"
  via: "缓存操作"
- from: "PermissionAspect.java"
  to: "PermissionChecker.java"
  via: "权限验证"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 权限缓存不一致 | 中 | 高 | 变更时清除缓存，设置合理TTL |
| 角色循环依赖 | 低 | 中 | 数据库外键约束，应用层检查 |
| 权限树深度过大 | 低 | 低 | 限制权限层级深度 |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/04-SUMMARY.md`
