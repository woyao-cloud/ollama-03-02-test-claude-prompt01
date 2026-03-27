---
phase: 3
plan: 06
title: 压力测试与优化
requirements_addressed: [PERF-01, PERF-02, PERF-03]
depends_on: [Plan 3.1, Plan 3.3, Plan 3.4]
wave: 4
autonomous: false
---

# Plan 3.6: 压力测试与优化

## Objective

执行全面的压力测试和性能优化，确保系统满足生产环境要求：
- 登录响应时间 < 100ms (P95)
- API 响应时间 < 200ms (P95)
- 支持 10,000+ 并发用户
- TPS >= 10,000
- 识别并解决性能瓶颈

**Purpose:** 压力测试验证系统容量和稳定性，发现性能瓶颈，确保生产环境可靠运行。

**Output:**
- JMeter/Gatling 测试脚本
- 压力测试报告
- 性能瓶颈分析与优化建议
- 调优后的配置参数
- 容量规划文档

---

## Context

### 性能目标

| 指标 | 目标值 | 测量方法 |
|------|--------|----------|
| 登录响应时间 | < 100ms (P95) | APM/Prometheus |
| API 响应时间 | < 200ms (P95) | APM/Prometheus |
| 并发用户数 | 10,000+ | 压力测试 |
| TPS (Transactions/s) | >= 10,000 | 压力测试 |
| 错误率 | < 0.1% | 压力测试 |
| CPU 使用率 | < 70% | Prometheus |
| 内存使用率 | < 80% | Prometheus |

### 测试类型

1. **基准测试 (Benchmark Test)**
   - 单用户场景，建立性能基线
   - 验证功能正确性

2. **负载测试 (Load Test)**
   - 逐步增加负载到预期峰值
   - 验证系统在正常和峰值负载下的表现

3. **压力测试 (Stress Test)**
   - 超出预期负载，测试系统极限
   - 发现系统崩溃点和恢复能力

4. **耐力测试 (Endurance Test)**
   - 长时间运行 (24-48 小时)
   - 检测内存泄漏、资源耗尽

5. **尖峰测试 (Spike Test)**
   - 突然大幅增加负载
   - 测试系统应对突发流量的能力

### 测试场景

| 场景 | 描述 | 并发用户 | 持续时间 |
|------|------|----------|----------|
| 登录 | 用户认证获取 Token | 500-1000 | 10 min |
| 用户查询 | 分页查询用户列表 | 1000-2000 | 15 min |
| 权限检查 | RBAC 权限验证 | 2000-5000 | 20 min |
| 混合场景 | 登录 + 查询 + 写入 | 5000-10000 | 30 min |
| 耐力测试 | 持续负载 | 3000 | 24 hours |

---

## Tasks

### Task 1: Add Test Dependencies

**<read_first>**
- `backend/pom.xml` (current dependencies)
- `backend/src/test/` (existing test structure)
**</read_first>**

**<action>**
Add to `backend/pom.xml`:
```xml
<!-- Gatling for load testing -->
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.9.5</version>
    <scope>test</scope>
</dependency>

<!-- JMeter for integration testing -->
<dependency>
    <groupId>org.apache.jmeter</groupId>
    <artifactId>ApacheJMeter_core</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>

<!-- JMeter HTTP sampler -->
<dependency>
    <groupId>org.apache.jmeter</groupId>
    <artifactId>ApacheJMeter_http</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>

<!-- Test containers for integration tests -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```
**</action>**

**<acceptance_criteria>**
- Gatling dependency added
- JMeter dependencies added
- Test containers dependencies added
- `mvn dependency:resolve` succeeds
**</acceptance_criteria>**

---

### Task 2: Create JMeter Test Plan

