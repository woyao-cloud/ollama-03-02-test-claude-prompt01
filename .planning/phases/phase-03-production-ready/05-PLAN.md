---
phase: 3
plan: 05
title: Kubernetes 部署
requirements_addressed: [PERF-07]
depends_on: [Plan 3.1, Plan 3.4]
wave: 3
autonomous: false
---

# Plan 3.5: Kubernetes 部署

## Objective

创建完整的 Kubernetes 部署配置，实现生产环境容器编排：
- Deployment 配置 (多副本、滚动更新)
- Service 配置 (ClusterIP/LoadBalancer)
- Ingress 配置 (HTTPS、域名)
- ConfigMap 与 Secret 管理
- HPA 自动扩缩容
- PodDisruptionBudget
- NetworkPolicy 网络隔离
- Resource Quotas 资源限制

**Purpose:** Kubernetes 提供高可用、可扩展的生产环境部署方案，支持自动故障恢复和弹性扩缩容。

**Output:**
- Kubernetes 资源清单文件 (YAML)
- Helm Chart (可选)
- Kustomize 配置
- CI/CD 集成配置
- 部署脚本

---

## Context

### 目标架构
```
                    ┌─────────────────┐
                    │   Ingress       │
                    │   Controller    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──────┐ ┌────▼────────┐ ┌───▼──────────┐
    │   App Pod x3   │ │  Redis      │ │  PostgreSQL  │
    │   (Stateless)  │ │  Cluster    │ │  StatefulSet │
    └────────────────┘ └─────────────┘ └──────────────┘
```

### 环境规划
| 环境 | Namespace | 副本数 | 资源限制 |
|------|-----------|--------|----------|
| SIT | sit | 1 | 0.5C/512M |
| UAT | uat | 2 | 1C/1G |
| PROD | production | 3+ | 2C/2G |

### K8s 组件
- Deployment: 无状态应用
- StatefulSet: PostgreSQL (可选，建议用云数据库)
- Service: 内部服务发现
- Ingress: 外部访问入口
- ConfigMap/Secret: 配置管理
- HPA: 水平自动扩缩容
- PDB: 故障转移保护

---

## Tasks

### Task 1: Create Namespace Definitions

**<action>**
Create `kubernetes/namespaces.yml`:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: sit
  labels:
    environment: sit
    team: platform

---
apiVersion: v1
kind: Namespace
metadata:
  name: uat
  labels:
    environment: uat
    team: platform

---
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    environment: production
    team: platform
```
**</action>**

**<acceptance_criteria>**
- SIT namespace defined
- UAT namespace defined
- Production namespace defined
- Environment labels applied
**</acceptance_criteria>**

---

### Task 2: Create ConfigMap

**<action>**
Create `kubernetes/configmap.yml`:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: usermanagement-config
  namespace: production
data:
  # Application configuration
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_APPLICATION_NAME: "usermanagement"

  # Server configuration
  SERVER_PORT: "8080"
  SERVER_SHUTDOWN: "graceful"

  # Logging
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_USERMANAGEMENT: "DEBUG"
  LOGGING_PATTERN_CONSOLE: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

  # Database (JDBC URL only, credentials in Secret)
  DATABASE_HOST: "postgres.production.svc.cluster.local"
  DATABASE_PORT: "5432"
  DATABASE_NAME: "user_management"

  # Redis
  REDIS_HOST: "redis.production.svc.cluster.local"
  REDIS_PORT: "6379"

  # Kafka
  KAFKA_BOOTSTRAP_SERVERS: "kafka.production.svc.cluster.local:9092"

  # JWT configuration
  JWT_EXPIRATION_MS: "3600000"
  JWT_REFRESH_EXPIRATION_MS: "604800000"

  # CORS
  CORS_ALLOWED_ORIGINS: "https://app.example.com,https://admin.example.com"

  # Management
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,metrics,prometheus"
  MANAGEMENT_METRICS_TAGS_APPLICATION: "usermanagement"
```
**</action>**

**<acceptance_criteria>**
- ConfigMap created with all application settings
- Namespace set to production
- Database/Redis/Kafka hostnames use K8s DNS
- Sensitive values (passwords) NOT in ConfigMap
**</acceptance_criteria>**

---

### Task 3: Create Secrets

**<action>**
Create `kubernetes/secrets.yml` (template - use sealed-secrets or external-secrets in production):
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: usermanagement-secrets
  namespace: production
