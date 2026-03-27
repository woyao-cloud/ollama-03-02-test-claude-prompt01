# Plan 2.6: Frontend Department Management UI

**Phase**: 2 - Department & Advanced
**Status**: Ready for Execution
**Priority**: High
**Estimated Effort**: 1-2 sessions

---

## Objective

Create frontend department management interface including:
- Department tree component (expandable/collapsible tree view)
- Department CRUD management page
- Department-user assignment integration
- Support for drag-and-drop department reordering

---

## Requirements Addressed

- DEPT-01: Department tree visualization
- DEPT-02: Department CRUD operations via UI
- DEPT-05: Department manager assignment UI
- DEPT-06: Department member management UI

---

## Current State

**Backend APIs Ready** (from Plan 2.1):
- `GET /api/v1/departments/tree` - Full department tree
- `GET /api/v1/departments` - Paginated department list
- `POST /api/v1/departments` - Create department
- `PUT /api/v1/departments/{id}` - Update department
- `DELETE /api/v1/departments/{id}` - Delete department
- `POST /api/v1/departments/{id}/move` - Move department
- `POST /api/v1/departments/{id}/manager` - Assign manager

**Frontend Stack**:
- Next.js 16 (App Router)
- TypeScript 5+
- shadcn/ui components
- Tailwind CSS
- Zustand state management

---

## Implementation Tasks

### Task 1: Create Department Type Definitions

**<read_first>**
- `frontend/src/types/user.ts` (existing user types)
- Backend DepartmentDTO structure (from Plan 2.1)
**</read_first>**

**<action>**
Create `frontend/src/types/department.ts` with:
```typescript
interface Department {
  id: string;
  name: string;
  code: string;
  description?: string;
  parentId?: string;
  level: number;
  path: string;
  sortOrder: number;
  managerId?: string;
  managerName?: string;
  status: 'ACTIVE' | 'INACTIVE';
  children?: Department[];
  userCount?: number;
  createdAt: string;
  updatedAt: string;
}

interface DepartmentTreeNode extends Department {
  children: DepartmentTreeNode[];
  isExpanded?: boolean;
  isSelected?: boolean;
}

interface CreateDepartmentRequest {
  name: string;
  code: string;
  description?: string;
  parentId?: string;
  managerId?: string;
  sortOrder?: number;
}

interface UpdateDepartmentRequest {
  name?: string;
  description?: string;
  managerId?: string;
  sortOrder?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}
```
**</action>**

**<acceptance_criteria>**
- `department.ts` exists with all interfaces defined
- `Department` interface matches backend DepartmentDTO fields
- `DepartmentTreeNode` extends Department with UI state properties
- All required request/response types defined
**</acceptance_criteria>**

---

### Task 2: Create Department API Client

**<read_first>**
- `frontend/src/lib/api.ts` (existing API client pattern)
- `frontend/src/types/department.ts` (from Task 1)
**</read_first>**

**<action>**
Create `frontend/src/lib/api/departments.ts` with methods:
- `getDepartmentTree()` - Fetch full tree
- `getDepartments(params)` - Paginated list
- `getDepartmentById(id)` - Single department
- `createDepartment(data)` - Create
- `updateDepartment(id, data)` - Update
- `deleteDepartment(id)` - Delete
- `moveDepartment(id, parentId)` - Move
- `assignManager(id, managerId)` - Assign manager

Use existing API client pattern with authentication headers.
**</action>**

**<acceptance_criteria>**
- `departments.ts` API client exists
- All 8 methods implemented with proper types
- Error handling follows existing pattern
- Uses `/api/v1/departments` base path
**</acceptance_criteria>**

---

### Task 3: Create Department Store (Zustand)

**<read_first>**
- `frontend/src/stores/authStore.ts` (existing store pattern)
- `frontend/src/types/department.ts`
**</read_first>**

**<action>**
Create `frontend/src/stores/departmentStore.ts` with:
```typescript
interface DepartmentState {
  departments: Department[];
  treeData: DepartmentTreeNode[];
  selectedDepartment: Department | null;
  loading: boolean;
  error: string | null;

  // Actions
  fetchTree: () => Promise<void>;
  fetchDepartments: (params?) => Promise<void>;
  selectDepartment: (dept: Department | null) => void;
  createDepartment: (data) => Promise<void>;
  updateDepartment: (id, data) => Promise<void>;
  deleteDepartment: (id) => Promise<void>;
  moveDepartment: (id, parentId) => Promise<void>;
  expandNode: (id) => void;
  collapseNode: (id) => void;
}
```
**</action>**

**<acceptance_criteria>**
- `departmentStore.ts` created with Zustand
- State includes departments, treeData, selectedDepartment, loading, error
- All async actions implemented with loading/error states
- Node expand/collapse actions for tree UI
**</acceptance_criteria>**

---

### Task 4: Create Department Tree Component

**<read_first>**
- shadcn/ui component patterns (if any exist)
- `frontend/src/types/department.ts`
- `frontend/src/stores/departmentStore.ts`
**</read_first>**

**<action>**
Create `frontend/src/components/department/DepartmentTree.tsx`:

