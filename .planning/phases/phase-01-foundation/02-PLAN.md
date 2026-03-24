---
phase: 1
plan: 02
title: JWT 认证与安全框架
requirements_addressed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, SEC-01, SEC-02, SEC-04, SEC-05, SEC-06, SEC-08, CONFIG-01, CONFIG-02, CONFIG-04]
depends_on: [01]
wave: 2
autonomous: false
---

# Plan 1.2: JWT 认证与安全框架

## Objective

实现完整的JWT认证系统，包括Spring Security配置、JWT Token生成与验证(RSA256)、登录/登出API、Redis会话存储、密码BCrypt加密，以及登录失败锁定机制。

**Purpose:** 认证授权是系统的安全基石，必须在业务功能之前完成，确保所有API都有统一的安全防护。

**Output:**
- Spring Security 安全配置
- JWT Token 服务 (RSA256签名)
- 认证 Controller (登录/登出/刷新)
- Redis 会话管理
- 密码加密服务 (BCrypt)
- 登录失败锁定机制

---

## Context

### 认证架构 (来自 ADR-002)
```
Client (Browser)
    │ POST /auth/login
    │ {email, password}
    ▼
┌─────────────────────────────┐
│      Auth Server            │
│  ┌─────────┐ ┌─────────┐    │
│  │ BCrypt  │ │ RSA256  │    │
│  │ Verify  │ │ JWT     │    │
│  └─────────┘ └─────────┘    │
└──────────┬──────────────────┘
           ▼
┌─────────────────────────────┐
│     Redis Cluster           │
│  • Session存储              │
│  • JWT黑名单                │
│  • 登录失败计数             │
└─────────────────────────────┘
```

### Token 设计
- **Access Token**: JWT, 有效期15分钟, 包含用户ID、角色、权限
- **Refresh Token**: JWT, 有效期7天, 用于获取新的Access Token
- **签名算法**: RSA256 (2048位密钥对)

### Redis Key 设计
```
session:{userId}:{sessionId} → Session数据
jwt:blacklist:{jti} → 过期时间
login:failed:{email} → 失败计数 (TTL: 30min)
```

---

## Tasks

### Task 1: 配置 Spring Security 安全框架

**描述:** 配置Spring Security基础设置，包括安全过滤器链、密码编码器、认证管理器

**文件:**
- `backend/src/main/java/com/usermanagement/security/SecurityConfig.java`
- `backend/src/main/java/com/usermanagement/security/JwtAuthenticationEntryPoint.java`
- `backend/src/main/java/com/usermanagement/security/JwtAccessDeniedHandler.java`

**依赖:** Plan 01 完成 (UserRepository可用)

**验收标准:**
- SecurityConfig.java: 配置 `SecurityFilterChain`，定义公开路径和受保护路径
- 公开路径: `/api/v1/auth/**`, `/actuator/health`, `/h2-console/**`
- 受保护路径: 其他所有API需要认证
- 禁用CSRF (使用JWT无状态认证)
- 配置 `BCryptPasswordEncoder` (strength=12)
- JwtAuthenticationEntryPoint: 未认证时返回401 JSON响应
- JwtAccessDeniedHandler: 无权限时返回403 JSON响应

