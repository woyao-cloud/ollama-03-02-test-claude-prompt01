# PROJECT: 全栈用户角色权限管理系统

## Core Value

为企业构建统一、灵活、安全的用户角色权限管理平台，解决权限分配混乱、管理效率低下、合规审计困难等核心痛点。

**关键指标**:
- 用户管理效率提升 80% (10分钟/用户 → 2分钟/用户)
- 权限配置时间减少 83% (30分钟/角色 → 5分钟/角色)
- 支持 1000万+ 注册用户，10,000+ 并发登录 TPS
- 审计合规率 100%

---

## Project Overview

| 属性 | 值 |
|------|-----|
| **名称** | 全栈用户角色权限管理系统 |
| **代号** | usermanagement |
| **版本** | v1.0 MVP |
| **目标日期** | 2026-04-30 |
| **状态** | 规划中 |

---

## Goals

### Primary Goals
1. **提升管理效率**: 将用户和权限管理效率提升 80%
2. **强化安全合规**: 实现 100% 操作可追溯，满足审计要求
3. **改善用户体验**: 提供直观的用户界面，降低使用门槛
4. **支撑高并发**: 支持 10,000+ 并发用户，峰值 50,000

### Technical Goals
- 登录接口响应 < 100ms (P95)
- API 平均响应 < 200ms
- 系统可用性 99.9%
- 后端测试覆盖率 ≥ 85%
- 前端测试覆盖率 ≥ 80%

---

## Constraints

### Technical Constraints
- **后端**: Spring Boot 3.5 + JDK 21
- **前端**: Next.js 16 + TypeScript 5+
- **数据库**: PostgreSQL 15+ (生产) / H2 (开发测试)
- **缓存**: Redis 7+ (必需，用于会话和高并发)
- **消息队列**: Kafka 3+ (推荐，用于审计日志)
- **部署**: Docker + Kubernetes

### Performance Constraints
- 登录 TPS ≥ 10,000
- 并发用户 ≥ 10,000
- 注册用户总数 ≥ 1000万
- 日志存储 ≥ 1000万条/天

### Security Constraints
- 密码加密: BCrypt strength ≥ 12
- JWT 签名: RSA256
- 传输加密: TLS 1.3
- 等保 2.0 三级合规

---

## Milestones

### Phase 1: Foundation (2026-04-15)
**目标**: 交付最小可用产品，支持基础用户权限管理

**核心功能**:
- 数据库设计与 Flyway 迁移
- JWT 认证与基础安全
- 用户 CRUD 管理
- 角色 CRUD 管理
- 基础 RBAC 权限 (菜单 + 操作)
- 审计日志框架

**质量门禁**:
- 单元测试覆盖率 > 85%
- 登录接口 < 100ms
- 基础安全审计通过

---

### Phase 2: Department & Advanced (2026-04-25)
**目标**: 完善组织架构，增强权限粒度

**核心功能**:
- 部门树形管理 (Materialized Path)
- 字段级权限控制
- 数据权限范围 (全部/部门/个人)
- OAuth2.0 第三方登录
- 用户批量导入/导出
- 高级密码策略

**质量门禁**:
- 部门操作 < 200ms
- 权限检查 < 50ms
- 集成测试通过率 100%

---

### Phase 3: Production Ready (2026-04-30)
**目标**: 生产就绪，支持高并发与监控

**核心功能**:
- Kafka 异步审计日志
- 双因素认证 (2FA/TOTP)
- 性能优化 (Redis 缓存策略)
- 监控与告警 (Prometheus/Grafana)
- Kubernetes 部署配置
- 完整 API 文档

**质量门禁**:
- 压测 TPS ≥ 10,000
- 可用性 99.9%
- 安全渗透测试通过

---

## Architecture Decisions