**<action>**
Create `performance/jmeter/user-management-test-plan.jmx`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan>
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.name">User Management Load Test</stringProp>
      <stringProp name="TestPlan.user_define_classpath">./lib</stringProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.value">http://localhost:8080/api/v1</stringProp>
          </elementProp>
          <elementProp name="CONCURRENT_USERS" elementType="Argument">
            <stringProp name="Argument.value">1000</stringProp>
          </elementProp>
          <elementProp name="RAMP_UP" elementType="Argument">
            <stringProp name="Argument.value">60</stringProp>
          </elementProp>
          <elementProp name="DURATION" elementType="Argument">
            <stringProp name="Argument.value">600</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <!-- HTTP Request Defaults -->
      <HTTPDefaults>
        <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
          <collectionProp name="Arguments.arguments"/>
        </elementProp>
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <stringProp name="HTTPSampler.concurrentPool">6</stringProp>
        <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
        <stringProp name="HTTPSampler.response_timeout">30000</stringProp>
      </HTTPDefaults>
      <hashTree/>

      <!-- Header Manager -->
      <HeaderManager>
        <collectionProp name="HeaderManager.headers">
          <elementProp name="Content-Type" elementType="Header">
            <stringProp name="Header.name">Content-Type</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
          <elementProp name="Accept" elementType="Header">
            <stringProp name="Header.name">Accept</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
        </collectionProp>
      </HeaderManager>
      <hashTree/>

      <!-- CSV Data Set Config for test data -->
      <ConfigTestElement>
        <stringProp name="filename">test-data/users.csv</stringProp>
        <stringProp name="variableNames">email,password</stringProp>
        <stringProp name="delimiter">,</stringProp>
        <boolProp name="recycle">true</boolProp>
        <boolProp name="stopThread">false</boolProp>
      </ConfigTestElement>
      <hashTree/>

      <!-- Thread Group for Login Test -->
      <ThreadGroup>
        <stringProp name="ThreadGroup.name">Login Test</stringProp>
        <stringProp name="ThreadGroup.num_threads">${CONCURRENT_USERS}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">${RAMP_UP}</stringProp>
        <stringProp name="ThreadGroup.duration">${DURATION}</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">true</boolProp>
        </elementProp>
      </ThreadGroup>
      <hashTree>
        <!-- Login Request -->
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.path">/api/v1/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <stringProp name="HTTPSampler.postBodyRaw">{"email":"${email}","password":"${password}"}</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <!-- JSON Extractor for token -->
          <JSONPostProcessor>
            <stringProp name="JSONPostProcessor.referenceNames">auth_token</stringProp>
            <stringProp name="JSONPostProcessor.jsonPathExpr">$.token</stringProp>
          </JSONPostProcessor>
          <hashTree/>
        </hashTree>

        <!-- Get User Profile (authenticated) -->
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.path">/api/v1/users/me</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager>
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Authorization" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${auth_token}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
        </hashTree>

        <!-- Response Assertion -->
          <ResponseAssertion>
            <stringProp name="Assertion.test_response">200</stringProp>
            <stringProp name="Assertion.assume_success">true</stringProp>
          </ResponseAssertion>
          <hashTree/>
      </hashTree>

      <!-- Thread Group for User Query -->
      <ThreadGroup>
        <stringProp name="ThreadGroup.name">User Query Test</stringProp>
        <stringProp name="ThreadGroup.num_threads">500</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      <hashTree>
        <!-- List Users with Pagination -->
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.path">/api/v1/users?page=0&amp;size=20</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree/>
      </hashTree>

      <!-- Summary Report Listener -->
      <ResultCollector>
        <stringProp name="filename">results/login-test-results.csv</stringProp>
        <boolProp name="ResultCollector.error_logging">false</boolProp>
      </ResultCollector>
      <hashTree/>

      <!-- HTML Report -->
      <ResultCollector>
        <stringProp name="filename">results/login-test-report</stringProp>
        <objProp name="ReportConfig">
          <stringProp name="ReportConfig.classname">org.apache.jmeter.report.dashboard.HtmlReportGenerator</stringProp>
        </objProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

Create `performance/jmeter/test-data/users.csv`:
```
testuser1@example.com,TestPass123!
testuser2@example.com,TestPass123!
testuser3@example.com,TestPass123!
...
```
**</action>**

