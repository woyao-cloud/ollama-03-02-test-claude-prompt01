# ROADMAP: 全栈用户角色权限管理系统

## Current Status

| 属性 | 值 |
|------|-----|
| **Current Phase** | 未开始 |
| **v1 Requirements** | 67 |
| **Phases** | 3 |
| **Target Completion** | 2026-04-30 |

---

## Phases

- [ ] **Phase 1: Foundation** - 数据库、JWT认证、用户、角色、基础RBAC、审计
- [ ] **Phase 2: Department & Advanced** - 部门树、字段/数据权限、OAuth2、批量操作
- [ ] **Phase 3: Production Ready** - Kafka、2FA、性能优化、监控、K8s部署

---

## Phase Details

### Phase 1: Foundation

**Goal**: 交付最小可用产品，支持基础用户权限管理

**Depends on**: Nothing (first phase)

**Requirements**:
- USER-01, USER-02, USER-03, USER-04, USER-08
- ROLE-01, ROLE-02, ROLE-06, ROLE-07
- PERM-01, PERM-02, PERM-05, PERM-06, PERM-07
- AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05
- AUDIT-01, AUDIT-02, AUDIT-04, AUDIT-06
- CONFIG-01, CONFIG-02, CONFIG-04
- PERF-01, PERF-02, PERF-03, PERF-04, PERF-05, PERF-06
- SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, SEC-07, SEC-08

**Success Criteria** (what must be TRUE):
1. 管理员可以创建、查询、更新、删除用户
2. 用户可以使用邮箱/密码登录，获得 JWT Token
3. 登录响应时间 < 100ms (P95)
4. 可以创建角色并分配给用户
5. 支持菜单级和操作级权限控制
6. 敏感操作记录审计日志
7. 单元测试覆盖率 >= 85%

**Plans**:
- Plan 1.1: 数据库设计与Flyway迁移
- Plan 1.2: JWT认证与安全框架
- Plan 1.3: 用户管理模块
- Plan 1.4: 角色权限模块
- Plan 1.5: 审计日志框架
- Plan 1.6: 前端基础架构

**Target Date**: 2026-04-15

---

### Phase 2: Department & Advanced

**Goal**: 完善组织架构，增强权限粒度，支持企业级功能

**Depends on**: Phase 1

**Requirements**:
- USER-05, USER-06, USER-07, USER-09
- DEPT-01, DEPT-02, DEPT-03, DEPT-04, DEPT-05, DEPT-06, DEPT-07
- ROLE-03, ROLE-05
- PERM-03, PERM-04
- AUTH-06, AUTH-07, AUTH-10
- AUDIT-05
- CONFIG-03, CONFIG-05

**Success Criteria** (what must be TRUE):
1. 管理员可以创建多层级部门树 (最多5级)
2. 用户归属部门，支持部门数据权限
3. 支持字段级权限控制 (字段可见/可编辑)
4. 支持用户批量导入导出 (Excel/CSV)
5. 支持 OAuth2.0 第三方登录
6. 审计日志支持导出功能

**Plans**:
- Plan 2.1: 部门管理模块
- Plan 2.2: 数据权限范围实现
- Plan 2.3: 字段级权限控制
- Plan 2.4: OAuth2.0集成
- Plan 2.5: 批量导入导出
- Plan 2.6: 前端部门管理界面

**Target Date**: 2026-04-25

---

### Phase 3: Production Ready

**Goal**: 生产就绪，支持高并发、监控告警、完整部署

**Depends on**: Phase 2

**Requirements**:
- ROLE-04
- AUTH-08, AUTH-09
- AUDIT-03, AUDIT-07
- PERF-07

**Success Criteria** (what must be TRUE):
1. 审计日志通过 Kafka 异步处理
2. 支持双因素认证 (TOTP/短信/邮件)
3. 系统通过压力测试 (TPS >= 10,000)
4. Prometheus + Grafana 监控告警
5. Kubernetes 部署配置完成
6. 完整的 API 文档 (OpenAPI)

**Plans**:
- Plan 3.1: Kafka 审计日志集成
- Plan 3.2: 双因素认证 (2FA)
- Plan 3.3: 性能优化与缓存策略
- Plan 3.4: 监控与告警系统
- Plan 3.5: Kubernetes 部署
- Plan 3.6: 压力测试与优化

**Target Date**: 2026-04-30

---

## Progress Table

| Phase | Plans Total | Plans Complete | Status | Completed |
|-------|-------------|----------------|--------|-----------|
| 1. Foundation | 6 | 0 | Not started | - |
| 2. Department & Advanced | 6 | 0 | Not started | - |
| 3. Production Ready | 6 | 0 | Not started | - |

