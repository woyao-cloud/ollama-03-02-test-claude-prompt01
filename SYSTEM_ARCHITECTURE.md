# 系统架构设计

## 1. 整体架构概览

### 1.1 架构风格

采用**分层架构 (Layered Architecture)** 结合 **六边形架构 (Hexagonal Architecture / Ports and Adapters)** 的混合风格：

- **分层架构**确保关注点分离
- **六边形架构**确保核心业务逻辑独立于框架和外部依赖
- **依赖关系**: 外层 → 内层 (Domain 是核心)

### 1.2 系统边界

```
┌─────────────────────────────────────────────────────────────┐
│                      外部系统边界                             │
│  ┌──────────────┐                              ┌─────────┐ │
│  │   Web 客户端  │                              │ 移动端  │ │
│  │  (Next.js)   │                              │ (未来)  │ │
│  └──────┬───────┘                              └────┬────┘ │
│         │                                           │      │
│         └──────────────────┬────────────────────────┘      │
│                            │                               │
│  ┌─────────────────────────▼─────────────────────────────┐ │
│  │                    API 网关层                          │ │
│  │              (Nginx / Traefik / Kong)                 │ │
│  │  • SSL 终止  • 负载均衡  • 速率限制  • WAF              │ │
│  └─────────────────────────┬─────────────────────────────┘ │
│                            │                               │
└────────────────────────────┼───────────────────────────────┘
                             │
┌────────────────────────────┼───────────────────────────────┐
│                      应用系统边界                            │
│  ┌─────────────────────────▼─────────────────────────────┐ │
│  │                  Spring Boot 后端                      │ │
│  │  ┌──────────┬──────────┬──────────┬──────────────┐   │ │
│  │  │ REST API │ Security │ Business │   Domain     │   │ │
│  │  │  Layer   │  Layer   │ Service  │   Entities   │   │ │
│  │  └──────────┴──────────┴──────────┴──────────────┘   │ │
│  └─────────────────────────┬─────────────────────────────┘ │
│                            │                               │
│  ┌─────────────────────────▼─────────────────────────────┐ │
│  │              PostgreSQL 数据库主从                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 架构组件图

```
                    ┌─────────────────────────────────────┐
                    │           前端 (Next.js)             │
                    │  ┌─────────┐ ┌─────────┐ ┌────────┐ │
                    │  │  Pages  │ │Components│ │ Store  │ │
                    │  └────┬────┘ └────┬────┘ └───┬────┘ │
                    │       └───────────┴──────────┘       │
                    │                   │                   │
                    └───────────────────┼───────────────────┘
                                        │ HTTPS/JSON
                    ┌───────────────────▼───────────────────┐
                    │         API Gateway (Nginx)           │
                    │      • SSL • Rate Limit • Routing     │
                    └───────────────────┬───────────────────┘
                                        │
┌───────────────────────────────────────▼───────────────────────────────┐
│                        Spring Boot Application                        │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     Presentation Layer                          │ │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐│ │
│  │  │AuthController│ │UserController│ │   Exception Handler      ││ │
│  │  └──────────────┘ └──────────────┘ └──────────────────────────┘│ │
│  │                         │                                       │ │
│  │                         ▼ DTOs / Records                        │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │                    Application Layer                        ││ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────────────────┐  ││ │
│  │  │  │AuthService │ │UserService │ │     RoleService        │  ││ │
│  │  │  └────────────┘ └────────────┘ └────────────────────────┘  ││ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────────────────┐  ││ │
│  │  │  │JwtService  │ │Permission  │ │   AuditService         │  ││ │
│  │  │  └────────────┘ └────────────┘ └────────────────────────┘  ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  │                         │                                       │ │
│  │                         ▼ Entities                               │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │                      Domain Layer                           ││ │
│  │  │  ┌────────┐ ┌────────┐ ┌────────────┐ ┌─────────────────┐  ││ │
│  │  │  │  User  │ │  Role  │ │ Permission │ │   AuditLog      │  ││ │
│  │  │  └────────┘ └────────┘ └────────────┘ └─────────────────┘  ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  │                         │                                       │ │
│  │                         ▼                                        │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │                   Infrastructure Layer                      ││ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────────────────┐  ││ │
│  │  │  │UserRepository│ │RoleRepository│ │ PermissionRepository  │  ││ │
│  │  │  └────────────┘ └────────────┘ └────────────────────────┘  ││ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────────────────┐  ││ │
│  │  │  │JwtTokenProvider│ │SecurityConfig │ │   Flyway Config    │  ││ │
│  │  │  └────────────┘ └────────────┘ └────────────────────────┘  ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  └─────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────┬───────────────────────────────┘
                                        │ JPA / JDBC
                    ┌───────────────────▼───────────────────┐
                    │         PostgreSQL Database           │
                    │  ┌─────────┐ ┌─────────┐ ┌─────────┐ │
                    │  │  users  │ │  roles  │ │ audit_  │ │
                    │  └─────────┘ └─────────┘ └─────────┘ │
                    └───────────────────────────────────────┘
