@echo off
chcp 65001 >nul
REM =============================================
REM 本地测试数据初始化脚本 (Windows)
REM 用于 Docker Compose 环境初始化测试数据
REM =============================================

echo =========================================
echo User Management System - 测试数据初始化
echo =========================================
echo.

REM 数据库连接配置 (使用 docker-compose 中的配置)
set "DB_HOST=%DB_HOST%"
if "%DB_HOST%"=="" set "DB_HOST=localhost"

set "DB_PORT=%DB_PORT%"
if "%DB_PORT%"=="" set "DB_PORT=5432"

set "DB_NAME=%DB_NAME%"
if "%DB_NAME%"=="" set "DB_NAME=user_management"

set "DB_USER=%DB_USER%"
if "%DB_USER%"=="" set "DB_USER=devuser"

set "DB_PASSWORD=%DB_PASSWORD%"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=devpassword"

echo 数据库配置:
echo   主机: %DB_HOST%
echo   端口: %DB_PORT%
echo   数据库: %DB_NAME%
echo   用户: %DB_USER%
echo.

REM 检查 psql 是否可用
where psql >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到 psql 命令
    echo 请安装 PostgreSQL 客户端工具:
    echo   - 下载地址: https://www.postgresql.org/download/windows/
    echo   - 或者使用 Docker: docker run --rm -it postgres:15 psql
    exit /b 1
)

REM 设置 PGPASSWORD
set "PGPASSWORD=%DB_PASSWORD%"

REM 测试数据库连接
echo 正在测试数据库连接...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 无法连接到数据库
    echo 请检查:
    echo   1. PostgreSQL 服务是否运行
    echo   2. 数据库配置是否正确
    echo   3. 网络连接是否正常
    exit /b 1
)

echo 数据库连接成功!
echo.

REM 获取脚本目录
set "SCRIPT_DIR=%~dp0"

echo 开始执行测试数据脚本...
echo.

REM 按顺序执行 SQL 脚本
call :execute_script "01-departments.sql"
call :execute_script "02-roles.sql"
call :execute_script "03-permissions.sql"
call :execute_script "04-role-permissions.sql"
call :execute_script "05-users.sql"
call :execute_script "06-user-roles.sql"

echo =========================================
echo 测试数据初始化完成!
echo =========================================
echo.
echo 测试账号 (密码: Test@123):
echo   超级管理员: superadmin@test.com
echo   系统管理员: admin@test.com
echo   部门经理:   tech.lead@test.com
echo   开发工程师: fe.dev1@test.com
echo   测试工程师: qa.tester1@test.com
echo.
echo 特殊状态账号:
echo   待激活: pending.user@test.com
echo   已锁定: locked.user@test.com
echo   已禁用: inactive.user@test.com
echo.

exit /b 0

:execute_script
set "SCRIPT_FILE=%~1"
set "SCRIPT_PATH=%SCRIPT_DIR%%SCRIPT_FILE%"

if exist "%SCRIPT_PATH%" (
    echo 执行: %SCRIPT_FILE% ...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCRIPT_PATH%"
    if %errorlevel% equ 0 (
        echo [OK] %SCRIPT_FILE% 执行成功
    ) else (
        echo [警告] %SCRIPT_FILE% 执行可能有错误
    )
    echo.
) else (
    echo 警告: 找不到脚本 %SCRIPT_PATH%
)
goto :eof
