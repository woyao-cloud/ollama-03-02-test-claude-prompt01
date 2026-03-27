/**
 * Department Tree Component
 *
 * Recursive tree view for displaying department hierarchy
 * Supports expand/collapse, selection, and drag-drop
 */

'use client';

import React, { useState, useCallback } from 'react';
import { ChevronRight, ChevronDown, Folder, FolderOpen, Users } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { DepartmentTreeNode, DepartmentStatus } from '@/types/department';
import { departmentHelpers } from '@/types/department';

export interface DepartmentTreeProps {
  /** Tree data to render */
  data: DepartmentTreeNode[];
  /** Currently selected department ID */
  selectedId?: string;
  /** Callback when a department is selected */
  onSelect?: (dept: DepartmentTreeNode) => void;
  /** Callback when a node is expanded */
  onExpand?: (id: string) => void;
  /** Callback when a node is collapsed */
  onCollapse?: (id: string) => void;
  /** Enable drag and drop */
  draggable?: boolean;
  /** Callback when a node is dropped */
  onDrop?: (draggedId: string, targetId: string) => void;
  /** Additional CSS classes */
  className?: string;
  /** Maximum level to display (for partial tree views) */
  maxLevel?: number;
  /** Show user count badges */
  showUserCount?: boolean;
  /** Show status indicators */
  showStatus?: boolean;
}

interface TreeNodeProps {
  node: DepartmentTreeNode;
  level: number;
  selectedId?: string;
  onSelect?: (dept: DepartmentTreeNode) => void;
  onExpand?: (id: string) => void;
  onCollapse?: (id: string) => void;
  draggable?: boolean;
  onDrop?: (draggedId: string, targetId: string) => void;
  maxLevel?: number;
  showUserCount?: boolean;
  showStatus?: boolean;
}

/**
 * Status Badge Component
 */
const StatusBadge: React.FC<{ status: DepartmentStatus }> = ({ status }) => {
  const colorClass =
    status === 'ACTIVE'
      ? 'bg-green-100 text-green-800 border-green-200'
      : 'bg-gray-100 text-gray-600 border-gray-200';

  return (
    <span
      className={cn(
        'ml-2 px-1.5 py-0.5 text-[10px] font-medium rounded border',
        colorClass
      )}
    >
      {departmentHelpers.getStatusDisplay(status)}
    </span>
  );
};

/**
 * Single Tree Node Component
 */
