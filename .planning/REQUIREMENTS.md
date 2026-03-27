# REQUIREMENTS: 全栈用户角色权限管理系统

## v1 Requirements

### Category: USER - 用户管理

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| USER-01 | 用户 CRUD: 创建、查询、更新、删除用户 | P0 | 1 | Pending |
| USER-02 | 用户状态管理: ACTIVE/INACTIVE/PENDING/LOCKED | P0 | 1 | Pending |
| USER-03 | 用户邮箱唯一性验证 | P0 | 1 | Pending |
| USER-04 | 用户密码 BCrypt 加密存储 | P0 | 1 | Pending |
| USER-05 | 用户批量导入 (Excel/CSV) | P1 | 2 | Pending |
| USER-06 | 用户批量导出 | P1 | 2 | Pending |
| USER-07 | 用户自助注册 | P1 | 2 | Pending |
| USER-08 | 用户个人资料管理 | P0 | 1 | Pending |
| USER-09 | 用户登录历史查看 | P1 | 2 | Pending |

### Category: DEPT - 部门管理

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| DEPT-01 | 部门 CRUD: 创建、查询、更新、删除部门 | P0 | 2 | Pending |
| DEPT-02 | 部门树形结构管理 (Materialized Path) | P0 | 2 | Pending |
| DEPT-03 | 部门层级支持 1-5 级 | P0 | 2 | Pending |
| DEPT-04 | 部门编码全局唯一 | P0 | 2 | Pending |
| DEPT-05 | 部门负责人关联 | P1 | 2 | Pending |
| DEPT-06 | 部门成员列表查看 | P1 | 2 | Pending |
| DEPT-07 | 部门调整时更新用户数据权限 | P1 | 2 | Pending |

### Category: ROLE - 角色管理

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| ROLE-01 | 角色 CRUD: 创建、查询、更新、删除角色 | P0 | 1 | Pending |
| ROLE-02 | 角色代码唯一性 | P0 | 1 | Pending |
| ROLE-03 | 角色数据权限范围配置 | P0 | 2 | Pending |
| ROLE-04 | 角色继承 (可选) | P1 | 3 | Pending |
| ROLE-05 | 角色权限模板 | P1 | 2 | Pending |
| ROLE-06 | 用户角色分配 (多对多) | P0 | 1 | Pending |
| ROLE-07 | 角色变更审计日志 | P0 | 1 | Pending |

### Category: PERM - 权限管理

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| PERM-01 | 菜单权限控制 (显示/隐藏) | P0 | 1 | Pending |
| PERM-02 | 操作权限控制 (CRUD) | P0 | 1 | Pending |
| PERM-03 | 字段权限控制 (可读/可写) | P1 | 2 | Pending |
| PERM-04 | 数据权限范围控制 (ALL/DEPT/SELF) | P1 | 2 | Pending |
| PERM-05 | 权限代码唯一性 (点号分隔) | P0 | 1 | Pending |
| PERM-06 | 权限缓存 (Redis) | P0 | 1 | Pending |
| PERM-07 | 权限实时校验 | P0 | 1 | Pending |

### Category: AUTH - 认证授权

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| AUTH-01 | JWT Token 生成与验证 | P0 | 1 | Pending |
| AUTH-02 | JWT RSA256 签名 | P0 | 1 | Pending |
| AUTH-03 | Access Token (15分钟) + Refresh Token (7天) | P0 | 1 | Pending |
| AUTH-04 | 登录失败锁定 (5次失败锁定30分钟) | P0 | 1 | Pending |
| AUTH-05 | 密码策略配置 (复杂度、过期、历史) | P0 | 1 | Pending |
| AUTH-06 | 会话管理 (单用户最多5个会话) | P1 | 2 | Pending |
| AUTH-07 | OAuth2.0 第三方登录 | P1 | 2 | Pending |
| AUTH-08 | 双因素认证 TOTP | P1 | 3 | Pending |
| AUTH-09 | 双因素认证 短信/邮件 | P1 | 3 | Pending |
| AUTH-10 | 记住我功能 (30天) | P1 | 2 | Pending |

### Category: AUDIT - 审计日志

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| AUDIT-01 | 敏感操作日志记录 (AOP拦截) | P0 | 1 | Pending |
| AUDIT-02 | 登录/登出日志记录 | P0 | 1 | Pending |
| AUDIT-03 | 日志异步写入 (Kafka) | P1 | 3 | Pending |
| AUDIT-04 | 日志多维度查询筛选 | P0 | 1 | Pending |
| AUDIT-05 | 日志导出 (Excel/PDF) | P1 | 2 | Pending |
| AUDIT-06 | 日志保留策略 (3年) | P0 | 1 | Pending |
| AUDIT-07 | 异常操作实时告警 | P1 | 3 | Pending |

### Category: CONFIG - 系统配置

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| CONFIG-01 | 密码策略配置 | P0 | 1 | Pending |
| CONFIG-02 | 会话超时配置 | P0 | 1 | Pending |
| CONFIG-03 | 邮件服务配置 | P1 | 2 | Pending |
| CONFIG-04 | 登录安全策略配置 | P0 | 1 | Pending |
| CONFIG-05 | 限流阈值配置 | P1 | 2 | Pending |

### Category: PERF - 性能需求

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| PERF-01 | 登录接口响应 < 100ms | P0 | 1 | Pending |
| PERF-02 | API 平均响应 < 200ms | P0 | 1 | Pending |
| PERF-03 | 支持 10,000 并发用户 | P0 | 1 | Pending |
| PERF-04 | Redis 缓存权限信息 | P0 | 1 | Pending |
| PERF-05 | 数据库连接池优化 | P0 | 1 | Pending |
| PERF-06 | 分页查询优化 | P0 | 1 | Pending |
| PERF-07 | 审计日志异步处理 | P1 | 3 | Pending |