```

---

## 2. 分层架构详解

### 2.1 表现层 (Presentation Layer)

**职责**: HTTP 请求处理、输入验证、DTO 转换、响应封装

**组件**:
- REST Controllers (`AuthController`, `UserController`, etc.)
- DTOs/Records (Request/Response objects)
- Global Exception Handler
- Request Validators

**设计原则**:
- 控制器无业务逻辑，仅协调调用
- 使用 Java Records 定义 DTOs (JDK 21 特性)
- Jakarta Bean Validation 用于输入验证
- RESTful API 设计遵循 HTTP 语义

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }
}
```

### 2.2 应用层 (Application Layer)

**职责**: 业务逻辑编排、事务管理、用例实现、领域事件发布

**组件**:
- Application Services (`UserService`, `AuthService`, etc.)
- Use Case implementations
- Transaction boundaries (`@Transactional`)
- Application Events

**设计原则**:
- 一个用例对应一个服务方法
- 声明式事务管理
- 不直接访问数据库，通过 Repository
- 处理跨领域逻辑

```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 业务规则验证
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        // 创建领域实体
        User user = User.create(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.firstName(),
            request.lastName()
        );

        // 持久化
        User saved = userRepository.save(user);

        // 发布领域事件
        eventPublisher.publishEvent(new UserCreatedEvent(saved.getId()));

        return UserResponse.from(saved);
    }
}
```

### 2.3 领域层 (Domain Layer)

**职责**: 核心业务实体、业务规则、领域事件、值对象

**组件**:
- Domain Entities (`User`, `Role`, `Permission`)
- Value Objects
- Domain Events
- Domain Services (当逻辑不适合放在实体时)

**设计原则**:
- 富领域模型 (Rich Domain Model)
- 业务逻辑封装在实体中
- 使用 JPA 注解映射数据库
- 保持与框架无关 (尽可能)

```java
@Entity
@Table(name = "users")
public class User extends AbstractAuditableEntity {
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", ...)
    private Set<Role> roles = new HashSet<>();

    // 业务方法
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }

    public boolean hasPermission(String permission) {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(p -> p.getName().equals(permission));
    }
}
```

### 2.4 基础设施层 (Infrastructure Layer)

**职责**: 技术细节实现、外部系统集成、框架配置

**组件**:
- Spring Data Repositories
- Security Configuration
- Database Configuration
- JWT Implementation
- Flyway Migrations

**设计原则**:
- 实现领域层定义的接口
- 框架特定的代码在此层
- 可替换的适配器模式

---

## 3. 组件交互流程

### 3.1 用户注册流程

```
Client                                    Backend
  |                                          |
  | POST /api/v1/auth/register               |
  | {username, email, password}               |
  |─────────────────────────────────────────>|
  |                                          |
  |    AuthController.register()             |
  |         │                                |
  |         ▼                                |
  |    @Valid LoginRequest                   |
  |    (Jakarta Validation)                  |
  |         │                                |
  |         ▼                                |
  |    AuthService.register()                |
  |         │                                |
  |         ├─ UserRepository.existsByEmail()│
  |         │         │                      |
  |         │         ▼                      |
  |         │    PostgreSQL (SELECT)         │
  |         │         │                      |
  |         │         ▼                      |
  |         │    false (not exists)          │
  |         │                                |
  |         ├─ User.create()                 │
  |         │    (Domain Entity)             │
  |         │                                |
  |         ├─ PasswordEncoder.encode()      │
  |         │    (BCrypt)                    │
  |         │                                |
  |         ├─ UserRepository.save()         │
  |         │         │                      |
  |         │         ▼                      |
  |         │    PostgreSQL (INSERT)         │
  |         │                                |
  |         ▼                                |
  |    UserResponse.from(user)               |
  |    (Record mapping)                      |
  |         │                                |
  |         ▼                                |
  |    ApiResponse.success(dto)              |
  |         │                                |
  |<─────────────────────────────────────────|
  | 201 Created                              |
  | {data: {id, username, email}}            |
```