type: Opaque
stringData:
  # Database credentials
  DATABASE_USERNAME: "usermanagement_prod"
  DATABASE_PASSWORD: "CHANGE_ME_USE_STRONG_PASSWORD"

  # Redis password (if enabled)
  REDIS_PASSWORD: "CHANGE_ME_USE_STRONG_PASSWORD"

  # JWT secrets
  JWT_SECRET_KEY: "CHANGE_ME_USE_BASE64_ENCODED_SECRET"
  JWT_PRIVATE_KEY: |
    -----BEGIN RSA PRIVATE KEY-----
    CHANGE_ME_USE_GENERATED_KEY
    -----END RSA PRIVATE KEY-----
  JWT_PUBLIC_KEY: |
    -----BEGIN PUBLIC KEY-----
    CHANGE_ME_USE_GENERATED_KEY
    -----END PUBLIC KEY-----

  # Email configuration
  MAIL_HOST: "smtp.example.com"
  MAIL_USERNAME: "noreply@example.com"
  MAIL_PASSWORD: "CHANGE_ME"

  # SMS provider (Twilio/Aliyun)
  SMS_API_KEY: "CHANGE_ME"
  SMS_API_SECRET: "CHANGE_ME"

  # OAuth2 clients
  OAUTH2_GOOGLE_CLIENT_ID: "CHANGE_ME"
  OAUTH2_GOOGLE_CLIENT_SECRET: "CHANGE_ME"
```

**Note:** In production, use:
- Sealed Secrets (Bitnami)
- External Secrets Operator (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
- SOPS + Age encryption
**</action>**

**<acceptance_criteria>**
- Secret template created
- Database credentials defined
- JWT keys defined
- OAuth2 credentials defined
- Warning about production secret management
**</acceptance_criteria>**

---

### Task 4: Create Deployment

**<action>**
Create `kubernetes/deployment.yml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: usermanagement
  template:
    metadata:
      labels:
        app: usermanagement
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      # Service account for K8s API access (if needed)
      serviceAccountName: usermanagement

      # Security context
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000

      # Init containers
      initContainers:
        # Wait for database to be ready
        - name: wait-for-postgres
          image: busybox:1.36
          command:
            - sh
            - -c
            - |
              until nc -z postgres.production.svc.cluster.local 5432; do
                echo "Waiting for PostgreSQL...";
                sleep 2;
              done;
              echo "PostgreSQL is ready!";

        # Wait for Redis
        - name: wait-for-redis
          image: busybox:1.36
          command:
            - sh
            - -c
            - |
              until nc -z redis.production.svc.cluster.local 6379; do
                echo "Waiting for Redis...";
                sleep 2;
              done;
              echo "Redis is ready!";

      # Main containers
      containers:
        - name: usermanagement
          image: registry.example.com/usermanagement:${IMAGE_TAG:-latest}
          imagePullPolicy: Always

          # Ports
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
            - name: actuator
              containerPort: 8081
              protocol: TCP

          # Environment variables
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: POD_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.podIP

          # ConfigMap and Secret references
          envFrom:
            - configMapRef:
                name: usermanagement-config
            - secretRef:
                name: usermanagement-secrets

          # Resource limits
          resources:
            requests:
              cpu: "1000m"
              memory: "1Gi"
            limits:
              cpu: "2000m"
              memory: "2Gi"

          # Health checks
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: actuator
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
            successThreshold: 1

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: actuator
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 5
            failureThreshold: 3
            successThreshold: 1

          startupProbe:
            httpGet:
              path: /actuator/health/startup
              port: actuator
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 5
            failureThreshold: 30

          # Volume mounts
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            - name: logs
              mountPath: /app/logs

          # Security context
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL

        # Sidecar for log shipping (optional)
        - name: log-shipping
          image: grafana/promtail:2.9.0
          args:
            - -config.file=/etc/promtail/config.yml
          volumeMounts:
            - name: logs
              mountPath: /app/logs
              readOnly: true
            - name: promtail-config
              mountPath: /etc/promtail

      # Volumes
      volumes:
        - name: tmp
          emptyDir: {}
        - name: logs
          emptyDir: {}
        - name: promtail-config
          configMap:
            name: promtail-config

      # Image pull secrets
      imagePullSecrets:
        - name: registry-credentials

      # Affinity rules
      affinity:
        # Spread pods across nodes
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: usermanagement
                topologyKey: kubernetes.io/hostname

      # Termination grace period
      terminationGracePeriodSeconds: 60

      # DNS configuration
      dnsPolicy: ClusterFirst

      # Node selector (optional)
      # nodeSelector:
      #   node-type: application

      # Tolerations (optional)
      # tolerations:
      #   - key: "dedicated"
      #     operator: "Equal"
      #     value: "application"
      #     effect: "NoSchedule"
