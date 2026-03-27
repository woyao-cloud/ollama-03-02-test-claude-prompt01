/**
 * Department Store
 *
 * Zustand state management for department operations
 */

import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import type {
  Department,
  DepartmentTreeNode,
  DepartmentQueryParams,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
} from '@/types/department';
import { departmentApi } from '@/lib/api/departments';
import { departmentHelpers } from '@/types/department';

export interface DepartmentState {
  // State
  departments: Department[];
  treeData: DepartmentTreeNode[];
  selectedDepartment: Department | null;
  loading: boolean;
  error: string | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;

  // Actions
  fetchTree: (includeInactive?: boolean) => Promise<void>;
  fetchDepartments: (params?: DepartmentQueryParams) => Promise<void>;
  fetchDepartmentById: (id: string) => Promise<Department | null>;
  selectDepartment: (dept: Department | null) => void;
  createDepartment: (data: CreateDepartmentRequest) => Promise<Department | null>;
  updateDepartment: (id: string, data: UpdateDepartmentRequest) => Promise<Department | null>;
  deleteDepartment: (id: string) => Promise<boolean>;
  moveDepartment: (id: string, parentId?: string) => Promise<boolean>;
  assignManager: (id: string, managerId?: string) => Promise<boolean>;
  updateStatus: (id: string, status: 'ACTIVE' | 'INACTIVE') => Promise<boolean>;
  expandNode: (id: string) => void;
  collapseNode: (id: string) => void;
  expandAll: () => void;
  collapseAll: () => void;
  clearError: () => void;
  refreshTree: () => Promise<void>;
}