**<acceptance_criteria>**
- JMeter test plan (.jmx) created
- Login test thread group configured
- User query thread group configured
- CSV data set for test users
- Token extraction configured
- Response assertions added
- Result listeners configured
**</acceptance_criteria>**

---

### Task 3: Create Gatling Simulation

**<action>**
Create `backend/src/test/scala/usermanagement/simulations/UserManagementSimulation.scala`:
```scala
package usermanagement.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.concurrent.duration._

class UserManagementSimulation extends Simulation {

  // Configuration
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  private val concurrentUsers = System.getProperty("concurrentUsers", "1000").toInt
  private val rampTime = System.getProperty("rampTime", "60").toInt
  private val testDuration = System.getProperty("testDuration", "600").toInt

  // HTTP protocol configuration
  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")
    .inferHtmlResources()

  // Headers with authentication
  private val authenticatedHeaders = Map(
    "Content-Type" -> "application/json",
    "Accept" -> "application/json"
  )

  // Test data feeder
  private val usersFeeder = csv("test-data/users.csv").circular

  // Scenario: Login and Query
  private val loginAndQuery = scenario("Login and Query User")
    .feed(usersFeeder)
    .exec(
      http("Login")
        .post("/api/v1/auth/login")
        .body(StringBody("""{"email":"${email}","password":"${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.token").saveAs("authToken"))
        .check(jsonPath("$.refreshToken").saveAs("refreshToken"))
    )
    .pause(100.milliseconds)
    .exec(
      http("Get Current User")
        .get("/api/v1/users/me")
        .headers(authenticatedHeaders)
        .header("Authorization", "Bearer ${authToken}")
        .check(status.is(200))
    )
    .pause(50.milliseconds)
    .exec(
      http("List Users")
        .get("/api/v1/users?page=0&size=20")
        .headers(authenticatedHeaders)
        .header("Authorization", "Bearer ${authToken}")
        .check(status.is(200))
    )
    .pause(100.milliseconds)
    .exec(
      http("Refresh Token")
        .post("/api/v1/auth/refresh")
        .body(StringBody("""{"refreshToken":"${refreshToken}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.token").saveAs("newAuthToken"))
    )
    .exec(session => {
      session.set("authToken", session("newAuthToken").as[String])
    })

  // Scenario: Permission Check (high frequency)
  private val permissionCheck = scenario("Permission Check")
    .feed(usersFeeder)
    .exec(
      http("Login for Permission Check")
        .post("/api/v1/auth/login")
        .body(StringBody("""{"email":"${email}","password":"${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.token").saveAs("authToken"))
    )
    .repeat(10) {
      exec(
        http("Check Permission")
          .get("/api/v1/permissions/check?resource=users&action=read")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
      ).pause(50.milliseconds)
    }

  // Scenario: Admin Operations
  private val adminOperations = scenario("Admin Operations")
    .exec(
      http("Admin Login")
        .post("/api/v1/auth/login")
        .body(StringBody("""{"email":"admin@example.com","password":"AdminPass123!"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.token").saveAs("adminToken"))
    )
    .exec(
      http("Create User")
        .post("/api/v1/users")
        .header("Authorization", "Bearer ${adminToken}")
        .body(StringBody("""{"email":"newuser_${randomId}@example.com","password":"TestPass123!","firstName":"Test","lastName":"User"}""")).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("newUserId"))
    )
    .pause(100.milliseconds)
    .exec(
      http("Update User")
        .put("/api/v1/users/${newUserId}")
        .header("Authorization", "Bearer ${adminToken}")
        .body(StringBody("""{"firstName":"Updated"}""")).asJson
        .check(status.is(200))
    )
    .pause(100.milliseconds)

  // Load profiles
  private val normalLoad = inject(
    rampUsers(concurrentUsers) during (rampTime.seconds),
    constantUsers(concurrentUsers / 10) during (testDuration.seconds)
  )

  private val stressLoad = inject(
    rampUsers(concurrentUsers * 2) during (rampTime.seconds),
    constantUsers(concurrentUsers * 2 / 10) during (testDuration.seconds)
  )

  private val spikeLoad = inject(
    constantUsers(concurrentUsers / 10) during (60.seconds),
    rampUsers(concurrentUsers) during (30.seconds),
    constantUsers(concurrentUsers) during (120.seconds),
    rampUsers(concurrentUsers / 10) during (30.seconds)
  )

  // Assertions
  private val performanceAssertions = assertions(
    global.responseTime.max.lt(2000),
    global.responseTime.percentile4.lt(500),
    global.successfulRequests.percent.is(99),
    forAll("Login").responseTime.percentile3.lt(100),
    forAll("Get Current User").responseTime.percentile3.lt(200)
  )

  // Setup: Normal load test
  setUp(
    loginAndQuery.inject(normalLoad).protocols(httpProtocol),
    permissionCheck.inject(constantUsers(500) during (testDuration.seconds)).protocols(httpProtocol)
  ).assertions(performanceAssertions)
    .assertions(global.failedRequests.percent.is(0))

  // Alternative: Stress test (uncomment to run)
  // setUp(
  //   loginAndQuery.inject(stressLoad).protocols(httpProtocol)
  // ).assertions(performanceAssertions)

  // Alternative: Spike test (uncomment to run)
  // setUp(
  //   loginAndQuery.inject(spikeLoad).protocols(httpProtocol)
  // ).assertions(performanceAssertions)
}
```
**</action>**

