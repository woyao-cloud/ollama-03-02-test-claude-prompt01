---
phase: 1
plan: 06
title: 前端基础架构
requirements_addressed: [AUTH-01, AUTH-02, AUTH-03, USER-01, USER-08, PERF-01, PERF-02, SEC-02, SEC-04, SEC-05]
depends_on: [02]
wave: 4
autonomous: false
---

# Plan 1.6: 前端基础架构

## Objective

搭建Next.js 16前端基础架构，包括项目初始化、shadcn/ui配置、Zustand状态管理、axios HTTP客户端配置、登录页面，以及整体布局组件。

**Purpose:** 前端基础架构是用户与系统交互的入口，需要统一的技术栈和良好的用户体验设计。

**Output:**
- Next.js 16 项目初始化 (App Router)
- shadcn/ui 组件库配置
- Zustand 状态管理 (用户、认证)
- axios HTTP 客户端 (拦截器、错误处理)
- 登录页面
- 主布局 (侧边栏、头部)

---

## Context

### 前端技术栈
| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Next.js | 16.x |
| 语言 | TypeScript | 5+ |
| 样式 | Tailwind CSS | 3.x |
| UI组件 | shadcn/ui | latest |
| 状态管理 | Zustand | 4.x |
| HTTP客户端 | axios | 1.x |
| 表单处理 | React Hook Form | 7.x |
| 验证库 | Zod | 3.x |

### 项目结构
```
frontend/
├── src/
│   ├── app/                    # App Router
│   │   ├── layout.tsx          # 根布局
│   │   ├── page.tsx            # 首页
│   │   ├── login/page.tsx      # 登录页
│   │   ├── users/              # 用户管理
│   │   ├── roles/              # 角色管理
│   │   ├── permissions/        # 权限管理
│   │   └── audit-logs/         # 审计日志
│   ├── components/             # 组件
│   │   ├── ui/                 # shadcn组件
│   │   ├── layout/             # 布局组件
│   │   │   ├── sidebar.tsx
│   │   │   ├── header.tsx
│   │   │   └── main-layout.tsx
│   │   └── common/             # 通用组件
│   ├── lib/                    # 工具函数
│   │   ├── api.ts              # axios配置
│   │   ├── utils.ts            # 工具函数
│   │   └── auth.ts             # 认证工具
│   ├── store/                  # Zustand状态
│   │   ├── auth-store.ts
│   │   └── user-store.ts
│   ├── hooks/                  # 自定义Hooks
│   │   ├── use-auth.ts
│   │   └── use-permission.ts
│   └── types/                  # TypeScript类型
│       ├── user.ts
│       ├── role.ts
│       └── api.ts
├── public/                     # 静态资源
├── components.json             # shadcn配置
├── tailwind.config.ts
├── next.config.js
└── package.json
```

---

## Tasks

### Task 1: 初始化 Next.js 项目并配置 shadcn/ui

**描述:** 创建Next.js 16项目，配置TypeScript、Tailwind CSS、shadcn/ui

**文件:**
- `frontend/package.json`
- `frontend/tsconfig.json`
- `frontend/tailwind.config.ts`
- `frontend/next.config.js`
- `frontend/components.json`
- `frontend/src/app/globals.css`
- `frontend/src/lib/utils.ts`

**依赖:** 无 (独立任务)

**验收标准:**
- 使用 `create-next-app` 创建项目，选择:
  - TypeScript: Yes
  - ESLint: Yes
  - Tailwind CSS: Yes
  - `src/` directory: Yes
  - App Router: Yes
- 初始化 shadcn/ui:
  - 运行 `npx shadcn-ui@latest init`
  - 配置组件路径: `src/components/ui`
  - 配置工具路径: `src/lib/utils`
- 安装基础组件:
  - button, input, label, card
  - form, select, dialog, table
  - dropdown-menu, avatar, badge
  - toast (使用 sonner)
- 配置全局样式:
  - CSS变量定义主题色
  - 暗色/亮色模式支持
  - 响应式断点

**安装命令:**
```bash
cd frontend
npx shadcn-ui@latest add button input label card form select dialog table dropdown-menu avatar badge sonner
```

---

### Task 2: 配置 Zustand 状态管理

**描述:** 创建Zustand Store，管理认证状态和用户信息

**文件:**
- `frontend/src/store/auth-store.ts`
- `frontend/src/store/user-store.ts`
- `frontend/src/types/auth.ts`
- `frontend/src/types/user.ts`

**依赖:** Task 1 完成

**验收标准:**
- auth-store.ts:
  - State: `user`, `accessToken`, `refreshToken`, `isAuthenticated`, `isLoading`
  - Actions:
    - `login(email, password)`: 登录
    - `logout()`: 登出
    - `refreshAccessToken()`: 刷新Token
    - `setUser(user)`: 设置用户信息
  - 使用persist中间件持久化到localStorage
  - Token过期自动刷新

