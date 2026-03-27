# Phase 1 验证策略

## 验证范围

本策略定义Phase 1 (Foundation) 各Plan的验证方法、验收标准和质量门禁。

---

## 1. Plan 01: 数据库设计与 Flyway 迁移

### 1.1 验证目标
- JPA实体类正确映射数据库表
- Flyway迁移脚本可成功执行
- 双数据库配置(PostgreSQL/H2)正常工作
- Repository接口提供完整CRUD功能

### 1.2 验证方法

#### 自动化测试
```bash
# 编译测试
./mvnw clean compile

# 单元测试 - Repository
./mvnw test -Dtest="*RepositoryTest"

# Flyway迁移测试
./mvnw flyway:migrate -Dspring.profiles.active=dev
./mvnw flyway:info

# H2数据库测试
./mvnw test -Dspring.profiles.active=test
```

#### 手动验证
```sql
-- 验证表创建
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- 验证约束
SELECT constraint_name, table_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'public';

-- 验证索引
SELECT indexname, tablename FROM pg_indexes
WHERE schemaname = 'public';

-- 验证初始数据
SELECT * FROM roles;
SELECT * FROM permissions;
```

### 1.3 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| 表数量 | 6个表创建成功 | SQL查询 |
| 约束 | 所有外键、唯一约束生效 | SQL查询 |
| 索引 | 查询字段均有索引 | SQL查询 |
| 迁移 | Flyway迁移无错误 | 命令执行 |
| 实体映射 | JPA实体可正常CRUD | 单元测试 |

---

## 2. Plan 02: JWT 认证与安全框架

### 2.1 验证目标
- JWT Token生成与验证正常
- 登录/登出API功能完整
- Redis会话管理正常工作
- 登录失败锁定机制有效
- 密码策略正确实施

### 2.2 验证方法

#### 自动化测试
```bash
# JWT测试
./mvnw test -Dtest="JwtTokenProviderTest,JwtTokenValidatorTest"

# 认证服务测试
./mvnw test -Dtest="AuthServiceTest"

# 安全集成测试
./mvnw test -Dtest="SecurityIntegrationTest"
```

#### API测试
```bash
# 登录测试
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}'

# 验证响应包含 accessToken 和 refreshToken

# 访问受保护资源
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {accessToken}"

# 刷新Token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"{refreshToken}"}'

# 登出
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer {accessToken}"
```

#### 安全测试
```bash
# 登录失败测试 (重复5次)
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"wrongpassword"}'
done

# 第6次应该返回账户锁定
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}'
```

### 2.3 性能测试
```bash
# 使用Apache Bench测试登录接口
ab -n 1000 -c 100 -T "application/json" \
  -p login.json \
  http://localhost:8080/api/v1/auth/login

# 目标: P95 < 100ms
```

### 2.4 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| Token生成 | JWT正确生成，包含预期claims | 解码验证 |
| Token验证 | RSA256签名验证通过 | 单元测试 |
| 登录响应 | < 100ms (P95) | 性能测试 |
| 会话管理 | Redis中会话数据正确 | Redis CLI |
| 失败锁定 | 5次失败后锁定30分钟 | API测试 |
| Token刷新 | Refresh Token可获取新Token | API测试 |

---

## 3. Plan 03: 用户管理模块

### 3.1 验证目标
- 用户CRUD API功能完整
- 分页查询性能达标
- 用户状态管理正常
- 角色分配功能正确
- 审计日志记录完整

### 3.2 验证方法

#### 自动化测试
```bash
# 服务层测试
./mvnw test -Dtest="UserServiceTest"

# 控制器测试
./mvnw test -Dtest="UserControllerTest"

# 集成测试
./mvnw test -Dtest="UserIntegrationTest"
```

#### API测试
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
    "roleIds": ["role-uuid"]
  }'

# 查询用户列表
curl "http://localhost:8080/api/v1/users?page=0&size=20&status=ACTIVE" \
  -H "Authorization: Bearer {token}"

# 更新用户状态
curl -X PATCH http://localhost:8080/api/v1/users/{id}/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'

# 分配角色
curl -X POST http://localhost:8080/api/v1/users/{id}/roles \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"roleIds": ["role-uuid-1", "role-uuid-2"]}'
```

#### 前端验证
- 用户列表页面正常显示
- 创建/编辑表单验证工作
- 分页组件正常
- 角色分配弹窗可用

### 3.3 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| CRUD功能 | 增删改查API全部可用 | API测试 |
| 分页查询 | < 200ms | 性能测试 |
| 邮箱唯一性 | 重复邮箱返回400错误 | API测试 |
| 状态流转 | 状态变更正常 | API测试 |
| 角色分配 | 用户可分配多个角色 | API测试 |
| 审计记录 | 操作记录审计日志 | 数据库查询 |

---

## 4. Plan 04: 角色权限模块

### 4.1 验证目标
- 角色CRUD功能完整
- 权限树形结构正确
- 权限缓存正常工作
- 方法级权限控制生效

### 4.2 验证方法

#### 自动化测试
```bash
# 角色服务测试
./mvnw test -Dtest="RoleServiceTest,PermissionServiceTest"

# 缓存测试
./mvnw test -Dtest="PermissionCacheTest"

# 权限校验测试
./mvnw test -Dtest="PermissionCheckerTest"
```

#### API测试
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

# 分配权限
curl -X POST http://localhost:8080/api/v1/roles/{roleId}/permissions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"permissionIds": ["perm-uuid-1"]}'

# 获取权限树
curl http://localhost:8080/api/v1/permissions/tree \
  -H "Authorization: Bearer {token}"
```