**<acceptance_criteria>**
- Gatling simulation created in Scala
- Login and Query scenario defined
- Permission Check scenario (high frequency)
- Admin Operations scenario
- Load profiles: normal, stress, spike
- Performance assertions defined
- Response time thresholds configured
**</acceptance_criteria>**

---

### Task 4: Create Performance Test Runner

**<action>**
Create `scripts/run-performance-tests.sh`:
```bash
#!/bin/bash
set -euo pipefail

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
CONCURRENT_USERS="${CONCURRENT_USERS:-1000}"
TEST_TYPE="${TEST_TYPE:-normal}"  # normal, stress, spike, endurance

echo "=========================================="
echo "Performance Test Runner"
echo "=========================================="
echo ""
echo "Configuration:"
echo "  Base URL: ${BASE_URL}"
echo "  Concurrent Users: ${CONCURRENT_USERS}"
echo "  Test Type: ${TEST_TYPE}"
echo ""

# Create results directory
RESULTS_DIR="performance/results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${RESULTS_DIR}"

# Generate test data
echo "Step 1: Generating test data..."
node scripts/generate-test-users.js --count 1000 --output performance/jmeter/test-data/users.csv

# Health check
echo "Step 2: Checking application health..."
if ! curl -s "${BASE_URL}/actuator/health" | grep -q "UP"; then
    echo "ERROR: Application is not healthy"
    exit 1
fi
echo "Application is healthy"

# Run JMeter tests
echo "Step 3: Running JMeter tests..."
cd performance/jmeter

case ${TEST_TYPE} in
    normal)
        CONCURRENT_USERS=1000
        RAMP_UP=60
        DURATION=600
        ;;
    stress)
        CONCURRENT_USERS=2000
        RAMP_UP=60
        DURATION=900
        ;;
    spike)
        CONCURRENT_USERS=5000
        RAMP_UP=30
        DURATION=300
        ;;
    endurance)
        CONCURRENT_USERS=3000
        RAMP_UP=120
        DURATION=86400  # 24 hours
        ;;
esac

# Run JMeter in non-GUI mode
jmeter -n -t user-management-test-plan.jmx \
    -JCONCURRENT_USERS=${CONCURRENT_USERS} \
    -JRAMP_UP=${RAMP_UP} \
    -JDURATION=${DURATION} \
    -l "${RESULTS_DIR}/jmeter-results.csv" \
    -e -o "${RESULTS_DIR}/jmeter-report"

cd -

# Run Gatling tests
echo "Step 4: Running Gatling tests..."
cd backend

mvn gatling:test -Dgatling.simulations=usermanagement.simulations.UserManagementSimulation \
    -DbaseUrl=${BASE_URL} \
    -DconcurrentUsers=${CONCURRENT_USERS} \
    -DrampTime=${RAMP_UP} \
    -DtestDuration=${DURATION}

cd -

# Copy Gatling results
cp -r backend/target/gatling/* "${RESULTS_DIR}/" 2>/dev/null || true

# Generate summary report
echo "Step 5: Generating summary report..."
node scripts/generate-performance-report.js \
    --jmeter "${RESULTS_DIR}/jmeter-results.csv" \
    --output "${RESULTS_DIR}/summary.md"

echo ""
echo "=========================================="
echo "Performance Test Complete!"
echo "=========================================="
echo ""
echo "Results:"
echo "  JMeter Report: ${RESULTS_DIR}/jmeter-report/index.html"
echo "  Gatling Report: ${RESULTS_DIR}/usermanagementsimulation-*/index.html"
echo "  Summary: ${RESULTS_DIR}/summary.md"
echo ""
echo "Open reports in browser:"
echo "  open ${RESULTS_DIR}/jmeter-report/index.html"
```
**</action>**

