# Docker Compose 集成指南

本文档供 **部署工程师** 参考，说明如何将测试数据生成脚本集成到 Docker Compose 环境中。

## 概述

测试数据生成脚本位于 `scripts/test-data/` 目录，包含：
- 6 个 SQL 数据文件（按顺序执行）
- 2 个执行脚本（Linux/macOS 和 Windows）

## 集成方式

### 方式一：PostgreSQL 初始化目录 (推荐用于首次部署)

PostgreSQL 官方镜像支持在首次启动时自动执行 `/docker-entrypoint-initdb.d/` 目录下的 SQL 脚本。

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: ums-postgres
    environment:
      POSTGRES_DB: user_management
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      # 挂载 Flyway 迁移脚本
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d/migrations:ro
      # 挂载测试数据脚本
      - ./scripts/test-data:/docker-entrypoint-initdb.d/test-data:ro
      # 创建初始化脚本确保执行顺序
      - ./scripts/init-db.sh:/docker-entrypoint-initdb.d/999-init-db.sh:ro
    networks:
      - ums-network

  backend:
    # ... 后端服务配置
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:

networks:
  ums-network:
    driver: bridge
```

创建初始化脚本 `scripts/init-db.sh`：

```bash
#!/bin/bash
set -e

echo "====================================="
echo "执行数据库迁移和测试数据初始化"
echo "====================================="

# 先执行 Flyway 迁移
for f in /docker-entrypoint-initdb.d/migrations/*.sql; do
    echo "执行迁移: $f"
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$f"
done

# 按顺序执行测试数据脚本
SCRIPTS=(
    "01-departments.sql"
    "02-roles.sql"
    "03-permissions.sql"
    "04-role-permissions.sql"
    "05-users.sql"
    "06-user-roles.sql"
)

for script in "${SCRIPTS[@]}"; do
    script_path="/docker-entrypoint-initdb.d/test-data/$script"
    if [ -f "$script_path" ]; then
        echo "执行测试数据: $script"
        psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$script_path"
    fi
done

echo "====================================="
echo "数据库初始化完成"
echo "====================================="
```

### 方式二：独立数据初始化容器

适用于需要在应用启动后执行测试数据的场景：

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: ums-postgres
    environment:
      POSTGRES_DB: user_management
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devuser -d user_management"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - ums-network

  # 数据初始化服务
  db-seed:
    image: postgres:15
    container_name: ums-db-seed
    environment:
      PGPASSWORD: devpassword
    volumes:
      - ./scripts/test-data:/scripts:ro
    command: >
      bash -c "
        echo '等待 PostgreSQL 就绪...' &&
        sleep 10 &&
        for f in /scripts/*.sql; do
          echo \"执行: $$f\" &&
          psql -h postgres -U devuser -d user_management -f \"$$f\";
        done &&
        echo '测试数据初始化完成'
      "
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - ums-network
    restart: "no"

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    container_name: ums-backend
    environment:
      DATABASE_URL: postgresql+asyncpg://devuser:devpassword@postgres:5432/user_management
      # ... 其他环境变量
    depends_on:
      - db-seed
    networks:
      - ums-network

volumes:
  postgres_data:

networks:
  ums-network:
    driver: bridge
```

### 方式三：后端应用初始化 (推荐用于开发环境)

让后端应用在启动时检查并初始化测试数据：

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    # ... 标准配置

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    container_name: ums-backend
    environment:
      DATABASE_URL: postgresql+asyncpg://devuser:devpassword@postgres:5432/user_management
      # 启用测试数据初始化
      SEED_TEST_DATA: "true"
      # 或者指向 SQL 文件路径
      SEED_DATA_PATH: "/app/scripts/test-data"
    volumes:
      - ./backend:/app
      - ./scripts/test-data:/app/scripts/test-data:ro
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - ums-network
```

### 方式四：手动执行 (推荐用于生产-like 环境)

```yaml
# docker-compose.override.yml (仅用于开发)
version: '3.8'

services:
  db-seed:
    image: postgres:15
    profiles: ["seed"]  # 使用 profiles 避免自动启动
    environment:
      PGPASSWORD: devpassword
    volumes:
      - ./scripts/test-data:/scripts:ro
    command: >
      bash -c "
        for f in /scripts/*.sql; do
          echo \"执行: $$f\" &&
          psql -h postgres -U devuser -d user_management -f \"$$f\";
        done
      "
    depends_on:
      - postgres
    networks:
      - ums-network
```

执行命令：

```bash
# 启动基础服务
docker-compose up -d postgres backend

# 手动执行数据初始化
docker-compose --profile seed run --rm db-seed

# 或者使用宿主机 psql
./scripts/test-data/init-test-data.sh
```

## CI/CD 集成

### GitHub Actions 示例

```yaml
# .github/workflows/test.yml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: user_management
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Setup test data
        run: |
          export PGPASSWORD=test
          for f in scripts/test-data/*.sql; do
            echo "执行: $f"
            psql -h localhost -U test -d user_management -f "$f"
          done

      - name: Run integration tests
        run: |
          # 执行集成测试
          ./mvnw test -Dtest=*IntegrationTest
```

### Makefile 示例

```makefile
# Makefile
.PHONY: seed-db reset-db

# 初始化测试数据
seed-db:
	@echo "初始化测试数据..."
	@./scripts/test-data/init-test-data.sh

# 重置数据库（删除所有数据并重新初始化）
reset-db:
	@echo "重置数据库..."
	@docker-compose exec -T postgres psql -U devuser -d user_management -c "DO \$$\$$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END \$$\$$;"
	@$(MAKE) seed-db

# 使用 Docker 执行
docker-seed:
	@docker-compose run --rm db-seed
```

## 环境变量配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | `localhost` | 数据库主机 |
| `DB_PORT` | `5432` | 数据库端口 |
| `DB_NAME` | `user_management` | 数据库名称 |
| `DB_USER` | `devuser` | 数据库用户 |
| `DB_PASSWORD` | `devpassword` | 数据库密码 |
| `SEED_TEST_DATA` | `false` | 是否自动初始化测试数据 |

## 最佳实践

1. **开发环境**: 使用方式一或方式三，自动初始化测试数据
2. **Team 环境**: 使用方式二，明确控制数据初始化时机
3. **CI/CD 环境**: 使用方式四，确保测试数据与代码版本一致
4. **生产环境**: 绝不使用测试数据脚本

## 故障排查

### 问题：脚本执行顺序错误

**原因**: SQL 文件没有按依赖关系执行

**解决**: 确保按编号顺序执行脚本 (01 -> 06)

### 问题：外键约束错误

**原因**: 关联数据尚未创建

**解决**: 检查 SQL 脚本执行顺序，确保父表数据先于子表

### 问题：权限不足

**原因**: 数据库用户没有执行权限

**解决**: 确保使用具有 DDL 权限的用户执行脚本

```sql
-- 授予权限示例
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO devuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO devuser;
```

## 联系信息

- **测试工程师**: 脚本内容问题
- **部署工程师**: Docker Compose 集成问题
