# 开发环境启动指南

本文档介绍如何启动用户管理系统的完整开发环境，包括 Docker 基础设施、后端服务和前端开发服务器。

---

## 环境要求

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| Docker Desktop | 20.10+ | 运行基础设施服务 |
| JDK | 21 | 后端 Spring Boot 运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端运行 |
| npm | 9+ | 前端依赖管理 |

---

## 1. 启动 Docker 基础设施服务

Docker Compose 配置包含以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 生产级数据库 |
| Redis | 6379 | 缓存与会话存储 |
| Zookeeper | 2181 | Kafka 协调服务 |
| Kafka | 9092 / 29092 | 消息队列 |
| Kafka UI | 8081 | Kafka 管理界面 |
| pgAdmin | 5050 | PostgreSQL 管理界面 |

### 1.1 完整启动（推荐用于集成测试）

在项目根目录执行：

```bash
docker-compose -f docker-compose.yml up -d postgres redis zookeeper kafka kafka-ui pgadmin
# 启动所有基础设施服务
docker-compose -f docker-compose.dev.yml up -d postgres redis zookeeper kafka kafka-ui pgadmin

# 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f
```

### 1.2 仅启动数据库和缓存（快速开发）

如果后端使用 H2 内存数据库开发模式，只需启动 Redis：

```bash
docker-compose -f docker-compose.dev.yml up -d redis
```

### 1.3 验证服务启动

```bash
# 检查 PostgreSQL
docker exec ums-postgres pg_isready -U devuser

# 检查 Redis
docker exec ums-redis redis-cli ping
# 应返回: PONG

# 检查 Kafka
docker exec ums-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### 1.4 访问管理界面

| 服务 | 地址 | 默认账号 |
|------|------|---------|
| Kafka UI | http://localhost:8081 | 无需登录 |
| pgAdmin | http://localhost:5050 | admin@example.com / admin123 |

---

## 2. 启动后端服务（Spring Boot）

### 2.1 方式一：本地启动（推荐开发模式）

#### 步骤 1：进入后端目录

```bash
cd backend
```

#### 步骤 2：使用 H2 内存数据库（快速开发）

```bash
#本地
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 使用开发配置（默认使用 H2 内存数据库，无需 Docker）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 或 Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 步骤 3：使用 PostgreSQL 数据库（需要 Docker 基础设施）

```bash
# 修改 application-dev.yml 或创建本地配置
# 确保 docker-compose 中的 PostgreSQL 已启动

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 步骤 4：验证后端启动

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 预期响应
{
  "status": "UP"
}
```

### 2.2 方式二：IDE 中启动（推荐日常开发）

#### IntelliJ IDEA

1. 打开 `backend/pom.xml` 作为 Maven 项目
2. 找到 `com.usermanagement.Application` 类
3. 右键点击 → **Run 'Application.main()'**

#### VS Code

1. 安装 "Extension Pack for Java"
2. 打开 `backend/src/main/java/com/usermanagement/Application.java`
3. 点击上方的 **Run** 按钮

### 2.3 后端服务信息

| 项目 | 信息 |
|------|------|
| 服务地址 | http://localhost:8080 |
| API 前缀 | /api/v1 |
| H2 Console | http://localhost:8080/h2-console |
| Actuator | http://localhost:8080/actuator |

---

## 3. 启动前端开发服务器（Next.js）

### 3.1 安装依赖（首次）

```bash
cd frontend

# 使用 npm
npm install

# 或使用国内镜像加速
npm install --registry=https://registry.npmmirror.com
```

### 3.2 配置环境变量

创建 `.env.local` 文件（如果不存在）：

```bash
cp .env.example .env.local
```

编辑 `.env.local`：

```env
# API 配置
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1

# 开发环境
NODE_ENV=development
```

### 3.3 启动开发服务器

```bash
npm run dev
```

### 3.4 前端服务信息

| 项目 | 信息 |
|------|------|
| 开发服务器 | http://localhost:3000 |
| API 代理 | http://localhost:3000/api → http://localhost:8080/api |
| 热重载 | 已启用 |

---

## 4. 快速启动脚本

### 4.1 一键启动全部（Linux/Mac）

创建 `scripts/start-dev.sh`：