```
**</action>**

**<acceptance_criteria>**
- Deployment with 3 replicas
- Rolling update strategy (maxSurge=1, maxUnavailable=0)
- Init containers wait for dependencies
- Liveness/readiness/startup probes configured
- Resource requests/limits defined
- Security context configured
- Pod anti-affinity for HA
- ConfigMap and Secret mounted via envFrom
**</acceptance_criteria>**

---

### Task 5: Create Service

**<action>**
Create `kubernetes/service.yml`:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
  annotations:
    # Prometheus annotations
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
spec:
  type: ClusterIP
  ports:
    - name: http
      port: 80
      targetPort: http
      protocol: TCP
    - name: actuator
      port: 8081
      targetPort: actuator
      protocol: TCP
  selector:
    app: usermanagement

---
# Headless service for direct pod access (optional)
apiVersion: v1
kind: Service
metadata:
  name: usermanagement-headless
  namespace: production
spec:
  type: ClusterIP
  clusterIP: None
  ports:
    - name: http
      port: 8080
      targetPort: http
  selector:
    app: usermanagement
```
**</action>**

**<acceptance_criteria>**
- ClusterIP Service created
- Port 80 maps to container port 8080
- Actuator port exposed for monitoring
- Selector matches Deployment labels
- Headless service for direct pod access
**</acceptance_criteria>**

---

### Task 6: Create Ingress

**<action>**
Create `kubernetes/ingress.yml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
  annotations:
    # NGINX Ingress Controller annotations
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"

    # Rate limiting
    nginx.ingress.kubernetes.io/limit-rps: "100"
    nginx.ingress.kubernetes.io/limit-burst: "200"

    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://app.example.com,https://admin.example.com"
    nginx.ingress.kubernetes.io/cors-allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
    nginx.ingress.kubernetes.io/cors-allow-headers: "Authorization, Content-Type, X-Requested-With"

    # Security headers
    nginx.ingress.kubernetes.io/configuration-snippet: |
      add_header X-Frame-Options "SAMEORIGIN" always;
      add_header X-Content-Type-Options "nosniff" always;
      add_header X-XSS-Protection "1; mode=block" always;
      add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Proxy settings
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"

    # SSL certificate
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
    - hosts:
        - api.example.com
        - admin.example.com
      secretName: usermanagement-tls
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: usermanagement
                port:
                  number: 80
    - host: admin.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: usermanagement
                port:
                  number: 80
```
**</action>**

**<acceptance_criteria>**
- Ingress resource created
- TLS termination configured
- SSL redirect enabled
- Rate limiting configured (100 RPS)
- CORS headers configured
- Security headers added
- cert-manager integration for Let's Encrypt
**</acceptance_criteria>**

---

### Task 7: Create HPA (Horizontal Pod Autoscaler)

**<action>**
Create `kubernetes/hpa.yml`:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: usermanagement
  minReplicas: 3
  maxReplicas: 10

  # Scaling metrics
  metrics:
    # CPU-based scaling
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70

    # Memory-based scaling
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80

    # Custom metric: requests per second
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "100"

  # Scaling behavior
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 4
          periodSeconds: 15
      selectPolicy: Max
```
**</action>**

**<acceptance_criteria>**
- HPA v2 API used
- Min 3, Max 10 replicas
- CPU scaling at 70% utilization
- Memory scaling at 80% utilization
- Custom metric scaling (RPS)
- Scale down stabilization: 5 minutes
- Scale up: aggressive (no stabilization)
**</acceptance_criteria>**

---

### Task 8: Create PodDisruptionBudget

**<action>**
Create `kubernetes/pdb.yml`:
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: usermanagement

---
# Alternative: maxUnavailable version
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: usermanagement-max-unavailable
  namespace: production
  labels:
    app: usermanagement
spec:
  maxUnavailable: 1
  selector:
    matchLabels:
      app: usermanagement
```
**</action>**