Features:
- Recursive tree rendering
- Expand/collapse functionality
- Node selection
- Visual hierarchy indentation
- Department icons (folder icon)
- User count badge
- Status indicator (active/inactive)

Props:
```typescript
interface DepartmentTreeProps {
  data: DepartmentTreeNode[];
  selectedId?: string;
  onSelect: (dept: Department) => void;
  onExpand: (id: string) => void;
  onCollapse: (id: string) => void;
  draggable?: boolean;
  onDrop?: (draggedId: string, targetId: string) => void;
}
```

Use shadcn/ui `Collapsible` or custom implementation.
**</action>**

**<acceptance_criteria>**
- `DepartmentTree.tsx` component exists
- Renders recursive tree structure
- Handles expand/collapse state
- Shows department name, code, user count
- Visual distinction for active/inactive departments
- Selected state visually indicated
**</acceptance_criteria>**

---

### Task 5: Create Department Form Component

**<read_first>**
- shadcn/ui form components (Input, Select, Textarea, Button)
- `frontend/src/types/department.ts`
**</read_first>**

**<action>**
Create `frontend/src/components/department/DepartmentForm.tsx`:

Features:
- Create/edit department
- Form fields: name, code, description, parent, manager
- Parent department dropdown (tree-select style)
- Manager selection dropdown
- Validation
- Submit/cancel actions

Props:
```typescript
interface DepartmentFormProps {
  department?: Department; // undefined for create mode
  onSubmit: (data: CreateDepartmentRequest | UpdateDepartmentRequest) => void;
  onCancel: () => void;
  loading?: boolean;
}
```

Use shadcn/ui components: Form, Input, Textarea, Select, Button.
**</action>**

**<acceptance_criteria>**
- `DepartmentForm.tsx` component exists
- Supports create and edit modes
- All required fields present
- Form validation implemented
- Cancel button functional
- Loading state during submit
**</acceptance_criteria>**

---

### Task 6: Create Department Management Page

**<read_first>**
- `frontend/src/app/users/page.tsx` (existing page pattern)
- Department components from Tasks 4-5
**</read_first>**

**<action>**
Create `frontend/src/app/departments/page.tsx`:

Layout:
- Left sidebar: DepartmentTree (30% width)
- Right panel: Department details/actions (70% width)

Features:
- Tree view on left
- Selected department details on right
- Create button (opens form modal)
- Edit button (opens form modal with data)
- Delete button (with confirmation)
- Move button (drag-drop or modal)
- Member count display
- Manager display

Empty state when no department selected.
**</action>**

**<acceptance_criteria>**
- `/departments` page exists and accessible
- Two-pane layout (tree + details)
- Department selection updates right panel
- CRUD operations functional
- Delete with confirmation dialog
- Toast notifications for actions
**</acceptance_criteria>**

---

### Task 7: Add Department to User Management

**<read_first>**
- `frontend/src/app/users/page.tsx`
- `frontend/src/components/user/UserForm.tsx` (if exists)
**</read_first>**

**<action>**
Update user management to include department:

1. Add department column to user list
2. Add department selector in user create/edit form
3. Filter users by department (optional)

Update files:
- User list table - add "Department" column
- User form - add department dropdown
**</action>**

**<acceptance_criteria>**
- User list shows department name
- User form has department selection
- Department data loads properly
**</acceptance_criteria>**

---

### Task 8: Implement Drag-and-Drop (Optional Enhancement)

**<read_first>**
- `@dnd-kit/core` or similar library
- `DepartmentTree.tsx`
**</read_first>**

**<action>**
Add drag-and-drop for department reordering:

Install: `@dnd-kit/core`, `@dnd-kit/sortable`

Update `DepartmentTree.tsx`:
- Wrap with DndContext
- Make tree nodes draggable
- Handle drop events
- Call `moveDepartment` API on drop

Constraints:
- Max 5 levels
- Cannot drop into own descendant
- Cannot drop when target would exceed max level
**</action>**

**<acceptance_criteria>**
- Drag-and-drop functional (if implemented)
- Level constraints enforced
- API call on successful drop
- Visual feedback during drag
**</acceptance_criteria>**

---

## Verification Criteria

- [ ] All department types defined
- [ ] API client complete with all endpoints
- [ ] Zustand store manages state correctly
- [ ] Tree component renders hierarchy properly
- [ ] Form component validates input
- [ ] Department page functional (CRUD)
- [ ] User management integrates department
- [ ] Responsive design (desktop/tablet)
- [ ] Error handling for all API calls
- [ ] Loading states during async operations

---

## Dependencies

**Required** (must be completed first):
- Plan 2.1: Department Management Module (backend APIs)
- Plan 1.6: Frontend Base Architecture

**Nice to have**:
- shadcn/ui installed
- Existing user management page (for integration reference)

---

## Success Criteria

1. Administrator can view department tree structure
2. Administrator can create/edit/delete departments
3. Department hierarchy visually clear (max 5 levels)
4. User management shows department affiliation
5. UI is responsive and user-friendly

---

*Plan: 06*
*Phase: phase-02-advanced*
*Created: 2026-03-25*
