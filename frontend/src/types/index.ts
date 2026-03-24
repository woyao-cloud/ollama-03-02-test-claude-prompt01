// User Types
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  status: 'ACTIVE' | 'PENDING' | 'LOCKED' | 'INACTIVE';
  emailVerified: boolean;
  departmentId?: string;
  departmentName?: string;
  roles: RoleInfo[];
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoleInfo {
  id: string;
  name: string;
  code: string;
}

// Role Types
export interface Role {
  id: string;
  name: string;
  code: string;
  description?: string;
  dataScope: 'ALL' | 'DEPT' | 'SELF' | 'CUSTOM';
  status: 'ACTIVE' | 'INACTIVE';
  isSystem: boolean;
  permissions?: Permission[];
  permissionCount?: number;
  createdAt: string;
  updatedAt: string;
}

// Permission Types
export interface Permission {
  id: string;
  name: string;
  code: string;
  type: 'MENU' | 'ACTION' | 'FIELD' | 'DATA';
  resource: string;
  action?: string;
  icon?: string;
  route?: string;
  sortOrder: number;
  status: 'ACTIVE' | 'INACTIVE';
  parentId?: string;
  children?: Permission[];
}

// Audit Log Types
export interface AuditLog {
  id: string;
  userId?: string;
  username: string;
  operation: string;
  operationDescription: string;
  resourceType: string;
  resourceId?: string;
  description?: string;
  oldValue?: Record<string, unknown>;
  newValue?: Record<string, unknown>;
  success: boolean;
  errorMessage?: string;
  clientIp?: string;
  createdAt: string;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Auth Types
export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

// UI Types
export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message?: string;
  duration?: number;
}

export interface PaginationParams {
  page: number;
  size: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface FilterParams {
  keyword?: string;
  status?: string;
  [key: string]: unknown;
}
