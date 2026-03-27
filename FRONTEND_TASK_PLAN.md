# 前端任务规划文档

## 项目概述
全栈用户管理系统前端部分，基于 Next.js 16 (App Router)、TypeScript 5+、shadcn/ui、Tailwind CSS 和 Zustand 状态管理。

## 需求分析总结

根据 `.planning/REQUIREMENTS.md` 分析，需要前端实现的需求类别：

| 类别 | 需求数量 | 需要前端页面 | 优先级 |
|------|----------|--------------|--------|
| USER | 9 | ✅ 是 | P0 |
| DEPT | 7 | ✅ 是 | P0 |
| ROLE | 7 | ✅ 是 | P0 |
| PERM | 7 | ✅ 是 | P0 |
| AUTH | 10 | ✅ 是 | P0 |
| AUDIT | 7 | ✅ 是 | P0 |
| CONFIG | 5 | ✅ 是 | P0 |
| PERF | 7 | ❌ 否 (后端) | P0 |
| SEC | 8 | ❌ 否 (后端) | P0 |

## 前端页面规划

### 1. 认证与用户相关页面

#### 1.1 登录页面 (`/login`)
- **功能**: 用户登录、记住我、忘记密码
- **对应需求**: AUTH-01, AUTH-03, AUTH-04, AUTH-10
- **组件**:
  - 登录表单 (邮箱/密码)
  - 验证码/双因素认证 (AUTH-08, AUTH-09)
  - 第三方登录 (AUTH-07)
  - 错误提示和锁定状态显示

#### 1.2 注册页面 (`/register`)
- **功能**: 用户自助注册
- **对应需求**: USER-07
- **组件**: 注册表单、邮箱验证、密码强度检查

#### 1.3 个人资料页面 (`/profile`)
- **功能**: 用户个人资料管理
- **对应需求**: USER-08
- **组件**: 个人信息表单、头像上传、密码修改

#### 1.4 用户管理页面 (`/users`)
- **功能**: 用户CRUD、状态管理、批量操作
- **对应需求**: USER-01, USER-02, USER-03, USER-05, USER-06
- **子页面**:
  - 用户列表 (`/users`)
  - 用户详情 (`/users/[id]`)
  - 创建用户 (`/users/new`)
  - 编辑用户 (`/users/[id]/edit`)
- **组件**:
  - 用户表格 (分页、筛选、排序)
  - 批量导入/导出 (Excel/CSV)
  - 用户状态切换 (ACTIVE/INACTIVE/PENDING/LOCKED)
  - 用户角色分配

#### 1.5 用户登录历史 (`/users/[id]/login-history`)
- **功能**: 查看用户登录历史
- **对应需求**: USER-09
- **组件**: 登录历史表格、IP地址、时间、设备信息

### 2. 角色管理页面

#### 2.1 角色列表页面 (`/roles`)
- **功能**: 角色CRUD、权限分配
- **对应需求**: ROLE-01, ROLE-02, ROLE-03, ROLE-05, ROLE-06
- **子页面**:
  - 角色列表 (`/roles`)
  - 角色详情 (`/roles/[id]`)
  - 创建角色 (`/roles/new`)
  - 编辑角色 (`/roles/[id]/edit`)
- **组件**:
  - 角色表格
  - 权限分配树形组件
  - 数据权限范围配置 (ALL/DEPT/SELF)
  - 用户角色分配弹窗

#### 2.2 角色权限模板 (`/roles/templates`)
- **功能**: 预定义权限模板
- **对应需求**: ROLE-05
- **组件**: 模板列表、模板应用

### 3. 部门管理页面

#### 3.1 部门管理页面 (`/departments`)
- **功能**: 部门树形结构管理
- **对应需求**: DEPT-01, DEPT-02, DEPT-03, DEPT-04, DEPT-05, DEPT-06
- **组件**:
  - 部门树形视图 (Materialized Path)
  - 部门CRUD操作
  - 部门成员列表
  - 部门负责人关联
  - 部门编码唯一性验证

### 4. 权限管理页面

#### 4.1 权限管理页面 (`/permissions`)
- **功能**: 权限配置管理
- **对应需求**: PERM-01, PERM-02, PERM-03, PERM-04, PERM-05
- **组件**:
  - 权限树形结构 (点号分隔格式)
  - 菜单权限配置
  - 操作权限配置 (CRUD)
  - 字段权限配置 (可读/可写)
  - 数据权限范围配置

### 5. 审计日志页面

#### 5.1 审计日志页面 (`/audit-logs`)
- **功能**: 操作日志查询、筛选、导出
- **对应需求**: AUDIT-01, AUDIT-02, AUDIT-04, AUDIT-05
- **组件**:
  - 日志表格 (分页、筛选)
  - 多维度查询 (用户、操作类型、时间范围)
  - 日志详情查看
  - 导出功能 (Excel/PDF)
  - 实时告警显示 (AUDIT-07)

