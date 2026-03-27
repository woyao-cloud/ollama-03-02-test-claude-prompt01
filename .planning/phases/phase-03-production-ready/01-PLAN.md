# Plan 3.1: Kafka Audit Log Integration

**Phase**: 3 - Production Ready
**Status**: Ready for Execution
**Priority**: High
**Estimated Effort**: 2-3 sessions

---

## Objective

Convert the current synchronous audit logging to asynchronous processing using Apache Kafka:
- Decouple audit log production from main application flow
- Improve application performance by offloading audit writes
- Enable reliable audit log processing with message persistence
- Support high-throughput audit logging for enterprise scale

---

## Requirements Addressed

- **AUDIT-03**: Kafka 异步日志 (Asynchronous audit logging via Kafka)
- **PERF-07**: 审计日志异步处理 (Asynchronous audit log processing)

---

## Current State

**Existing Audit Log Implementation**:
- `AuditLog` entity with JPA annotations and JSONB columns
- `AuditLogService` interface with `logOperation()`, `logSuccess()`, `logFailure()` methods
- `AuditLogServiceImpl` uses `@Async("auditLogExecutor")` for async DB writes
- 17 operation types supported (CREATE, UPDATE, DELETE, LOGIN, etc.)
- Direct PostgreSQL storage via JPA repository

**Current Flow**:
```
Application → @Async Executor → JPA Repository → PostgreSQL
```

**Target Flow**:
```
Application → Kafka Producer → Kafka Topic → Kafka Consumer → JPA Repository → PostgreSQL
```

---

## Implementation Tasks

### Task 1: Add Kafka Dependencies

**<read_first>**
- `backend/pom.xml` (current dependencies)
- Check Spring Boot Kafka starter compatibility
**</read_first>**

**<action>**
Add to `backend/pom.xml`:
```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Kafka Test -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers Kafka -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
```
**</action>**

**<acceptance_criteria>**
- Kafka dependencies added to pom.xml
- `mvn dependency:resolve` succeeds
- Spring Kafka version compatible with Spring Boot 3.5
**</acceptance_criteria>**

---

### Task 2: Create Audit Log Event DTO

**<read_first>**
- `backend/src/main/java/com/usermanagement/domain/entity/AuditLog.java` (entity structure)
- `backend/src/main/java/com/usermanagement/service/dto/AuditLogDTO.java` (existing DTO pattern)
**</read_first>**

**<action>**
Create `backend/src/main/java/com/usermanagement/service/dto/AuditLogEvent.java`:
```java
/**
 * Audit Log Event for Kafka messaging
 * Serializable event representing an audit log entry
 */
public class AuditLogEvent {
    private UUID userId;
    private String username;
    private AuditLog.OperationType operation;
    private String resourceType;
    private UUID resourceId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String description;
    private String clientIp;
    private String userAgent;
    private String sessionId;
    private boolean success;
    private String errorMessage;
    private Integer executionTimeMs;
    private Instant timestamp;

    // Factory method from current request context
    public static AuditLogEvent fromContext(
            OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            boolean success,
            String errorMessage) { ... }

    // Getters and setters
}
```
**</action>**

**<acceptance_criteria>**
- `AuditLogEvent.java` created with all required fields
- Fields match `AuditLog` entity structure
- Serializable for Kafka message transport
- Factory method for easy creation from context
**</acceptance_criteria>**

---

### Task 3: Configure Kafka Properties

**<read_first>**
- `backend/src/main/resources/application.yml` (existing configuration)
- `backend/src/main/resources/application-dev.yml` (dev profile)
**</read_first>**

**<action>**
Add Kafka configuration to `application.yml`:
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      retries: 3
      acks: all
      compression-type: snappy
    consumer:
      group-id: audit-log-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.usermanagement.service.dto

# Application-specific Kafka settings
app:
  kafka:
    topics:
      audit-logs: audit-logs
      audit-logs-retry: audit-logs-retry
      audit-logs-dlt: audit-logs-dlt
    partitions: 3
    replication-factor: 1
```
**</action>**

**<acceptance_criteria>**
- Kafka bootstrap servers configurable via environment
- Producer configured with JSON serializer and retries
- Consumer configured with proper group ID and offset reset
- Topic names externalized to configuration
**</acceptance_criteria>**

---

### Task 4: Create Kafka Producer Service

**<action>**
Create `backend/src/main/java/com/usermanagement/service/kafka/AuditLogKafkaProducer.java`:
```java
/**
 * Kafka Producer for Audit Log Events
 * Publishes audit log events to Kafka topic
 */