```bash
#!/bin/bash
set -e

echo "🚀 启动开发环境..."

# 1. 启动 Docker 基础设施
echo "📦 启动 Docker 服务..."
docker-compose -f docker-compose.dev.yml up -d postgres redis zookeeper kafka kafka-ui pgadmin

# 2. 等待服务就绪
echo "⏳ 等待服务就绪..."
sleep 10

# 3. 启动后端（在新终端）
echo "🔧 启动后端服务..."
osascript -e 'tell app "Terminal" to do script "cd '$(pwd)'/backend && ./mvnw spring-boot:run"'

# 4. 启动前端（在新终端）
echo "🎨 启动前端服务..."
osascript -e 'tell app "Terminal" to do script "cd '$(pwd)'/frontend && npm run dev"'

echo "✅ 开发环境启动完成！"
echo "📋 服务地址："
echo "   - 前端: http://localhost:3000"
echo "   - 后端: http://localhost:8080"
echo "   - Kafka UI: http://localhost:8081"
echo "   - pgAdmin: http://localhost:5050"
```

### 4.2 Windows PowerShell 脚本

创建 `scripts/start-dev.ps1`：

```powershell
Write-Host "🚀 启动开发环境..." -ForegroundColor Green

# 1. 启动 Docker 基础设施
Write-Host "📦 启动 Docker 服务..." -ForegroundColor Cyan
docker-compose -f docker-compose.dev.yml up -d postgres redis zookeeper kafka kafka-ui pgadmin

# 2. 等待服务就绪
Write-Host "⏳ 等待服务就绪..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 3. 启动后端（新窗口）
Write-Host "🔧 启动后端服务..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-Command", "cd '$PWD/backend'; ./mvnw.cmd spring-boot:run"

# 4. 启动前端（新窗口）
Write-Host "🎨 启动前端服务..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-Command", "cd '$PWD/frontend'; npm run dev"

Write-Host "✅ 开发环境启动完成！" -ForegroundColor Green
Write-Host "📋 服务地址："
Write-Host "   - 前端: http://localhost:3000"
Write-Host "   - 后端: http://localhost:8080"
Write-Host "   - Kafka UI: http://localhost:8081"
Write-Host "   - pgAdmin: http://localhost:5050"
```

---

## 5. 常用操作命令

### 5.1 Docker 操作

```bash
# 停止所有服务
docker-compose -f docker-compose.dev.yml down

# 停止并删除数据卷（谨慎使用）
docker-compose -f docker-compose.dev.yml down -v

# 重启单个服务
docker-compose -f docker-compose.dev.yml restart redis

# 查看服务日志
docker-compose -f docker-compose.dev.yml logs -f postgres
docker-compose -f docker-compose.dev.yml logs -f kafka
```

### 5.2 后端操作

```bash
cd backend

# 编译
./mvnw clean compile

# 运行测试
./mvnw test

# 打包
./mvnw clean package

# 生成测试覆盖率报告
./mvnw jacoco:report
```

### 5.3 前端操作

```bash
cd frontend

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 运行测试
npm test

# 代码检查
npm run lint
```

---

## 6. 故障排查

### 6.1 端口冲突

```bash
# 检查端口占用（Linux/Mac）
lsof -i :8080  # 后端端口
lsof -i :3000  # 前端端口
lsof -i :5432  # PostgreSQL

# Windows
netstat -ano | findstr :8080
```

### 6.2 Docker 服务无法启动

```bash
# 检查 Docker 状态
docker info

# 清理 Docker 缓存（谨慎使用）
docker system prune -f
docker volume prune -f
```

### 6.3 后端启动失败

1. 检查 JDK 版本：`java -version` （应为 21）
2. 检查 Maven 依赖：`./mvnw dependency:resolve`
3. 检查日志：`backend/target/logs/spring.log`

### 6.4 前端启动失败

1. 清除 node_modules：`rm -rf node_modules && npm install`
2. 检查 Node 版本：`node -v` （应为 18+）
3. 清除 Next.js 缓存：`rm -rf .next`

---

## 7. 开发工作流程

```
┌─────────────────────────────────────────────────────────────┐
│                    启动开发环境                              │
├─────────────────────────────────────────────────────────────┤
│  1. 启动 Docker 基础设施                                     │
│     └─ docker-compose -f docker-compose.dev.yml up -d       │
│                                                             │
│  2. 启动后端服务                                             │
│     └─ ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev│
│                                                             │
│  3. 启动前端服务                                             │
│     └─ cd frontend && npm run dev                           │
│                                                             │
│  4. 访问应用                                                 │
│     └─ 打开 http://localhost:3000                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 默认账号信息

系统启动时会自动创建以下默认账号：

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 系统管理员 | admin | Admin@123 | 全部权限 |
| 普通用户 | user | User@123 | 基础权限 |

---

## 相关文档

- [项目架构](./BACKEND_ARCHITECTURE.md)
- [API 规范](./API_SPECIFICATION.md)
- [认证流程](./AUTHENTICATION_FLOW.md)
- [测试策略](./TESTING_STRATEGY.md)
