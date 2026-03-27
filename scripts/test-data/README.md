# 本地测试数据生成脚本

## 概述

本目录包含用于本地开发和测试环境的测试数据生成脚本。这些脚本由 **测试工程师** 维护，供 **部署工程师** 在生成 Docker Compose 文件时使用。

## 文件说明

### SQL 数据脚本

按顺序执行：

| 序号 | 文件名 | 说明 | 数据量 |
|------|--------|------|--------|
| 01 | `01-departments.sql` | 部门数据 | 12 个部门 |
| 02 | `02-roles.sql` | 角色数据 | 16 种角色 |
| 03 | `03-permissions.sql` | 权限数据 | 30+ 权限 |
| 04 | `04-role-permissions.sql` | 角色权限关联 | 按角色分配 |
| 05 | `05-users.sql` | 用户数据 | 35+ 测试用户 |
| 06 | `06-user-roles.sql` | 用户角色关联 | 按用户分配 |

### 执行脚本

| 文件名 | 说明 | 适用平台 |
|--------|------|----------|
| `init-test-data.sh` | 一键初始化脚本 | Linux/macOS/Git Bash |
| `init-test-data.bat` | 一键初始化脚本 | Windows CMD/PowerShell |

## 快速开始

### 方式一：使用 Shell 脚本 (推荐)

```bash
# Linux/macOS/Git Bash
./init-test-data.sh

# 使用自定义数据库配置
export DB_HOST=myhost
export DB_PORT=5432
export DB_NAME=mydb
export DB_USER=myuser
export DB_PASSWORD=mypassword
./init-test-data.sh
```

```batch
REM Windows
init-test-data.bat

REM 使用自定义数据库配置
set DB_HOST=myhost
set DB_PORT=5432
set DB_NAME=mydb
set DB_USER=myuser
set DB_PASSWORD=mypassword
init-test-data.bat
```

### 方式二：手动执行 SQL

```bash
# 进入脚本目录
cd scripts/test-data

# 按顺序执行 SQL 文件
psql -h localhost -U devuser -d user_management -f 01-departments.sql
psql -h localhost -U devuser -d user_management -f 02-roles.sql
psql -h localhost -U devuser -d user_management -f 03-permissions.sql
psql -h localhost -U devuser -d user_management -f 04-role-permissions.sql
psql -h localhost -U devuser -d user_management -f 05-users.sql
psql -h localhost -U devuser -d user_management -f 06-user-roles.sql
```

## 测试账号

所有测试账号的默认密码为：`Test@123`

### 管理员账号

| 账号 | 角色 | 说明 |
|------|------|------|
| `superadmin@test.com` | 超级管理员 | 拥有所有权限 |
| `admin@test.com` | 系统管理员 | 系统管理权限 |

### 业务账号

| 账号 | 角色 | 部门 |
|------|------|------|
| `tech.lead@test.com` | 技术总监 | 技术研发中心 |
| `fe.dev1@test.com` | 前端开发 | 前端开发部 |
| `be.dev1@test.com` | 后端开发 | 后端开发部 |
| `qa.lead@test.com` | 测试负责人 | 测试质量部 |
| `qa.tester1@test.com` | 测试工程师 | 测试质量部 |
| `ops.lead@test.com` | 运维负责人 | 运维安全部 |
| `product.lead@test.com` | 产品总监 | 产品运营中心 |
| `pm1@test.com` | 产品经理 | 产品策划部 |
| `ui.designer1@test.com` | UI设计师 | 产品设计部 |
| `sales.lead@test.com` | 销售总监 | 市场销售中心 |
| `hr.manager@test.com` | HR经理 | 人力资源部 |
| `finance.manager@test.com` | 财务经理 | 财务行政部 |

### 特殊状态账号

| 账号 | 状态 | 说明 |
|------|------|------|
| `pending.user@test.com` | 待激活 | 测试待激活流程 |
| `locked.user@test.com` | 已锁定 | 测试账号锁定 |
| `inactive.user@test.com` | 已禁用 | 测试禁用状态 |

## 数据结构

### 部门层级

```
科技有限公司
├── 技术研发中心
│   ├── 前端开发部
│   ├── 后端开发部
│   ├── 测试质量部
│   └── 运维安全部
├── 产品运营中心
│   ├── 产品设计部
│   ├── 产品策划部
│   └── 用户运营部
├── 市场销售中心
│   ├── 华北销售部
│   ├── 华南销售部
│   ├── 华东销售部
│   └── 市场策划部
├── 人力资源部
└── 财务行政部
```

### 角色分类

- **系统角色**: 超级管理员、系统管理员
- **管理角色**: 部门经理、项目经理、技术负责人
- **技术角色**: 开发工程师、测试工程师、运维工程师
- **业务角色**: 产品经理、设计师、运营专员
- **销售角色**: 销售经理、销售代表
- **职能角色**: HR专员、财务专员
- **基础角色**: 普通用户、访客

## Docker Compose 集成

部署工程师可将以下配置添加到 `docker-compose.yml` 中：

```yaml
services:
  postgres:
    # ... 其他配置
    volumes:
      - postgres_data:/var/lib/postgresql/data
      # 挂载测试数据初始化脚本
      - ./scripts/test-data:/docker-entrypoint-initdb.d/test-data:ro
    # 或者使用初始化脚本
    # 在应用启动后执行: ./scripts/test-data/init-test-data.sh
```

更多详细信息，请参考 `DOCKER_INTEGRATION.md`。

## 维护指南

### 添加新测试数据

1. 确定数据类型和依赖关系
2. 在对应 SQL 文件中添加 INSERT 语句
3. 更新本 README 中的数据量统计
4. 测试脚本执行

### 修改现有数据

1. 编辑对应 SQL 文件
2. 确保外键约束和关联关系正确
3. 验证脚本可以重复执行（幂等性）

### 数据清理

如需清理所有测试数据：

```sql
-- 按依赖关系倒序清理
TRUNCATE TABLE user_roles CASCADE;
TRUNCATE TABLE role_permissions CASCADE;
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE permissions CASCADE;
TRUNCATE TABLE departments CASCADE;
```

## 注意事项

1. **执行顺序**: 必须按编号顺序执行 SQL 脚本
2. **外键依赖**: 后续脚本依赖前面脚本生成的数据
3. **幂等性**: 脚本设计为可重复执行（使用 TRUNCATE）
4. **密码**: 测试账号使用统一的 BCrypt 哈希密码
5. **环境**: 仅限本地开发和测试环境使用

## 联系

- **测试工程师**: 负责脚本维护和数据更新
- **部署工程师**: 负责集成到 Docker Compose 和 CI/CD 流程