- user-store.ts:
  - State: `currentUser`, `permissions`, `roles`
  - Actions:
    - `fetchCurrentUser()`: 获取当前用户信息
    - `hasPermission(permission)`: 检查权限
    - `hasRole(role)`: 检查角色
  - 从Token解析权限信息

- TypeScript类型定义:
  - User, Role, Permission 类型
  - AuthResponse, LoginRequest 类型
  - API响应统一类型

**Auth Store 示例:**
```typescript
interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // ... state and actions
    }),
    { name: 'auth-storage' }
  )
);
```

---

### Task 3: 配置 axios HTTP 客户端

**描述:** 创建axios实例，配置拦截器、错误处理、Token刷新

**文件:**
- `frontend/src/lib/api.ts`
- `frontend/src/lib/interceptors.ts`
- `frontend/src/lib/error-handler.ts`

**依赖:** Task 2 完成

**验收标准:**
- api.ts:
  - 创建axios实例
  - 配置baseURL (环境变量)
  - 配置超时 (30秒)
  - 配置请求/响应拦截器

- 请求拦截器:
  - 自动附加Authorization头
  - 添加Content-Type: application/json
  - 添加X-Request-ID

- 响应拦截器:
  - 统一错误处理
  - 401时自动刷新Token
  - Token刷新失败时跳转登录页
  - 显示错误提示 (toast)

- 错误处理:
  - 网络错误提示
  - 业务错误码映射
  - 表单验证错误处理

**API配置示例:**
```typescript
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
api.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    // Token刷新逻辑
    // 错误处理逻辑
  }
);
```

---

### Task 4: 创建登录页面

**描述:** 创建登录页面，包含表单验证、错误提示、加载状态

**文件:**
- `frontend/src/app/login/page.tsx`
- `frontend/src/components/auth/login-form.tsx`
- `frontend/src/app/layout.tsx` (根布局)

**依赖:** Task 2, Task 3 完成

**验收标准:**
- 登录页面 (app/login/page.tsx):
  - 居中布局的登录卡片
  - 系统Logo和标题
  - 登录表单组件
  - 背景样式 (渐变或图片)

- 登录表单组件 (login-form.tsx):
  - 邮箱输入框 (带验证)
  - 密码输入框 (可显示/隐藏)
  - "记住我" 复选框
  - 登录按钮 (带加载状态)
  - 错误提示 (表单验证错误、登录失败)
  - 使用 React Hook Form + Zod 验证

- 表单验证规则:
  - 邮箱: 必填，邮箱格式
  - 密码: 必填，最少8位

- 登录流程:
  1. 表单提交
  2. 调用authStore.login()
  3. 成功: 跳转首页
  4. 失败: 显示错误信息

- 根布局:
  - 配置全局Provider (Toaster, 主题)
  - 配置字体
  - 配置元数据

---

### Task 5: 创建主布局和导航组件

**描述:** 创建主布局组件，包括侧边栏导航、顶部栏、面包屑

**文件:**
- `frontend/src/components/layout/main-layout.tsx`
- `frontend/src/components/layout/sidebar.tsx`
- `frontend/src/components/layout/header.tsx`
- `frontend/src/components/layout/breadcrumb.tsx`
- `frontend/src/components/layout/user-nav.tsx`
- `frontend/src/config/navigation.ts`

**依赖:** Task 4 完成

**验收标准:**
- main-layout.tsx:
  - 响应式布局 (移动端侧边栏可收起)
  - 固定侧边栏和顶部栏
  - 主内容区域可滚动
  - 权限控制菜单显示

- sidebar.tsx:
  - 系统Logo区域
  - 导航菜单列表
  - 支持展开/折叠子菜单
  - 当前菜单高亮
  - 根据权限动态显示菜单项

- header.tsx:
  - 侧边栏展开/折叠按钮
  - 面包屑导航
  - 用户头像下拉菜单
  - 通知图标

- user-nav.tsx:
  - 用户头像
  - 用户姓名和角色
  - 个人资料链接
  - 退出登录按钮

- navigation.ts:
  - 定义导航菜单配置
  - 包含: 标题、图标、路径、所需权限、子菜单

**导航菜单配置:**
```typescript
export const navigation = [
  {
    title: '用户管理',
    icon: 'Users',
    path: '/users',
    permission: 'user:menu:view',
  },
  {
    title: '角色权限',
    icon: 'Shield',
    permission: 'role:menu:view',
    children: [
      { title: '角色管理', path: '/roles', permission: 'role:menu:view' },
      { title: '权限管理', path: '/permissions', permission: 'permission:menu:view' },
    ],
  },
  {
    title: '审计日志',
    icon: 'FileText',
    path: '/audit-logs',
    permission: 'audit:menu:view',
  },
];
```