#### 权限校验测试
```bash
# 使用无权限用户访问需要权限的接口
# 应该返回 403 Forbidden
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {noPermissionToken}"
```

### 4.3 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| 角色CRUD | 角色管理API全部可用 | API测试 |
| 权限分配 | 可以给角色分配权限 | API测试 |
| 权限树 | 树形结构返回正确 | API测试 |
| 权限缓存 | Redis缓存权限数据 | Redis CLI |
| 权限校验 | @RequirePermission生效 | API测试 |
| 缓存更新 | 权限变更后缓存刷新 | 集成测试 |

---

## 5. Plan 05: 审计日志框架

### 5.1 验证目标
- 敏感操作被记录到审计日志
- 日志包含完整操作信息
- 日志查询功能正常
- 日志保留策略生效

### 5.2 验证方法

#### 自动化测试
```bash
# 审计服务测试
./mvnw test -Dtest="AuditLogServiceTest,AuditAspectTest"

# 日志清理测试
./mvnw test -Dtest="AuditLogCleanupJobTest"
```

#### API测试
```bash
# 查询审计日志
curl "http://localhost:8080/api/v1/audit-logs?page=0&size=20" \
  -H "Authorization: Bearer {token}"

# 按条件筛选
curl "http://localhost:8080/api/v1/audit-logs?operation=USER_CREATE&startTime=2026-03-01T00:00:00Z" \
  -H "Authorization: Bearer {token}"

# 导出日志
curl "http://localhost:8080/api/v1/audit-logs/export?startTime=2026-03-01T00:00:00Z" \
  -H "Authorization: Bearer {token}" \
  --output audit_logs.csv
```

#### 数据库验证
```sql
-- 验证审计日志记录
SELECT operation, resource_type, username, success, created_at
FROM audit_log
ORDER BY created_at DESC
LIMIT 10;

-- 验证JSONB字段
SELECT old_value, new_value
FROM audit_log
WHERE operation = 'USER_UPDATE'
LIMIT 1;
```

### 5.3 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| 日志记录 | 敏感操作被记录 | 数据库查询 |
| 日志内容 | 包含用户、操作、时间、IP等 | 数据库查询 |
| 数据变更 | old_value/new_value记录完整 | 数据库查询 |
| 日志查询 | 支持多维度筛选 | API测试 |
| 日志导出 | CSV导出功能正常 | API测试 |
| 日志清理 | 过期日志自动清理 | 定时任务测试 |

---

## 6. Plan 06: 前端基础架构

### 6.1 验证目标
- Next.js项目正常运行
- 登录页面功能完整
- 状态管理正常工作
- 路由保护生效
- 主布局组件显示正确

### 6.2 验证方法

#### 自动化测试
```bash
# 类型检查
cd frontend && npm run type-check

# 构建测试
npm run build

# Lint检查
npm run lint
```

#### 手动测试
```bash
# 启动开发服务器
npm run dev

# 访问 http://localhost:3000
```

#### 功能测试清单
- [ ] 登录页面正常显示
- [ ] 表单验证工作正常 (邮箱格式、密码长度)
- [ ] 登录成功跳转到首页
- [ ] 登录失败显示错误提示
- [ ] Token存储到localStorage
- [ ] 侧边栏导航正常显示
- [ ] 未登录时重定向到登录页
- [ ] 刷新页面保持登录状态

### 6.3 验收标准
| 检查项 | 标准 | 验证方式 |
|--------|------|----------|
| 项目启动 | 无错误启动 | 手动测试 |
| 登录功能 | 可正常登录 | 手动测试 |
| 表单验证 | 验证规则生效 | 手动测试 |
| 状态管理 | Zustand状态正确 | 开发者工具 |
| Token管理 | Token存储和刷新正常 | 手动测试 |
| 路由保护 | 未登录重定向 | 手动测试 |
| 布局组件 | 侧边栏、头部显示正确 | 手动测试 |

---

## 7. 端到端验证

### 7.1 完整流程测试

```bash
# 1. 用户登录 -> 创建用户 -> 分配角色 -> 查询审计日志

# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}'

# 创建用户
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"张","lastName":"三","email":"zhangsan@test.com","roleIds":["role-uuid"]}'

# 查询审计日志
curl "http://localhost:8080/api/v1/audit-logs?operation=USER_CREATE" \
  -H "Authorization: Bearer {token}"

# 验证日志中包含创建用户操作
```

### 7.2 性能验证

```bash
# 登录接口压测
ab -n 10000 -c 100 -T "application/json" \
  -p login.json \
  http://localhost:8080/api/v1/auth/login

# 用户列表查询压测
ab -n 5000 -c 50 \
  -H "Authorization: Bearer {token}" \
  http://localhost:8080/api/v1/users?page=0&size=20
```

### 7.3 覆盖率验证

```bash
# 生成测试报告
./mvnw jacoco:report

# 检查覆盖率
# 目标: 后端 >= 85%, 前端 >= 80%
```

---

## 8. 质量门禁

### 8.1 Phase 1 完成标准

| 门禁项 | 标准 | 检查方式 |
|--------|------|----------|
| 单元测试覆盖率 | 后端 >= 85%, 前端 >= 80% | JaCoCo/Jest报告 |
| 登录接口性能 | P95 < 100ms | 压测报告 |
| API功能 | 所有API测试通过 | Postman/Newman |
| 安全审计 | 无高危漏洞 | OWASP DC扫描 |
| 代码质量 | SonarQube A级 | SonarQube报告 |
| 功能完整性 | 用户/角色/权限/审计功能可用 | 功能测试 |

### 8.2 验收签字

| 角色 | 签字 | 日期 |
|------|------|------|
| 技术负责人 | | |
| 测试工程师 | | |
| 产品经理 | | |