**配置示例:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
}
```

---

### Task 2: 实现 JWT Token 服务

**描述:** 创建JWT Token生成、验证、解析服务，使用RSA256非对称加密

**文件:**
- `backend/src/main/java/com/usermanagement/security/jwt/JwtTokenProvider.java`
- `backend/src/main/java/com/usermanagement/security/jwt/JwtTokenValidator.java`
- `backend/src/main/java/com/usermanagement/security/jwt/JwtAuthenticationFilter.java`
- `backend/src/main/resources/keys/app.key` (私钥)
- `backend/src/main/resources/keys/app.pub` (公钥)

**依赖:** Task 1 完成

**验收标准:**
- JwtTokenProvider.java:
  - 生成Access Token (15分钟有效期)
  - 生成Refresh Token (7天有效期)
  - Token包含claims: sub, iss, aud, iat, exp, jti, roles, permissions
  - 使用RSA私钥签名
- JwtTokenValidator.java:
  - 验证Token签名 (RSA公钥)
  - 验证Token是否过期
  - 检查Token是否在Redis黑名单
  - 从Token提取用户信息和权限
- JwtAuthenticationFilter.java:
  - 拦截请求，提取Authorization头
  - 验证Token并设置SecurityContext
  - 支持Bearer Token格式
- 密钥文件: 生成2048位RSA密钥对，添加到resources/keys目录

**Token Payload 结构:**
```json
{
  "sub": "user-uuid",
  "iss": "usermanagement",
  "aud": "usermanagement-api",
  "iat": 1711273200,
  "exp": 1711274100,
  "jti": "unique-token-id",
  "roles": ["ROLE_ADMIN", "ROLE_USER"],
  "permissions": ["user:create", "user:read"]
}
```

---

### Task 3: 实现认证服务与登录/登出 API

**描述:** 创建认证服务处理登录逻辑，以及REST API端点

**文件:**
- `backend/src/main/java/com/usermanagement/service/AuthService.java`
- `backend/src/main/java/com/usermanagement/service/AuthServiceImpl.java`
- `backend/src/main/java/com/usermanagement/web/controller/AuthController.java`
- `backend/src/main/java/com/usermanagement/web/dto/LoginRequest.java`
- `backend/src/main/java/com/usermanagement/web/dto/LoginResponse.java`
- `backend/src/main/java/com/usermanagement/web/dto/RefreshTokenRequest.java`

**依赖:** Task 1, Task 2 完成

**验收标准:**
- AuthService.java:
  - login(email, password): 验证密码，检查锁定状态，生成Token，记录登录日志
  - logout(token): 将Token加入黑名单，清除Redis会话
  - refreshToken(refreshToken): 验证Refresh Token，生成新的Token对
  - 实现登录失败计数和锁定逻辑
- AuthController.java:
  - POST `/api/v1/auth/login`: 登录，返回Token
  - POST `/api/v1/auth/logout`: 登出，需要认证
  - POST `/api/v1/auth/refresh`: 刷新Token
- DTO类: 使用Bean Validation进行输入校验

**登录流程:**
1. 验证邮箱和密码 (BCrypt比对)
2. 检查账户状态 (是否锁定、是否激活)
3. 检查登录失败次数 (Redis)
4. 生成Access Token和Refresh Token
5. 存储会话到Redis
6. 清除登录失败计数
7. 异步记录登录日志
8. 返回Token

---

### Task 4: 配置 Redis 会话与缓存

**描述:** 配置Redis连接，实现会话存储、Token黑名单、登录失败计数

**文件:**
- `backend/src/main/java/com/usermanagement/config/RedisConfig.java`
- `backend/src/main/java/com/usermanagement/service/RedisService.java`
- `backend/src/main/java/com/usermanagement/service/SessionService.java`

**依赖:** Task 3 完成

**验收标准:**
- RedisConfig.java:
  - 配置Redis连接工厂
  - 配置RedisTemplate (String, Object)
  - 配置序列化方式 (JSON)
- RedisService.java:
  - 基础Redis操作: set, get, delete, expire
  - 支持设置TTL
- SessionService.java:
  - createSession(userId, sessionId, accessTokenJti, refreshTokenJti): 创建会话
  - getSession(userId, sessionId): 获取会话
  - deleteSession(userId, sessionId): 删除会话
  - addToBlacklist(jti, expiration): 添加Token到黑名单
  - isBlacklisted(jti): 检查Token是否在黑名单
  - incrementFailedLogin(email): 增加登录失败计数
  - getFailedLoginCount(email): 获取失败计数
  - resetFailedLogin(email): 重置失败计数
  - isLocked(email): 检查账户是否锁定

**Redis Key 定义:**
```java
private static final String SESSION_PREFIX = "session:";
private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
private static final String FAILED_LOGIN_PREFIX = "login:failed:";
```

---

### Task 5: 实现密码策略与安全配置

**描述:** 实现密码复杂度验证、密码历史、密码过期策略

**文件:**
- `backend/src/main/java/com/usermanagement/service/PasswordPolicyService.java`
- `backend/src/main/java/com/usermanagement/validation/PasswordValidator.java`
- `backend/src/main/java/com/usermanagement/domain/PasswordHistory.java`
- `backend/src/main/java/com/usermanagement/repository/PasswordHistoryRepository.java`

**依赖:** Task 1 完成

**验收标准:**
- PasswordPolicyService.java:
  - validatePassword(String password): 验证密码复杂度
  - isPasswordExpired(userId): 检查密码是否过期
  - checkPasswordHistory(userId, password): 检查密码历史
- PasswordValidator.java:
  - 最小长度8位
  - 包含大小写字母
  - 包含数字
  - 包含特殊字符
  - 不能包含用户名
- PasswordHistory.java: 密码历史实体，保存用户最近12次密码哈希
- 配置类: 密码策略参数可配置 (最小长度、复杂度要求、过期天数)

**密码复杂度规则:**
```java
public boolean validatePassword(String password) {
    return password.length() >= 8 &&
           password.matches(".*[A-Z].*") &&
           password.matches(".*[a-z].*") &&
           password.matches(".*\\d.*") &&
           password.matches(".*[!@#$%^&*()].*");
}
```

---

### Task 6: 实现系统安全配置实体

**描述:** 创建系统安全配置实体，存储密码策略、登录策略等配置

**文件:**
- `backend/src/main/java/com/usermanagement/domain/SystemConfig.java`
- `backend/src/main/java/com/usermanagement/repository/SystemConfigRepository.java`
- `backend/src/main/java/com/usermanagement/service/SystemConfigService.java`

**依赖:** Task 1 完成

**验收标准:**
- SystemConfig.java:
  - key: 配置项名称
  - value: 配置值
  - description: 描述
  - category: 配置分类 (SECURITY/PASSWORD/SESSION)
- 初始化配置数据:
  - `password.minLength`: 8
  - `password.requireUppercase`: true
  - `password.requireLowercase`: true
  - `password.requireDigit`: true
  - `password.requireSpecialChar`: true
  - `password.expiryDays`: 90
  - `login.maxFailedAttempts`: 5
  - `login.lockDurationMinutes`: 30
  - `session.maxConcurrentSessions`: 5
  - `session.timeoutMinutes`: 30

---

## Verification

### 自动化验证

```bash
# 1. 编译项目
./mvnw clean compile