**<acceptance_criteria>**
- PDB with minAvailable: 2
- Ensures HA during voluntary disruptions
- Selector matches Deployment labels
**</acceptance_criteria>**

---

### Task 9: Create NetworkPolicy

**<action>**
Create `kubernetes/networkpolicy.yml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: usermanagement
  namespace: production
  labels:
    app: usermanagement
spec:
  podSelector:
    matchLabels:
      app: usermanagement
  policyTypes:
    - Ingress
    - Egress

  # Ingress rules
  ingress:
    # Allow from Ingress controller
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080
        - protocol: TCP
          port: 8081

    # Allow from monitoring namespace (Prometheus)
    - from:
        - namespaceSelector:
            matchLabels:
              name: monitoring
      ports:
        - protocol: TCP
          port: 8080

  # Egress rules
  egress:
    # Allow DNS resolution
    - to:
        - namespaceSelector: {}
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53

    # Allow PostgreSQL
    - to:
        - namespaceSelector:
            matchLabels:
              name: production
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432

    # Allow Redis
    - to:
        - namespaceSelector:
            matchLabels:
              name: production
        - podSelector:
            matchLabels:
              app: redis
      ports:
        - protocol: TCP
          port: 6379

    # Allow Kafka
    - to:
        - namespaceSelector:
            matchLabels:
              name: production
        - podSelector:
            matchLabels:
              app: kafka
      ports:
        - protocol: TCP
          port: 9092

    # Allow HTTPS outbound (for OAuth2, email, SMS)
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
            except:
              - 10.0.0.0/8
              - 172.16.0.0/12
              - 192.168.0.0/16
      ports:
        - protocol: TCP
          port: 443
```
**</action>**

**<acceptance_criteria>**
- NetworkPolicy created
- Ingress restricted to Ingress controller and monitoring
- Egress restricted to DNS, database, cache, message queue
- External HTTPS allowed for OAuth2/email/SMS
- Private IP ranges blocked for egress
**</acceptance_criteria>**

---

### Task 10: Create Kustomize Configuration

**<action>**
Create `kubernetes/base/kustomization.yml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - namespace.yml
  - configmap.yml
  - secrets.yml
  - deployment.yml
  - service.yml
  - ingress.yml
  - hpa.yml
  - pdb.yml
  - networkpolicy.yml

commonLabels:
  app: usermanagement
  managed-by: kustomize
```

Create `kubernetes/overlays/production/kustomization.yml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: production

resources:
  - ../../base

namePrefix: prod-

replicas:
  - name: usermanagement
    count: 3

patches:
  # Override image tag
  - patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/image
        value: registry.example.com/usermanagement:v1.0.0
    target:
      kind: Deployment
      name: usermanagement

  # Override resource limits for production
  - patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/cpu
        value: "2000m"
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/memory
        value: "2Gi"
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/cpu
        value: "4000m"
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/memory
        value: "4Gi"
    target:
      kind: Deployment
      name: usermanagement

configMapGenerator:
  - name: usermanagement-config
    behavior: merge
    literals:
      - SPRING_PROFILES_ACTIVE=prod
      - LOGGING_LEVEL_ROOT=WARN

secretGenerator:
  - name: usermanagement-secrets
    behavior: merge
    envs:
      - secrets.env

images:
  - name: registry.example.com/usermanagement
    newTag: v1.0.0
```

Create `kubernetes/overlays/staging/kustomization.yml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: sit

resources:
  - ../../base

namePrefix: staging-

replicas:
  - name: usermanagement
    count: 1

patches:
  # Reduced resources for staging
  - patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/cpu
        value: "500m"
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/memory
        value: "512Mi"
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/cpu
        value: "1000m"
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/memory
        value: "1Gi"
    target:
      kind: Deployment
      name: usermanagement
```
**</action>**

**<acceptance_criteria>**
- Kustomize base configuration created
- Production overlay with 3 replicas
- Staging overlay with 1 replica
- Resource limits customized per environment
- Image tags managed via Kustomize
**</acceptance_criteria>**

---

### Task 11: Create Deployment Scripts