### 6. 系统配置页面

#### 6.1 系统配置页面 (`/settings`)
- **功能**: 系统参数配置
- **对应需求**: CONFIG-01, CONFIG-02, CONFIG-03, CONFIG-04, CONFIG-05
- **标签页**:
  - 密码策略配置 (复杂度、过期时间、历史密码)
  - 会话配置 (超时时间、最大会话数)
  - 邮件服务配置
  - 安全策略配置 (登录失败锁定、限流阈值)
  - 系统参数配置

### 7. 仪表板页面

#### 7.1 主仪表板 (`/dashboard`)
- **功能**: 系统概览、统计信息
- **组件**:
  - 用户统计卡片 (活跃用户、新增用户)
  - 角色权限分布
  - 登录活动图表
  - 系统健康状态
  - 最近操作日志

## 前端技术栈实现规划

### 1. 项目结构
```
frontend/
├── app/                    # Next.js App Router
│   ├── (auth)/            # 认证相关路由组
│   │   ├── login/
│   │   ├── register/
│   │   └── forgot-password/
│   ├── (dashboard)/       # 仪表板路由组
│   │   ├── dashboard/
│   │   ├── users/
│   │   ├── roles/
│   │   ├── departments/
│   │   ├── permissions/
│   │   ├── audit-logs/
│   │   └── settings/
│   ├── api/               # API路由
│   │   ├── auth/
│   │   └── proxy/         # 后端API代理
│   ├── layout.tsx         # 根布局
│   └── page.tsx           # 首页
├── components/
│   ├── ui/                # shadcn/ui组件
│   ├── forms/             # 表单组件
│   │   ├── UserForm/
│   │   ├── RoleForm/
│   │   ├── DepartmentForm/
│   │   └── PermissionForm/
│   ├── layout/            # 布局组件
│   │   ├── Header/
│   │   ├── Sidebar/
│   │   ├── MainLayout/
│   │   └── Breadcrumb/
│   ├── data-display/      # 数据展示组件
│   │   ├── DataTable/
│   │   ├── TreeView/
│   │   ├── PermissionTree/
│   │   └── AuditLogTable/
│   └── features/          # 功能组件
│       ├── UserManagement/
│       ├── RoleManagement/
│       └── ImportExport/
├── lib/
│   ├── api/               # API客户端
│   │   ├── client.ts      # Axios配置
│   │   ├── auth.ts        # 认证API
│   │   ├── users.ts       # 用户API
│   │   ├── roles.ts       # 角色API
│   │   └── departments.ts # 部门API
│   ├── utils/             # 工具函数
│   │   ├── validation.ts  # 验证工具
│   │   ├── formatters.ts  # 格式化工具
│   │   └── permissions.ts # 权限检查工具
│   ├── schemas/           # Zod验证模式
│   │   ├── user.schema.ts
│   │   ├── role.schema.ts
│   │   └── department.schema.ts
│   └── constants/         # 常量定义
│       ├── routes.ts      # 路由常量
│       └── permissions.ts # 权限常量
├── stores/                # Zustand状态管理
│   ├── auth.store.ts      # 认证状态
│   ├── user.store.ts      # 用户状态
│   ├── ui.store.ts        # UI状态
│   └── notification.store.ts # 通知状态
├── types/                 # TypeScript类型定义
│   ├── api.ts             # API响应类型
│   ├── user.ts            # 用户类型
│   ├── role.ts            # 角色类型
│   └── department.ts      # 部门类型
├── hooks/                 # 自定义Hook
│   ├── useAuth.ts         # 认证Hook
│   ├── usePermissions.ts  # 权限Hook
│   └── useApi.ts          # API Hook
└── styles/                # 样式文件
    └── globals.css        # 全局样式
```

### 2. 核心组件规划

#### 2.1 认证相关组件
- `AuthGuard`: 路由守卫组件
- `LoginForm`: 登录表单组件
- `RegisterForm`: 注册表单组件
- `ForgotPasswordForm`: 忘记密码表单

#### 2.2 数据表格组件
- `DataTable`: 通用数据表格 (分页、筛选、排序)
- `UserTable`: 用户表格 (带状态管理)
- `RoleTable`: 角色表格 (带权限分配)
- `AuditLogTable`: 审计日志表格 (多维度筛选)

#### 2.3 表单组件
- `UserForm`: 用户创建/编辑表单
- `RoleForm`: 角色创建/编辑表单
- `DepartmentForm`: 部门创建/编辑表单
- `PermissionForm`: 权限配置表单

