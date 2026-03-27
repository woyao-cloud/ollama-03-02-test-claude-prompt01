import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 10000,
    });

    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // Handle 401 Unauthorized
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
              const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                refreshToken,
              });

              const { accessToken } = response.data.data;
              localStorage.setItem('accessToken', accessToken);

              originalRequest.headers.Authorization = `Bearer ${accessToken}`;
              return this.client(originalRequest);
            }
          } catch (refreshError) {
            // Refresh failed, logout user
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse = await this.client.get(url, config);
    return response.data;
  }

  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse = await this.client.post(url, data, config);
    return response.data;
  }

  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse = await this.client.put(url, data, config);
    return response.data;
  }

  async patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse = await this.client.patch(url, data, config);
    return response.data;
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse = await this.client.delete(url, config);
    return response.data;
  }
}

export const apiClient = new ApiClient();

// API endpoints
export const api = {
  auth: {
    login: (email: string, password: string) =>
      apiClient.post('/auth/login', { email, password }),
    logout: () => apiClient.post('/auth/logout', {}),
    refresh: (refreshToken: string) =>
      apiClient.post('/auth/refresh', { refreshToken }),
    me: () => apiClient.get('/users/me'),
  },
  users: {
    getAll: (params?: Record<string, unknown>) =>
      apiClient.get('/users', { params }),
    getById: (id: string) => apiClient.get(`/users/${id}`),
    create: (data: unknown) => apiClient.post('/users', data),
    update: (id: string, data: unknown) => apiClient.put(`/users/${id}`, data),
    delete: (id: string) => apiClient.delete(`/users/${id}`),
    updateStatus: (id: string, status: string) =>
      apiClient.patch(`/users/${id}/status`, { status }),
    assignRoles: (id: string, roleIds: string[]) =>
      apiClient.post(`/users/${id}/roles`, { roleIds }),
  },
  roles: {
    getAll: (params?: Record<string, unknown>) =>
      apiClient.get('/roles', { params }),
    getAllActive: () => apiClient.get('/roles/all'),
    getById: (id: string) => apiClient.get(`/roles/${id}`),
    create: (data: unknown) => apiClient.post('/roles', data),
    update: (id: string, data: unknown) => apiClient.put(`/roles/${id}`, data),
    delete: (id: string) => apiClient.delete(`/roles/${id}`),
    assignPermissions: (id: string, permissionIds: string[]) =>
      apiClient.post(`/roles/${id}/permissions`, { permissionIds }),
  },
  permissions: {
    getAll: (params?: Record<string, unknown>) =>
      apiClient.get('/permissions', { params }),
    getTree: () => apiClient.get('/permissions/tree'),
    getMenu: () => apiClient.get('/permissions/menu'),
    getById: (id: string) => apiClient.get(`/permissions/${id}`),
    create: (data: unknown) => apiClient.post('/permissions', data),
    update: (id: string, data: unknown) => apiClient.put(`/permissions/${id}`, data),
    delete: (id: string) => apiClient.delete(`/permissions/${id}`),
  },
  auditLogs: {
    getAll: (params?: Record<string, unknown>) =>
      apiClient.get('/audit-logs', { params }),
    getStatistics: () => apiClient.get('/audit-logs/statistics'),
    export: (data: unknown) => apiClient.post('/audit-logs/export', data),
  },
};
