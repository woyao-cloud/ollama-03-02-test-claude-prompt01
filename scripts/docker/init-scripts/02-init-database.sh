#!/bin/bash
# =============================================
# PostgreSQL 初始化脚本 - Team 环境
# 在 PostgreSQL 首次启动时执行
# 执行顺序: 先建表，后插入测试数据
# =============================================

set -e

echo "======================================"
echo "PostgreSQL 初始化脚本"
echo "======================================"

# 等待 PostgreSQL 完全启动
sleep 5

# 获取环境变量
DB_USER="${POSTGRES_USER:-teamuser}"
DB_NAME="${POSTGRES_DB:-user_management}"

echo ""
echo "============================================"
echo "第一步: 创建数据库扩展"
echo "============================================"

# 创建扩展
psql -v ON_ERROR_STOP=1 --username "$DB_USER" --dbname "$DB_NAME" <<-EOSQL
    -- 创建必要的扩展
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";

    -- 设置时区
    SET timezone = 'Asia/Shanghai';
EOSQL

echo "扩展创建完成"

echo ""
echo "============================================"
echo "第二步: 创建数据库表结构"
echo "============================================"

# 检查建表脚本目录是否存在
if [ -d "/docker-entrypoint-initdb.d/migrations" ]; then
    for f in /docker-entrypoint-initdb.d/migrations/V*.sql; do
        if [ -f "$f" ]; then
            echo "执行建表脚本: $(basename $f)"
            psql -v ON_ERROR_STOP=1 --username "$DB_USER" --dbname "$DB_NAME" -f "$f" || {
                echo "错误: $f 执行失败"
                exit 1
            }
        fi
    done
    echo "表结构创建完成"
else
    echo "警告: 未找到建表脚本目录 /docker-entrypoint-initdb.d/migrations"
fi

echo ""
echo "============================================"
echo "第三步: 插入测试数据"
echo "============================================"

# 检查测试数据脚本目录是否存在
if [ -d "/docker-entrypoint-initdb.d/test-data" ]; then
    for f in /docker-entrypoint-initdb.d/test-data/01-*.sql \
             /docker-entrypoint-initdb.d/test-data/02-*.sql \
             /docker-entrypoint-initdb.d/test-data/03-*.sql \
             /docker-entrypoint-initdb.d/test-data/04-*.sql \
             /docker-entrypoint-initdb.d/test-data/05-*.sql \
             /docker-entrypoint-initdb.d/test-data/06-*.sql; do
        if [ -f "$f" ]; then
            echo "执行测试数据: $(basename $f)"
            psql -v ON_ERROR_STOP=0 --username "$DB_USER" --dbname "$DB_NAME" -f "$f" || {
                echo "警告: $f 执行失败，继续执行后续脚本"
            }
        fi
    done
    echo "测试数据插入完成"
else
    echo "警告: 未找到测试数据脚本目录 /docker-entrypoint-initdb.d/test-data"
fi

echo ""
echo "======================================"
echo "数据库初始化完成!"
echo "======================================"
echo ""
echo "测试账号 (密码: Test@123):"
echo "  超级管理员: superadmin@test.com"
echo "  系统管理员: admin@test.com"
echo ""
