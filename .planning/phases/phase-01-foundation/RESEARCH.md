# Phase 1 技术研究

## 研究范围

本研究覆盖Phase 1 (Foundation) 所需的技术选型、最佳实践和实现细节。

---

## 1. 数据库与持久层

### 1.1 JPA与Spring Data JPA

**版本选择:** Spring Data JPA 3.5 (与Spring Boot 3.5一致)

**关键决策:**
| 决策点 | 选择 | 理由 |
|--------|------|------|
| 主键生成 | UUID v4 | 分布式友好，不可预测 |
| 乐观锁 | @Version | 并发更新保护 |
| 审计字段 | @CreatedDate/@LastModifiedDate | 自动维护时间戳 |
| 软删除 | deleted_at字段 | 保留审计数据 |
| 关联加载 | LAZY | 默认懒加载，避免N+1 |

**性能优化:**
- 使用`@BatchSize`优化关联查询
- 使用`@Fetch(FetchMode.SUBSELECT)`优化集合加载
- 使用`EntityGraph`优化特定查询

### 1.2 Flyway迁移

**版本:** Flyway 10.x

**最佳实践:**
- 使用语义化版本号: V1__init.sql, V1.1__add_index.sql
- 每个迁移脚本只包含一个逻辑变更
- 从不修改已执行的迁移脚本
- 生产环境使用`validate-on-migrate: true`

**H2兼容性:**
- H2 2.x支持PostgreSQL兼容模式
- 分区表语法在H2中需要特殊处理
- 开发环境使用普通表代替分区表

---

## 2. 认证与安全

### 2.1 JWT实现

**库选择:** `io.jsonwebtoken:jjwt` 0.12.x

**JJWT配置:**
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
</dependency>
```

**RSA密钥生成:**
```bash
# 生成私钥
openssl genrsa -out app.key 2048

# 生成公钥
openssl rsa -in app.key -pubout -out app.pub
```

### 2.2 Spring Security配置

**关键配置点:**
- 禁用CSRF (无状态JWT认证)
- 禁用Session (STATELESS)
- 配置CORS (跨域)
- 配置自定义AuthenticationEntryPoint

**BCrypt配置:**
```java
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // strength=12
}
```

### 2.3 Redis会话管理

**Key设计:**
```
session:{userId}:{sessionId} → Hash
jwt:blacklist:{jti} → String (TTL)
login:failed:{email} → String (TTL: 30min)
user:permissions:{userId} → Set
user:roles:{userId} → Set
```

**Spring Data Redis配置:**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

## 3. API设计

### 3.1 RESTful API规范

**URL设计:**
```
GET    /api/v1/resources          # 列表 (分页)
GET    /api/v1/resources/{id}     # 详情
POST   /api/v1/resources          # 创建
PUT    /api/v1/resources/{id}     # 全量更新
PATCH  /api/v1/resources/{id}     # 部分更新
DELETE /api/v1/resources/{id}     # 删除
```

**响应格式:**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {},
  "timestamp": "2026-03-24T10:00:00Z"
}
```

### 3.2 SpringDoc OpenAPI

**依赖:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

**配置:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("用户权限管理系统 API")
                .version("1.0")
                .description("API文档"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

---

## 4. 前端技术

### 4.1 Next.js 16 App Router

**关键特性:**
- Server Components (默认)
- Client Components (按需使用 'use client')
- 路由拦截 (Parallel Routes, Intercepting Routes)
- 服务端渲染优化

**项目配置:**
```javascript
// next.config.js
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/v1/:path*',
      },
    ];
  },
};
```

### 4.2 shadcn/ui组件

**初始化:**
```bash
npx shadcn-ui@latest init
```

**基础组件安装:**
```bash
npx shadcn-ui@latest add button input label card form select dialog table dropdown-menu avatar badge sonner
```

**主题配置:**
- 使用CSS变量定义主题色
- 支持暗色/亮色模式
- 自定义颜色方案

### 4.3 Zustand状态管理

**安装:**
```bash
npm install zustand immer
```

**最佳实践:**
- 使用immer简化不可变更新
- 使用persist中间件持久化
- 按功能拆分Store
- TypeScript类型安全

---

## 5. 审计日志

### 5.1 AOP实现

**Spring AOP配置:**
```java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint point, Auditable auditable) throws Throwable {
        // 实现审计逻辑
    }
}
```

### 5.2 异步处理

**使用Spring @Async:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        return executor;
    }
}
```

---

## 6. 测试策略

### 6.1 后端测试

**单元测试:**
- JUnit 5 + Mockito
- 服务层独立测试
- 使用@MockBean注入依赖

**集成测试:**
- @SpringBootTest
- TestContainers (PostgreSQL, Redis)
- @Transactional回滚数据

**API测试:**
- MockMvc
- RestAssured

### 6.2 前端测试

**单元测试:**
- Jest + React Testing Library
- 组件渲染测试
- Hook测试

**E2E测试:**
- Playwright
- 关键用户流程测试

---

## 7. 性能优化

### 7.1 数据库优化

**索引策略:**
- 主键索引 (自动)
- 外键索引
- 查询条件字段索引
- 组合索引 (最左前缀)

**查询优化:**
- 使用Explain分析查询计划
- 避免SELECT *
- 使用连接池 (HikariCP)
- 分页查询优化

### 7.2 缓存策略

**Redis缓存:**
- 权限信息缓存 (TTL: 15分钟)
- 用户信息缓存 (TTL: 30分钟)
- 部门树缓存 (TTL: 1小时)
- 本地缓存 (Caffeine) + Redis二级缓存

**缓存一致性:**
- Cache Aside模式
- 写操作先更新数据库，再删除缓存
- 使用消息通知缓存更新

---

## 8. 安全最佳实践

### 8.1 输入验证

**Bean Validation:**
```java
public class CreateUserRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度2-50字符")
    private String firstName;
}
```

### 8.2 SQL注入防护

- 使用JPA参数化查询
- 避免字符串拼接SQL
- 使用@Query注解

### 8.3 XSS防护

- 前端输入验证
- 后端输出编码
- Content Security Policy (CSP)

---

## 9. 参考资源

### 文档链接
- [Spring Boot 3.5 Reference](https://docs.spring.io/spring-boot/docs/3.5.x/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JJWT Documentation](https://github.com/jwtk/jjwt#readme)
- [Next.js 16 Documentation](https://nextjs.org/docs)
- [shadcn/ui Documentation](https://ui.shadcn.com/docs)
- [Zustand Documentation](https://docs.pmnd.rs/zustand)

### 示例项目
- [Spring Boot JWT Example](https://github.com/auth0-samples/auth0-spring-security-mvc-sample)
- [Next.js Auth Example](https://github.com/nextauthjs/next-auth-example)

---

## 10. 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| JJWT版本兼容性 | 中 | 中 | 使用稳定版本0.12.x，关注更新 |
| Next.js 14新特性问题 | 低 | 中 | 充分测试App Router功能 |
| shadcn/ui组件定制 | 中 | 低 | 基于Radix UI，可自由扩展 |
| Redis连接问题 | 中 | 高 | 配置连接池，异常重试 |
