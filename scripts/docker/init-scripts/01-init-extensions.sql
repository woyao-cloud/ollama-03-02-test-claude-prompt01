#!/bin/bash
# =============================================
# PostgreSQL 初始化脚本 - Team 环境
# 在 PostgreSQL 首次启动时执行
# =============================================

set -e

echo "======================================"
echo "PostgreSQL 初始化脚本"
echo "======================================"

# 等待 PostgreSQL 完全启动
sleep 5

# 创建扩展
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- 创建必要的扩展
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";

    -- 设置时区
    SET timezone = 'Asia/Shanghai';

    -- 创建应用用户 (如果有需要)
    -- CREATE USER appuser WITH PASSWORD 'apppassword';
    -- GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO appuser;
EOSQL

echo "扩展创建完成"

# 创建 Flyway 历史表（如果不存在）
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE TABLE IF NOT EXISTS flyway_schema_history (
        installed_rank INTEGER NOT NULL,
        version VARCHAR(50),
        description VARCHAR(200) NOT NULL,
        type VARCHAR(20) NOT NULL,
        script VARCHAR(1000) NOT NULL,
        checksum INTEGER,
        installed_by VARCHAR(100) NOT NULL,
        installed_on TIMESTAMP NOT NULL DEFAULT now(),
        execution_time INTEGER NOT NULL,
        success BOOLEAN NOT NULL,
        PRIMARY KEY (installed_rank)
    );
EOSQL

echo "Flyway 历史表检查完成"
echo "======================================"
echo "初始化完成"
echo "======================================"
