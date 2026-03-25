/**
 * User Type Definitions
 *
 * TypeScript interfaces for user management
 */

import type { Role } from './role';

export type UserStatus = 'ACTIVE' | 'PENDING' | 'LOCKED' | 'INACTIVE';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  status: UserStatus;
  emailVerified: boolean;
  departmentId?: string;
  departmentName?: string;
  roles: Role[];
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  password?: string;
  departmentId?: string;
  status?: UserStatus;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: UserStatus;
  departmentId?: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  avatarUrl?: string;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
}

export interface UserQueryParams {
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: UserStatus;
  departmentId?: string;
  roleId?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface AssignRolesRequest {
  roleIds: string[];
}

export interface UserListResponse {
  content: User[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const userHelpers = {
  /**
   * Get full display name
   */
  getFullName: (user: User): string => {
    return user.fullName || `${user.firstName} ${user.lastName}`.trim();
  },

  /**
   * Get initials for avatar
   */
  getInitials: (user: User): string => {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  },

  /**
   * Get status display text
   */
  getStatusDisplay: (status: UserStatus): string => {
    const statusMap: Record<UserStatus, string> = {
      ACTIVE: 'Active',
      PENDING: 'Pending',
      LOCKED: 'Locked',
      INACTIVE: 'Inactive',
    };
    return statusMap[status] || status;
  },

  /**
   * Get status color
   */
  getStatusColor: (status: UserStatus): string => {
    const colorMap: Record<UserStatus, string> = {
      ACTIVE: 'green',
      PENDING: 'yellow',
      LOCKED: 'red',
      INACTIVE: 'gray',
    };
    return colorMap[status] || 'gray';
  },

  /**
   * Get status badge variant
   */
  getStatusBadgeVariant: (status: UserStatus): 'default' | 'secondary' | 'destructive' | 'outline' => {
    const variantMap: Record<UserStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      ACTIVE: 'default',
      PENDING: 'secondary',
      LOCKED: 'destructive',
      INACTIVE: 'outline',
    };
    return variantMap[status] || 'outline';
  },

  /**
   * Check if user is active
   */
  isActive: (user: User): boolean => {
    return user.status === 'ACTIVE';
  },

  /**
   * Check if user can login
   */
  canLogin: (user: User): boolean => {
    return user.status === 'ACTIVE' || user.status === 'PENDING';
  },
};
