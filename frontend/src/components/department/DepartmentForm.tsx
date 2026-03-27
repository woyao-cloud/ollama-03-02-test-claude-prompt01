/**
 * Department Form Component
 *
 * Form for creating and editing departments
 * Supports validation and tree-select for parent department
 */

'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';

import type {
  Department,
  DepartmentTreeNode,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
  DepartmentStatus,
} from '@/types/department';
import { departmentHelpers } from '@/types/department';
import { departmentApi } from '@/lib/api/departments';

// Form validation schema
const departmentFormSchema = z.object({
  name: z
    .string()
    .min(1, 'Department name is required')
    .max(100, 'Department name must be less than 100 characters'),
  code: z
    .string()
    .min(2, 'Department code must be at least 2 characters')
    .max(50, 'Department code must be less than 50 characters')
    .regex(
      /^[a-zA-Z0-9_-]+$/,
      'Code can only contain letters, numbers, hyphens, and underscores'
    ),
  description: z
    .string()
    .max(500, 'Description must be less than 500 characters')
    .optional(),
  parentId: z.string().optional(),
  managerId: z.string().optional(),
  sortOrder: z.coerce.number().min(0).max(9999).default(0),
  status: z.enum(['ACTIVE', 'INACTIVE'] as const).default('ACTIVE'),
});

type DepartmentFormData = z.infer<typeof departmentFormSchema>;

export interface DepartmentFormProps {
  /** Department to edit (undefined for create mode) */
  department?: Department;
  /** Tree data for parent selection */
  treeData?: DepartmentTreeNode[];
  /** Users list for manager selection */
  users?: Array<{ id: string; name: string; email: string }>;
  /** Form submission handler */
  onSubmit: (data: CreateDepartmentRequest | UpdateDepartmentRequest) => void;
  /** Cancel handler */
  onCancel: () => void;
  /** Loading state */
  loading?: boolean;
  /** Additional CSS classes */
  className?: string;
}

/**
 * Flatten tree for parent selection dropdown
 */
const flattenTreeForSelect = (
  nodes: DepartmentTreeNode[],
  level = 0,
  excludeId?: string
): Array<{ id: string; name: string; level: number }> => {
  const result: Array<{ id: string; name: string; level: number }> = [];

  for (const node of nodes) {
    // Skip the current department (can't be its own parent)
    if (node.id !== excludeId) {
      result.push({
        id: node.id,
        name: node.name,
        level,
      });

      // Recursively add children
      if (node.children && node.children.length > 0) {
        result.push(...flattenTreeForSelect(node.children, level + 1, excludeId));
      }
    }
  }

  return result;
};

