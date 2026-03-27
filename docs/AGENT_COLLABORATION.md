# Agent 协作详解

本文档详细说明各个 Agent 如何协作、交接规范、触发机制和交付物标准。

## 目录

1. [核心协作流程](#核心协作流程)
2. [阶段详解](#阶段详解)
3. [Agent 交接规范](#agent-交接规范)
4. [输出标准](#输出标准)
5. [触发机制](#触发机制)
6. [交付物验收](#交付物验收)
7. [变更与变异](#变更与变异)

---

## 核心协作流程

### 整体时序图

```
需求阶段 (Week 1-2)
  │
  ├─► 产品经理
  │   └─► 输出: PRD, 路线图, 用户画像
  │
  └─► 业务分析师
      ├─► FRD (功能需求说明书)
      ├─► NFRD (非功能需求说明书)  
      └─► CONTEXT.md (用户决策 + 技术约束)
           │
           ├─► [评审门禁] 需求通过评审
           │
           └─► 架构阶段 (Week 3-4)
                │
                ├─► 架构师
                │   ├─► SYSTEM_ARCHITECTURE.md
                │   ├─► ADR/*.md (架构决策)
                │   └─► TECHNICAL_CONSTRAINTS.md
                │
                ├─► 安全审查员 ◄─── [并行]
                │   └─► 安全审查报告
                │
                └─► [评审门禁] 架构通过评审
                     │
                     └─► 规划阶段 (Week 4-5)
                          │
                          └─► 规划师
                              └─── PLAN.md (可执行计划)
                                   │
                                   ├─[评审门禁] 计划通过评审
                                   │
                                   └─► 开发阶段 (Week 6-12, 并行)
                                        │
                                        ├─► 后端工程师 [使用 tdd-guide]
                                        │   ├─► 测试代码 (>85% 覆盖)
                                        │   └─► 实现代码
                                        │
                                        ├─► 前端工程师 [使用 tdd-guide]
                                        │   ├─► 测试代码 (>85% 覆盖)
                                        │   └─► 实现代码
                                        │
                                        ├─► 数据库工程师
                                        │   └─► Flyway 迁移脚本
                                        │
                                        └─► [代码评审]
                                             │
                                             ├─► code-reviewer
                                             │   └─── 代码质量报告
                                             │
                                             ├─► security-reviewer
                                             │   └─── 安全审查报告
                                             │
                                             └─[门禁] 无 CRITICAL 问题
                                                  │
                                                  └─► 测试阶段 (Week 12-13)
                                                       │
                                                       ├─► e2e-runner
                                                       │   └─── E2E 测试报告
                                                       │
                                                       └─[门禁] E2E 通过
                                                            │
                                                            └─► 部署阶段
                                                                └─── 生产发布
```

### 协作关系矩阵

| 前置Agent | 输出 | 后续Agent | 依赖条件 | 并行可能 |
|----------|------|----------|--------|--------|
| business-analyst | FRD, NFRD, CONTEXT | architect | 需求评审通过 | ❌ 串行 |
| architect | SYSTEM_ARCHITECTURE, ADR | gsd-planner | 架构评审通过 | ✅ 可与 security-reviewer 并行 |
| security-reviewer | 安全审查报告 | gsd-planner | 无漏洞 | ✅ 与 architect 并行 |
| gsd-planner | PLAN.md | tdd-guide + 开发工程师 | 计划评审通过 | ❌ 串行 |
| tdd-guide | 测试代码 | code-reviewer | 测试编写完成 | ❌ 串行 |
| code-reviewer | 质量报告 | 修改/合并 | 无CRITICAL问题 | ⚠️ 可反复 |
| security-reviewer (代码阶段) | 漏洞报告 | 修改 | 无CRITICAL漏洞 | ⚠️ 可反复 |
| e2e-runner | E2E报告 | 部署 | 测试通过 | ❌ 串行 |

---

## 阶段详解

### 第1阶段: 需求分析 (1-2周)

#### 参与者: business-analyst

**启动条件**:
- 产品 PRD 确定
- 利益相关者明确
- 业务目标清晰

**工作流**:
1. **需求发现 (Day 1-2)**
   - 阅读产品 PRD
   - 与产品经理、用户进行访谈
   - 识别利益相关者
   - 列出假设与约束

2. **需求分析 (Day 3-5)**
   - 业务流程建模 (AS-IS → TO-BE)
   - 用户角色与权限梳理
   - 功能需求对标 (Epic → User Story → AC)
   - 非功能需求分类 (性能、安全、可用性、可靠性、可维护性)
   - 外部系统与依赖识别

3. **需求文档化 (Day 6-8)**
   - 编写 FUNCTIONAL_REQUIREMENTS.md (FRD)
   - 编写 NON_FUNCTIONAL_REQUIREMENTS.md (NFRD)
   - 编写 USER_STORIES.md (用户故事列表)
   - 编写 REQUIREMENTS_TRACEABILITY_MATRIX.md (RTM)

4. **需求验证 (Day 9-10)**
   - 完整性检查 (所有用户角色都有需求吗?)
   - 一致性检查 (需求是否有冲突?)
   - 可测试性检查 (每个需求都有验收标准吗?)
   - 与架构师/开发团队评审

5. **Agent 交接 (Day 11)**
   - 生成 CONTEXT.md (包含用户决策)
   - 触发下游 Agent

**交付物**:
```
docs/requirements/
├── FUNCTIONAL_REQUIREMENTS.md      (功能需求说明书)
├── NON_FUNCTIONAL_REQUIREMENTS.md  (非功能需求说明书)
├── USER_STORIES.md                 (用户故事)
├── REQUIREMENTS_TRACEABILITY_MATRIX.md
└── CONTEXT.md                      (用户决策 + 技术约束)
```

**验收标准**:
- ✅ 每个功能需求都有验收标准 (AC)
- ✅ 每个用户故事都对应需求 ID
- ✅ CONTEXT.md 包含所有用户决策 (Locked/Deferred/Discretion)
- ✅ 非功能需求具体量化 (无"性能要好"这类模糊表述)
- ✅ 已识别的风险和假设已记录

---

### 第2阶段: 架构设计 (1-2周, 与第1阶段后续并行)

#### 参与者: architect, security-reviewer (并行)

**启动条件**:
- FRD/NFRD 完成并通过评审
- CONTEXT.md 已生成
- 业务目标和技术约束明确

**工作流**:

**2.1 架构师工作流**:
1. **需求理解 (Day 1-2)**
   - 深入阅读 FRD/NFRD/CONTEXT.md
   - 与业务分析师澄清需求
   - 提取关键非功能需求 (性能目标、可用性、安全等级)

2. **技术选型评估 (Day 3-4)**
   - 评估替代技术方案
   - 分析每个方案的优缺点
   - 基于 NFR 推荐最优方案 (已在 CONTEXT.md 中确定)
   - 识别技术风险和学习曲线

3. **架构设计 (Day 5-8)**
   - **高层架构设计**:
     - 系统边界与外部依赖
     - 分层设计 (展示层、应用层、数据层)
     - 组件交互与通信协议
     - 错误处理与容错机制
   - **详细设计**:
     - 数据流与API设计
     - 数据库模型 (与数据库工程师协作)
     - 缓存策略
     - 安全认证流程

4. **架构决策文档化 (Day 9)**
   - 编写 SYSTEM_ARCHITECTURE.md
   - 编写 ADR (Architecture Decision Records)
   - 编写 TECHNICAL_CONSTRAINTS.md (技术约束、限制条件)

**2.2 安全审查员工作流** (与架构师并行):
1. **安全需求识别 (Day 1-2)**
   - 提取 NFRD 中的安全相关需求
   - 列出 OWASP Top 10 检查清单
   - 识别关键安全流程 (认证、授权、加密、审计)

2. **架构级安全评审 (Day 3-8)**
   - 威胁建模 (STRIDE)
   - 认证方案评估 (JWT vs OAuth2 vs SSO)
   - 数据保护设计评审
   - 网络隔离和防火墙规则

3. **安全架构输出 (Day 9)**
   - 编写 SECURITY_ARCHITECTURE.md (如存在)
   - 或将安全相关内容纳入 SYSTEM_ARCHITECTURE.md

**交付物**:
```
docs/architecture/
├── SYSTEM_ARCHITECTURE.md           (系统架构设计)
├── SECURITY_ARCHITECTURE.md         (安全架构)
├── DATA_FLOW_AND_API.md            (数据流与API)
├── adr/
│   ├── ADR-001-技术栈选择.md
│   ├── ADR-002-认证方案.md
│   ├── ADR-003-高并发架构.md
│   ├── ADR-004-数据库设计.md
│   ├── ADR-005-缓存策略.md
│   └── ...
└── TECHNICAL_CONSTRAINTS.md         (技术约束与限制)
```

**验收标准**:
- ✅ 架构图清晰展示组件交互
- ✅ 每个架构决策都有 ADR 记录 (问题、选项、决策、影响)
- ✅ NFR 都有对应的架构设计 (如性能 < 200ms 对应缓存策略)
- ✅ 安全审查无遗漏 (OWASP Top 10 都有设计对应)
- ✅ 架构可应对预期的并发量和数据规模

---

### 第3阶段: 项目规划 (1周)

#### 参与者: gsd-planner

**启动条件**:
- SYSTEM_ARCHITECTURE.md 完成
- ADR 已编写
- 安全审查完成

**工作流**:
1. **项目分解 (Day 1-2)**
   - 按模块拆分任务 (后端、前端、数据库、基础设施)
   - 识别关键路径和依赖关系
   - 估算每项任务的工作量

2. **时间规划 (Day 2-3)**
   - 计算工期（基于依赖关系和并行度）
   - 识别关键路径上的关键任务
   - 制定里程碑和交付日期

3. **风险识别 (Day 3)**
   - 技术风险 (学习曲线、新技术采用)
   - 依赖风险 (外部系统、第三方库)
   - 人力风险 (技能缺陷、团队容量)

4. **计划输出 (Day 4)**
   - 编写 PLAN.md
   - 生成甘特图 (Mermaid)
   - 定义验收标准

**交付物**:
```
PLAN.md
├── 项目概述
├── 阶段划分 (Phase 1-4)
├── 任务清单 (按阶段 + 按模块)
├── 依赖关系
├── 里程碑与交付日期
├── 甘特图
├── 风险识别与缓解
└── 成功标准
```

**验收标准**:
- ✅ 任务粒度合理 (每项 2-5 天)
- ✅ 依赖关系清晰 (无循环依赖)
- ✅ 关键路径明确
- ✅ 工期估算有依据
- ✅ 风险识别到位

---

### 第4阶段: 开发实现 (6-8周, 并行)

#### 参与者: 后端工程师 + 前端工程师 + 数据库工程师 (使用 tdd-guide)

**启动条件**:
- PLAN.md 通过评审
- 开发环境配置完成

**工作流**:

**4.1 测试驱动开发 (TDD) 循环**:

每个任务遵循 RED-GREEN-REFACTOR 循环:

```
┌─────────────────────────────────────┐
│        1. RED: 编写失败的测试        │
│   ┌────────────────────────────────┐│
│   │ - 根据需求编写测试用例          ││
│   │ - 测试应该因为功能不存在而失败  ││
│   │ - 验证测试有效 (稍改代码验证)  ││
│   └────────────────────────────────┘│
└─────────────────────────────────────┘
              │ 执行测试
              ▼
┌─────────────────────────────────────┐
│    2. GREEN: 实现最小化功能         │
│   ┌────────────────────────────────┐│
│   │ - 编写代码使测试通过            ││
│   │ - 优先通过 (代码不需要完美)    ││
│   │ - 运行所有测试 (包括旧测试)    ││
│   └────────────────────────────────┘│
└─────────────────────────────────────┘
              │ 所有测试通过
              ▼
┌─────────────────────────────────────┐
│      3. REFACTOR: 代码优化          │
│   ┌────────────────────────────────┐│
│   │ - 提高代码质量 (可读性、性能)   ││
│   │ - 消除重复代码                  ││
│   │ - 重新运行测试 (验证重构成功)  ││
│   └────────────────────────────────┘│
└─────────────────────────────────────┘
              │ 测试仍然通过
              ▼
         ┌─────────────┐
         │ 下一个需求  │
         └─────────────┘
```

**4.2 后端工程师工作流**:
1. 选择任务 (来自 PLAN.md)
2. 根据层级选择测试类型 (Controller → 集成测试 / Service → 单元测试 / Repository → @DataJpaTest)
3. 编写测试代码 (RED)
4. 实现功能 (GREEN)
5. 重构与优化 (REFACTOR)
6. 提交代码审查

**4.3 前端工程师工作流**:
1. 实现 React 组件 (使用 @testing-library)
2. 编写组件单元测试
3. 实现页面交互逻辑
4. 编写集成测试

**4.4 数据库工程师工作流**:
1. 设计表结构 (基于 ADR-004)
2. 编写 Flyway 迁移脚本 (`V*/R__`)
3. 与后端工程师协作 JPA 实体映射
4. 性能优化 (索引、查询优化)

**交付物**:
```
backend/
├── src/main/java/com/usermanagement/
│   ├── domain/          (JPA 实体)
│   ├── service/         (业务逻辑)
│   ├── web/             (Controller + DTO)
│   └── repository/      (Spring Data)
├── src/test/java/       (单元/集成测试, >85% 覆盖)
└── src/main/resources/db/migration/
    └── V*/R__*.sql     (Flyway 迁移脚本)

frontend/
├── src/app/            (Next.js 页面)
├── src/components/     (React 组件)
├── tests/              (单元/集成测试)
└── ...
```

**质量门禁**:
- ✅ 单元测试覆盖率 > 85% (后端 JaCoCo, 前端 Istanbul)
- ✅ 所有测试通过 (包括旧测试)
- ✅ 代码规范检查通过 (Checkstyle/ESLint)

---

### 第5阶段: 代码审查 (持续)

#### 参与者: code-reviewer, security-reviewer

**启动条件**:
- 代码提交 (PR)
- 测试通过
- 自动化检查通过

**工作流**:

**5.1 code-reviewer**:
1. **功能正确性** - 代码是否实现了需求?
2. **性能** - 有性能瓶颈吗? (O(n²) 查询、N+1 问题、内存泄漏)
3. **安全性** - 是否遵循安全最佳实践? (输入验证、SQL 注入防护等)
4. **代码质量** - 可读性、可维护性、是否遵循约定?
5. **架构遵循** - 是否遵循分层架构? 依赖关系是否正确?

**5.2 security-reviewer**:
1. **OWASP Top 10** - 检查常见漏洞
2. **认证授权** - JWT/OAuth2 实现是否正确?
3. **敏感数据保护** - 数据库密码、API密钥是否硬编码?
4. **加密传输** - 是否使用 HTTPS?
5. **审计日志** - 关键操作是否记录?

**审查结果标记**:
| 级别 | 含义 | 处理方式 |
|------|------|--------|
| CRITICAL | 高度安全/性能/功能问题 | 🚫 阻止合并，必须修改 |
| HIGH | 明显的设计缺陷或性能问题 | ⚠️ 建议修改 (通常被接受) |
| MEDIUM | 代码质量/可维护性问题 | 💡 建议改进 (可defer) |
| LOW | 风格/注释/文档建议 | 📝 参考建议 |

**验收标准**:
- ✅ 无 CRITICAL 问题
- ✅ HIGH 问题已解决或有充分理由defer
- ✅ 至少一个与审查员同行完成

---

### 第6阶段: E2E 测试 (1-2周)

#### 参与者: e2e-runner

**启动条件**:
- 后端 API 可用
- 前端部署到 SIT/UAT 环境
- 基础功能测试通过

**工作流**:
1. **测试环境准备**
   - 配置 Playwright (多浏览器、移动端)
   - 准备测试数据 (API setup/teardown)
   - 配置 Page Object Model

2. **关键用户流程测试**
   - 用户登录 → 权限检查 → 操作 → 登出 流程
   - 数据 CRUD 操作完整流程
   - 异常分支处理 (权限不足、业务规则违反)

3. **跨浏览器测试**
   - Chrome、Firefox、Safari
   - 响应式设计 (桌面、平板、手机)

4. **视觉回归测试**
   - 截图与基线比较
   - 检测 UI 意外变更

5. **性能监控**
   - 页面加载时间
   - 交互响应延迟
   - 资源加载量

**交付物**:
```
frontend/e2e/
├── specs/
│   ├── login.spec.ts         (登录流程)
│   ├── user-management.spec.ts
│   └── ...
├── pages/
│   ├── LoginPage.ts
│   ├── DashboardPage.ts
│   └── ...
├── fixtures/
│   └── test-data.ts
├── snapshots/
│   └── [视觉回归基线]
└── reports/
    └── [HTML/JSON/JUnit 报告]
```

**验收标准**:
- ✅ 所有关键流程 E2E 测试通过
- ✅ 跨浏览器兼容性验证
- ✅ 无视觉回归 (截图对比通过)
- ✅ 性能指标达到 NFR 要求

---

### 第7阶段: 部署 (1-2天)

#### 参与者: 部署工程师 + 全团队

**启动条件**:
- E2E 测试通过
- 发布版本标签已创建
- 部署文档已准备

**工作流**:
1. **灰度部署** - 部署到 5-10% 生产流量
2. **监控** - 监控错误率、性能指标
3. **全量部署** - 如无异常，部署到 100%
4. **回滚预案** - 如出现问题，立即回滚

**验收标准**:
- ✅ 部署成功
- ✅ 关键指标正常 (错误率 < 0.1%, 响应时间 < 200ms)
- ✅ 用户反馈无异常

---

## Agent 交接规范

### 交接文件格式: CONTEXT.md

所有需求和设计交接都应该通过 CONTEXT.md 文件。格式如下:

```markdown
# 需求上下文与决策

## 项目信息
- 项目名称: [name]
- 版本: [version]
- 日期: [date]
- 业务目标: [清晰阐述为什么做这个项目]

## 用户决策（Locked Decisions）

### D-01: 后端框架选择: Spring Boot 3.5
- **决策**: Spring Boot 3.5 + JDK 21
- **原因**: 虚拟线程支持高并发，企业级生态
- **影响**: 依赖 Maven/Gradle, 开发周期+2周
- **状态**: LOCKED ✅

### D-02: 数据库: PostgreSQL 15
- **决策**: PostgreSQL 15 + Flyway 迁移管理
- **原因**: JSONB 支持，开源免费，性能优秀
- **影响**: 须学习 PostgreSQL 特性，Flyway 脚本编写
- **状态**: LOCKED ✅

### ... (更多决策)

## 延迟决策（Deferred Decisions）

### DF-01: 前端状态管理方案
- **内容**: Redux vs Zustand vs Jotai
- **原因**: 前端架构设计未成熟，先实现基础功能
- **预计解决**: Phase 2 (前端架构细化时)

## Claude 自由裁量区（Discretion）

### CD-01: 错误处理实现细节
- **范围**: 全局异常处理器的具体实现、错误码设计
- **约束**: 必须遵守 API_SPECIFICATION.md 中的错误响应格式

## 关键需求摘要

### Must Have（必须实现）
- [x] REQ-001: 用户注册与登录 (JWT认证)
- [x] REQ-002: 用户/角色/权限管理
- [x] REQ-003: RBAC 权限控制

### Should Have（应该实现）
- [ ] REQ-101: OAuth2 第三方登录
- [ ] REQ-102: 用户审计日志

### Could Have（可以实现）
- [ ] REQ-201: 用户自助密码重置
- [ ] REQ-202: 国际化支持 (i18n)

## 非功能需求关键指标

| 类别 | 指标 | 目标值 |
|------|------|--------|
| **性能** | 响应时间 P95 | < 200ms |
| **性能** | 登录时间 | < 100ms |
| **可用性** | SLA | 99.9% |
| **安全** | 认证方式 | JWT + Spring Security |
| **可扩展** | 支持用户数 | 1000万+ |
| **可扩展** | 并发用户 | 10000+ TPS |

## 风险与假设

### 风险
- **R1**: Spring Boot 学习曲线 (缓解: 预留2周学习时间)
- **R2**: PostgreSQL 迁移脚本复杂性 (缓解: 使用 Flyway + Code Review)
- **R3**: 前后端接口契约变更 (缓解: 使用 OpenAPI + Contract Testing)

### 假设
- **A1**: 团队有 Java/Spring Boot 基础
- **A2**: PostgreSQL 已在基础设施上可用
- **A3**: 网络环境支持 HTTPS/TLS

## 参考文档
- FRD: `./docs/requirements/FUNCTIONAL_REQUIREMENTS.md`
- NFRD: `./docs/requirements/NON_FUNCTIONAL_REQUIREMENTS.md`
- User Stories: `./docs/requirements/USER_STORIES.md`
```

### 交接三要素

**1. 输入要求** (前置Agent必须满足)
```
business-analyst 完成 ──────► architect
  ✅ 条件: FRD/NFRD/CONTEXT.md 完成
  ❌ 前置: 需求通过评审 (Lead Architect/PM 签字)
  ⏱️ 时间表: Day 11 EST
```

**2. 输出标准** (当前Agent必须交付)
```
architect 完成 ──────► gsd-planner
  ✅ 输出: SYSTEM_ARCHITECTURE.md + ADR(s)
  ✅ 质量: 无遗漏的 NFR 设计，安全审查通过
  ✅ 格式: Markdown, 包含图表, 无歧义
```

**3. 触发条件** (后续Agent启动)
```
gsd-planner 启动 当:
  ✅ 架构评审通过
  ✅ 安全审查完成
  ✅ CONTEXT.md 已更新 (包含架构决策)
```

---

## 输出标准

### 表格: 各角色输出物标准

| Agent | 输出文件 | 格式 | 内容要求 | 验收标准 |
|-------|---------|------|--------|--------|
| business-analyst | FUNCTIONAL_REQUIREMENTS.md | Markdown | 用例、User Story、AC | ✅ 每个需求有验收标准 |
| business-analyst | NON_FUNCTIONAL_REQUIREMENTS.md | Markdown | 性能、安全、可用性量化 | ✅ 所有 NFR 都有指标 |
| business-analyst | CONTEXT.md | Markdown | Locked/Deferred/Discretion 决策 | ✅ D-*, DF-*, CD-* 都有原因 |
| architect | SYSTEM_ARCHITECTURE.md | Markdown + Mermaid | 分层设计、组件图、数据流 | ✅ NFR 都有对应设计 |
| architect | adr/*.md | Markdown | 问题、选项、决策、影响 | ✅ ADR 遵循模板 |
| tdd-guide | *Test.java / *.spec.ts | Java/TypeScript | 单元/集成/E2E 测试 | ✅ 覆盖率 > 85% |
| code-reviewer | Review Report | Markdown/Comment | CRITICAL/HIGH/MEDIUM/LOW 问题 | ✅ 无 CRITICAL, HIGH 已解决 |
| security-reviewer | Security Report | Markdown | 漏洞、严重级别、修复建议 | ✅ 无 CRITICAL 漏洞 |
| e2e-runner | E2E Report | HTML/JSON/JUnit | 测试用例、结果、性能数据 | ✅ 所有用户流程通过 |

---

## 触发机制

### 自动触发规则

**业务分析师完成** → **触发架构师**:
```yaml
当:
  - FUNCTIONAL_REQUIREMENTS.md 存在
  - NON_FUNCTIONAL_REQUIREMENTS.md 存在
  - CONTEXT.md 生成 (包含 D-*, DF-*)
  - 评审通过 (Lead 签字注释 "/approve")

动作: 创建 architect 任务, 输入 CONTEXT.md 路径
```

**架构师完成** → **触发规划师**:
```yaml
当:
  - SYSTEM_ARCHITECTURE.md 存在
  - adr/ 目录包含至少 3 个 ADR 文件
  - 安全审查报告已生成 (无 CRITICAL)
  - 评审通过 ("/approve")

动作: 创建 gsd-planner 任务, 输入架构文档路径
```

**规划完成** → **触发开发**:
```yaml
当:
  - PLAN.md 存在
  - 包含清晰的当前阶段任务清单
  - 评审通过 ("/approve")

动作: 创建开发任务, 开发工程师选择任务并应用 tdd-guide
```

**代码提交** → **触发审查**:
```yaml
当:
  - PR 提交
  - CI 自动检查通过 (单元测试、linting)

动作: 自动分配给 code-reviewer + security-reviewer
```

---

## 交付物验收

### 质量门禁检查表

**第1阶段 - 需求分析生效**:
- [ ] FRD/NFRD/USER_STORIES 已编写
- [ ] 每个功能需求有验收标准
- [ ] CONTEXT.md 包含所有用户决策
- [ ] 已进行完整性/一致性/可测试性检查
- [ ] 业务分析师与架构师已评审并签字

**第2阶段 - 架构设计生效**:
- [ ] SYSTEM_ARCHITECTURE.md 完整
- [ ] 每个 NFR 都有对应的架构设计
- [ ] ADR 记录了所有关键决策
- [ ] 安全审查完成，无 CRITICAL 问题
- [ ] 架构师与安全审查员已签字

**第3阶段 - 项目规划生效**:
- [ ] PLAN.md 完整，包含任务清单和甘特图
- [ ] 关键路径标记清晰
- [ ] 所有依赖关系映射
- [ ] 风险识别到位
- [ ] 规划师和项目经理已签字

**第4阶段 - 开发监控**:
- [ ] 单元测试覆盖率 > 85%
- [ ] 所有测试通过 (包括旧测试)
- [ ] 代码规范检查通过
- [ ] 无 CRITICAL/HIGH 问题
- [ ] At least one peer review per PR

**第5阶段 - 测试生效**:
- [ ] E2E 测试用例编写完整
- [ ] 所有关键用户流程测试通过
- [ ] 跨浏览器兼容性验证
- [ ] 视觉回归测试通过
- [ ] E2E 测试工程师已签字

**第6阶段 - 上线生效**:
- [ ] 灰度部署成功
- [ ] 监控指标正常
- [ ] 用户反馈无误
- [ ] 回滚预案已验证
- [ ] 部署工程师已签字

---

## 变更与变异

### 需求变更流程

```
需求变更请求
    │
    ▼
影响分析 (与架构师/开发团队协作)
    │
    ├─► CRITICAL (影响 Core 功能)
    │   └─► 必须评审通过
    │
    ├─► MAJOR (影响现有设计)
    │   └─► 评审通过，更新 CONTEXT.md
    │
    └─► MINOR (仅影响单个需求)
        └─► 记录日志，快速审批
    │
    ▼
变更审批 (业务价值 vs 成本)
    │
    ├─► 批准 → 更新需求文档 → 触发 replan
    │
    └─► 拒绝 → 标记为 DF-* (延迟决策)
```

### 架构变更流程

```
架构变更提案
    │
    ▼
创建新 ADR 记录变更
    │
    ▼
与利益相关者评审 (开发团队、架构师、安全)
    │
    ├─► 批准 → 更新 SYSTEM_ARCHITECTURE.md
    │
    └─► 拒绝 → 记录决策理由
```

---

## 故障排查

### 常见问题

**Q: 某个 Agent 阻塞了，后续工作怎么办?**

A: 创建 Deferred Decision (DF-*) 并继续:
```markdown
### DF-01: 缓存策略细节
- 预计解决: 开发阶段第2周
- 临时方案: 使用默认缓存配置，后续优化
```

**Q: Agent 输出物与预期不符，怎么处理?**

A: 返工流程:
1. 在 PR review 中标记为 CHANGES_REQUESTED
2. Agent 回到上一步重新工作
3. 重新提交评审

**Q: 两个 Agent 对设计有分歧，怎么解决?**

A: 升级决策:
1. 记录双方意见
2. 约项目 Steward (通常是 Lead Architect + Product Manager) 仲裁
3. 更新 ADR 记录最终决策和仲裁理由

---

## Agent 协作最佳实践

1. **及时反馈** - 不要等到评审才提反馈，尽早沟通
2. **清晰交付物** - 输出物应该是可理解、可验证的
3. **充分文档** - 记录决策理由，便于后续维护
4. **充分测试** - 不要跳过测试阶段
5. **定期同步** - 周会梳理进度、识别阻塞

---

**文档版本**: 1.0
**最后更新**: 2026-03-27
**维护者**: Claude (Business Analyst Mode)
