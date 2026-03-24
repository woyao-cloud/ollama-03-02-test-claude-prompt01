# 部署指南

## 环境架构总览

### 5环境部署架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   本地开发    │ →  │  Team开发   │ →  │   SIT测试   │ →  │  UAT预发布  │ →  │   生产环境   │
│  (Docker)   │    │  (Docker)   │    │    (K8s)    │    │    (K8s)    │    │    (K8s)    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 环境对比

| 环境 | 用途 | 部署方式 | PostgreSQL | Redis | 数据策略 |
|------|------|----------|------------|-------|----------|
| **本地开发** | 个人开发调试 | Docker Compose | 单容器 | 单容器 | 测试数据集 |
| **Team开发** | 团队协作联调 | Docker Compose | 单容器 | 单容器 | 共享测试数据 |
| **SIT测试** | 系统集成测试 | Kubernetes | 云RDS实例 | 云缓存 | 预发布数据 |
| **UAT预发布** | 用户验收测试 | Kubernetes | 云RDS实例 | Redis集群 | 生产脱敏数据 |
| **生产环境** | 正式业务运行 | Kubernetes | 云RDS集群 | Redis集群 | 生产数据 |

## 快速启动

### 本地开发环境
```bash
# 启动 PostgreSQL + Redis
docker-compose -f docker-compose.local.yml up -d

# 查看状态
docker-compose -f docker-compose.local.yml ps
```

### Team开发环境
```bash
# 启动完整开发栈（含前后端）
docker-compose -f docker-compose.team.yml up -d
```

## 环境配置详情

### 1. 本地开发环境 (docker-compose.local.yml)

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 开发数据库，命名卷持久化 |
| Redis | 6379 | 开发缓存 |
| pgAdmin (可选) | 5050 | 数据库管理界面 |

### 2. Team开发环境 (docker-compose.team.yml)

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 团队共享数据库 |
| Redis | 6379 | 团队共享缓存 |
| Backend | 8080 | Spring Boot 应用 |
| Frontend | 3000 | Next.js 应用 |

### 3. SIT/UAT/生产环境 (Kubernetes)

使用外部托管服务：
- **PostgreSQL**: 云RDS (阿里云RDS/AWS RDS/腾讯云CDB)
- **Redis**: 云Redis服务或自建集群

## 数据库与数据管理

### Flyway迁移脚本位置
```
backend/src/main/resources/db/
├── migration/              # 表结构迁移（所有环境）
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_roles_permissions.sql
│   └── V3__Add_audit_tables.sql
├── data/
│   ├── local/              # 本地开发数据种子
│   ├── team/               # Team开发数据种子
│   └── common/             # 通用基础数据
└── test/                   # 测试专用数据
```

### 数据流向策略
- 生产 → UAT: 脱敏导出（用于验收测试）
- 生产 → SIT: 脱敏导出（用于问题复现）
- Team → 本地: SQL导出（协作调试）
- 禁止逆向数据流动

## 容器化部署

### 开发环境
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 生产环境
- **后端 (Spring Boot)**:
  - 副本数: 3
  - 内存限制: 1GB (JVM 堆内存 768MB)
  - JVM 参数: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
  - 健康检查: `/actuator/health`
- **前端**: 3 副本, 256MB 内存限制
- **PostgreSQL**: 主从架构
- **Redis**: 集群模式 (可选，用于缓存和会话)

### Spring Boot 容器化

#### Dockerfile
```dockerfile
# 多阶段构建
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# JVM 配置 (生产环境)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 内存配置建议

| 容器内存限制 | JVM 堆内存 (-Xmx) | 推荐场景 |
|--------------|-------------------|----------|
| 512MB | 384MB | 开发/测试 |
| 1GB | 768MB | 小型生产 |
| 2GB | 1536MB | 中型生产 |
| 4GB | 3072MB | 大型生产 |

## CI/CD 流程

### Spring Boot 后端构建流程

1. **代码提交触发构建**
   ```bash
   git push origin feature/xxx
   ```

2. **Maven 构建和测试**
   ```bash
   ./mvnw clean verify
   # 包含: 编译、测试、覆盖率检查、包构建
   ```

3. **代码质量检查**
   ```bash
   ./mvnw checkstyle:check
   ./mvnw spotbugs:check
   ```

4. **构建 Docker 镜像**
   ```bash
   docker build -t usermanagement-backend:${VERSION} .
   docker push registry/usermanagement-backend:${VERSION}
   ```

5. **部署到 Kubernetes**
   ```bash
   kubectl apply -f k8s/
   kubectl rollout status deployment/backend
   ```

6. **运行冒烟测试**
   ```bash
   curl http://backend:8080/actuator/health
   ```

### GitHub Actions 工作流示例

```yaml
name: Build and Deploy

on:
  push:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: ./mvnw clean verify

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

      - name: Build Docker image
        run: docker build -t backend:${{ github.sha }} .
```

## 监控告警

### Spring Boot 监控
- **Spring Boot Actuator**: 健康检查、指标、信息端点
  - `/actuator/health` - 健康状态
  - `/actuator/metrics` - JVM、HTTP、数据库指标
  - `/actuator/prometheus` - Prometheus 格式指标
- **Micrometer + Prometheus**: 应用指标收集
- **Grafana**: 可视化仪表板
- **日志聚合**: ELK Stack (Elasticsearch + Logstash + Kibana)
- **错误追踪**: Sentry / Sentry-Spring

### 关键监控指标

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| JVM 内存使用 | > 80% | 堆内存使用率 |
| HTTP 响应时间 | P95 > 500ms | API 响应延迟 |
| 错误率 | > 1% | HTTP 5xx 错误比例 |
| 活跃线程数 | > 200 | 虚拟线程池监控 |
| 数据库连接池 | > 80% | HikariCP 连接使用率 |
| GC 暂停时间 | > 1s | 垃圾回收停顿 |