**<acceptance_criteria>**
- Test runner script created
- Supports normal, stress, spike, endurance tests
- Health check before tests
- Test data generation
- JMeter and Gatling execution
- Summary report generation
**</acceptance_criteria>**

---

### Task 5: Create Performance Monitoring Script

**<action>**
Create `scripts/monitor-performance.sh`:
```bash
#!/bin/bash

# Monitor system metrics during load test
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
RESULTS_DIR="performance/monitoring/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${RESULTS_DIR}"

echo "Starting performance monitoring..."
echo "Prometheus: ${PROMETHEUS_URL}"
echo "Output: ${RESULTS_DIR}"

# Collect metrics every 5 seconds
while true; do
    TIMESTAMP=$(date +%s)

    # Query Prometheus for key metrics
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"}" \
        > "${RESULTS_DIR}/memory_${TIMESTAMP}.json"

    curl -s "${PROMETHEUS_URL}/api/v1/query?query=rate(http_server_requests_seconds_count[1m])" \
        > "${RESULTS_DIR}/requests_${TIMESTAMP}.json"

    curl -s "${PROMETHEUS_URL}/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket[1m]))by(le))" \
        > "${RESULTS_DIR}/latency_${TIMESTAMP}.json"

    curl -s "${PROMETHEUS_URL}/api/v1/query?query=system_cpu_usage" \
        > "${RESULTS_DIR}/cpu_${TIMESTAMP}.json"

    sleep 5
done
```
**</action>**

**<acceptance_criteria>**
- Monitoring script created
- Queries Prometheus for memory, requests, latency, CPU
- Collects metrics every 5 seconds
- Saves to timestamped files
**</acceptance_criteria>**

---

### Task 6: Execute Baseline Test

**<action>**
Execute baseline test with 100 concurrent users:
```bash
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=100 \
TEST_TYPE=normal \
./scripts/run-performance-tests.sh
```

Record baseline metrics:
- Average response time
- P95 response time
- P99 response time
- Success rate
- Throughput (requests/second)
**</action>**

**<acceptance_criteria>**
- Baseline test executed
- Metrics recorded
- No errors at baseline load
**</acceptance_criteria>**

---

### Task 7: Execute Load Test

**<action>**
Execute load test with increasing concurrency:
```bash
# Test 1: 500 concurrent users
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=500 \
TEST_TYPE=normal \
./scripts/run-performance-tests.sh

# Test 2: 1000 concurrent users
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=1000 \
TEST_TYPE=normal \
./scripts/run-performance-tests.sh

# Test 3: 2000 concurrent users
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=2000 \
TEST_TYPE=normal \
./scripts/run-performance-tests.sh
```

Record metrics at each level and identify breaking point.
**</action>**

