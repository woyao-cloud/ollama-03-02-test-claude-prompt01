# 多代理协作配置

## 代理角色

| 角色 | 职责 | 权限边界 |
|------|------|----------|
| 产品经理 | 产品战略、PRD 编写、路线图规划、用户研究 | 只修改产品文档，不修改代码 |
| 业务分析师 | 需求梳理、功能/非功能需求定义、业务流程建模 | 只修改需求文档，不修改代码 |
| 架构师 | 系统设计、技术选型、接口定义 | 只修改架构文档 |
| 后端工程师 | Spring Boot 实现、API、业务逻辑 | 只修改后端代码 |
| 前端工程师 | Next.js 开发、UI 组件 | 只修改前端代码 |
| 数据库工程师 | 数据模型、迁移、优化 | 只修改数据库文件 |
| 测试工程师 | 测试策略、用例实现 | 访问所有代码测试 |
| E2E测试工程师 | Playwright E2E测试、关键用户流程验证 | 只修改 e2e/ 目录 |
| 部署工程师 | CI/CD、容器化、监控 | 只修改基础设施文件 |

## 协作流程

1. **阶段一**: 产品经理定义产品 → 业务分析师梳理需求 → 架构师设计 → 数据库工程师建模
2. **阶段二**: 前后端并行开发 → 测试工程师编写单元/集成测试
3. **阶段三**: API 集成调试 → E2E测试工程师编写端到端测试
4. **阶段四**: 部署工程师配置 → 全团队验证

## 详细协作时序

```
产品经理
    ↓ 输出: PRD, 产品路线图, 用户画像
业务分析师
    ↓ 输出: FRD, NFRD, CONTEXT.md
架构师
    ↓ 输出: SYSTEM_ARCHITECTURE.md, ADRs
数据库工程师 + 后端工程师 + 前端工程师（并行）
    ↓
测试工程师（单元/集成测试）
    ↓
E2E测试工程师（Playwright E2E测试）
    ↓
部署工程师（CI/CD, 容器化）
    ↓
全团队验证
```

## 交付物清单

### 产品经理输出
- `docs/product/PRD.md` - 产品需求文档
- `docs/product/ROADMAP.md` - 产品路线图
- `docs/product/USER_PERSONAS.md` - 用户画像
- `docs/product/COMPETITIVE_ANALYSIS.md` - 竞品分析

### 业务分析师输出
- `docs/requirements/FUNCTIONAL_REQUIREMENTS.md` - 系统功能需求说明书
- `docs/requirements/NON_FUNCTIONAL_REQUIREMENTS.md` - 系统非功能需求说明书
- `docs/requirements/USER_STORIES.md` - 用户故事列表
- `docs/requirements/REQUIREMENTS_TRACEABILITY_MATRIX.md` - 需求跟踪矩阵

### 数据库工程师输出
- `backend/src/main/java/com/usermanagement/domain/*.java` - JPA 实体
- `backend/src/main/resources/db/migration/V*__*.sql` - Flyway 迁移脚本
- `DATABASE_SCHEMA.md` - 数据库设计文档
- `docs/database/ER_DIAGRAM.md` - 实体关系图

### E2E测试工程师输出
- `frontend/e2e/playwright.config.ts` - Playwright 配置
- `frontend/e2e/pages/*.ts` - Page Object Model
- `frontend/e2e/specs/**/*.spec.ts` - E2E 测试用例
- `frontend/e2e/fixtures/*.ts` - 测试数据
- `frontend/e2e/snapshots/` - 视觉回归基线
- `e2e-report/` - 测试报告（HTML/JSON/JUnit）

### 架构师输出
- `docs/architecture/SYSTEM_ARCHITECTURE.md` - 系统架构设计
- `docs/architecture/adr/ADR-XXX.md` - 架构决策记录
- `docs/architecture/DIAGRAMS/*.md` - 架构图

### 测试工程师输出
- `backend/src/test/**/*Test.java` - 单元/集成测试
- `backend/target/site/jacoco/` - 覆盖率报告
- `frontend/**/*.test.ts` - 前端单元测试

### 其他角色输出
参见各角色具体工作说明书。


## 质量门禁

- 需求文档通过评审（业务分析师输出）
- 架构设计通过评审（架构师输出）
- 代码规范检查通过
- 单元测试覆盖率 > 85%
- 集成测试通过
- E2E 测试通过（关键用户流程）
- 安全审查通过（security-reviewer）
- 性能基准测试通过
- 文档同步更新