### 3.2 用户认证流程 (JWT)

```
Client                                    Backend
  |                                          |
  | POST /api/v1/auth/login                  |
  | {email, password}                        |
  |─────────────────────────────────────────>|
  |                                          |
  |    AuthController.login()                |
  |         │                                |
  |         ▼                                |
  |    AuthService.login()                   |
  |         │                                |
  |         ├─ UserRepository.findByEmail()  │
  |         │         │                      |
  |         │         ▼                      |
  |         │    PostgreSQL (SELECT)         │
  |         │                                |
  |         ├─ PasswordEncoder.matches()     │
  |         │    (BCrypt check)              │
  |         │                                |
  |         ├─ JwtTokenProvider.generate()   │
  |         │    ┌───────────────────────┐   │
  |         │    │ • Create JWT Claims   │   │
  |         │    │ • Sign with RSA Key   │   │
  |         │    │ • Set Expiration      │   │
  |         │    └───────────────────────┘   │
  |         │                                |
  |         ├─ User.updateLastLogin()        │
  |         │                                |
  |         ├─ UserSessionRepository.save()  │
  |         │    (Session tracking)          │
  |         │                                |
  |         ▼                                |
  |    TokenResponse(accessToken,            │
  |                    refreshToken)         │
  |         │                                |
  |<─────────────────────────────────────────|
  | 200 OK                                   |
  | {data: {accessToken, refreshToken}}      |
```

### 3.3 受保护资源访问流程

```
Client                                    Backend
  |                                          |
  | GET /api/v1/users/me                     |
  | Authorization: Bearer <token>            |
  |─────────────────────────────────────────>|
  |                                          |
  |    JwtAuthenticationFilter               |
  |    (OncePerRequestFilter)                |
  |         │                                |
  |         ├─ Extract Bearer Token          │
  |         ├─ JwtTokenProvider.validate()   │
  |         │    ┌───────────────────────┐   │
  |         │    │ • Parse JWT           │   │
  |         │    │ • Verify Signature    │   │
  |         │    │ • Check Expiration    │   │
  |         │    └───────────────────────┘   │
  |         ├─ Load UserDetails              │
  |         ├─ Create Authentication         │
  |         ├─ Set SecurityContext           │
  |         │                                |
  |         ▼                                |
  |    UserController.getCurrentUser()       |
  |         │                                |
  |         ├─ @PreAuthorize("isAuthenticated()")
  |         │    (Method Security)           │
  |         │                                |
  |         ├─ SecurityContextHolder         │
  |         │    .getContext()               │
  |         │    .getAuthentication()        │
  |         │                                |
  |         ├─ UserService.findById()        │
  |         │                                |
  |         ▼                                |
  |    UserResponse.from(user)               │
  |         │                                |
  |<─────────────────────────────────────────|
  | 200 OK                                   |
  | {data: {id, username, email, ...}}       |
```

---

## 4. 数据模型设计

### 4.1 实体关系图 (ER Diagram)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │ user_roles   │       │    roles     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ PK id        │◄──────┤ PK user_id   │       │ PK id        │
│    username  │       │ FK role_id   │──────►│    name      │
│    email     │       │    created_at│       │    desc      │
│    password  │       └──────────────┘       │    is_default│
│    is_active │                              └──────┬───────┘
│    created_at│                                     │
└──────────────┘                                     │
       │                                             │
       │    ┌──────────────┐       ┌──────────────┐ │
       │    │ user_sessions│       │ role_perm    │ │
       │    ├──────────────┤       ├──────────────┤ │
       └───►│ FK user_id   │       │ PK role_id   │◄┘
            │    token     │       │ FK perm_id   │
            │    expires_at│       └──────────────┘
            │    created_at│              │
            └──────────────┘              │
                                          ▼
                                   ┌──────────────┐
                                   │  permissions │
                                   ├──────────────┤
                                   │ PK id        │
                                   │    name      │
                                   │    resource  │
                                   │    action    │
                                   └──────────────┘
