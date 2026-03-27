/**
 * Department API Client
 *
 * API methods for department CRUD operations
 * Base path: /api/v1/departments
 */

import type {
  Department,
  DepartmentListResponse,
  DepartmentQueryParams,
  DepartmentTreeNode,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
  MoveDepartmentRequest,
  AssignManagerRequest,
} from '@/types/department';

// API response wrapper matching backend ApiResponse<T>
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: number;
}

// Base API URL
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Default fetch options
const defaultOptions: RequestInit = {
  headers: {
    'Content-Type': 'application/json',
  },
  credentials: 'include',
};

/**
 * Get auth token from storage
 */
const getAuthToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('accessToken');
  }
  return null;
};

/**
 * Build request headers with auth token
 */
const buildHeaders = (): HeadersInit => {
  const token = getAuthToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

/**
 * Handle API response
 */
const handleResponse = async <T>(response: Response): Promise<T> => {
  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || `Request failed with status ${response.status}`);
  }

  const result: ApiResponse<T> = await response.json();

  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }

  return result.data;
};

/**
 * Build query string from params
 */
const buildQueryString = (params?: Record<string, unknown>): string => {
  if (!params) return '';

  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      searchParams.append(key, String(value));
    }
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
};

/**
 * Get all departments with pagination and filters
 */
export const getDepartments = async (
  params?: DepartmentQueryParams
): Promise<DepartmentListResponse> => {
  const queryString = buildQueryString(params as Record<string, unknown>);

  const response = await fetch(`${API_BASE_URL}/api/v1/departments${queryString}`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include',
  });

  return handleResponse<DepartmentListResponse>(response);
};

/**
 * Get department by ID
 */
export const getDepartmentById = async (id: string): Promise<Department> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include',
  });

  return handleResponse<Department>(response);
};

/**
 * Get department by code
 */
export const getDepartmentByCode = async (code: string): Promise<Department> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/code/${code}`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include',
  });

  return handleResponse<Department>(response);
};

/**
 * Create a new department
 */
export const createDepartment = async (
  data: CreateDepartmentRequest
): Promise<Department> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments`, {
    method: 'POST',
    headers: buildHeaders(),
    credentials: 'include',
    body: JSON.stringify(data),
  });

  return handleResponse<Department>(response);
};

/**
 * Update an existing department
 */
export const updateDepartment = async (
  id: string,
  data: UpdateDepartmentRequest
): Promise<Department> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}`, {
    method: 'PUT',
    headers: buildHeaders(),
    credentials: 'include',
    body: JSON.stringify(data),
  });

  return handleResponse<Department>(response);
};

/**
 * Delete a department
 */
export const deleteDepartment = async (id: string): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}`, {
    method: 'DELETE',
    headers: buildHeaders(),
    credentials: 'include',
  });

  await handleResponse<void>(response);
};

/**
 * Get department tree structure
 */
export const getDepartmentTree = async (
  includeInactive = false
): Promise<DepartmentTreeNode[]> => {
  const queryString = includeInactive ? '?includeInactive=true' : '';

  const response = await fetch(`${API_BASE_URL}/api/v1/departments/tree${queryString}`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include',
  });

  const departments = await handleResponse<Department[]>(response);

  // Convert to tree nodes with UI state
  return departments.map(dept => ({
    ...dept,
    children: dept.children ? dept.children.map(child => ({
      ...child,
      children: [],
      isExpanded: false,
      isSelected: false,
    })) : [],
    isExpanded: false,
    isSelected: false,
  }));
};

/**
 * Get children of a department
 */
export const getChildren = async (
  id: string,
  includeInactive = false
): Promise<Department[]> => {
  const queryString = includeInactive ? '?includeInactive=true' : '';

  const response = await fetch(
    `${API_BASE_URL}/api/v1/departments/${id}/children${queryString}`,
    {
      method: 'GET',
      headers: buildHeaders(),
      credentials: 'include',
    }
  );

  return handleResponse<Department[]>(response);
};

/**
 * Move department to new parent
 */
export const moveDepartment = async (
  id: string,
  parentId?: string
): Promise<void> => {
  const data: MoveDepartmentRequest = { parentId };

  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}/move`, {
    method: 'POST',
    headers: buildHeaders(),
    credentials: 'include',
    body: JSON.stringify(data),
  });

  await handleResponse<void>(response);
};

/**
 * Assign manager to department
 */
export const assignManager = async (
  id: string,
  managerId?: string
): Promise<void> => {
  const data: AssignManagerRequest = { managerId };

  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}/manager`, {
    method: 'POST',
    headers: buildHeaders(),
    credentials: 'include',
    body: JSON.stringify(data),
  });

  await handleResponse<void>(response);
};

/**
 * Update department status
 */
export const updateDepartmentStatus = async (
  id: string,
  status: 'ACTIVE' | 'INACTIVE'
): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}/status`, {
    method: 'PATCH',
    headers: buildHeaders(),
    credentials: 'include',
    body: JSON.stringify({ status }),
  });

  await handleResponse<void>(response);
};

/**
 * Get all descendant department IDs
 */
export const getDescendantIds = async (id: string): Promise<string[]> => {
  const response = await fetch(`${API_BASE_URL}/api/v1/departments/${id}/descendants`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include',
  });

  return handleResponse<string[]>(response);
};

/**
 * Check if department code is available
 */
export const checkCodeAvailability = async (
  code: string,
  excludeId?: string
): Promise<boolean> => {
  const params: Record<string, string> = { code };
  if (excludeId) {
    params.excludeId = excludeId;
  }

  const queryString = buildQueryString(params as Record<string, unknown>);

  const response = await fetch(
    `${API_BASE_URL}/api/v1/departments/check-code${queryString}`,
    {
      method: 'GET',
      headers: buildHeaders(),
      credentials: 'include',
    }
  );

  return handleResponse<boolean>(response);
};

/**
 * Department API client object
 */
export const departmentApi = {
  getDepartments,
  getDepartmentById,
  getDepartmentByCode,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  getDepartmentTree,
  getChildren,
  moveDepartment,
  assignManager,
  updateDepartmentStatus,
  getDescendantIds,
  checkCodeAvailability,
};

export default departmentApi;
