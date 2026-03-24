---
phase: 1
plan: 05
title: 审计日志框架
requirements_addressed: [AUDIT-01, AUDIT-02, AUDIT-04, AUDIT-06, SEC-08]
depends_on: [01, 02, 03]
wave: 4
autonomous: false
---

# Plan 1.5: 审计日志框架

## Objective

实现完整的审计日志框架，包括审计日志实体与表、AOP日志拦截器、审计日志查询API，以及前端审计日志页面。

**Purpose:** 审计日志是安全合规的核心要求，记录所有敏感操作，支持操作追溯和安全分析。

**Output:**
- 审计日志实体与表 (支持分区)
- AOP日志拦截器
- 审计日志查询 API
- 日志保留策略实现
- 前端审计日志页面

---

## Context

### 审计日志架构
```
操作发生 → AOP拦截器 → 日志收集 → 同步/异步保存
                                              ↓
                                    PostgreSQL (audit_log表)
                                              ↓
                                    按月分区 (audit_log_2026_03)
```

### 需记录的敏感操作
| 操作类型 | 说明 | 优先级 |
|----------|------|--------|
| USER_CREATE | 创建用户 | P0 |
| USER_UPDATE | 更新用户 | P0 |
| USER_DELETE | 删除用户 | P0 |
| USER_LOGIN | 用户登录 | P0 |
| USER_LOGOUT | 用户登出 | P0 |
| ROLE_ASSIGN | 分配角色 | P0 |
| PERMISSION_CHANGE | 权限变更 | P0 |
| PASSWORD_RESET | 密码重置 | P0 |
| SYSTEM_CONFIG | 系统配置变更 | P1 |

### 日志保留策略
- 在线保留: 90天 (热数据)
- 归档保留: 3年 (冷数据，Phase 3实现)
- 定期清理: 定时任务删除过期数据

---

## Tasks

### Task 1: 完善审计日志实体和 Repository

**描述:** 完善AuditLog实体，创建Repository接口和查询方法

**文件:**
- `backend/src/main/java/com/usermanagement/domain/AuditLog.java` (完善)
- `backend/src/main/java/com/usermanagement/repository/AuditLogRepository.java` (完善)
- `backend/src/main/java/com/usermanagement/repository/spec/AuditLogSpecification.java`
- `backend/src/main/java/com/usermanagement/service/dto/AuditLogQueryRequest.java`

**依赖:** Plan 01 完成

**验收标准:**
- AuditLog.java 实体完善:
  - `id`: UUID 主键
  - `userId`: 操作用户ID
  - `username`: 操作用户名 (冗余存储)
  - `operation`: 操作类型 (枚举)
  - `resourceType`: 资源类型 (USER/ROLE/PERMISSION等)
  - `resourceId`: 资源ID
  - `oldValue`: 操作前数据 (JSONB)
  - `newValue`: 操作后数据 (JSONB)
  - `description`: 操作描述
  - `clientIp`: 客户端IP
  - `userAgent`: 浏览器UA
  - `sessionId`: 会话ID
  - `success`: 是否成功
  - `errorMessage`: 错误信息
  - `executionTimeMs`: 执行耗时(毫秒)
  - `createdAt`: 操作时间

- AuditLogRepository.java 增强:
  - 继承JpaRepository
  - 自定义查询方法支持动态条件
  - 支持时间范围查询

- AuditLogSpecification.java:
  - 动态查询条件构建
  - 支持按用户、操作类型、资源类型、时间范围筛选

---

### Task 2: 实现审计日志服务

**描述:** 创建AuditLogService，实现日志记录和查询

**文件:**
- `backend/src/main/java/com/usermanagement/service/AuditLogService.java`
- `backend/src/main/java/com/usermanagement/service/AuditLogServiceImpl.java`
- `backend/src/main/java/com/usermanagement/service/dto/AuditLogDTO.java`

**依赖:** Task 1 完成

**验收标准:**
- AuditLogService.java:
  - `void log(AuditLogDTO log)`: 记录单条日志
  - `void logAsync(AuditLogDTO log)`: 异步记录日志
  - `Page<AuditLogDTO> queryLogs(AuditLogQueryRequest query)`: 查询日志
  - `AuditLogDTO getLogById(UUID id)`: 根据ID查询
  - `void cleanExpiredLogs(LocalDateTime before)`: 清理过期日志

- AuditLogServiceImpl.java 实现:
  - 同步保存到数据库
  - 支持批量保存 (优化性能)
  - 查询结果按时间倒序
  - 敏感数据脱敏处理