export const useDepartmentStore = create<DepartmentState>()(
  devtools(
    (set, get) => ({
      // Initial state
      departments: [],
      treeData: [],
      selectedDepartment: null,
      loading: false,
      error: null,
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 20,

      // Fetch department tree
      fetchTree: async (includeInactive = false) => {
        set({ loading: true, error: null });
        try {
          const treeData = await departmentApi.getDepartmentTree(includeInactive);
          set({ treeData, loading: false });
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to fetch department tree',
            loading: false,
          });
        }
      },

      // Fetch paginated departments
      fetchDepartments: async (params = {}) => {
        set({ loading: true, error: null });
        try {
          const response = await departmentApi.getDepartments(params);
          set({
            departments: response.content,
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            currentPage: response.number,
            pageSize: response.size,
            loading: false,
          });
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to fetch departments',
            loading: false,
          });
        }
      },

      // Fetch single department
      fetchDepartmentById: async (id: string) => {
        set({ loading: true, error: null });
        try {
          const department = await departmentApi.getDepartmentById(id);
          set({ loading: false });
          return department;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to fetch department',
            loading: false,
          });
          return null;
        }
      },

      // Select department
      selectDepartment: (dept: Department | null) => {
        const { treeData } = get();

        // Update selection state in tree
        const updateSelection = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] => {
          return nodes.map(node => ({
            ...node,
            isSelected: node.id === dept?.id,
            children: node.children ? updateSelection(node.children) : [],
          }));
        };

        set({
          selectedDepartment: dept,
          treeData: updateSelection(treeData),
        });
      },

      // Create department
      createDepartment: async (data: CreateDepartmentRequest) => {
        set({ loading: true, error: null });
        try {
          const department = await departmentApi.createDepartment(data);
          set({ loading: false });
          // Refresh tree after creation
          await get().refreshTree();
          return department;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to create department',
            loading: false,
          });
          return null;
        }
      },

      // Update department
      updateDepartment: async (id: string, data: UpdateDepartmentRequest) => {
        set({ loading: true, error: null });
        try {
          const department = await departmentApi.updateDepartment(id, data);

          // Update selected department if it's the one being updated
          const { selectedDepartment } = get();
          if (selectedDepartment?.id === id) {
            set({ selectedDepartment: department });
          }

          set({ loading: false });
          // Refresh tree after update
          await get().refreshTree();
          return department;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to update department',
            loading: false,
          });
          return null;
        }
      },

      // Delete department
      deleteDepartment: async (id: string) => {
        set({ loading: true, error: null });
        try {
          await departmentApi.deleteDepartment(id);

          // Clear selection if deleted department was selected
          const { selectedDepartment } = get();
          if (selectedDepartment?.id === id) {
            set({ selectedDepartment: null });
          }

          set({ loading: false });
          // Refresh tree after deletion
          await get().refreshTree();
          return true;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to delete department',
            loading: false,
          });
          return false;
        }
      },

      // Move department
      moveDepartment: async (id: string, parentId?: string) => {
        set({ loading: true, error: null });
        try {
          await departmentApi.moveDepartment(id, parentId);
          set({ loading: false });
          // Refresh tree after move
          await get().refreshTree();
          return true;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to move department',
            loading: false,
          });
          return false;
        }
      },

      // Assign manager
      assignManager: async (id: string, managerId?: string) => {
        set({ loading: true, error: null });
        try {
          await departmentApi.assignManager(id, managerId);

          // Update selected department
          const { selectedDepartment } = get();
          if (selectedDepartment?.id === id) {
            set({
              selectedDepartment: {
                ...selectedDepartment,
                managerId,
              },
            });
          }

          set({ loading: false });
          // Refresh tree after manager assignment
          await get().refreshTree();
          return true;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to assign manager',
            loading: false,
          });
          return false;
        }
      },

      // Update department status
      updateStatus: async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
        set({ loading: true, error: null });
        try {
          await departmentApi.updateDepartmentStatus(id, status);

          // Update selected department
          const { selectedDepartment } = get();
          if (selectedDepartment?.id === id) {
            set({
              selectedDepartment: {
                ...selectedDepartment,
                status,
              },
            });
          }

          set({ loading: false });
          // Refresh tree after status update
          await get().refreshTree();
          return true;
        } catch (err) {
          set({
            error: err instanceof Error ? err.message : 'Failed to update status',
            loading: false,
          });
          return false;
        }
      },

      // Expand node in tree
      expandNode: (id: string) => {
        const { treeData } = get();

        const updateNode = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] => {
          return nodes.map(node => {
            if (node.id === id) {
              return { ...node, isExpanded: true };
            }
            if (node.children) {
              return { ...node, children: updateNode(node.children) };
            }
            return node;
          });
        };

        set({ treeData: updateNode(treeData) });
      },

      // Collapse node in tree
      collapseNode: (id: string) => {
        const { treeData } = get();

        const updateNode = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] => {
          return nodes.map(node => {
            if (node.id === id) {
              return { ...node, isExpanded: false };
            }
            if (node.children) {
              return { ...node, children: updateNode(node.children) };
            }
            return node;
          });
        };

        set({ treeData: updateNode(treeData) });
      },

      // Expand all nodes
      expandAll: () => {
        const { treeData } = get();

        const expandAllNodes = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] => {
          return nodes.map(node => ({
            ...node,
            isExpanded: true,
            children: node.children ? expandAllNodes(node.children) : [],
          }));
        };

        set({ treeData: expandAllNodes(treeData) });
      },

      // Collapse all nodes
      collapseAll: () => {
        const { treeData } = get();

        const collapseAllNodes = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] => {
          return nodes.map(node => ({
            ...node,
            isExpanded: false,
            children: node.children ? collapseAllNodes(node.children) : [],
          }));
        };

        set({ treeData: collapseAllNodes(treeData) });
      },

      // Clear error
      clearError: () => {
        set({ error: null });
      },

      // Refresh tree
      refreshTree: async () => {
        const { fetchTree } = get();
        await fetchTree(true); // Include inactive to show full picture
      },
    }),
    {
      name: 'department-store',
    }
  )
);

export default useDepartmentStore;