const TreeNode: React.FC<TreeNodeProps> = ({
  node,
  level,
  selectedId,
  onSelect,
  onExpand,
  onCollapse,
  draggable,
  onDrop,
  maxLevel,
  showUserCount,
  showStatus,
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);

  const hasChildren = node.children && node.children.length > 0;
  const isExpanded = node.isExpanded ?? false;
  const isSelected = node.id === selectedId;

  // Handle expand/collapse
  const handleToggle = useCallback(
    (e: React.MouseEvent) => {
      e.stopPropagation();
      if (hasChildren) {
        if (isExpanded) {
          onCollapse?.(node.id);
        } else {
          onExpand?.(node.id);
        }
      }
    },
    [hasChildren, isExpanded, node.id, onCollapse, onExpand]
  );

  // Handle selection
  const handleSelect = useCallback(() => {
    onSelect?.(node);
  }, [node, onSelect]);

  // Drag handlers
  const handleDragStart = useCallback(
    (e: React.DragEvent) => {
      if (!draggable) return;
      e.dataTransfer.setData('text/plain', node.id);
      e.dataTransfer.effectAllowed = 'move';
      setIsDragging(true);
    },
    [draggable, node.id]
  );

  const handleDragEnd = useCallback(() => {
    setIsDragging(false);
  }, []);

  const handleDragOver = useCallback(
    (e: React.DragEvent) => {
      if (!draggable || !onDrop) return;
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      setIsDragOver(true);
    },
    [draggable, onDrop]
  );

  const handleDragLeave = useCallback(() => {
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      if (!draggable || !onDrop) return;
      e.preventDefault();
      setIsDragOver(false);
      const draggedId = e.dataTransfer.getData('text/plain');
      if (draggedId && draggedId !== node.id) {
        onDrop(draggedId, node.id);
      }
    },
    [draggable, node.id, onDrop]
  );

  // Check if we should render children (respect maxLevel)
  const shouldRenderChildren =
    hasChildren && isExpanded && (!maxLevel || level < maxLevel);

  return (
    <div className="select-none">
      {/* Node Row */}
      <div
        className={cn(
          'flex items-center py-1.5 px-2 cursor-pointer transition-colors',
          'hover:bg-gray-50',
          isSelected && 'bg-blue-50 hover:bg-blue-100',
          isDragging && 'opacity-50',
          isDragOver && 'bg-green-50 border-2 border-dashed border-green-300',
          level === 0 && 'font-medium'
        )}
        style={{ paddingLeft: `${level * 16 + 8}px` }}
        onClick={handleSelect}
        draggable={draggable}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {/* Expand/Collapse Toggle */}
        <button
          onClick={handleToggle}
          className={cn(
            'w-5 h-5 flex items-center justify-center mr-1 rounded',
            'hover:bg-gray-200 transition-colors',
            !hasChildren && 'invisible'
          )}
          type="button"
        >
          {isExpanded ? (
            <ChevronDown className="w-4 h-4 text-gray-500" />
          ) : (
            <ChevronRight className="w-4 h-4 text-gray-500" />
          )}
        </button>

        {/* Folder Icon */}
        <div className="mr-2">
          {isExpanded && hasChildren ? (
            <FolderOpen className="w-5 h-5 text-blue-500" />
          ) : (
            <Folder className="w-5 h-5 text-gray-400" />
          )}
        </div>

        {/* Department Name */}
        <span
          className={cn(
            'flex-1 text-sm truncate',
            isSelected ? 'text-blue-700 font-medium' : 'text-gray-700',
            node.status === 'INACTIVE' && 'text-gray-400'
          )}
        >
          {node.name}
        </span>

        {/* Status Badge */}
        {showStatus && node.status && (
          <StatusBadge status={node.status} />
        )}

        {/* User Count */}
        {showUserCount && node.userCount !== undefined && node.userCount > 0 && (
          <span className="ml-2 flex items-center text-xs text-gray-500">
            <Users className="w-3 h-3 mr-0.5" />
            {node.userCount}
          </span>
        )}
      </div>

      {/* Children */}
      {shouldRenderChildren && (
        <div className="animate-in slide-in-from-top-1 duration-200">
          {node.children!.map((child) => (
            <TreeNode
              key={child.id}
              node={child}
              level={level + 1}
              selectedId={selectedId}
              onSelect={onSelect}
              onExpand={onExpand}
              onCollapse={onCollapse}
              draggable={draggable}
              onDrop={onDrop}
              maxLevel={maxLevel}
              showUserCount={showUserCount}
              showStatus={showStatus}
            />
          ))}
        </div>
      )}
    </div>
  );
};

/**
 * Main Department Tree Component
 */
export const DepartmentTree: React.FC<DepartmentTreeProps> = ({
  data,
  selectedId,
  onSelect,
  onExpand,
  onCollapse,
  draggable,
  onDrop,
  className,
  maxLevel,
  showUserCount = true,
  showStatus = true,
}) => {
  if (!data || data.length === 0) {
    return (
      <div className={cn('p-4 text-center text-gray-500 text-sm', className)}>
        No departments found
      </div>
    );
  }

  return (
    <div className={cn('overflow-auto', className)}>
      {data.map((node) => (
        <TreeNode
          key={node.id}
          node={node}
          level={0}
          selectedId={selectedId}
          onSelect={onSelect}
          onExpand={onExpand}
          onCollapse={onCollapse}
          draggable={draggable}
          onDrop={onDrop}
          maxLevel={maxLevel}
          showUserCount={showUserCount}
          showStatus={showStatus}
        />
      ))}
    </div>
  );
};

export default DepartmentTree;