---

### Task 6: 实现路由守卫和权限控制

**描述:** 创建路由守卫组件，保护需要认证的页面，控制权限访问

**文件:**
- `frontend/src/components/auth/protected-route.tsx`
- `frontend/src/components/auth/permission-guard.tsx`
- `frontend/src/hooks/use-auth.ts`
- `frontend/src/hooks/use-permission.ts`
- `frontend/src/middleware.ts`

**依赖:** Task 4, Task 5 完成

**验收标准:**
- protected-route.tsx:
  - 检查用户是否已认证
  - 未认证时重定向到登录页
  - 认证后重定向回原始页面
  - 显示加载状态

- permission-guard.tsx:
  - 检查用户是否有指定权限
  - 无权限时显示无权限页面或隐藏内容
  - 支持检查多个权限 (any/all)

- middleware.ts:
  - Next.js中间件
  - 服务端检查Token有效性
  - 无效时重定向登录

- use-auth.ts:
  - 封装认证相关逻辑
  - 提供登录状态、用户信息

- use-permission.ts:
  - 提供权限检查方法
  - `hasPermission(permission)`
  - `hasAnyPermission([...])`
  - `hasAllPermissions([...])`

**权限守卫使用示例:**
```typescript
// 页面级别保护
export default function UsersPage() {
  return (
    <ProtectedRoute>
      <PermissionGuard permission="user:read">
        <UserList />
      </PermissionGuard>
    </ProtectedRoute>
  );
}

// 组件级别保护
function UserActions() {
  const { hasPermission } = usePermission();

  return (
    <div>
      {hasPermission('user:create') && (
        <Button>创建用户</Button>
      )}
    </div>
  );
}
```

---

## Verification

### 自动化验证

```bash
# 1. 类型检查
cd frontend && npm run type-check

# 2. 构建测试
npm run build

# 3. Lint检查
npm run lint
```

### 手动验证清单

- [ ] 项目可以正常启动 (npm run dev)
- [ ] shadcn/ui组件可以正常使用
- [ ] 登录页面样式正确
- [ ] 表单验证工作正常
- [ ] 登录成功后Token正确存储
- [ ] 登录失败显示错误提示
- [ ] 侧边栏导航正常显示
- [ ] 未登录时重定向到登录页
- [ ] Token过期自动刷新

---

## Success Criteria

1. **项目结构**: Next.js 14项目结构清晰，配置正确
2. **UI组件**: shadcn/ui组件库配置完成，组件可用
3. **状态管理**: Zustand Store正常工作，状态持久化
4. **HTTP客户端**: axios配置正确，拦截器工作正常
5. **登录功能**: 用户可以正常登录，Token管理正确
6. **路由保护**: 认证和权限控制生效
7. **布局组件**: 主布局、侧边栏、头部显示正常

---

## must_haves

### truths
- Next.js 16项目初始化完成，使用App Router
- shadcn/ui组件库配置完成
- Zustand管理认证状态和用户信息
- axios配置Token自动附加和刷新
- 登录页面功能完整，表单验证正常
- 路由守卫保护需要认证的页面

### artifacts
- path: "frontend/package.json"
  provides: "项目依赖配置"
  min_lines: 30
- path: "frontend/src/store/auth-store.ts"
  provides: "认证状态管理"
  min_lines: 60
- path: "frontend/src/lib/api.ts"
  provides: "axios HTTP客户端"
  min_lines: 50
- path: "frontend/src/app/login/page.tsx"
  provides: "登录页面"
  min_lines: 50
- path: "frontend/src/components/layout/main-layout.tsx"
  provides: "主布局组件"
  min_lines: 40
- path: "frontend/src/middleware.ts"
  provides: "路由中间件"
  min_lines: 30

### key_links
- from: "login/page.tsx"
  to: "auth-store.ts"
  via: "登录状态管理"
- from: "api.ts"
  to: "auth-store.ts"
  via: "Token获取"
- from: "main-layout.tsx"
  to: "sidebar.tsx"
  via: "布局组合"
- from: "middleware.ts"
  to: "api.ts"
  via: "Token验证"

---

## Risks & Mitigation

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| Token刷新并发问题 | 中 | 中 | 使用锁机制，避免重复刷新 |
| 状态持久化安全 | 中 | 中 | 仅存储非敏感信息，Token使用httpOnly cookie备选 |
| 构建性能问题 | 低 | 低 | 使用swc，优化图片加载 |

---

## Output

After completion, create `.planning/phases/phase-01-foundation/06-SUMMARY.md`
