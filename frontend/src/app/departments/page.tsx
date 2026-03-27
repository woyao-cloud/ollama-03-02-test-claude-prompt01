/**
 * Department Management Page
 *
 * Two-pane layout with department tree on the left
 * and department details/actions on the right
 */

'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  Plus,
  Edit,
  Trash2,
  Users,
  UserCog,
  RefreshCw,
  ChevronRight,
  Folder,
  AlertTriangle,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

import { DepartmentTree } from '@/components/department/DepartmentTree';
import { DepartmentForm } from '@/components/department/DepartmentForm';
import { useDepartmentStore } from '@/stores/departmentStore';
import { useToast } from '@/hooks/use-toast';
import type { Department, DepartmentTreeNode } from '@/types/department';
import { departmentHelpers } from '@/types/department';

export default function DepartmentsPage() {
  const router = useRouter();
  const { toast } = useToast();

  // Store state
  const {
    treeData,
    selectedDepartment,
    loading,
    error,
    fetchTree,
    selectDepartment,
    createDepartment,
    updateDepartment,
    deleteDepartment,
    expandNode,
    collapseNode,
    expandAll,
    collapseAll,
    refreshTree,
  } = useDepartmentStore();

  // Local state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Fetch tree on mount
  useEffect(() => {
    fetchTree(true);
  }, [fetchTree]);

  // Handle errors
  useEffect(() => {
    if (error) {
      toast({
        title: 'Error',
        description: error,
        variant: 'destructive',
      });
    }
  }, [error, toast]);

  // Handle department selection
  const handleSelect = useCallback(
    (dept: DepartmentTreeNode) => {
      selectDepartment(dept);
    },
    [selectDepartment]
  );

  // Handle expand
  const handleExpand = useCallback(
    (id: string) => {
      expandNode(id);
    },
    [expandNode]
  );

  // Handle collapse
  const handleCollapse = useCallback(
    (id: string) => {
      collapseNode(id);
    },
    [collapseNode]
  );

  // Handle refresh
  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    await refreshTree();
    setIsRefreshing(false);
    toast({
      title: 'Refreshed',
      description: 'Department tree has been refreshed.',
    });
  }, [refreshTree, toast]);

  // Handle create
  const handleCreate = useCallback(async (data: Parameters<typeof createDepartment>[0]) => {
    const result = await createDepartment(data);
    if (result) {
      toast({
        title: 'Success',
        description: 'Department created successfully.',
      });
      setIsFormOpen(false);
    }
  }, [createDepartment, toast]);

  // Handle update
  const handleUpdate = useCallback(async (data: Parameters<typeof updateDepartment>[1]) => {
    if (!selectedDepartment) return;
    const result = await updateDepartment(selectedDepartment.id, data);
    if (result) {
      toast({
        title: 'Success',
        description: 'Department updated successfully.',
      });
      setIsFormOpen(false);
    }
  }, [selectedDepartment, updateDepartment, toast]);

  // Handle delete
  const handleDelete = useCallback(async () => {
    if (!selectedDepartment) return;
    const success = await deleteDepartment(selectedDepartment.id);
    if (success) {
      toast({
        title: 'Success',
        description: 'Department deleted successfully.',
      });
      setIsDeleteDialogOpen(false);
    }
  }, [selectedDepartment, deleteDepartment, toast]);

  // Get breadcrumb path
  const getBreadcrumb = (dept: Department): string[] => {
    if (!dept.path) return [dept.name];
    // Path format: /parent-id/current-id/
    const pathIds = dept.path.split('/').filter(Boolean);
    // This is simplified - ideally we'd look up names from tree data
    return pathIds.length > 0
      ? [...pathIds.map(() => '...'), dept.name]
      : [dept.name];
  };

  return (
    <div className="container mx-auto py-6 px-4">
      {/* Page Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Department Management</h1>
          <p className="text-gray-500 mt-1">
            Manage organizational structure and hierarchy
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={handleRefresh}
            disabled={loading || isRefreshing}
          >
            <RefreshCw
              className={`w-4 h-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`}
            />
            Refresh
          </Button>
          <Button onClick={() => setIsFormOpen(true)}>
            <Plus className="w-4 h-4 mr-2" />
            New Department
          </Button>
        </div>
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Panel - Tree View */}
        <Card className="lg:col-span-1 h-[calc(100vh-250px)]">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg flex items-center">
                <Folder className="w-5 h-5 mr-2" />
                Departments
              </CardTitle>
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={expandAll}
                  className="h-8 px-2"
                >
                  Expand
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={collapseAll}
                  className="h-8 px-2"
                >
                  Collapse
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-0 overflow-auto">
            <DepartmentTree
              data={treeData}
              selectedId={selectedDepartment?.id}
              onSelect={handleSelect}
              onExpand={handleExpand}
              onCollapse={handleCollapse}
              showUserCount
              showStatus
              className="py-2"
            />
          </CardContent>
        </Card>

        {/* Right Panel - Details */}
        <Card className="lg:col-span-2 h-[calc(100vh-250px)]">
          {selectedDepartment ? (
            <>
              <CardHeader className="pb-3">
                {/* Breadcrumb */}
                <div className="flex items-center text-sm text-gray-500 mb-2">
                  {getBreadcrumb(selectedDepartment).map((name, index, arr) => (
                    <React.Fragment key={index}>
                      <span>{name}</span>
                      {index < arr.length - 1 && (
                        <ChevronRight className="w-4 h-4 mx-1" />
                      )}
                    </React.Fragment>
                  ))}
                </div>

                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-xl flex items-center gap-2">
                      {selectedDepartment.name}
                      <Badge
                        variant={
                          selectedDepartment.status === 'ACTIVE'
                            ? 'default'
                            : 'secondary'
                        }
                      >
                        {departmentHelpers.getStatusDisplay(
                          selectedDepartment.status
                        )}
                      </Badge>
                    </CardTitle>
                    <p className="text-gray-500 mt-1">
                      Code: {selectedDepartment.code}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setIsFormOpen(true)}
                    >
                      <Edit className="w-4 h-4 mr-1" />
                      Edit
                    </Button>
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => setIsDeleteDialogOpen(true)}
                    >
                      <Trash2 className="w-4 h-4 mr-1" />
                      Delete
                    </Button>
                  </div>
                </div>
              </CardHeader>

              <CardContent className="space-y-6">
                {/* Description */}
                {selectedDepartment.description && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-700 mb-1">
                      Description
                    </h3>
                    <p className="text-gray-600">
                      {selectedDepartment.description}
                    </p>
                  </div>
                )}

                <Separator />

                {/* Stats */}
                <div className="grid grid-cols-3 gap-4">
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center text-gray-500 mb-1">
                      <Users className="w-4 h-4 mr-1" />
                      <span className="text-sm">Members</span>
                    </div>
                    <p className="text-2xl font-semibold">
                      {selectedDepartment.userCount || 0}
                    </p>
                  </div>

                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center text-gray-500 mb-1">
                      <Folder className="w-4 h-4 mr-1" />
                      <span className="text-sm">Level</span>
                    </div>
                    <p className="text-2xl font-semibold">
                      {selectedDepartment.level + 1}
                    </p>
                  </div>

                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center text-gray-500 mb-1">
                      <UserCog className="w-4 h-4 mr-1" />
                      <span className="text-sm">Manager</span>
                    </div>
                    <p className="text-sm font-medium truncate">
                      {selectedDepartment.managerName || 'Not assigned'}
                    </p>
                  </div>
                </div>

                <Separator />

                {/* Additional Info */}
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">Created:</span>
                    <span className="ml-2">
                      {new Date(
                        selectedDepartment.createdAt
                      ).toLocaleDateString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-500">Updated:</span>
                    <span className="ml-2">
                      {new Date(
                        selectedDepartment.updatedAt
                      ).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </CardContent>
            </>
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-gray-400">
              <Folder className="w-16 h-16 mb-4 opacity-50" />
              <p className="text-lg font-medium">No department selected</p>
              <p className="text-sm mt-1">
                Select a department from the tree to view details
              </p>
            </div>
          )}
        </Card>
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedDepartment ? 'Edit Department' : 'Create Department'}
            </DialogTitle>
            <DialogDescription>
              {selectedDepartment
                ? 'Update department information'
                : 'Create a new department in the organization'}
            </DialogDescription>
          </DialogHeader>
          <DepartmentForm
            department={selectedDepartment || undefined}
            treeData={treeData}
            users={[]} // TODO: Fetch users for manager selection
            onSubmit={selectedDepartment ? handleUpdate : handleCreate}
            onCancel={() => setIsFormOpen(false)}
            loading={loading}
          />
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <AlertDialog
        open={isDeleteDialogOpen}
        onOpenChange={setIsDeleteDialogOpen}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-red-500" />
              Delete Department
            </AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete{' '}
              <strong>{selectedDepartment?.name}</strong>? This action cannot be
              undone.
              {(selectedDepartment?.userCount || 0) > 0 && (
                <p className="mt-2 text-red-600">
                  Warning: This department has {selectedDepartment?.userCount}{' '}
                  member(s). Please reassign them first.
                </p>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-500 hover:bg-red-600"
              disabled={loading}
            >
              {loading ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