# 2. 运行安全相关测试
./mvnw test -Dtest="*AuthTest,*JwtTest,*SecurityTest"

# 3. 集成测试
./mvnw test -Dtest="AuthIntegrationTest"
```

### API 测试

```bash
# 登录测试
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}'

# 预期响应:
# {
#   "success": true,
#   "data": {
#     "accessToken": "eyJ...",
#     "refreshToken": "eyJ...",
#     "tokenType": "Bearer",
#     "expiresIn": 900
#   }
# }

# 访问受保护资源
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {accessToken}"
```

### 手动验证清单

- [ ] 正确凭据登录成功，返回Token
- [ ] 错误密码登录失败，增加失败计数
- [ ] 5次失败后账户锁定30分钟
- [ ] Access Token 15分钟后过期
- [ ] Refresh Token 可以获取新的Access Token
- [ ] 登出后Token加入黑名单
- [ ] 黑名单Token无法访问受保护资源
- [ ] Redis中会话数据正确存储

---

## Success Criteria

1. **登录功能**: 用户可以正确使用邮箱/密码登录，获得JWT Token
2. **Token验证**: 所有API可以正确验证JWT Token
3. **会话管理**: Redis会话存储、Token黑名单正常工作
4. **安全机制**: 登录失败锁定、密码复杂度验证有效
5. **性能要求**: 登录接口响应时间 < 100ms (P95)

---

## must_haves

### truths
- 用户可以使用邮箱/密码登录并获得JWT Token
- JWT Token使用RSA256签名，私钥服务端保存
- Access Token有效期15分钟，Refresh Token有效期7天
- 登录失败5次后账户锁定30分钟
- Token黑名单可以正确阻止已登出的Token

### artifacts
- path: "backend/src/main/java/com/usermanagement/security/SecurityConfig.java"
  provides: "Spring Security配置"
  min_lines: 80
- path: "backend/src/main/java/com/usermanagement/security/jwt/JwtTokenProvider.java"
  provides: "JWT Token生成服务"
  min_lines: 100
- path: "backend/src/main/java/com/usermanagement/web/controller/AuthController.java"
  provides: "认证API控制器"
  min_lines: 60
- path: "backend/src/main/java/com/usermanagement/service/SessionService.java"
  provides: "Redis会话管理服务"
  min_lines: 80

### key_links
- from: "AuthController.java"
  to: "AuthService.java"
  via: "依赖注入调用"
- from: "JwtAuthenticationFilter.java"
  to: "JwtTokenValidator.java"
  via: "Token验证"
- from: "AuthService.java"
  to: "RedisService.java"
  via: "会话存储"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| RSA密钥泄露 | 低 | 极高 | 密钥存储在K8s Secret，定期轮换 |
| Token劫持 | 中 | 高 | HTTPS传输，短有效期，IP绑定 |
| Redis宕机 | 中 | 高 | Redis Cluster高可用配置 |
| 暴力破解 | 中 | 中 | 登录限流，账户锁定策略 |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/02-SUMMARY.md`
