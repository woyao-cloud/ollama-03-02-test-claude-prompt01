#!/bin/sh
# =============================================
# 等待数据库就绪脚本
# 用于 Docker Compose 健康检查
# =============================================

set -e

HOST="$1"
PORT="$2"
USER="$3"
PASSWORD="$4"
MAX_RETRIES=60
RETRY_INTERVAL=2

if [ -z "$HOST" ] || [ -z "$PORT" ] || [ -z "$USER" ] || [ -z "$PASSWORD" ]; then
    echo "用法: $0 <host> <port> <user> <password>"
    exit 1
fi

echo "等待 PostgreSQL 就绪..."
echo "主机: $HOST:$PORT"
echo "用户: $USER"

count=0
while [ $count -lt $MAX_RETRIES ]; do
    if pg_isready -h "$HOST" -p "$PORT" -U "$USER" > /dev/null 2>&1; then
        echo "数据库已就绪!"
        exit 0
    fi

    count=$((count + 1))
    echo "等待数据库... ($count/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

echo "错误: 数据库连接超时"
exit 1
