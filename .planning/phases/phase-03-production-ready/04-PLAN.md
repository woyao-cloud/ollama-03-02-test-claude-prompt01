---
phase: 3
plan: 04
title: 监控与告警系统
requirements_addressed: [AUDIT-07, PERF-07]
depends_on: [Plan 3.1, Plan 3.3]
wave: 2
autonomous: false
---

# Plan 3.4: 监控与告警系统

## Objective

建立完整的监控告警系统，实现系统可观测性：
- Prometheus 指标采集与存储
- Grafana 可视化仪表盘
- 告警规则配置与通知
- 应用性能监控 (APM)
- 日志聚合与分析

**Purpose:** 监控告警系统是生产环境稳定运行的保障，能够及时发现问题并通知相关人员。

**Output:**
- Prometheus Server 配置与部署
- Grafana 仪表盘配置
- AlertManager 告警配置
- Micrometer 指标采集
- 日志收集配置 (Loki 或 ELK)

---

## Context

### 技术栈
- 指标采集：Prometheus + Micrometer
- 可视化：Grafana
- 告警：AlertManager + 钉钉/企业微信/邮件
- APM: Spring Boot Actuator
- 日志：Loki + Promtail (或 ELK Stack)

### 监控指标
- JVM 指标：内存、GC、线程
- HTTP 指标：请求量、响应时间、错误率
- 数据库指标：连接池、查询延迟
- Redis 指标：命中率、内存使用
- Kafka 指标：消费者延迟、吞吐量
- 业务指标：登录次数、活跃用户

### 告警规则
- 错误率 > 5% (5 分钟窗口)
- P95 响应时间 > 500ms
- JVM 内存使用率 > 80%
- 数据库连接池耗尽
- Kafka 消费者延迟 > 1000

---

## Tasks

### Task 1: Add Monitoring Dependencies

**<read_first>**
- `backend/pom.xml` (current dependencies)
**</read_first>**

**<action>**
Add to `backend/pom.xml`:
```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Micrometer Tracing (optional for distributed tracing) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```
**</action>**

**<acceptance_criteria>**
- `spring-boot-starter-actuator` dependency added
- `micrometer-registry-prometheus` dependency added
- `mvn dependency:resolve` succeeds
**</acceptance_criteria>**

---

### Task 2: Configure Micrometer Metrics

**<read_first>**
- `backend/src/main/resources/application.yml` (existing config)
**</read_first>**

**<action>**
Add to `application.yml`:
```yaml
management:
  # Endpoints exposure
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,logfile
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      show-components: always
    prometheus:
      enabled: true
    metrics:
      enabled: true

  # Metrics configuration
  metrics:
    enable:
      all: false
      jvm: true
      system: true
      process: true
      http: true
      logback: true
      executor: true
      jdbc: true
      redis: true
      kafka: true
      hibernate: true

    # Global tags
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}

    # Distribution statistics
    distribution:
      percentiles-histogram:
        http.server.requests: true
        http.client.requests: true
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s, 5s

    # Meter filters
    meters:
      tags:
        - pattern: http.server.requests
          tags:
            - region: ${REGION:unknown}

  # Prometheus specific
  prometheus:
    metrics:
      export:
        enabled: true
        step: 60s  # Export interval

  # Health indicators
  health:
    redis:
      enabled: true
    kafka:
      enabled: true
    db:
      enabled: true
    mail:
      enabled: false
```
**</action>**

**<acceptance_criteria>**
- Actuator endpoints configured
- Prometheus endpoint exposed at `/actuator/prometheus`
- JVM, HTTP, Redis, Kafka metrics enabled
- Global tags configured
- SLO defined for HTTP requests
**</acceptance_criteria>**

---

### Task 3: Create Custom Metrics Configuration

**<action>**
Create `backend/src/main/java/com/usermanagement/config/MetricsConfig.java`:
```java
@Configuration
public class MetricsConfig {

    /**
     * Custom HTTP client metrics
     */
    @Bean
    public MeterBinder httpClientMetrics() {
        return (registry) -> {
            // Add custom HTTP client metrics if needed
        };
    }

    /**
     * Business metrics
     */
    @Bean
    public MeterBinder businessMetrics(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        return (registry) -> {
            Gauge.builder("users.total", userRepository, UserRepository::count)
                .description("Total number of users")
                .register(registry);

            Counter.builder("audit.logs.created")
                .description("Total audit logs created")
                .register(registry);
        };
    }

    /**
     * Cache metrics binder
     */
    @Bean
    public MeterBinder cacheMetrics(CacheManager cacheManager) {
        return (registry) -> {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                // Bind cache hit/miss metrics
            });
        };
    }
}
```
**</action>**

**<acceptance_criteria>**
- `MetricsConfig.java` created
- Custom business metrics defined
- Cache metrics binder configured
**</acceptance_criteria>**

---

### Task 4: Create Prometheus Configuration

**<action>**
Create `monitoring/prometheus/prometheus.yml`:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'usermanagement'

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

