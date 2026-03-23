# 全栈用户管理系统 - Claude Code 项目配置

## 项目概述

全栈用户管理系统，采用 Spring Boot 后端和 Next.js 前端，提供用户注册、登录、权限管理、角色分配等功能。

## 技术栈

### 后端
- Spring Boot 3.5 + JDK 21
- Spring Data JPA + Flyway
- PostgreSQL (生产) / H2 (开发测试)
- Spring Security + JWT + OAuth2 认证

### 前端
- Next.js 14 (App Router)
- TypeScript 5+
- shadcn/ui + Tailwind CSS
- Zustand 状态管理

### 基础设施
- Docker + Docker Compose
- GitHub Actions CI/CD

## 开发原则

1. 类型安全: TypeScript + Java 强类型系统
2. 测试驱动: 覆盖率 > 85%
3. 分层架构: Controller → Service → Repository → Entity
4. 安全第一: 最小权限、输入验证、防御性编程

## 项目结构

```
usermanagement/
├── backend/          # Spring Boot 后端
│   ├── src/main/java/com/usermanagement/
│   │   ├── domain/       # JPA 实体
│   │   ├── repository/   # Spring Data 仓库
│   │   ├── service/      # 业务服务
│   │   ├── web/          # Controller + DTO
│   │   ├── security/     # 安全配置
│   │   └── config/       # 应用配置
│   └── src/main/resources/
│       └── db/migration/ # Flyway 迁移脚本
├── frontend/         # Next.js 前端
├── docs/            # 文档
└── scripts/         # 脚本
```

## 关键文档

- AGENTS.md - 多代理协作配置
- DATABASE_SCHEMA.md - 数据库设计
- API_SPECIFICATION.md - API 接口规范
- AUTHENTICATION_FLOW.md - 认证流程
- BACKEND_ARCHITECTURE.md - 后端架构
- FRONTEND_ARCHITECTURE.md - 前端架构
- TESTING_STRATEGY.md - 测试策略
- DEPLOYMENT_GUIDE.md - 部署指南
- DEVELOPMENT_WORKFLOW.md - 开发流程

## 质量指标

- 后端测试覆盖率 ≥ 85%
- 前端测试覆盖率 ≥ 80%
- API 响应时间 P95 < 200ms