- 日志脱敏规则:
  - 密码字段: 替换为 `******`
  - 手机号: 显示前3后4位 (如 138****8000)
  - 邮箱: 显示前2位和域名 (如 zh***@example.com)

---

### Task 3: 实现 AOP 日志拦截器

**描述:** 创建AOP拦截器，自动拦截标记的方法并记录审计日志

**文件:**
- `backend/src/main/java/com/usermanagement/audit/AuditAspect.java` (完善)
- `backend/src/main/java/com/usermanagement/audit/Auditable.java` (注解)
- `backend/src/main/java/com/usermanagement/audit/AuditContext.java`

**依赖:** Task 2 完成

**验收标准:**
- Auditable.java 注解:
  - `operation()`: 操作类型 (必填)
  - `resourceType()`: 资源类型 (必填)
  - `resourceIdExpression()`: SpEL表达式提取资源ID
  - `description()`: 操作描述

- AuditAspect.java 完善:
  - `@Around` 拦截带@Auditable注解的方法
  - 方法执行前: 记录旧值 (如果是更新操作)
  - 方法执行后: 记录新值，保存审计日志
  - 异常时: 记录失败状态和错误信息
  - 收集上下文信息: 用户ID、IP、User-Agent、会话ID

- AuditContext.java:
  - 存储当前请求上下文
  - 提供获取当前用户、IP等方法
  - 使用ThreadLocal存储

**使用示例:**
```java
@RestController
public class UserController {

    @Auditable(
        operation = AuditOperation.USER_CREATE,
        resourceType = ResourceType.USER,
        description = "创建新用户"
    )
    @PostMapping("/api/v1/users")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody CreateUserRequest request) {
        // 自动记录审计日志
    }

    @Auditable(
        operation = AuditOperation.USER_UPDATE,
        resourceType = ResourceType.USER,
        resourceIdExpression = "#id",
        description = "更新用户信息"
    )
    @PutMapping("/api/v1/users/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        // 自动记录审计日志，包含旧值和新值
    }
}
```

---

### Task 4: 实现审计日志 REST API

**描述:** 创建AuditLogController，提供日志查询接口

**文件:**
- `backend/src/main/java/com/usermanagement/web/controller/AuditLogController.java`

**依赖:** Task 2, Task 3 完成

**验收标准:**
- AuditLogController.java:
  - `GET /api/v1/audit-logs`: 查询审计日志列表
    - 支持分页参数: page, size
    - 支持筛选参数: userId, operation, resourceType, resourceId, startTime, endTime
    - 支持排序: createdAt
  - `GET /api/v1/audit-logs/{id}`: 查询单条日志详情
  - `GET /api/v1/audit-logs/export`: 导出日志 (CSV格式)

- 权限控制:
  - 查询审计日志需要 `audit:read` 权限
  - 导出日志需要 `audit:export` 权限
  - 普通用户只能查看自己的操作日志
  - 管理员可以查看所有日志

- 响应示例:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "log-uuid",
        "userId": "user-uuid",
        "username": "admin",
        "operation": "USER_CREATE",
        "resourceType": "USER",
        "resourceId": "new-user-uuid",
        "oldValue": null,
        "newValue": "{\"email\":\"zhangsan@example.com\",\"name\":\"张三\"}",
        "description": "创建新用户",
        "clientIp": "192.168.1.1",
        "userAgent": "Mozilla/5.0...",
        "success": true,
        "executionTimeMs": 150,
        "createdAt": "2026-03-24T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1000
  }
}
```

---

### Task 5: 实现日志保留策略

**描述:** 实现定时任务，自动清理过期审计日志

**文件:**
- `backend/src/main/java/com/usermanagement/scheduler/AuditLogCleanupJob.java`
- `backend/src/main/java/com/usermanagement/config/SchedulerConfig.java`

**依赖:** Task 2 完成

**验收标准:**
- AuditLogCleanupJob.java:
  - 每天凌晨2点执行清理任务
  - 删除90天前的日志
  - 记录清理结果 (清理条数)
  - 支持手动触发

- 配置参数:
  - `audit.log.retention.days`: 保留天数 (默认90)
  - `audit.log.cleanup.enabled`: 是否启用清理 (默认true)

- 安全考虑:
  - 清理前备份到归档表 (可选)
  - 记录清理操作到审计日志
  - 仅清理软删除的数据

---

### Task 6: 创建前端审计日志页面

**描述:** 创建Next.js审计日志查询页面

**文件:**
- `frontend/src/app/audit-logs/page.tsx` (审计日志列表页)
- `frontend/src/components/audit-logs/AuditLogTable.tsx`
- `frontend/src/components/audit-logs/AuditLogFilter.tsx`
- `frontend/src/components/audit-logs/AuditLogDetail.tsx`
- `frontend/src/lib/api/audit-logs.ts`

**依赖:** Task 4 完成 (API已就绪)

**验收标准:**
- 审计日志列表页:
  - 表格展示日志数据 (时间、用户、操作、资源、结果)
  - 分页组件
  - 高级筛选: 时间范围、用户、操作类型、资源类型
  - 操作按钮: 查看详情、导出
- 日志详情弹窗:
  - 展示完整日志信息
  - 格式化显示JSON数据 (oldValue/newValue)
  - 显示地理位置 (根据IP)
- 导出功能:
  - 导出当前筛选结果为CSV
  - 异步导出大量数据

---

## Verification

### 自动化验证

```bash
# 1. 单元测试
./mvnw test -Dtest="AuditLogServiceTest,AuditAspectTest"