export const DepartmentForm: React.FC<DepartmentFormProps> = ({
  department,
  treeData = [],
  users = [],
  onSubmit,
  onCancel,
  loading = false,
  className,
}) => {
  const isEditMode = !!department;
  const [codeAvailable, setCodeAvailable] = useState<boolean | null>(null);
  const [checkingCode, setCheckingCode] = useState(false);

  // Initialize form
  const form = useForm<DepartmentFormData>({
    resolver: zodResolver(departmentFormSchema),
    defaultValues: {
      name: '',
      code: '',
      description: '',
      parentId: undefined,
      managerId: undefined,
      sortOrder: 0,
      status: 'ACTIVE',
    },
  });

  // Set form values when editing
  useEffect(() => {
    if (department) {
      form.reset({
        name: department.name,
        code: department.code,
        description: department.description || '',
        parentId: department.parentId || undefined,
        managerId: department.managerId || undefined,
        sortOrder: department.sortOrder,
        status: department.status,
      });
    }
  }, [department, form]);

  // Watch code for availability check
  const watchedCode = form.watch('code');

  // Check code availability
  const checkCodeAvailability = useCallback(
    async (code: string) => {
      if (!code || code.length < 2) return;

      setCheckingCode(true);
      try {
        const available = await departmentApi.checkCodeAvailability(
          code,
          department?.id
        );
        setCodeAvailable(available);
      } catch {
        setCodeAvailable(null);
      } finally {
        setCheckingCode(false);
      }
    },
    [department?.id]
  );

  // Debounced code availability check
  useEffect(() => {
    const timer = setTimeout(() => {
      if (watchedCode && watchedCode !== department?.code) {
        checkCodeAvailability(watchedCode);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [watchedCode, department?.code, checkCodeAvailability]);

  // Handle form submission
  const handleSubmit = (data: DepartmentFormData) => {
    if (isEditMode && department) {
      const updateData: UpdateDepartmentRequest = {
        name: data.name,
        code: data.code,
        description: data.description,
        parentId: data.parentId,
        managerId: data.managerId,
        sortOrder: data.sortOrder,
        status: data.status,
      };
      onSubmit(updateData);
    } else {
      const createData: CreateDepartmentRequest = {
        name: data.name,
        code: data.code,
        description: data.description,
        parentId: data.parentId,
        managerId: data.managerId,
        sortOrder: data.sortOrder,
      };
      onSubmit(createData);
    }
  };

  // Get flattened tree for parent selection
  const parentOptions = flattenTreeForSelect(treeData, 0, department?.id);

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(handleSubmit)}
        className={cn('space-y-4', className)}
      >
        {/* Name Field */}
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                Department Name <span className="text-red-500">*</span>
              </FormLabel>
              <FormControl>
                <Input
                  placeholder="Enter department name"
                  {...field}
                  disabled={loading}
                />
              </FormControl>
              <FormDescription>
                Display name for the department
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Code Field */}
        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                Department Code <span className="text-red-500">*</span>
              </FormLabel>
              <FormControl>
                <div className="relative">
                  <Input
                    placeholder="e.g., HR, ENG, SALES"
                    {...field}
                    disabled={loading || isEditMode}
                    className={cn(
                      codeAvailable === false && 'border-red-500 focus-visible:ring-red-500',
                      codeAvailable === true && 'border-green-500 focus-visible:ring-green-500'
                    )}
                  />
                  {checkingCode && (
                    <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin text-gray-400" />
                  )}
                </div>
              </FormControl>
              <FormDescription>
                Unique code for the department (letters, numbers, hyphens, underscores)
              </FormDescription>
              {codeAvailable === false && !isEditMode && (
                <p className="text-sm text-red-500">This code is already taken</p>
              )}
              {codeAvailable === true && !isEditMode && (
                <p className="text-sm text-green-600">Code is available</p>
              )}
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Description Field */}
        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="Enter department description"
                  {...field}
                  disabled={loading}
                  rows={3}
                />
              </FormControl>
              <FormDescription>Optional description</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Parent Department Field */}
        <FormField
          control={form.control}
          name="parentId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Parent Department</FormLabel>
              <Select
                value={field.value || '__root__'}
                onValueChange={(value) =>
                  field.onChange(value === '__root__' ? undefined : value)
                }
                disabled={loading}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Select parent department (optional)" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="__root__">None (Root Department)</SelectItem>
                  {parentOptions.map((option) => (
                    <SelectItem key={option.id} value={option.id}>
                      {'\u00A0\u00A0'.repeat(option.level)}
                      {option.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                Parent department in the hierarchy (max 5 levels)
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Manager Field */}
        <FormField
          control={form.control}
          name="managerId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Department Manager</FormLabel>
              <Select
                value={field.value || '__none__'}
                onValueChange={(value) =>
                  field.onChange(value === '__none__' ? undefined : value)
                }
                disabled={loading}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Select manager (optional)" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="__none__">None</SelectItem>
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id}>
                      {user.name} ({user.email})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>Department manager (optional)</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Sort Order Field */}
        <FormField
          control={form.control}
          name="sortOrder"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Sort Order</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  min={0}
                  max={9999}
                  {...field}
                  disabled={loading}
                />
              </FormControl>
              <FormDescription>
                Display order (lower numbers appear first)
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Status Field (edit mode only) */}
        {isEditMode && (
          <FormField
            control={form.control}
            name="status"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Status</FormLabel>
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={loading}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="ACTIVE">Active</SelectItem>
                    <SelectItem value="INACTIVE">Inactive</SelectItem>
                  </SelectContent>
                </Select>
                <FormDescription>
                  Inactive departments are hidden from selection
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        {/* Action Buttons */}
        <div className="flex justify-end gap-3 pt-4 border-t">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={
              loading ||
              (!isEditMode && codeAvailable === false)
            }
          >
            {loading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
            {isEditMode ? 'Update Department' : 'Create Department'}
          </Button>
        </div>
      </form>
    </Form>
  );
};

export default DepartmentForm;