```

### 4.2 聚合边界

| 聚合 | 根实体 | 包含实体 | 业务规则 |
|------|--------|----------|----------|
| User | User | UserSession | 用户名唯一、邮箱唯一、密码强度 |
| Role | Role | Permission (关联) | 角色名唯一、默认角色只能有一个 |
| Audit | AuditLog | - | 自动记录创建时间、不可修改 |

---

## 5. 集成点设计

### 5.1 前后端集成

**API 契约**:
- Base URL: `/api/v1`
- Content-Type: `application/json`
- Authentication: `Authorization: Bearer {token}`

**CORS 配置**:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### 5.2 数据库集成

**JPA 配置**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 仅验证，不生成
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

**连接池 (HikariCP)**:
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      pool-name: UserManagementHikariPool
```

### 5.3 外部服务集成 (未来扩展)

| 服务 | 用途 | 集成方式 |
|------|------|----------|
| Redis | 缓存、速率限制 | Spring Data Redis |
| RabbitMQ/Kafka | 异步事件 | Spring Cloud Stream |
| AWS S3 | 文件存储 | AWS SDK |
| SendGrid | 邮件服务 | REST API |

---

## 6. 非功能性需求设计

### 6.1 性能设计

| 指标 | 目标 | 实现策略 |
|------|------|----------|
| API 响应时间 P95 | < 200ms | 数据库索引、连接池、JVM 调优 |
| 并发用户数 | 1000+ | 虚拟线程 (JDK 21)、水平扩展 |
| 数据库查询 | < 50ms | 索引优化、查询优化、EntityGraph |
| 启动时间 | < 30s | 懒加载、AOT 编译 (未来) |

### 6.2 可扩展性设计

- **水平扩展**: 无状态应用设计，支持多实例
- **数据库分片**: 预留用户ID分片能力
- **缓存层**: 预留 Redis 集成点
- **事件驱动**: 预留消息队列集成

### 6.3 可观测性设计

**日志**:
- 结构化日志 (JSON 格式)
- 请求追踪 ID (Correlation ID)
- 分级日志: ERROR, WARN, INFO, DEBUG

**指标** (Micrometer + Prometheus):
- JVM 内存、GC、线程
- HTTP 请求数、响应时间、错误率
- 数据库连接池状态
- 自定义业务指标

**追踪** (可选):
- OpenTelemetry / Jaeger 分布式追踪
- 跨服务调用链路

---

## 7. 技术约束与假设

### 7.1 约束条件

- **JDK 21**: 必须使用虚拟线程特性
- **Spring Boot 3.5**: 依赖 Jakarta EE 命名空间
- **PostgreSQL 14+**: 支持 JSONB、UUID 等特性
- **无状态设计**: 不支持 Session 亲和性

### 7.2 假设条件

- 网络延迟 < 100ms (同数据中心)
- 前端使用现代浏览器 (ES2020+)
- 数据库主从复制延迟 < 1s
- JWT 令牌时钟偏差 < 60s

---

## 8. 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| JPA N+1 查询问题 | 中 | 高 | 使用 EntityGraph、批量抓取 |
| JWT 令牌泄露 | 低 | 高 | 短期令牌、刷新机制、黑名单 |
| 数据库连接池耗尽 | 中 | 高 | 监控告警、连接池调优 |
| 并发更新冲突 | 中 | 中 | 乐观锁 (@Version)、重试 |
| 内存泄漏 | 低 | 高 | 定期 Heap Dump 分析 |

---

## 9. 附录

### 9.1 相关文档

- [BACKEND_ARCHITECTURE.md](./BACKEND_ARCHITECTURE.md) - 后端详细设计
- [API_SPECIFICATION.md](./API_SPECIFICATION.md) - API 接口规范
- [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) - 数据库设计
- [AUTHENTICATION_FLOW.md](./AUTHENTICATION_FLOW.md) - 认证流程

### 9.2 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2024-03-23 | 架构师 | 初始版本，Spring Boot 3.5 + JDK 21 |
