/**
 * Department Type Definitions
 *
 * TypeScript interfaces for department management
 * Mirrors backend DepartmentDTO structure
 */

export type DepartmentStatus = 'ACTIVE' | 'INACTIVE';

export interface Department {
  id: string;
  name: string;
  code: string;
  description?: string;
  parentId?: string;
  parentName?: string;
  managerId?: string;
  managerName?: string;
  level: number;
  path: string;
  sortOrder: number;
  status: DepartmentStatus;
  children?: Department[];
  userCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentTreeNode extends Department {
  children: DepartmentTreeNode[];
  isExpanded?: boolean;
  isSelected?: boolean;
}

export interface CreateDepartmentRequest {
  name: string;
  code: string;
  description?: string;
  parentId?: string;
  managerId?: string;
  sortOrder?: number;
}

export interface UpdateDepartmentRequest {
  name?: string;
  code?: string;
  description?: string;
  parentId?: string;
  managerId?: string;
  sortOrder?: number;
  status?: DepartmentStatus;
}

export interface DepartmentQueryParams {
  name?: string;
  code?: string;
  parentId?: string;
  managerId?: string;
  status?: DepartmentStatus;
  level?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface MoveDepartmentRequest {
  parentId?: string;
}

export interface AssignManagerRequest {
  managerId?: string;
}

export interface DepartmentListResponse {
  content: Department[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DepartmentTreeResponse {
  departments: DepartmentTreeNode[];
}

/**
 * Helper functions for department operations
 */
export const departmentHelpers = {
  /**
   * Check if a department is a root department
   */
  isRoot: (dept: Department): boolean => {
    return !dept.parentId;
  },

  /**
   * Get department display name with code
   */
  getDisplayName: (dept: Department): string => {
    return `${dept.name} (${dept.code})`;
  },

  /**
   * Get status display text
   */
  getStatusDisplay: (status: DepartmentStatus): string => {
    const statusMap: Record<DepartmentStatus, string> = {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
    };
    return statusMap[status] || status;
  },

  /**
   * Get status color for UI
   */
  getStatusColor: (status: DepartmentStatus): string => {
    const colorMap: Record<DepartmentStatus, string> = {
      ACTIVE: 'green',
      INACTIVE: 'gray',
    };
    return colorMap[status] || 'gray';
  },

  /**
   * Flatten tree structure to array
   */
  flattenTree: (nodes: DepartmentTreeNode[]): Department[] => {
    const result: Department[] = [];

    const traverse = (nodeList: DepartmentTreeNode[]) => {
      for (const node of nodeList) {
        const { children, isExpanded, isSelected, ...dept } = node;
        result.push(dept);
        if (children && children.length > 0) {
          traverse(children);
        }
      }
    };

    traverse(nodes);
    return result;
  },

  /**
   * Find department by ID in tree
   */
  findById: (
    nodes: DepartmentTreeNode[],
    id: string
  ): DepartmentTreeNode | undefined => {
    for (const node of nodes) {
      if (node.id === id) {
        return node;
      }
      if (node.children && node.children.length > 0) {
        const found = departmentHelpers.findById(node.children, id);
        if (found) {
          return found;
        }
      }
    }
    return undefined;
  },

  /**
   * Get all descendant IDs (children, grandchildren, etc.)
   */
  getDescendantIds: (node: DepartmentTreeNode): string[] => {
    const ids: string[] = [];

    const collect = (n: DepartmentTreeNode) => {
      if (n.children) {
        for (const child of n.children) {
          ids.push(child.id);
          collect(child);
        }
      }
    };

    collect(node);
    return ids;
  },

  /**
   * Check if dropping a department into a target would create a cycle
   */
  wouldCreateCycle: (
    nodes: DepartmentTreeNode[],
    draggedId: string,
    targetId: string
  ): boolean => {
    const target = departmentHelpers.findById(nodes, targetId);
    if (!target) return false;

    // Check if target is a descendant of dragged node
    const dragged = departmentHelpers.findById(nodes, draggedId);
    if (!dragged) return false;

    const descendantIds = departmentHelpers.getDescendantIds(dragged);
    return descendantIds.includes(targetId);
  },
};