| ADR | 标题 | 状态 | 路径 |
|-----|------|------|------|
| ADR-001 | 后端框架选择 Spring Boot 3.5 | 已批准 | docs/architecture/adr/ADR-001-spring-boot-framework.md |
| ADR-002 | 前端框架选择 Next.js 16 | 已批准 | docs/architecture/adr/ADR-002-nextjs-frontend.md |
| ADR-003 | 数据库选择 PostgreSQL 15 | 已批准 | docs/architecture/adr/ADR-003-postgresql-database.md |
| ADR-004 | 缓存选择 Redis 7 | 已批准 | docs/architecture/adr/ADR-004-redis-cache.md |
| ADR-005 | 认证方案 JWT + RSA256 | 已批准 | docs/architecture/adr/ADR-005-jwt-authentication.md |
| ADR-006 | 权限模型 RBAC 四级 | 已批准 | docs/architecture/adr/ADR-006-rbac-permission-model.md |
| ADR-007 | 消息队列选择 Kafka 3 | 已批准 | docs/architecture/adr/ADR-007-kafka-messaging.md |

---

## Key Metrics

### Performance Metrics
| 指标 | 目标值 | 测量方式 |
|------|--------|----------|
| 登录接口响应 | < 100ms | P95 监控 |
| API 平均响应 | < 200ms | P95 监控 |
| 页面加载时间 | < 2s | Lighthouse |
| 登录 TPS | ≥ 10,000 | JMeter 压测 |

### Quality Metrics
| 指标 | 目标值 | 工具 |
|------|--------|------|
| 后端覆盖率 | ≥ 85% | JaCoCo |
| 前端覆盖率 | ≥ 80% | Jest |
| 代码质量 | A 级 | SonarQube |
| 安全漏洞 | 0 高危 | OWASP DC |

### Business Metrics
| 指标 | 目标值 |
|------|--------|
| 用户管理效率 | 2分钟/用户 |
| 权限配置时间 | 5分钟/角色 |
| 审计合规率 | 100% |
| 系统可用性 | 99.9% |

---

## Project Structure

```
usermanagement/
├── .planning/              # GSD 规划文件
│   ├── PROJECT.md         # 本文件
│   ├── config.json        # 工作流配置
│   ├── REQUIREMENTS.md    # 需求汇总
│   ├── ROADMAP.md         # 路线图
│   └── STATE.md           # 项目状态
├── backend/               # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/usermanagement/
│   │       ├── domain/    # JPA 实体
│   │       ├── repository/# 数据仓库
│   │       ├── service/   # 业务服务
│   │       ├── web/       # Controller + DTO
│   │       ├── security/  # 安全配置
│   │       └── config/    # 应用配置
│   └── src/main/resources/
│       └── db/migration/  # Flyway 迁移
├── frontend/              # Next.js 前端
│   ├── src/
│   │   ├── app/          # App Router
│   │   ├── components/   # UI 组件
│   │   ├── lib/          # 工具函数
│   │   └── store/        # Zustand 状态
│   └── public/
├── docs/                  # 项目文档
│   ├── product/          # 产品文档
│   ├── architecture/     # 架构文档
│   └── requirements/     # 需求文档
├── k8s/                   # Kubernetes 配置
├── docker-compose.yml     # 本地开发环境
└── README.md
```

---

## Stakeholders

| 角色 | 姓名 | 职责 |
|------|------|------|
| 产品经理 | - | 需求定义与优先级 |
| 架构师 | - | 技术架构设计 |
| 后端开发 | - | Spring Boot 开发 |
| 前端开发 | - | Next.js 开发 |
| 测试工程师 | - | 测试策略与执行 |
| 运维工程师 | - | 部署与运维 |

---

## Communication

- **文档**: docs/ 目录
- **路线图**: .planning/ROADMAP.md
- **状态**: .planning/STATE.md

---

## Appendix

### Reference Documents
- PRD: docs/product/PRD.md
- FRD: docs/requirements/FUNCTIONAL_REQUIREMENTS.md
- NFRD: docs/requirements/NON_FUNCTIONAL_REQUIREMENTS.md
- Architecture: docs/architecture/SYSTEM_ARCHITECTURE.md

### GSD Commands
```bash
# 查看项目状态
cat .planning/STATE.md

# 查看当前阶段
cat .planning/ROADMAP.md

# 开始下一阶段
/gsd:start-phase
```
