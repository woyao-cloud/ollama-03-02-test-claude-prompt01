/**
 * User Department Cell Component
 *
 * Displays department information in user tables
 */

'use client';

import React from 'react';
import { Building2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import type { User } from '@/types/user';

export interface UserDepartmentCellProps {
  user: User;
  onDepartmentClick?: (departmentId: string) => void;
}

export const UserDepartmentCell: React.FC<UserDepartmentCellProps> = ({
  user,
  onDepartmentClick,
}) => {
  if (!user.departmentId) {
    return (
      <span className="text-gray-400 text-sm">Not assigned</span>
    );
  }

  const handleClick = () => {
    if (onDepartmentClick && user.departmentId) {
      onDepartmentClick(user.departmentId);
    }
  };

  return (
    <Badge
      variant="secondary"
      className={`flex items-center gap-1 w-fit ${
        onDepartmentClick ? 'cursor-pointer hover:bg-gray-200' : ''
      }`}
      onClick={handleClick}
    >
      <Building2 className="w-3 h-3" />
      <span className="truncate max-w-[150px]">{user.departmentName}</span>
    </Badge>
  );
};

export default UserDepartmentCell;
