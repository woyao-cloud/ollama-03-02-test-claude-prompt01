# 提示词文件索引

> **单一事实源（SoT）说明**: 所有 Agent 提示词的权威存储位置为 `~/.claude/agents/`，本目录作为项目级备份与元数据记录。

## 快速导航

- [全局 Agent 提示词](#全局-agent-提示词) - 可在任何项目中调用
- [项目专用提示词](#项目专用提示词) - 本项目的额外提示词定义
- [协作规范](#协作规范) - Agent 交接与触发机制
- [提示词元数据](#提示词元数据) - 版本控制与维护信息

---

## 全局 Agent 提示词

**位置**: `~/.claude/agents/`

### 核心业务 Agent

#### 1. business-analyst.md
- **状态**: ✅ Active (v2.0)
- **角色**: 业务分析师
- **职责**: 需求梳理、FRD/NFRD 编写、用户故事定义、业务流程建模
- **工作流**: 
  1. 需求发现（Discovery）
  2. 需求分析（Analysis）
  3. 需求文档化（Documentation）
  4. 需求验证（Validation）
  5. Agent 交接（Handoff）
- **输出物**:
  - `docs/requirements/FUNCTIONAL_REQUIREMENTS.md`
  - `docs/requirements/NON_FUNCTIONAL_REQUIREMENTS.md`
  - `docs/requirements/USER_STORIES.md`
  - `docs/requirements/CONTEXT.md` （包含用户决策）
- **触发时机**: 产品需求明确后
- **下游Agent**: 
  - `architect` - 技术架构设计
  - `security-reviewer` - 安全需求审查
  - `gsd-planner` - 项目规划
- **特点**: 
  - 包含完整的模板库（用户故事、用例、NFR 模板）
  - 质量检查清单（完整性、一致性、可验证性、可追溯性）
  - 需求验证闭环机制（UAT 反向验证、需求变更流程、RTM 追溯）

#### 2. architect.md ⭐ NEW
- **状态**: ✅ Active (v1.0)
- **角色**: 系统架构师
- **职责**: 技术架构设计、技术选型评估、架构决策记录
- **工作流**:
  1. 需求理解 - 读取 FRD/NFRD/CONTEXT.md
  2. 技术选型评估 - 评估替代方案
  3. 架构设计 - 高层设计 + 详细设计
  4. 架构决策文档化 - ADR 记录
- **输出物**:
  - `docs/architecture/SYSTEM_ARCHITECTURE.md`
  - `docs/architecture/adr/ADR-*.md` (架构决策记录)
  - `docs/architecture/TECHNICAL_CONSTRAINTS.md`
- **触发时机**: FRD + NFRD 完成后
- **前置条件**: business-analyst 输出的需求文档
- **后续Agent**: `gsd-planner` - 基于架构制定实施计划
- **特点**:
  - 4 阶段架构设计流程
  - ADR 模板与决策记录
  - 技术选型评估框架

### 开发工作 Agent

#### 3. tdd-guide.md ⭐ NEW
- **状态**: ✅ Active (v1.0)
- **角色**: TDD 引导者
- **职责**: 测试驱动开发指导、测试策略制定、测试实现指导
- **工作流**: RED-GREEN-REFACTOR 循环
  1. RED - 编写失败的测试
  2. GREEN - 实现代码使测试通过
  3. REFACTOR - 代码重构优化
- **输出物**:
  - 测试代码（单元/集成/端到端）
  - 测试覆盖率报告（目标: >85%）
- **触发时机**: 设计完成，开发开始前
- **分层指导**:
  - Controller 层：集成测试 (@WebMvcTest)
  - Service 层：单元测试 (Mock)
  - Repository 层：@DataJpaTest
  - Security 层：安全相关测试
- **特点**:
  - FIRST 原则检查清单
  - Mock 正确性验证
  - Anti-Patterns 避免指南
- **与 code-reviewer 协作**: 代码审查时检查测试质量

#### 4. code-reviewer.md ⭐ NEW
- **状态**: ✅ Active (v1.0)
- **角色**: 代码审查员
- **职责**: 代码质量审查（功能/性能/安全/质量/架构）
- **审查维度**:
  1. **功能正确性**: 代码逻辑、业务规则实现
  2. **性能**: 算法复杂度、数据库查询优化
  3. **安全性**: OWASP Top 10、输入验证、加密
  4. **代码质量**: 可读性、可维护性、约定
  5. **架构遵循**: 设计模式、分层原则
- **输出物**: 审查报告 (CRITICAL/HIGH/MEDIUM/LOW)
- **Java 检查清单**: 
  - 异常处理
  - 资源管理
  - 线程安全
  - 性能陷阱
- **TypeScript/React 检查清单**: 
  - 类型安全
  - Hook 规则
  - 组件性能
  - 状态管理

#### 5. security-reviewer.md ⭐ NEW
- **状态**: ✅ Active (v1.0)
- **角色**: 安全审查员
- **职责**: 安全审查、漏洞扫描、合规检查
- **覆盖范围**: OWASP Top 10
  - SQL 注入防护
  - XSS 防护
  - CSRF 防护
  - 认证授权
  - 敏感数据保护
  - 加密传输
- **触发时机**:
  - 识别安全相关需求后（business-analyst 输出）
  - 核心安全代码完成后
  - 上线前最终审查
- **输出物**: 
  - 安全审查报告
  - 漏洞清单 + 修复建议
- **CRITICAL 问题阻断**: 发现 CRITICAL 级漏洞直接阻止合并

#### 6. e2e-runner.md ⭐ NEW
- **状态**: ✅ Active (v1.0)
- **角色**: E2E 测试工程师
- **职责**: 端到端测试实现、跨浏览器测试、视觉回归测试
- **测试框架**: Playwright
- **测试范围**:
  - 关键用户流程（登录、CRUD、完整工作流）
  - 跨浏览器验证（Chrome/Firefox/Safari）
  - 移动端响应式测试
  - 视觉回归测试（截图对比）
- **输出物**:
  - `frontend/e2e/specs/**/*.spec.ts` - E2E 测试用例
  - `frontend/e2e/pages/*.ts` - Page Object Model
  - E2E 测试报告 (HTML/JSON/JUnit)
- **特点**:
  - Page Object Model 模式
  - API 辅助函数 (setup/teardown)
  - CI/CD 集成配置 (GitHub Actions)

### 规划工作 Agent

#### 7. gsd-planner.md
- **状态**: ✅ Active (v1.0+)
- **角色**: 项目规划师
- **职责**: 项目规划、任务拆分、里程碑制定、进度跟踪
- **工作流**:
  1. 目标回溯 - 从最终目标反推任务
  2. 依赖分析 - 识别关键路径
  3. 并行优化 - 数据驱动的并行度估算
  4. 动态调整 - 基于进度反馈调整
- **输出物**: `PLAN.md` 可执行计划
- **扩展性**: 支持分阶段规划（Phase 1-4）
- **文件大小**: ~1300+ 行（后续考虑拆分模块化）
- **特点**:
  - 目标回溯、依赖分析、并行优化
  - Mermaid 甘特图支持
  - 风险识别与缓解

#### 8. gsd-executor.md
- **状态**: ✅ Active
- **角色**: 执行者
- **职责**: 任务执行脚手架、进度跟踪
- **与 tdd-guide 协作**: 执行开发任务

### 全局规则与工程规范

**位置**: `~/.claude/rules/`

| 文件 | 内容 |
|------|------|
| agents.md | Agent 协作指南、可用 Agent 列表、触发机制 |
| coding-style.md | 代码规范（不变性原则、文件组织、错误处理） |
| development-workflow.md | 开发工作流（研究→规划→TDD→审查→提交） |
| testing.md | 测试要求（80%+ 覆盖率、TDD 流程） |
| security.md | 安全检查清单、Secret 管理、应急响应 |
| performance.md | 性能优化策略、模型选择、Context 管理 |
| git-workflow.md | Git 提交规范、PR 流程 |

---

## 项目专用提示词

### 项目配置
- **AGENTS.md** - 多代理协作配置（角色定义表、提示词位置索引）
- **CLAUDE.md** - 项目级 Claude 配置（技术栈、质量指标）

---

## 协作规范

### Agent 触发时序图

```
需求阶段                  设计阶段                  开发阶段               测试阶段                 部署阶段
      │                         │                      │                   │                        │
business-analyst ──────▶ architect ──────▶ (backend/frontend) ──────▶ tdd-guide ──────▶ code-reviewer ──────▶ deploy
                                   │                      │                   │                        │
                                   ▼                      │                   ▼                        │
                            security-reviewer ◀──── ──── ┴─────────────────────────────────────────┘
                                                  │
                                                  ▼
                                            e2e-runner ◀─────────────────┘
                                                  │
                                                  ▼
                                            gsd-executor (监控进度)
```

### 交接机制

| 前置 Agent | 输出物 | 触发后置 Agent | 触发条件 |
|-----------|--------|----------------|--------|
| business-analyst | FRD, NFRD, CONTEXT.md | architect | 需求文档通过评审 |
| architect | SYSTEM_ARCHITECTURE.md, ADR | gsd-planner | 架构设计完成 |
| gsd-planner | PLAN.md | tdd-guide + gsd-executor | 计划通过评审 |
| tdd-guide | 测试代码 (覆盖率>85%) | code-reviewer | 测试编写完成 |
| code-reviewer | 质量报告 (无CRITICAL) | 下一步 (部署/修改) | 代码审查通过 |
| security-reviewer | 安全报告 | - | 发现问题直接阻断 |
| e2e-runner | E2E 报告 | - | 测试通过后确认 |

### 输出标准

#### CONTEXT.md 格式
所有提示词的交接文件应包含：
```markdown
# 需求上下文与决策

## 项目信息
- 项目名称
- 版本
- 日期

## 用户决策（Locked/Deferred/Discretion）
### D-01: [决策标题]
- 决策: [内容]
- 原因: [用户原因]
- 影响: [对后续工作的影响]

## 关键需求摘要
### Must Have / Should Have / Could Have

## 非功能需求关键指标
| 类别 | 指标 | 值 |
|------|------|-----|
| 性能 | 响应时间 P95 | < 200ms |

## 参考文档
- FRD: ./FUNCTIONAL_REQUIREMENTS.md
- NFRD: ./NON_FUNCTIONAL_REQUIREMENTS.md
```

---

## 提示词元数据

### 版本管理

| Agent | 当前版本 | 上次更新 | 维护者 | 状态 |
|-------|--------|--------|-------|------|
| business-analyst | 2.0 | 2026-03-27 | @claude | Active |
| architect | 1.0 | 2026-03-27 | @claude | Active ⭐ |
| tdd-guide | 1.0 | 2026-03-27 | @claude | Active ⭐ |
| code-reviewer | 1.0 | 2026-03-27 | @claude | Active ⭐ |
| security-reviewer | 1.0 | 2026-03-27 | @claude | Active ⭐ |
| e2e-runner | 1.0 | 2026-03-27 | @claude | Active ⭐ |
| gsd-planner | 1.0+ | 2026-03-27 | @claude | Active (需模块化) |
| gsd-executor | 1.0 | 2026-03-27 | @claude | Active |

### 维护和优化计划

#### 短期（1 个月内）
- [ ] 为 gsd-planner 模块化拆分（Planning/Estimation/Roadmap 子模块）
- [ ] 建立 docs/templates/ 目录存放标准模板
- [ ] 创建 PROMPT_STANDARDS.md - 规范提示词格式

#### 中期（3 个月内）
- [ ] 为每个提示词添加 YAML frontmatter 元数据
- [ ] 创建自动检则脚本 (scripts/check-prompt-duplicates.sh)
- [ ] 添加 CI 门禁 - 提示词质量检查

#### 长期（半年内）
- [ ] 建立提示词变更评审流程（PR + reviewers）
- [ ] 按领域拆分提示词（Domain-specific prompts）
- [ ] 实现版本历史追踪（git tags 标记版本）

---

## 常见问题

### Q: 如何调用特定 Agent？
```bash
claude agent business-analyst
claude agent architect
claude agent tdd-guide
# ... 等
```

### Q: 如何找到 Agent 的详细提示词？
位置: `~/.claude/agents/{agent-name}.md`

本索引文件提供了快速参考，详细提示词见全局存储位置。

### Q: 提示词更新如何同步？
1. 全局 Agent 提示词：直接编辑 `~/.claude/agents/*.md`
2. 项目配置：编辑项目的 AGENTS.md、CLAUDE.md
3. 本索引：记录元数据变更，不含实现细节

### Q: 如何判断哪个 Agent 应该处理我的任务？
- **需求明确** → business-analyst
- **技术设计** → architect
- **项目规划** → gsd-planner
- **编码开发** → gsd-executor + tdd-guide
- **代码质量** → code-reviewer
- **安全审查** → security-reviewer
- **端到端测试** → e2e-runner

---

## 参考资源

- [Agent 协作详解](AGENT_COLLABORATION.md) - 待创建
- [项目质量门禁](../QUALITY_GATES.md) - 待创建
- [交付物清单](../DELIVERABLES.md) - 待创建
- [开发工作流程](../DEVELOPMENT_WORKFLOW.md)
- [测试策略](../TESTING_STRATEGY.md)
- [项目 AGENTS.md](../AGENTS.md)

---

**上次更新**: 2026-03-27
**维护者**: Claude (Business Analyst Mode)
**版本**: 1.0
