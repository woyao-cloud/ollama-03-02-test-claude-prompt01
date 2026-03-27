# Plan 3.3: Performance Optimization and Caching Strategy

**Phase**: 3 - Production Ready
**Status**: Ready for Execution
**Priority**: High
**Estimated Effort**: 2-3 sessions

---

## Objective

Optimize system performance to support 10,000+ concurrent users with API response times under 200ms:
- Implement Redis caching for frequently accessed data
- Optimize database queries and connection pooling
- Configure JVM tuning for JDK 21 virtual threads
- Add application performance monitoring
- Implement query result caching

---

## Requirements Addressed

- **PERF-01**: Login response < 100ms
- **PERF-02**: API average response < 200ms
- **PERF-03**: Support 10,000 concurrent users
- **PERF-04**: Redis cache permissions
- **PERF-05**: Database connection pool optimization
- **PERF-06**: Pagination query optimization

---

## Current State

**Existing Infrastructure**:
- Spring Boot 3.5 + JDK 21 (virtual threads enabled)
- PostgreSQL database
- Redis (used for sessions and permission cache)
- Kafka for async audit logging
- HikariCP connection pool (default settings)

**Current Configuration**:
- Hibernate batch_size: 20
- Connection isolation: READ_COMMITTED
- JPA open-in-view: false
- No explicit cache configuration

---

## Implementation Tasks

### Task 1: Add Spring Cache Dependencies and Configuration

**<read_first>**
- `backend/pom.xml` (current dependencies)
- `backend/src/main/resources/application.yml` (current config)
**</read_first>**

**<action>**
Add to `backend/pom.xml`:
```xml
<!-- Spring Boot Cache Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine Cache (for local cache) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Add to `application.yml`:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour default
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "ums:cache:"

# Cache configuration
app:
  cache:
    user:
      ttl: 3600  # 1 hour
    permission:
      ttl: 1800  # 30 minutes
    department:
      ttl: 7200  # 2 hours
```
**</action>**

**<acceptance_criteria>**
- `spring-boot-starter-cache` dependency added
- `caffeine` dependency added
- `spring.cache.type=redis` configured
- Cache TTL values configured for user, permission, department
**</acceptance_criteria>**

---

### Task 2: Create Cache Configuration Class

**<read_first>**
- `backend/src/main/java/com/usermanagement/config/` (existing configs)
**</read_first>**

**<action>**
Create `backend/src/main/java/com/usermanagement/config/CacheConfig.java`:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("permissions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("departments", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("roles", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }

    @Bean
    public KeyGenerator methodKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(".");
            sb.append(method.getName()).append("(");
            for (Object param : params) {
                sb.append(param != null ? param.toString() : "null");
            }
            sb.append(")");
            return sb.toString();
        };
    }
}
```
**</action>**

**<acceptance_criteria>**
- `CacheConfig.java` created with `@EnableCaching`
- RedisCacheManager configured with JSON serialization
- Separate TTL configurations for users, permissions, departments, roles caches
- Custom KeyGenerator bean defined
**</acceptance_criteria>**

---

### Task 3: Add Caching to UserService

**<read_first>**
- `backend/src/main/java/com/usermanagement/service/UserService.java`
- `backend/src/main/java/com/usermanagement/service/UserServiceImpl.java`
**</read_first>**

**<action>**
Update `UserServiceImpl.java` with caching annotations:
```java
@Service
public class UserServiceImpl implements UserService {

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserDTO getUserById(UUID id) { ... }