**<action>**
Create `scripts/deploy-k8s.sh`:
```bash
#!/bin/bash
set -euo pipefail

# Configuration
NAMESPACE="${NAMESPACE:-production}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${REGISTRY:-registry.example.com}"
KUSTOMIZE_PATH="${KUSTOMIZE_PATH:-kubernetes/overlays/${NAMESPACE}}"

echo "=========================================="
echo "Deploying UserManagement to ${NAMESPACE}"
echo "=========================================="
echo ""

# 1. Build and push image
echo "Step 1: Building Docker image..."
docker build -t ${REGISTRY}/usermanagement:${IMAGE_TAG} -f backend/Dockerfile backend/

echo "Step 2: Pushing image to registry..."
docker push ${REGISTRY}/usermanagement:${IMAGE_TAG}

# 2. Update Kustomize with image tag
echo "Step 3: Updating Kustomize..."
cd kubernetes/overlays/${NAMESPACE}
kustomize edit set image ${REGISTRY}/usermanagement:${IMAGE_TAG}
cd -

# 3. Apply Kubernetes manifests
echo "Step 4: Applying Kubernetes manifests..."
kubectl apply -k ${KUSTOMIZE_PATH}

# 4. Wait for rollout
echo "Step 5: Waiting for rollout to complete..."
kubectl -n ${NAMESPACE} rollout status deployment/usermanagement

# 5. Verify deployment
echo "Step 6: Verifying deployment..."
kubectl -n ${NAMESPACE} get pods -l app=usermanagement
kubectl -n ${NAMESPACE} get svc usermanagement
kubectl -n ${NAMESPACE} get ingress usermanagement

echo ""
echo "=========================================="
echo "Deployment complete!"
echo "=========================================="
echo ""
echo "Useful commands:"
echo "  kubectl -n ${NAMESPACE} get pods"
echo "  kubectl -n ${NAMESPACE} logs -l app=usermanagement"
echo "  kubectl -n ${NAMESPACE} describe deployment usermanagement"
```
**</action>**

**<acceptance_criteria>**
- Deployment script created
- Image build and push
- Kustomize application
- Rollout status check
- Deployment verification
**</acceptance_criteria>**

---

### Task 12: Create Helm Chart (Optional)

**<action>**
Create `helm/usermanagement/Chart.yaml`:
```yaml
apiVersion: v2
name: usermanagement
description: User Management System Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - user-management
  - rbac
  - spring-boot
home: https://github.com/example/usermanagement
sources:
  - https://github.com/example/usermanagement
maintainers:
  - name: Platform Team
    email: platform@example.com
```

Create `helm/usermanagement/values.yaml`:
```yaml
# Default values for usermanagement
replicaCount: 3

image:
  repository: registry.example.com/usermanagement
  pullPolicy: Always
  tag: "latest"

imagePullSecrets:
  - name: registry-credentials

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: api.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: usermanagement-tls
      hosts:
        - api.example.com

resources:
  limits:
    cpu: 2000m
    memory: 2Gi
  requests:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

# Database configuration
database:
  host: postgres.production.svc.cluster.local
  port: 5432
  name: user_management

# Redis configuration
redis:
  enabled: true
  host: redis.production.svc.cluster.local
  port: 6379

# Monitoring
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
```
**</action>**

**<acceptance_criteria>**
- Helm Chart.yaml created
- values.yaml with configurable parameters
- Image, service, ingress, resources configurable
- Autoscaling configuration
- Database and Redis configuration
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] Namespace definitions created
- [ ] ConfigMap with application configuration
- [ ] Secrets template created (with production warning)
- [ ] Deployment with rolling update strategy
- [ ] Service (ClusterIP) created
- [ ] Ingress with TLS and security headers
- [ ] HPA with CPU/memory/custom metrics
- [ ] PodDisruptionBudget for HA
- [ ] NetworkPolicy for network isolation
- [ ] Kustomize overlays for environments
- [ ] Deployment script functional
- [ ] Helm chart created (optional)

---

## Dependencies

**Required**:
- Plan 3.1: Kafka Audit Log Integration (completed)
- Plan 3.4: 监控与告警系统 (completed)
- Kubernetes cluster (1.25+)
- Container registry access

**Optional**:
- Helm 3.x
- Kustomize (included in kubectl 1.14+)
- cert-manager for TLS
- NGINX Ingress Controller

---

## Success Criteria

1. Application deploys successfully to Kubernetes
2. Rolling updates work without downtime
3. HPA scales based on load
4. NetworkPolicy restricts traffic appropriately
5. Ingress routes traffic with TLS termination
6. Health checks pass
7. Monitoring integration working
8. Deployment script automates the process

---

*Plan: 05*
*Phase: phase-03-production-ready*
*Created: 2026-03-27*