---

## Requirements Coverage

### Phase 1 Coverage (27 requirements)

**User Management**:
- USER-01: 用户 CRUD
- USER-02: 用户状态管理
- USER-03: 邮箱唯一性验证
- USER-04: 密码 BCrypt 加密
- USER-08: 个人资料管理

**Role Management**:
- ROLE-01: 角色 CRUD
- ROLE-02: 角色代码唯一性
- ROLE-06: 用户角色分配
- ROLE-07: 角色变更审计

**Permission Management**:
- PERM-01: 菜单权限
- PERM-02: 操作权限
- PERM-05: 权限代码唯一性
- PERM-06: 权限缓存 (Redis)
- PERM-07: 权限实时校验

**Authentication**:
- AUTH-01: JWT Token
- AUTH-02: RSA256 签名
- AUTH-03: Access/Refresh Token
- AUTH-04: 登录失败锁定
- AUTH-05: 密码策略

**Audit**:
- AUDIT-01: 操作日志记录
- AUDIT-02: 登录日志
- AUDIT-04: 日志查询筛选
- AUDIT-06: 日志保留策略

**Performance**:
- PERF-01~06: 响应时间、并发、缓存、连接池、分页

**Security**:
- SEC-01~08: 加密、HTTPS、注入防护、XSS、CSRF、限流、脱敏、防篡改

### Phase 2 Coverage (21 requirements)

**User Management**:
- USER-05: 批量导入
- USER-06: 批量导出
- USER-07: 自助注册
- USER-09: 登录历史

**Department Management**:
- DEPT-01~07: 部门CRUD、树形结构、层级、编码、负责人、成员、数据权限

**Role Management**:
- ROLE-03: 数据权限范围
- ROLE-05: 角色模板

**Permission Management**:
- PERM-03: 字段权限
- PERM-04: 数据权限

**Authentication**:
- AUTH-06: 会话管理
- AUTH-07: OAuth2.0
- AUTH-10: 记住我

**Audit**:
- AUDIT-05: 日志导出

**Configuration**:
- CONFIG-03: 邮件配置
- CONFIG-05: 限流配置

### Phase 3 Coverage (6 requirements)

**Role Management**:
- ROLE-04: 角色继承

**Authentication**:
- AUTH-08: 2FA TOTP
- AUTH-09: 2FA 短信/邮件

**Audit**:
- AUDIT-03: Kafka 异步日志
- AUDIT-07: 异常告警

**Performance**:
- PERF-07: 审计日志异步处理

---

## Dependency Graph

```
Phase 1: Foundation
    ├── Database Schema (Flyway)
    ├── JWT Authentication
    ├── User Management
    ├── Role Management
    ├── Basic RBAC (Menu + Action)
    └── Audit Framework

Phase 2: Department & Advanced
    ├── Phase 1
    ├── Department Tree
    ├── Field Permission
    ├── Data Permission
    ├── OAuth2.0
    └── Batch Operations

Phase 3: Production Ready
    ├── Phase 2
    ├── Kafka Audit
    ├── 2FA
    ├── Performance Tuning
    ├── Monitoring
    └── Kubernetes
```

---

## Milestones

| Milestone | Date | Phase | Criteria |
|-----------|------|-------|----------|
| Foundation Complete | 2026-04-15 | 1 | 用户/角色/权限基础功能可用，测试覆盖>85% |
| Advanced Features | 2026-04-25 | 2 | 部门管理、OAuth2、数据权限完成 |
| Production Ready | 2026-04-30 | 3 | 压测通过，监控就绪，K8s部署完成 |

---

## Risk & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| 开发延期 | 中 | 高 | 分阶段交付，MVP优先 |
| 性能瓶颈 | 中 | 高 | 早期性能测试，预留优化时间 |
| 安全漏洞 | 低 | 高 | 安全审查，代码审计 |
| 需求变更 | 高 | 中 | 敏捷开发，快速迭代 |

---

## Appendix

### Reference
- PRD: docs/product/PRD.md
- FRD: docs/requirements/FUNCTIONAL_REQUIREMENTS.md
- NFRD: docs/requirements/NON_FUNCTIONAL_REQUIREMENTS.md
- Architecture: docs/architecture/SYSTEM_ARCHITECTURE.md

### GSD Commands
```bash
# 查看路线图
cat .planning/ROADMAP.md

# 查看当前阶段详情
cat .planning/ROADMAP.md | grep -A 50 "Phase 1"

# 开始下一阶段
/gsd:start-phase 1
```