    @Override
    @Cacheable(value = "users", key = "#email")
    public UserDTO getUserByEmail(String email) { ... }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public UserDTO updateUser(UUID id, UpdateUserRequest request) { ... }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(UUID id) { ... }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "permissions", key = "#userId")
    })
    public void assignRoles(UUID userId, Set<UUID> roleIds) { ... }
}
```
**</action>**

**<acceptance_criteria>**
- `@Cacheable` on getUserById, getUserByEmail methods
- `@CacheEvict` on updateUser, deleteUser methods
- Proper cache key expressions using SpEL
- @Caching used for methods affecting multiple caches
**</acceptance_criteria>**

---

### Task 4: Add Caching to PermissionService

**<read_first>**
- `backend/src/main/java/com/usermanagement/service/PermissionService.java`
- `backend/src/main/java/com/usermanagement/service/PermissionServiceImpl.java`
**</read_first>**

**<action>**
Update `PermissionServiceImpl.java`:
```java
@Service
public class PermissionServiceImpl implements PermissionService {

    @Override
    @Cacheable(value = "permissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) { ... }

    @Override
    @Cacheable(value = "permissions", key = "#roleId")
    public List<PermissionDTO> getRolePermissions(UUID roleId) { ... }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionDTO createPermission(CreatePermissionRequest request) { ... }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public void assignPermissionToRole(UUID roleId, UUID permissionId) { ... }
}
```
**</action>**

**<acceptance_criteria>**
- User permissions cached by userId
- Role permissions cached by roleId
- Cache evicted on permission mutations
- All @Cacheable methods use proper SpEL keys
**</acceptance_criteria>**

---

### Task 5: Add Caching to DepartmentService

**<read_first>**
- `backend/src/main/java/com/usermanagement/service/DepartmentService.java`
- `backend/src/main/java/com/usermanagement/service/DepartmentServiceImpl.java`
**</read_first>**

**<action>**
Update `DepartmentServiceImpl.java`:
```java
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Override
    @Cacheable(value = "departments", key = "'tree'")
    public List<DepartmentTreeNode> getDepartmentTree() { ... }

    @Override
    @Cacheable(value = "departments", key = "#id")
    public DepartmentDTO getDepartmentById(UUID id) { ... }

    @Override
    @Cacheable(value = "departments", key = "#code")
    public DepartmentDTO getDepartmentByCode(String code) { ... }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDTO createDepartment(CreateDepartmentRequest request) { ... }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDTO updateDepartment(UUID id, UpdateDepartmentRequest request) { ... }
}
```
**</action>**

**<acceptance_criteria>**
- Department tree cached with key "tree"
- Individual departments cached by id and code
- All cache entries evicted on department mutations
**</acceptance_criteria>**

---

### Task 6: Optimize HikariCP Connection Pool

**<read_first>**
- `backend/src/main/resources/application.yml` (current datasource config)
**</read_first>**

**<action>**
Add optimized HikariCP configuration to `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:user_management}
    username: ${DB_USER:devuser}
    password: ${DB_PASSWORD:devpassword}
    hikari:
      # Pool sizing for 10,000 concurrent users
      maximum-pool-size: 50
      minimum-idle: 20
      # Connection timeouts
      connection-timeout: 20000  # 20 seconds
      idle-timeout: 300000      # 5 minutes
      max-lifetime: 1200000     # 20 minutes
      # Validation
      connection-test-query: SELECT 1
      validation-timeout: 3000  # 3 seconds
      # Performance
      leak-detection-threshold: 60000  # 1 minute
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false
```
**</action>**

**<acceptance_criteria>**
- maximum-pool-size: 50 configured
- minimum-idle: 20 configured
- Connection timeout: 20000ms
- MySQL/PostgreSQL performance properties added
- Leak detection threshold: 60000ms
**</acceptance_criteria>**

---

### Task 7: Optimize JPA/Hibernate Settings

**<read_first>**
- `backend/src/main/resources/application.yml` (current JPA config)
**</read_first>**

**<action>**
Update JPA configuration in `application.yml`:
```yaml
spring:
  jpa:
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        # Batch operations
        jdbc:
          batch_size: 50
          fetch_size: 50
          batch_versioned_data: true
          order_inserts: true
          order_updates: true
        # Query caching
        cache:
          use_query_cache: true
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.internal.JCacheRegionFactory
        # Performance optimizations
        id:
          new_generator_mappings: true
        query:
          plan_cache_max_size: 4096
          plan_parameter_metadata_max_size: 128
        # Statistics (enable for monitoring)
        generate_statistics: true
        # Logging (disable in production)
        show_sql: false
        format_sql: false
