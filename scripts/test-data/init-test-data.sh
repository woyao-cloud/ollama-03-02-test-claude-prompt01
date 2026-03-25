#!/bin/bash
# =============================================
# 本地测试数据初始化脚本
# 用于 Docker Compose 环境初始化测试数据
# =============================================

set -e

echo "========================================="
echo "User Management System - 测试数据初始化"
echo "========================================="
echo ""

# 数据库连接配置 (使用 docker-compose 中的配置)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-user_management}"
DB_USER="${DB_USER:-devuser}"
DB_PASSWORD="${DB_PASSWORD:-devpassword}"

# 可选：通过环境变量覆盖
# export DB_HOST=myhost
# export DB_PORT=5432
# export DB_NAME=mydb
# export DB_USER=myuser
# export DB_PASSWORD=mypassword

echo "数据库配置:"
echo "  主机: $DB_HOST"
echo "  端口: $DB_PORT"
echo "  数据库: $DB_NAME"
echo "  用户: $DB_USER"
echo ""

# 检查 psql 是否安装
if ! command -v psql &> /dev/null; then
    echo "错误: 未找到 psql 命令"
    echo "请安装 PostgreSQL 客户端工具:"
    echo "  - Ubuntu/Debian: sudo apt-get install postgresql-client"
    echo "  - macOS: brew install postgresql"
    echo "  - Windows: 使用 psql 所在目录的完整路径"
    exit 1
fi

# 设置 PGPASSWORD 环境变量
export PGPASSWORD="$DB_PASSWORD"

# 测试数据库连接
echo "正在测试数据库连接..."
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo "错误: 无法连接到数据库"
    echo "请检查:"
    echo "  1. PostgreSQL 服务是否运行"
    echo "  2. 数据库配置是否正确"
    echo "  3. 网络连接是否正常"
    exit 1
fi

echo "数据库连接成功!"
echo ""

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "开始执行测试数据脚本..."
echo ""

# 按顺序执行 SQL 脚本
SCRIPTS=(
    "01-departments.sql"
    "02-roles.sql"
    "03-permissions.sql"
    "04-role-permissions.sql"
    "05-users.sql"
    "06-user-roles.sql"
)

for script in "${SCRIPTS[@]}"; do
    script_path="$SCRIPT_DIR/$script"
    if [ -f "$script_path" ]; then
        echo "执行: $script ..."
        if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$script_path" 2>&1 | grep -E "(status|total_|ERROR)"; then
            echo "✓ $script 执行成功"
        else
            echo "✓ $script 执行完成"
        fi
        echo ""
    else
        echo "警告: 找不到脚本 $script_path"
    fi
done

echo "========================================="
echo "测试数据初始化完成!"
echo "========================================="
echo ""
echo "测试账号 (密码: Test@123):"
echo "  超级管理员: superadmin@test.com"
echo "  系统管理员: admin@test.com"
echo "  部门经理:   tech.lead@test.com"
echo "  开发工程师: fe.dev1@test.com"
echo "  测试工程师: qa.tester1@test.com"
echo ""
echo "特殊状态账号:"
echo "  待激活: pending.user@test.com"
echo "  已锁定: locked.user@test.com"
echo "  已禁用: inactive.user@test.com"
echo ""