# 2. 集成测试
./mvnw test -Dtest="AuditLogIntegrationTest"

# 3. API测试
curl -X GET "http://localhost:8080/api/v1/audit-logs?page=0&size=20" \
  -H "Authorization: Bearer {token}"
```

### API 测试用例

```bash
# 查询审计日志
curl -X GET "http://localhost:8080/api/v1/audit-logs?operation=USER_CREATE&startTime=2026-03-01T00:00:00Z" \
  -H "Authorization: Bearer {token}"

# 查询单条日志
curl -X GET http://localhost:8080/api/v1/audit-logs/{logId} \
  -H "Authorization: Bearer {token}"

# 导出日志
curl -X GET "http://localhost:8080/api/v1/audit-logs/export?startTime=2026-03-01T00:00:00Z" \
  -H "Authorization: Bearer {token}" \
  --output audit_logs.csv
```

### 手动验证清单

- [ ] 创建用户操作被记录到审计日志
- [ ] 日志包含正确的操作前后数据
- [ ] 可以按条件筛选日志
- [ ] 可以分页查看日志
- [ ] 日志详情展示完整
- [ ] 导出功能正常
- [ ] 日志保留策略生效

---

## Success Criteria

1. **日志记录**: 所有敏感操作都被记录到审计日志
2. **日志完整性**: 日志包含操作前后数据、用户信息、时间、IP等
3. **日志查询**: 支持多维度查询和分页
4. **日志保留**: 过期日志自动清理
5. **防篡改**: 日志数据不可修改、不可删除 (仅标记)
6. **前端功能**: 审计日志页面可以正常使用

---

## must_haves

### truths
- 所有敏感操作 (用户CRUD、角色分配、权限变更) 都被记录
- 审计日志包含操作前后数据 (JSON格式)
- 可以按用户、操作类型、时间范围查询日志
- 日志保留90天，过期自动清理
- 日志数据不可修改，满足合规要求

### artifacts
- path: "backend/src/main/java/com/usermanagement/audit/AuditAspect.java"
  provides: "审计日志AOP拦截器"
  min_lines: 100
- path: "backend/src/main/java/com/usermanagement/service/AuditLogService.java"
  provides: "审计日志服务"
  min_lines: 30
- path: "backend/src/main/java/com/usermanagement/web/controller/AuditLogController.java"
  provides: "审计日志API控制器"
  min_lines: 60
- path: "backend/src/main/java/com/usermanagement/scheduler/AuditLogCleanupJob.java"
  provides: "日志清理定时任务"
  min_lines: 40
- path: "frontend/src/app/audit-logs/page.tsx"
  provides: "前端审计日志页面"
  min_lines: 80

### key_links
- from: "AuditAspect.java"
  to: "AuditLogService.java"
  via: "日志记录"
- from: "AuditLogController.java"
  to: "AuditLogService.java"
  via: "查询接口"
- from: "AuditLogCleanupJob.java"
  to: "AuditLogService.java"
  via: "清理任务"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 日志量过大 | 高 | 中 | 分区表设计，定期清理 |
| 性能影响 | 中 | 中 | 异步记录，批量保存 |
| 敏感信息泄露 | 中 | 高 | 日志脱敏处理 |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/05-SUMMARY.md`