```
**</action>**

**<acceptance_criteria>**
- batch_size increased to 50
- fetch_size set to 50
- Query cache enabled
- Second-level cache enabled
- Statistics generation enabled
**</acceptance_criteria>**

---

### Task 8: Create Query Optimization Index

**<read_first>**
- `backend/src/main/java/com/usermanagement/repository/` (existing repositories)
- `backend/src/main/resources/db/migration/` (existing migrations)
**</read_first>**

**<action>**
Create `backend/src/main/resources/db/migration/V2__add_performance_indexes.sql`:
```sql
-- User table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_status ON users(email, status) WHERE status = 'ACTIVE';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_department_id ON users(department_id) WHERE department_id IS NOT NULL;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- Audit log indexes (for faster queries)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_user_time ON audit_logs(user_id, created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);

-- Department indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_departments_parent_id ON departments(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_departments_path ON departments(path);

-- Role and permission indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);

-- Session indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id) WHERE active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_expires ON user_sessions(expires_at) WHERE active = true;
```
**</action>**

**<acceptance_criteria>**
- Migration file V2__add_performance_indexes.sql created
- Indexes for frequently queried columns
- Partial indexes for filtered queries
- Concurrent index creation to avoid locking
**</acceptance_criteria>**

---

### Task 9: Add Pagination Optimization

**<read_first>**
- `backend/src/main/java/com/usermanagement/repository/UserRepository.java`
- `backend/src/main/java/com/usermanagement/service/UserServiceImpl.java`
**</read_first>**

**<action>**
Update repositories with optimized pagination:
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Use count query for better performance
    @Query(value = "SELECT u FROM User u WHERE u.status = :status",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") UserStatus status, Pageable pageable);

    // Optimized query with fetch join to avoid N+1
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.department WHERE u.id IN :ids")
    List<User> findAllWithDepartmentByIdIn(@Param("ids") List<UUID> ids);

    // Keyset pagination for large datasets
    @Query("SELECT u FROM User u WHERE u.createdAt < :cursor ORDER BY u.createdAt DESC")
    List<User> findNextPage(@Param("cursor") Instant cursor, Pageable pageable);
}
```
**</action>**

**<acceptance_criteria>**
- Explicit countQuery for pagination
- Fetch join queries to prevent N+1 problem
- Keyset pagination method for large datasets
- @EntityGraph or JOIN FETCH for relationships
**</acceptance_criteria>**

---

### Task 10: Create Performance Monitoring Service

**<read_first>**
- `backend/src/main/java/com/usermanagement/service/` (existing services)
- `backend/pom.xml` (check actuator dependency)
**</read_first>**

**<action>**
Create `backend/src/main/java/com/usermanagement/service/PerformanceMonitorService.java`:
```java
@Service
public class PerformanceMonitorService {

    private final MeterRegistry meterRegistry;
    private final EntityManagerFactory entityManagerFactory;

    public PerformanceMonitorService(MeterRegistry meterRegistry,
                                     EntityManagerFactory entityManagerFactory) {
        this.meterRegistry = meterRegistry;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void recordCacheMetrics() {
        CacheManager cacheManager = ...; // inject
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            // Record cache metrics
        });
    }

    @Scheduled(fixedRate = 60000)
    public void recordHibernateMetrics() {
        if (entityManagerFactory instanceof SessionFactoryImpl) {
            Statistics stats = ((SessionFactoryImpl) entityManagerFactory).getStatistics();
            meterRegistry.gauge("hibernate.query.cache.hit", stats.getQueryCacheHitCount());
            meterRegistry.gauge("hibernate.query.cache.miss", stats.getQueryCacheMissCount());
            meterRegistry.gauge("hibernate.second.level.hit", stats.getSecondLevelCacheHitCount());
            meterRegistry.gauge("hibernate.second.level.miss", stats.getSecondLevelCacheMissCount());
        }
    }

    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        // Compile metrics
        return report;
    }
}
```
**</action>**