@Service
public class AuditLogKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaProducer.class);

    private final KafkaTemplate<String, AuditLogEvent> kafkaTemplate;
    private final String auditLogsTopic;

    public AuditLogKafkaProducer(
            KafkaTemplate<String, AuditLogEvent> kafkaTemplate,
            @Value("${app.kafka.topics.audit-logs}") String auditLogsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditLogsTopic = auditLogsTopic;
    }

    /**
     * Send audit log event to Kafka
     */
    public void sendAuditLogEvent(AuditLogEvent event) {
        String key = event.getUserId() != null ? event.getUserId().toString() : "anonymous";

        CompletableFuture<SendResult<String, AuditLogEvent>> future =
            kafkaTemplate.send(auditLogsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to send audit log event to Kafka", ex);
            } else {
                logger.debug("Audit log event sent to topic {} partition {} offset {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
```
**</action>**

**<acceptance_criteria>**
- `AuditLogKafkaProducer.java` created
- Uses `KafkaTemplate` for message sending
- Configurable topic name via `@Value`
- Non-blocking send with CompletableFuture callback
- Proper error logging for failed sends
**</acceptance_criteria>**

---

### Task 5: Create Async Audit Log Service (Kafka-Based)

**<read_first>**
- `backend/src/main/java/com/usermanagement/service/AuditLogServiceImpl.java` (current implementation)
**</read_first>**

**<action>**
Create `backend/src/main/java/com/usermanagement/service/AsyncAuditLogService.java`:
```java
/**
 * Asynchronous Audit Log Service using Kafka
 * Replaces direct DB writes with Kafka message production
 */
@Service
@Primary
public class AsyncAuditLogService implements AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncAuditLogService.class);

    private final AuditLogKafkaProducer kafkaProducer;
    private final AuditLogRepository auditLogRepository; // For queries
    private final SecurityUtils securityUtils;

    public AsyncAuditLogService(
            AuditLogKafkaProducer kafkaProducer,
            AuditLogRepository auditLogRepository,
            SecurityUtils securityUtils) {
        this.kafkaProducer = kafkaProducer;
        this.auditLogRepository = auditLogRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public void logOperation(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            boolean success,
            String errorMessage) {

        try {
            AuditLogEvent event = AuditLogEvent.builder()
                .userId(securityUtils.getCurrentUserId())
                .username(getCurrentUsername())
                .operation(operation)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .clientIp(getClientIp())
                .userAgent(getUserAgent())
                .sessionId(getSessionId())
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .build();

            kafkaProducer.sendAuditLogEvent(event);
            logger.debug("Audit log event queued for operation: {} on {}:{}",
                operation, resourceType, resourceId);

        } catch (Exception e) {
            logger.error("Failed to queue audit log event", e);
            // Don't throw - audit logging should not break main flow
        }
    }

    // Delegate query methods to repository
    @Override
    public Page<AuditLogDTO> getAuditLogs(AuditLogQueryRequest query) {
        // Delegate to existing repository queries
    }

    // ... other query methods delegate to repository
}
```
**</action>**

**<acceptance_criteria>**
- `AsyncAuditLogService.java` created and marked `@Primary`
- Implements `AuditLogService` interface
- `logOperation()` produces to Kafka instead of direct DB write
- Query methods delegate to existing repository
- Non-blocking - doesn't throw exceptions to caller
**</acceptance_criteria>**

---

### Task 6: Create Kafka Consumer

**<action>**
Create `backend/src/main/java/com/usermanagement/service/kafka/AuditLogKafkaConsumer.java`:
```java
/**
 * Kafka Consumer for Audit Log Events
 * Consumes from Kafka topic and persists to database
 */
@Component
public class AuditLogKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogKafkaConsumer(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consume audit log event from Kafka
     */
    @KafkaListener(
        topics = "${app.kafka.topics.audit-logs}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditLogEvent(
            @Payload AuditLogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.debug("Received audit log event from partition {} offset {}: {}",
            partition, offset, event.getOperation());

        try {
            AuditLog auditLog = convertToEntity(event);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log saved to database: {}", auditLog.getId());

        } catch (Exception e) {
            logger.error("Failed to process audit log event", e);
            throw new AuditLogProcessingException("Failed to process audit log", e);
        }
    }

    /**
     * Retry listener for failed messages
     */
    @KafkaListener(
        topics = "${app.kafka.topics.audit-logs-retry}",
        groupId = "${spring.kafka.consumer.group-id}-retry"
    )
    public void consumeRetryEvent(@Payload AuditLogEvent event) {
        logger.warn("Processing retry for audit log event: {}", event.getOperation());
        consumeAuditLogEvent(event, -1, -1);
    }

    /**
     * Dead letter topic listener
     */
    @KafkaListener(
        topics = "${app.kafka.topics.audit-logs-dlt}",
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void consumeDeadLetterEvent(
            @Payload AuditLogEvent event,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        logger.error("Audit log event moved to DLT: {} - Error: {}",
            event.getOperation(), errorMessage);
        // Alert/notify ops team about failed audit log
    }

    private AuditLog convertToEntity(AuditLogEvent event) {
        AuditLog auditLog = new AuditLog();
        // Map fields from event to entity
        if (event.getUserId() != null) {
            User user = userRepository.findById(event.getUserId()).orElse(null);
            auditLog.setUser(user);
        }
        auditLog.setUsername(event.getUsername());
        auditLog.setOperation(event.getOperation());
        auditLog.setResourceType(event.getResourceType());
        auditLog.setResourceId(event.getResourceId());
        auditLog.setOldValue(event.getOldValue());
        auditLog.setNewValue(event.getNewValue());
        auditLog.setDescription(event.getDescription());
        auditLog.setClientIp(event.getClientIp());
        auditLog.setUserAgent(event.getUserAgent());
        auditLog.setSessionId(event.getSessionId());
        auditLog.setSuccess(event.isSuccess());
        auditLog.setErrorMessage(event.getErrorMessage());
        auditLog.setExecutionTimeMs(event.getExecutionTimeMs());
        auditLog.setCreatedAt(event.getTimestamp());
        return auditLog;
    }
}
```
**</action>**

**<acceptance_criteria>**
- `AuditLogKafkaConsumer.java` created with `@KafkaListener`
- Consumes from configured audit-logs topic
- Converts `AuditLogEvent` to `AuditLog` entity
- Persists to database via repository
- Retry topic and DLT (Dead Letter Topic) handlers
- Proper error handling and logging
**</acceptance_criteria>**

---

### Task 7: Configure Kafka Topics and Error Handling

**<action>**
Create `backend/src/main/java/com/usermanagement/config/KafkaConfig.java`:
```java
/**
 * Kafka Configuration
 * Topic definitions and producer/consumer factories
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${app.kafka.topics.audit-logs}")
    private String auditLogsTopic;

    @Value("${app.kafka.topics.audit-logs-retry}")
    private String auditLogsRetryTopic;

    @Value("${app.kafka.topics.audit-logs-dlt}")
    private String auditLogsDltTopic;

    @Value("${app.kafka.partitions:3}")
    private int partitions;

    @Value("${app.kafka.replication-factor:1}")
    private short replicationFactor;

    // Topic definitions
    @Bean
    public NewTopic auditLogsTopic() {
        return TopicBuilder.name(auditLogsTopic)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic auditLogsRetryTopic() {
        return TopicBuilder.name(auditLogsRetryTopic)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic auditLogsDltTopic() {
        return TopicBuilder.name(auditLogsDltTopic)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }

    // Producer factory with JSON serializer
    @Bean
    public ProducerFactory<String, AuditLogEvent> producerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(
            kafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AuditLogEvent> kafkaTemplate(
            ProducerFactory<String, AuditLogEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Consumer factory with JSON deserializer
    @Bean
    public ConsumerFactory<String, AuditLogEvent> consumerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(
            kafkaProperties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,
            "com.usermanagement.service.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
            "com.usermanagement.service.dto.AuditLogEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent>
            kafkaListenerContainerFactory(
                ConsumerFactory<String, AuditLogEvent> consumerFactory,
                KafkaTemplate<String, AuditLogEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Error handling with retry and DLT
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 3L) // 3 retries with 1 second interval
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
```
**</action>**

**<acceptance_criteria>**
- `KafkaConfig.java` created with `@EnableKafka`
- Topic beans for main, retry, and DLT topics
- Producer factory with JSON serializer
- Consumer factory with JSON deserializer and trusted packages
- Error handler with retry logic (3 retries, 1s interval)
- Dead letter publishing for failed messages
**</acceptance_criteria>**

---

### Task 8: Update Docker Compose for Kafka

**<read_first>**
- `docker-compose.yml` (if exists) or check project structure
**</read_first>**

**<action>**
Add Kafka and Zookeeper services to Docker Compose:
```yaml
services:
  # ... existing services (postgres, redis, app)

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: usermanagement-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - usermanagement-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: usermanagement-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    networks:
      - usermanagement-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: usermanagement-kafka-ui
    depends_on:
      - kafka
    ports:
      - "8081:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    networks:
      - usermanagement-network
```

Update app service environment:
```yaml
  app:
    # ... existing config
    environment:
      # ... existing env vars
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
      kafka:
        condition: service_healthy
```
**</action>**

**<acceptance_criteria>**
- Zookeeper service configured
- Kafka service with PLAINTEXT listeners
- Kafka UI for topic monitoring
- App service depends on Kafka health check
- `KAFKA_BOOTSTRAP_SERVERS` environment variable set
**</acceptance_criteria>**

---

### Task 9: Add Fallback/Synchronous Mode

**<action>**
Create `backend/src/main/java/com/usermanagement/service/FallbackAuditLogService.java`:
```java
/**
 * Fallback Audit Log Service
 * Direct DB write when Kafka is unavailable
 */
@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
public class FallbackAuditLogService extends AuditLogServiceImpl {
    // Extends existing implementation for direct DB writes
    // Used when Kafka is disabled
}
```

Update `application.yml`:
```yaml
app:
  kafka:
    enabled: true  # Set to false to disable Kafka and use direct DB writes
```
**</action>**

**<acceptance_criteria>**
- Fallback service for when Kafka is disabled
- Configuration flag `app.kafka.enabled`
- Graceful degradation to direct DB writes
**</acceptance_criteria>**

---

### Task 10: Write Unit Tests

**<action>**
Create `backend/src/test/java/com/usermanagement/service/kafka/AuditLogKafkaProducerTest.java`:
- Test sending audit log events
- Test error handling for failed sends

Create `backend/src/test/java/com/usermanagement/service/kafka/AuditLogKafkaConsumerTest.java`:
- Test consuming and persisting events
- Test retry logic
- Test DLT handling

Create `backend/src/test/java/com/usermanagement/service/AsyncAuditLogServiceTest.java`:
- Test log operation produces to Kafka
- Test query methods delegate to repository

Use `@EmbeddedKafka` for integration tests:
```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"audit-logs", "audit-logs-retry", "audit-logs-dlt"})
class AuditLogKafkaIntegrationTest {
    // Integration tests
}
```
**</action>**

**<acceptance_criteria>**
- Unit tests for Kafka producer
- Unit tests for Kafka consumer
- Integration tests with `@EmbeddedKafka`
- Test coverage >= 85%
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] Kafka dependencies added and resolved
- [ ] `AuditLogEvent` DTO created
- [ ] Kafka configuration in `application.yml`
- [ ] `AuditLogKafkaProducer` sends events to topic
- [ ] `AsyncAuditLogService` marked as `@Primary`
- [ ] `AuditLogKafkaConsumer` persists events to DB
- [ ] Retry and DLT topics configured
- [ ] Docker Compose includes Kafka and Zookeeper
- [ ] Kafka UI accessible for monitoring
- [ ] Unit tests cover producer, consumer, and service
- [ ] Integration tests with embedded Kafka pass
- [ ] Fallback mode works when Kafka disabled

---

## Dependencies

**Required** (must be completed first):
- Plan 1.5: Audit Log Framework (existing audit log entity and repository)

**Nice to have**:
- Docker and Docker Compose for local testing
- Kafka UI for monitoring topics

---

## Success Criteria

1. Audit log events are published to Kafka topic
2. Consumer processes events and persists to database
3. Main application flow is not blocked by audit logging
4. Retry mechanism handles transient failures
5. Failed messages go to DLT for manual inspection
6. Kafka can be disabled via configuration (fallback to sync)
7. All tests pass with >= 85% coverage

---

*Plan: 01*
*Phase: phase-03-production-ready*
*Created: 2026-03-25*