**<acceptance_criteria>**
- Load tests executed at 500, 1000, 2000 concurrent users
- Performance metrics recorded for each level
- Breaking point identified
- Bottlenecks documented
**</acceptance_criteria>**

---

### Task 8: Execute Stress Test

**<action>**
Execute stress test beyond expected capacity:
```bash
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=5000 \
TEST_TYPE=stress \
./scripts/run-performance-tests.sh
```

Monitor for:
- Memory exhaustion
- Connection pool depletion
- Thread pool saturation
- Database connection timeouts
- Kafka producer/consumer lag
**</action>**

**<acceptance_criteria>**
- Stress test executed at 5000+ concurrent users
- System breaking point documented
- Failure modes identified
- Recovery behavior observed
**</acceptance_criteria>**

---

### Task 9: Execute Endurance Test

**<action>**
Execute endurance test (24 hours):
```bash
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=3000 \
TEST_TYPE=endurance \
./scripts/run-performance-tests.sh
```

Monitor for:
- Memory leaks (gradual increase in heap usage)
- Connection leaks
- Thread leaks
- Disk space exhaustion
- Log file growth
**</action>**

**<acceptance_criteria>**
- 24-hour endurance test completed
- Memory trend analyzed (no leaks)
- Resource utilization stable
- No degradation over time
**</acceptance_criteria>**

---

### Task 10: Analyze Results and Optimize

**<action>**
Create `performance/reports/PERFORMANCE_ANALYSIS.md`:
```markdown
# Performance Analysis Report

## Executive Summary

| Metric | Target | Achieved | Status |
|--------|--------|----------|--------|
| Login Response (P95) | < 100ms | XX ms | ✓/✗ |
| API Response (P95) | < 200ms | XX ms | ✓/✗ |
| Concurrent Users | 10,000+ | XX | ✓/✗ |
| TPS | >= 10,000 | XX | ✓/✗ |
| Error Rate | < 0.1% | X.X% | ✓/✗ |

## Test Results

### Baseline Test (100 users)
- Avg Response Time: XX ms
- P95 Response Time: XX ms
- P99 Response Time: XX ms
- Throughput: XX req/s
- Error Rate: X.X%

### Load Test Results

| Concurrent Users | Avg RT | P95 RT | P99 RT | Throughput | Error Rate |
|-----------------|--------|--------|--------|------------|------------|
| 500 | XX ms | XX ms | XX ms | XX req/s | X.X% |
| 1000 | XX ms | XX ms | XX ms | XX req/s | X.X% |
| 2000 | XX ms | XX ms | XX ms | XX req/s | X.X% |
| 5000 | XX ms | XX ms | XX ms | XX req/s | X.X% |

### Stress Test Results
- Maximum concurrent users handled: XX
- Breaking point: XX users
- Failure mode: [Memory exhaustion / Connection pool / Thread pool / etc.]
- Recovery time: XX seconds

### Endurance Test Results
- Duration: 24 hours
- Memory trend: [Stable / Gradual increase of X MB/hour]
- CPU trend: [Stable at X%]
- GC frequency: X times/hour
- Any leaks detected: [Yes/No]

## Bottleneck Analysis

### Identified Bottlenecks

1. **Database Connection Pool**
   - Symptom: Connection timeouts at XX concurrent users
   - Root cause: Pool size too small (current: XX, needed: XX)
   - Recommendation: Increase hikaricp.maximum-pool-size to XX

2. **JVM Memory**
   - Symptom: Frequent GC pauses
   - Root cause: Heap size insufficient
   - Recommendation: Increase -Xmx to XX GB

3. **Thread Pool**
   - Symptom: Request queuing at high load
   - Root cause: Tomcat thread pool exhausted
   - Recommendation: Increase server.tomcat.threads.max to XX

4. **Redis Cache**
   - Symptom: Cache miss rate high (XX%)
   - Root cause: TTL too short / cache eviction aggressive
   - Recommendation: Increase TTL to XX minutes

5. **Kafka Producer**
   - Symptom: Producer latency at high throughput
   - Root cause: Batch size / linger.ms settings
   - Recommendation: Tune batch.size and linger.ms

## Optimization Recommendations

### Immediate Actions (High Impact)
1. [Action 1]
2. [Action 2]
3. [Action 3]

### Medium-term Improvements
1. [Action 1]
2. [Action 2]

### Long-term Architecture Changes
1. [Action 1]
2. [Action 2]

## Capacity Planning

Based on test results:

| Metric | Current Capacity | Target | Gap |
|--------|-----------------|--------|-----|
| Concurrent Users | XX | 10,000 | +/- XX |
| TPS | XX | 10,000 | +/- XX |
| Memory | XX GB | - | - |
| CPU | XX cores | - | - |

### Scaling Recommendations
- Vertical scaling: Increase to XX GB RAM, XX CPU cores
- Horizontal scaling: Add XX more replicas
- Database: Consider read replicas for query scaling
- Cache: Increase Redis memory to XX GB
```
**</action>**