**<acceptance_criteria>**
- PerformanceMonitorService created
- Scheduled cache metrics collection
- Hibernate statistics collection
- Micrometer registry integration
**</acceptance_criteria>**

---

### Task 11: Configure Micrometer Metrics

**<read_first>**
- `backend/src/main/resources/application.yml`
**</read_first>**

**<action>**
Add Micrometer configuration to `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
  endpoint:
    health:
      show-details: when_authorized
    metrics:
      enabled: true
    caches:
      enabled: true
  metrics:
    enable:
      jvm: true
      system: true
      process: true
      hibernate: true
      jdbc: true
    distribution:
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s,5s
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
```
**</action>**

**<acceptance_criteria>**
- Prometheus endpoint exposed
- Cache metrics enabled
- JVM and Hibernate metrics enabled
- SLO configured for HTTP requests
**</acceptance_criteria>**

---

### Task 12: Write Performance Tests

**<read_first>**
- `backend/src/test/` (existing test structure)
- `backend/pom.xml` (check test dependencies)
**</read_first>**

**<action>**
Create `backend/src/test/java/com/usermanagement/performance/PerformanceIntegrationTest.java`:
```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "management.metrics.enable.hibernate=true"
})
public class PerformanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @RepeatedTest(10)
    void getUserById_ResponseTimeUnder100ms() throws Exception {
        long start = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/users/{id}", testUserId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 100, "Response time should be under 100ms, was: " + duration + "ms");
    }

    @Test
    void getDepartmentTree_ResponseTimeUnder200ms() throws Exception {
        long start = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/departments/tree")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 200, "Response time should be under 200ms, was: " + duration + "ms");
    }

    @Test
    void cacheHit_ShouldBeFasterThanCacheMiss() throws Exception {
        // First call (cache miss)
        long missStart = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/users/{id}", testUserId)
                .header("Authorization", "Bearer " + token));
        long missDuration = System.currentTimeMillis() - missStart;

        // Second call (cache hit)
        long hitStart = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/users/{id}", testUserId)
                .header("Authorization", "Bearer " + token));
        long hitDuration = System.currentTimeMillis() - hitStart;

        assertTrue(hitDuration < missDuration,
            "Cache hit (" + hitDuration + "ms) should be faster than cache miss (" + missDuration + "ms)");
    }
}
```
**</action>**

**<acceptance_criteria>**
- PerformanceIntegrationTest created
- Response time tests for critical endpoints
- Cache hit vs miss comparison test
- @RepeatedTest for statistical significance
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] Spring Cache dependencies added
- [ ] CacheConfig with RedisCacheManager created
- [ ] @Cacheable annotations on UserService read methods
- [ ] @CacheEvict annotations on UserService write methods
- [ ] PermissionService caching implemented
- [ ] DepartmentService caching implemented
- [ ] HikariCP optimized for 10,000 concurrent users
- [ ] Hibernate batch and query caching enabled
- [ ] Database indexes migration created
- [ ] Pagination queries optimized with countQuery
- [ ] Performance monitoring service created
- [ ] Micrometer metrics configured
- [ ] Performance tests pass (response times under thresholds)

---

## Dependencies

**Required**:
- Plan 3.1: Kafka Audit Log Integration (completed)
- Redis server running
- Existing UserService, PermissionService, DepartmentService

---

## Success Criteria

1. Login API response time < 100ms (P95)
2. General API response time < 200ms (P95)
3. Cache hit ratio > 80% for frequently accessed data
4. Database connection pool handles 10,000 concurrent users
5. All performance tests pass
6. Prometheus metrics available for monitoring

---

*Plan: 03*
*Phase: phase-03-production-ready*
*Created: 2026-03-25*