#### 2.4 树形组件
- `DepartmentTree`: 部门树形结构
- `PermissionTree`: 权限树形结构
- `RolePermissionTree`: 角色权限分配树

#### 2.5 布局组件
- `MainLayout`: 主布局 (Header + Sidebar + Content)
- `Sidebar`: 侧边栏导航 (基于权限动态渲染)
- `Header`: 顶部导航 (用户信息、通知、设置)

### 3. 状态管理规划

#### 3.1 认证状态 (`auth.store.ts`)
```typescript
interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
  setUser: (user: User) => void;
}
```

#### 3.2 用户状态 (`user.store.ts`)
```typescript
interface UserState {
  users: User[];
  selectedUser: User | null;
  pagination: Pagination;
  filters: UserFilters;
  isLoading: boolean;

  // Actions
  fetchUsers: (params?: FetchParams) => Promise<void>;
  createUser: (userData: CreateUserData) => Promise<void>;
  updateUser: (id: string, userData: UpdateUserData) => Promise<void>;
  deleteUser: (id: string) => Promise<void>;
  importUsers: (file: File) => Promise<void>;
  exportUsers: () => Promise<void>;
}
```

#### 3.3 角色状态 (`role.store.ts`)
```typescript
interface RoleState {
  roles: Role[];
  selectedRole: Role | null;
  permissions: Permission[];
  rolePermissions: Record<string, string[]>;

  // Actions
  fetchRoles: () => Promise<void>;
  fetchPermissions: () => Promise<void>;
  assignPermissions: (roleId: string, permissionIds: string[]) => Promise<void>;
  assignUsersToRole: (roleId: string, userIds: string[]) => Promise<void>;
}
```

### 4. API客户端规划

#### 4.1 Axios配置 (`lib/api/client.ts`)
```typescript
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加Token
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器 - Token刷新、错误处理
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Token过期处理
    if (error.response?.status === 401) {
      await refreshToken();
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);
```

#### 4.2 API模块划分
- `auth.ts`: 登录、注册、刷新Token、登出
- `users.ts`: 用户CRUD、批量操作、状态管理
- `roles.ts`: 角色CRUD、权限分配
- `departments.ts`: 部门CRUD、树形结构
- `permissions.ts`: 权限管理
- `audit-logs.ts`: 审计日志查询
- `settings.ts`: 系统配置

### 5. 路由与权限控制

#### 5.1 路由结构
```typescript
const routes = [
  // 公开路由
  { path: '/login', component: LoginPage, public: true },
  { path: '/register', component: RegisterPage, public: true },

  // 受保护路由
  { path: '/dashboard', component: DashboardPage, requiredPermissions: ['dashboard.view'] },
  { path: '/users', component: UsersPage, requiredPermissions: ['users.list'] },
  { path: '/users/:id', component: UserDetailPage, requiredPermissions: ['users.read'] },
  { path: '/roles', component: RolesPage, requiredPermissions: ['roles.list'] },
  { path: '/departments', component: DepartmentsPage, requiredPermissions: ['departments.list'] },
  { path: '/permissions', component: PermissionsPage, requiredPermissions: ['permissions.list'] },
  { path: '/audit-logs', component: AuditLogsPage, requiredPermissions: ['audit_logs.list'] },
  { path: '/settings', component: SettingsPage, requiredPermissions: ['settings.view'] },
];
```

#### 5.2 权限检查中间件
```typescript
// app/middleware.ts
export function middleware(request: NextRequest) {
  const token = request.cookies.get('token')?.value;
  const pathname = request.nextUrl.pathname;

  // 检查是否登录
  if (!token && !publicRoutes.includes(pathname)) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // 检查权限
  if (token && !checkPermissions(pathname, token)) {
    return NextResponse.redirect(new URL('/unauthorized', request.url));
  }

  return NextResponse.next();
}
```

### 6. 测试规划

#### 6.1 测试覆盖率目标
- 单元测试: ≥ 90%
- 组件测试: ≥ 85%
- E2E测试: 关键用户流程100%覆盖

#### 6.2 测试工具
- **单元测试**: Jest + React Testing Library
- **组件测试**: Storybook + Chromatic
- **E2E测试**: Playwright
- **API测试**: MSW (Mock Service Worker)

#### 6.3 测试重点
1. 认证流程测试 (登录、注册、Token刷新)
2. 用户管理测试 (CRUD、批量操作)
3. 权限控制测试 (路由守卫、按钮权限)
4. 表单验证测试 (Zod schema验证)
5. 错误处理测试 (API错误、网络异常)

## 开发阶段规划