# Rule files
rule_files:
  - "alerts/*.yml"

# Scrape configurations
scrape_configs:
  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Spring Boot application
  - job_name: 'usermanagement'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'http_server_requests_seconds'
        action: keep

  # PostgreSQL exporter
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # Kafka exporter
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']

  # Node exporter (system metrics)
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```
**</action>**

**<acceptance_criteria>**
- `prometheus.yml` created with scrape configs
- Application metrics endpoint configured
- PostgreSQL, Redis, Kafka exporters configured
- Alertmanager integration configured
**</acceptance_criteria>**

---

### Task 5: Create Alert Rules

**<action>**
Create `monitoring/prometheus/alerts/application-alerts.yml`:
```yaml
groups:
  - name: application_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} over the last 5 minutes"

      # Slow response time (P95)
      - alert: SlowResponseTime
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response time"
          description: "P95 response time is {{ $value | humanize }}s"

      # High memory usage
      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM heap usage is {{ $value | humanizePercentage }}"

      # Database connection pool exhaustion
      - alert: DatabaseConnectionPoolExhausted
        expr: |
          hikaricp_active_connections / hikaricp_max_connections > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value | humanizePercentage }}"

      # Kafka consumer lag
      - alert: KafkaConsumerLag
        expr: |
          kafka_consumer_lag > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka consumer lag"
          description: "Consumer lag is {{ $value }} messages"

      # Redis high memory
      - alert: RedisHighMemory
        expr: |
          redis_memory_used_bytes / redis_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage high"
          description: "Redis memory usage is {{ $value | humanizePercentage }}"

      # Application down
      - alert: ApplicationDown
        expr: |
          up{job="usermanagement"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Application is down"
          description: "UserManagement application has been down for more than 1 minute"
```
**</action>**

**<acceptance_criteria>**
- Alert rules file created
- Error rate alert (> 5%)
- Response time alert (P95 > 500ms)
- Memory usage alert (> 80%)
- Connection pool alert (> 90%)
- Kafka lag alert (> 1000)
- Application down alert
**</acceptance_criteria>**

---

### Task 6: Create AlertManager Configuration

**<action>**
Create `monitoring/alertmanager/alertmanager.yml`:
```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@example.com'
  smtp_auth_username: 'alertmanager@example.com'
  smtp_auth_password: '${SMTP_PASSWORD}'

# Templates for notifications
templates:
  - 'templates/*.tmpl'

# Routing rules
route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - match:
        severity: critical
      receiver: 'critical-receiver'
      repeat_interval: 1h
    - match:
        severity: warning
      receiver: 'warning-receiver'
      repeat_interval: 4h

# Inhibition rules
inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']

# Receivers
receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'dev-team@example.com'
        send_resolved: true

  - name: 'critical-receiver'
    email_configs:
      - to: 'oncall@example.com'
        send_resolved: true
        html: '{{ template "email.html" . }}'
    # DingTalk webhook (optional)
    # webhook_configs:
    #   - url: 'http://dingtalk-webhook:8080/dingtalk/webhook'
    #     send_resolved: true

  - name: 'warning-receiver'
    email_configs:
      - to: 'dev-team@example.com'
        send_resolved: true
```
**</action>**

**<acceptance_criteria>**
- `alertmanager.yml` created
- Email notification configured
- Routing rules for critical/warning alerts
- Inhibition rules (critical suppresses warning)
- Repeat intervals configured
**</acceptance_criteria>**

---

### Task 7: Create Grafana Dashboards

**<action>**
Create `monitoring/grafana/dashboards/application-dashboard.json`:
```json
{
  "dashboard": {
    "title": "UserManagement Application",
    "tags": ["spring-boot", "application"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[1m]))",
            "legendFormat": "Requests/s"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Response Time (P50, P95, P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "P99"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100",
            "legendFormat": "Error Rate %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      },
      {
        "id": 5,
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_active_connections",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_idle_connections",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikaricp_max_connections",
            "legendFormat": "Max"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16}
      },
      {
        "id": 6,
        "title": "Cache Hit Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(rate(cache_hits_total[5m])) / (sum(rate(cache_hits_total[5m])) + sum(rate(cache_misses_total[5m]))) * 100",
            "legendFormat": "Hit Rate %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16}
      }
    ],
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    }
  }
}
```
**</action>**

**<acceptance_criteria>**
- Grafana dashboard JSON created
- Request rate panel
- Response time (P50/P95/P99) panel
- Error rate panel
- JVM memory panel
- Database connections panel
- Cache hit rate gauge
**</acceptance_criteria>**

---

### Task 8: Create Docker Compose for Monitoring Stack

**<action>**
Create `docker-compose.monitoring.yml`:
```yaml
services:
  # Prometheus
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: usermanagement-prometheus
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/prometheus/alerts:/etc/prometheus/alerts
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"
    networks:
      - monitoring
    restart: unless-stopped

  # AlertManager
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: usermanagement-alertmanager
    volumes:
      - ./monitoring/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--cluster.listen-address='
    ports:
      - "9093:9093"
    networks:
      - monitoring
    restart: unless-stopped

  # Grafana
  grafana:
    image: grafana/grafana:10.0.0
    container_name: usermanagement-grafana
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards
    environment:
      - GF_SECURITY_ADMIN_USER=${GRAFANA_ADMIN_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    ports:
      - "3000:3000"
    networks:
      - monitoring
    depends_on:
      - prometheus
    restart: unless-stopped

  # Node Exporter (system metrics)
  node-exporter:
    image: prom/node-exporter:v1.6.0
    container_name: usermanagement-node-exporter
    command:
      - '--path.rootfs=/host'
    volumes:
      - '/:/host:ro,rslave'
    ports:
      - "9100:9100"
    networks:
      - monitoring
    restart: unless-stopped

  # PostgreSQL Exporter
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:v0.14.0
    container_name: usermanagement-postgres-exporter
    environment:
      - DATA_SOURCE_NAME=postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=disable
    ports:
      - "9187:9187"
    networks:
      - monitoring
    restart: unless-stopped

  # Redis Exporter
  redis-exporter:
    image: oliver006/redis_exporter:latest
    container_name: usermanagement-redis-exporter
    environment:
      - REDIS_ADDR=redis://redis:6379
    ports:
      - "9121:9121"
    networks:
      - monitoring
    restart: unless-stopped

  # Kafka Exporter
  kafka-exporter:
    image: danielqsj/kafka-exporter:latest
    container_name: usermanagement-kafka-exporter
    command:
      - '--kafka.server=kafka:9092'
    ports:
      - "9308:9308"
    networks:
      - monitoring
    depends_on:
      - kafka
    restart: unless-stopped

  # Loki (Log aggregation - optional)
  loki:
    image: grafana/loki:2.9.0
    container_name: usermanagement-loki
    volumes:
      - ./monitoring/loki/loki.yml:/etc/loki/local-config.yaml
      - loki_data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    ports:
      - "3100:3100"
    networks:
      - monitoring
    restart: unless-stopped

  # Promtail (Log shipper)
  promtail:
    image: grafana/promtail:2.9.0
    container_name: usermanagement-promtail
    volumes:
      - ./monitoring/promtail/promtail.yml:/etc/promtail/config.yml
      - /var/log:/var/log
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
    command: -config.file=/etc/promtail/config.yml
    networks:
      - monitoring
    restart: unless-stopped

networks:
  monitoring:
    driver: bridge

volumes:
  prometheus_data:
  alertmanager_data:
  grafana_data:
  loki_data:
```
**</action>**

**<acceptance_criteria>**
- `docker-compose.monitoring.yml` created
- Prometheus service configured
- AlertManager service configured
- Grafana service configured
- Node Exporter for system metrics
- PostgreSQL/Redis/Kafka exporters
- Loki + Promtail for log aggregation
**</acceptance_criteria>**

---

### Task 9: Create Grafana Datasource Provisioning

**<action>**
Create `monitoring/grafana/provisioning/datasources/prometheus.yml`:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: "15s"
      queryTimeout: "60s"
```
**</action>**

**<acceptance_criteria>**
- Prometheus datasource provisioned
- Set as default datasource
**</acceptance_criteria>**

---

### Task 10: Write Integration Tests

**<action>**
Create `backend/src/test/java/com/usermanagement/monitoring/MetricsIntegrationTest.java`:
```java
@SpringBootTest
@AutoConfigureMockMvc
public class MetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpoint_ShouldReturnMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_memory_used_bytes")))
            .andExpect(content().string(containsString("http_server_requests_seconds")));
    }

    @Test
    void healthEndpoint_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthEndpoint_ShouldIncludeComponentDetails() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.db").exists())
            .andExpect(jsonPath("$.components.redis").exists());
    }
}
```
**</action>**

**<acceptance_criteria>**
- `MetricsIntegrationTest.java` created
- Prometheus endpoint test
- Health endpoint test
- Component health details test
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] Micrometer metrics enabled and exporting
- [ ] Prometheus endpoint accessible at `/actuator/prometheus`
- [ ] Prometheus scrape configuration working
- [ ] Alert rules defined for critical metrics
- [ ] AlertManager notification configured
- [ ] Grafana dashboard created with key metrics
- [ ] Docker Compose monitoring stack runs successfully
- [ ] Custom business metrics registered
- [ ] Integration tests pass

---

## Dependencies

**Required**:
- Plan 3.1: Kafka Audit Log Integration (completed)
- Plan 3.3: Performance Optimization (completed)
- Running application with Actuator enabled

---

## Success Criteria

1. Prometheus successfully scrapes application metrics
2. Grafana dashboard displays real-time metrics
3. AlertManager sends notifications for triggered alerts
4. All critical alerts defined and tested
5. Dashboard shows request rate, response time, error rate
6. JVM memory and DB connection monitoring working
7. Integration tests pass

---

*Plan: 04*
*Phase: phase-03-production-ready*
*Created: 2026-03-27*
