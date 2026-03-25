/**
 * Department Select Component
 *
 * Dropdown for selecting a department from the tree
 */

'use client';

import React, { useEffect, useState } from 'react';
import { Building2, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import type { Department, DepartmentTreeNode } from '@/types/department';
import { departmentApi } from '@/lib/api/departments';

export interface DepartmentSelectProps {
  /** Selected department ID */
  value?: string;
  /** Callback when selection changes */
  onChange: (departmentId: string | undefined) => void;
  /** Placeholder text */
  placeholder?: string;
  /** Whether selection is required */
  required?: boolean;
  /** Disable the select */
  disabled?: boolean;
  /** Additional CSS classes */
  className?: string;
  /** Include inactive departments */
  includeInactive?: boolean;
  /** Show clear button */
  clearable?: boolean;
}

/**
 * Flatten tree for search
 */
const flattenTree = (
  nodes: DepartmentTreeNode[],
  level = 0
): Array<{ department: DepartmentTreeNode; level: number }> => {
  const result: Array<{ department: DepartmentTreeNode; level: number }> = [];

  for (const node of nodes) {
    result.push({ department: node, level });
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, level + 1));
    }
  }

  return result;
};

export const DepartmentSelect: React.FC<DepartmentSelectProps> = ({
  value,
  onChange,
  placeholder = 'Select department...',
  required = false,
  disabled = false,
  className,
  includeInactive = false,
  clearable = true,
}) => {
  const [open, setOpen] = useState(false);
  const [treeData, setTreeData] = useState<DepartmentTreeNode[]>([]);
  const [loading, setLoading] = useState(false);

  // Fetch department tree
  useEffect(() => {
    const fetchTree = async () => {
      setLoading(true);
      try {
        const data = await departmentApi.getDepartmentTree(includeInactive);
        setTreeData(data);
      } catch (error) {
        console.error('Failed to fetch department tree:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchTree();
  }, [includeInactive]);

  // Get flattened list for display
  const flattenedList = flattenTree(treeData);

  // Find selected department
  const selectedDept = flattenedList.find(
    (item) => item.department.id === value
  )?.department;

  // Handle selection
  const handleSelect = (departmentId: string) => {
    onChange(departmentId);
    setOpen(false);
  };

  // Handle clear
  const handleClear = () => {
    onChange(undefined);
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          disabled={disabled || loading}
          className={cn('w-full justify-between', className)}
        >
          <div className="flex items-center gap-2 overflow-hidden">
            <Building2 className="w-4 h-4 shrink-0 text-gray-500" />
            <span className="truncate">
              {selectedDept ? selectedDept.name : placeholder}
            </span>
          </div>
          <ChevronDown className="w-4 h-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[300px] p-0" align="start">
        <Command>
          <CommandInput placeholder="Search departments..." />
          <CommandList>
            <CommandEmpty>No departments found.</CommandEmpty>
            <CommandGroup>
              {!required && clearable && (
                <CommandItem
                  value=""
                  onSelect={handleClear}
                  className="text-gray-500"
                >
                  None
                </CommandItem>
              )}
              {flattenedList.map(({ department, level }) => (
                <CommandItem
                  key={department.id}
                  value={department.name}
                  onSelect={() => handleSelect(department.id)}
                  className={cn(
                    'flex items-center',
                    department.status === 'INACTIVE' && 'text-gray-400'
                  )}
                >
                  <div
                    className="flex items-center flex-1"
                    style={{ paddingLeft: `${level * 16}px` }}
                  >
                    <Building2 className="w-4 h-4 mr-2 text-gray-400" />
                    <span className="flex-1">{department.name}</span>
                    {department.status === 'INACTIVE' && (
                      <span className="text-xs text-gray-400 ml-2">
                        (inactive)
                      </span>
                    )}
                    {value === department.id && (
                      <span className="text-blue-500 ml-2">✓</span>
                    )}
                  </div>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
};

export default DepartmentSelect;