### Phase 1: 基础框架搭建 (Week 1-2)
1. 项目初始化 (Next.js + TypeScript + Tailwind)
2. 安装配置 shadcn/ui 组件库
3. 设置 Zustand 状态管理
4. 配置 API 客户端 (Axios + 拦截器)
5. 实现基础布局组件 (Header, Sidebar, MainLayout)
6. 设置路由结构和权限中间件

### Phase 2: 认证模块 (Week 3-4)
1. 登录页面实现
2. 注册页面实现
3. Token管理和刷新机制
4. 路由守卫实现
5. 个人资料页面

### Phase 3: 用户管理模块 (Week 5-6)
1. 用户列表页面 (表格、分页、筛选)
2. 用户详情页面
3. 用户创建/编辑表单
4. 用户状态管理
5. 批量导入/导出功能

### Phase 4: 角色权限模块 (Week 7-8)
1. 角色列表页面
2. 角色创建/编辑表单
3. 权限树形组件
4. 角色权限分配
5. 用户角色分配

### Phase 5: 部门管理模块 (Week 9-10)
1. 部门树形结构页面
2. 部门CRUD操作
3. 部门成员管理
4. 部门负责人关联

### Phase 6: 审计日志与配置 (Week 11-12)
1. 审计日志页面 (表格、筛选、导出)
2. 系统配置页面
3. 仪表板页面
4. 实时通知功能

### Phase 7: 测试与优化 (Week 13-14)
1. 单元测试编写
2. 组件测试编写
3. E2E测试编写
4. 性能优化
5. 代码审查和重构

## 技术挑战与解决方案

### 1. 权限动态渲染
**挑战**: 根据用户权限动态渲染菜单和按钮
**解决方案**:
- 使用权限检查Hook (`usePermissions`)
- 实现高阶组件 (`withPermission`)
- 侧边栏菜单基于权限动态生成

### 2. 树形结构管理
**挑战**: 部门树形结构、权限树形结构
**解决方案**:
- 使用递归组件实现树形视图
- 实现拖拽排序功能
- 使用虚拟滚动优化性能

### 3. 批量操作性能
**挑战**: 批量导入/导出大量数据
**解决方案**:
- 使用Web Worker处理大数据
- 分片上传/下载
- 进度条显示和取消功能

### 4. 实时数据更新
**挑战**: 审计日志实时更新、通知实时推送
**解决方案**:
- 使用WebSocket或Server-Sent Events
- 实现乐观更新 (Optimistic Updates)
- 使用SWR或React Query进行数据缓存

### 5. 多环境配置
**挑战**: 不同环境API地址、配置参数
**解决方案**:
- 使用环境变量管理配置
- 实现配置管理Hook
- 开发环境Mock数据

## 质量保证措施

### 1. 代码质量
- ESLint + Prettier 代码规范
- TypeScript 严格模式
- Husky + lint-staged 提交前检查
- 代码审查流程

### 2. 性能优化
- 图片懒加载和优化
- 代码分割和懒加载
- 服务端渲染优化
- 缓存策略优化

### 3. 安全性
- XSS防护 (DOMPurify)
- CSRF防护
- 输入验证 (Zod)
- 敏感信息脱敏

### 4. 可访问性
- ARIA标签支持
- 键盘导航支持
- 屏幕阅读器兼容
- 颜色对比度检查

## 交付物清单

### 文档
- [ ] 前端架构设计文档
- [ ] 组件API文档 (Storybook)
- [ ] 开发指南文档
- [ ] 部署指南文档

### 代码
- [ ] 完整的Next.js应用
- [ ] 所有规划页面的实现
- [ ] 组件库和工具函数
- [ ] 测试套件 (单元测试、组件测试、E2E测试)

### 配置
- [ ] CI/CD流水线配置
- [ ] 多环境部署配置
- [ ] 监控和告警配置
- [ ] 性能监控配置

## 风险评估与缓解

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 权限系统复杂度高 | 高 | 中 | 分阶段实现，先实现基础RBAC，再扩展 |
| 树形结构性能问题 | 中 | 高 | 使用虚拟滚动，分页加载，后端优化 |
| 批量操作超时 | 中 | 中 | 分片处理，进度反馈，后台任务 |
| 浏览器兼容性问题 | 低 | 低 | 使用现代浏览器特性，提供降级方案 |
| API接口变更 | 高 | 中 | 使用TypeScript严格类型，API版本管理 |

## 成功标准

1. **功能完整性**: 所有规划页面和功能100%实现
2. **代码质量**: 测试覆盖率≥80%，无严重bug
3. **性能指标**: 页面加载时间<3秒，API响应时间<200ms
4. **用户体验**: 用户满意度调查≥4.5/5分
5. **安全性**: 通过安全扫描，无高危漏洞

---

*文档版本: 1.0*
*创建日期: 2026-03-26*
*最后更新: 2026-03-26*