**<acceptance_criteria>**
- Performance analysis report created
- All test results documented
- Bottlenecks identified with root causes
- Optimization recommendations provided
- Capacity planning included
**</acceptance_criteria>**

---

### Task 11: Apply Optimizations

**<action>**
Based on analysis, apply targeted optimizations:

1. **Database optimization**
   - Adjust connection pool size
   - Add missing indexes
   - Optimize slow queries

2. **JVM tuning**
   - Adjust heap size
   - Tune GC parameters
   - Enable G1GC if not already

3. **Cache optimization**
   - Adjust TTL values
   - Increase cache coverage
   - Tune Redis configuration

4. **Thread pool tuning**
   - Adjust Tomcat thread pool
   - Configure async executor pool sizes

5. **Kafka optimization**
   - Tune producer batch settings
   - Adjust consumer concurrency
**</action>**

**<acceptance_criteria>**
- Optimizations applied based on analysis
- Configuration changes documented
- Before/after metrics compared
**</acceptance_criteria>**

---

### Task 12: Re-run Tests After Optimization

**<action>**
Re-run the same test suite after optimizations:
```bash
# Re-run load test
BASE_URL=http://localhost:8080 \
CONCURRENT_USERS=2000 \
TEST_TYPE=normal \
./scripts/run-performance-tests.sh

# Compare results
node scripts/compare-results.js \
    --before performance/results/before-optimization \
    --after performance/results/after-optimization \
    --output performance/results/improvement-report.md
```
**</action>**

**<acceptance_criteria>**
- Tests re-run after optimizations
- Improvement quantified
- Before/after comparison report generated
- Performance targets met
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] JMeter test plan created and executable
- [ ] Gatling simulation created and executable
- [ ] Test runner script functional
- [ ] Baseline test completed
- [ ] Load tests completed (500, 1000, 2000 users)
- [ ] Stress test completed (5000+ users)
- [ ] Endurance test completed (24 hours)
- [ ] Performance analysis report created
- [ ] Bottlenecks identified and documented
- [ ] Optimizations applied
- [ ] Post-optimization tests show improvement
- [ ] Performance targets met (or gaps documented)

---

## Dependencies

**Required**:
- Plan 3.1: Kafka Audit Log Integration (completed)
- Plan 3.3: Performance Optimization (completed)
- Plan 3.4: 监控与告警系统 (completed)
- Running application for testing
- Prometheus/Grafana for monitoring during tests

**Tools Required**:
- JMeter 5.x
- Gatling 3.x
- Node.js (for test data generation)

---

## Success Criteria

1. All test types executed successfully
2. Performance bottlenecks identified
3. Optimizations applied and validated
4. Login response time < 100ms (P95) achieved OR gaps documented
5. API response time < 200ms (P95) achieved OR gaps documented
6. System handles 10,000 concurrent users OR capacity documented
7. Comprehensive performance report generated

---

*Plan: 06*
*Phase: phase-03-production-ready*
*Created: 2026-03-27*