### Category: SEC - 安全需求

| ID | Requirement | Priority | Phase | Status |
|----|-------------|----------|-------|--------|
| SEC-01 | BCrypt 密码加密 (strength >= 12) | P0 | 1 | Pending |
| SEC-02 | HTTPS/TLS 1.3 传输加密 | P0 | 1 | Pending |
| SEC-03 | SQL 注入防护 (参数化查询) | P0 | 1 | Pending |
| SEC-04 | XSS 防护 (输入过滤+输出编码) | P0 | 1 | Pending |
| SEC-05 | CSRF 防护 | P0 | 1 | Pending |
| SEC-06 | 接口限流控制 | P0 | 1 | Pending |
| SEC-07 | 敏感数据脱敏 | P0 | 1 | Pending |
| SEC-08 | 日志完整性防篡改 | P0 | 1 | Pending |

---

## v2+ Requirements (Future)

| ID | Requirement | Priority | Target |
|----|-------------|----------|--------|
| V2-01 | SSO/SAML 集成 | P1 | v1.2 |
| V2-02 | LDAP/AD 集成 | P1 | v2.0 |
| V2-03 | API 密钥管理 | P1 | v1.2 |
| V2-04 | 权限申请审批流程 | P1 | v1.2 |
| V2-05 | 操作日志告警 | P1 | v1.2 |
| V2-06 | 国际化 (i18n) | P2 | v1.3 |
| V2-07 | 移动端适配优化 | P2 | v1.3 |
| V2-08 | AI 异常检测 | P2 | v2.0 |
| V2-09 | 智能权限推荐 | P2 | v2.0 |
| V2-10 | 多租户支持 | P2 | v2.1 |

---

## Traceability Matrix

| Requirement | Phase | Implementation | Test | Status |
|-------------|-------|----------------|------|--------|
| USER-01 | Phase 1 | TBD | TBD | Pending |
| USER-02 | Phase 1 | TBD | TBD | Pending |
| USER-03 | Phase 1 | TBD | TBD | Pending |
| USER-04 | Phase 1 | TBD | TBD | Pending |
| USER-05 | Phase 2 | TBD | TBD | Pending |
| USER-06 | Phase 2 | TBD | TBD | Pending |
| USER-07 | Phase 2 | TBD | TBD | Pending |
| USER-08 | Phase 1 | TBD | TBD | Pending |
| USER-09 | Phase 2 | TBD | TBD | Pending |
| DEPT-01 | Phase 2 | TBD | TBD | Pending |
| DEPT-02 | Phase 2 | TBD | TBD | Pending |
| DEPT-03 | Phase 2 | TBD | TBD | Pending |
| DEPT-04 | Phase 2 | TBD | TBD | Pending |
| ROLE-01 | Phase 1 | TBD | TBD | Pending |
| ROLE-02 | Phase 1 | TBD | TBD | Pending |
| ROLE-03 | Phase 2 | TBD | TBD | Pending |
| ROLE-06 | Phase 1 | TBD | TBD | Pending |
| ROLE-07 | Phase 1 | TBD | TBD | Pending |
| PERM-01 | Phase 1 | TBD | TBD | Pending |
| PERM-02 | Phase 1 | TBD | TBD | Pending |
| PERM-03 | Phase 2 | TBD | TBD | Pending |
| PERM-04 | Phase 2 | TBD | TBD | Pending |
| PERM-05 | Phase 1 | TBD | TBD | Pending |
| PERM-06 | Phase 1 | TBD | TBD | Pending |
| PERM-07 | Phase 1 | TBD | TBD | Pending |
| AUTH-01 | Phase 1 | TBD | TBD | Pending |
| AUTH-02 | Phase 1 | TBD | TBD | Pending |
| AUTH-03 | Phase 1 | TBD | TBD | Pending |
| AUTH-04 | Phase 1 | TBD | TBD | Pending |
| AUTH-05 | Phase 1 | TBD | TBD | Pending |
| AUTH-06 | Phase 2 | TBD | TBD | Pending |
| AUTH-07 | Phase 2 | TBD | TBD | Pending |
| AUTH-08 | Phase 3 | TBD | TBD | Pending |
| AUTH-09 | Phase 3 | TBD | TBD | Pending |
| AUDIT-01 | Phase 1 | TBD | TBD | Pending |
| AUDIT-02 | Phase 1 | TBD | TBD | Pending |
| AUDIT-03 | Phase 3 | TBD | TBD | Pending |
| AUDIT-04 | Phase 1 | TBD | TBD | Pending |
| AUDIT-05 | Phase 2 | TBD | TBD | Pending |
| AUDIT-06 | Phase 1 | TBD | TBD | Pending |
| AUDIT-07 | Phase 3 | TBD | TBD | Pending |

---

## Summary

| Category | v1 Count | v2+ Count | Total |
|----------|----------|-----------|-------|
| USER | 9 | 0 | 9 |
| DEPT | 7 | 0 | 7 |
| ROLE | 7 | 0 | 7 |
| PERM | 7 | 0 | 7 |
| AUTH | 10 | 0 | 10 |
| AUDIT | 7 | 0 | 7 |
| CONFIG | 5 | 0 | 5 |
| PERF | 7 | 0 | 7 |
| SEC | 8 | 0 | 8 |
| **Total** | **67** | **10** | **77** |

**v1 Requirements Mapped**: 67/67 (100%)
