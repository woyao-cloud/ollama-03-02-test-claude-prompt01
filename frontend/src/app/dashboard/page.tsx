'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/stores/authStore';
import { showToast } from '@/stores/toastStore';

export default function DashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated, logout, fetchCurrentUser } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    fetchCurrentUser();
  }, [isAuthenticated, router, fetchCurrentUser]);

  const handleLogout = async () => {
    try {
      await logout();
      showToast('success', '已退出登录');
      router.push('/login');
    } catch {
      showToast('error', '退出登录失败');
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">仪表板</h1>
          <div className="flex items-center space-x-4">
            <span className="text-gray-600">{user?.email}</span>
            <Button variant="outline" onClick={handleLogout}>
              退出登录
            </Button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>用户管理</CardTitle>
              <CardDescription>管理用户账号和权限</CardDescription>
            </CardHeader>
            <CardContent>
              <Button className="w-full" onClick={() => router.push('/users')}>
                进入
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>角色管理</CardTitle>
              <CardDescription>管理角色和权限分配</CardDescription>
            </CardHeader>
            <CardContent>
              <Button className="w-full" onClick={() => router.push('/roles')}>
                进入
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>审计日志</CardTitle>
              <CardDescription>查看系统操作记录</CardDescription>
            </CardHeader>
            <CardContent>
              <Button className="w-full" onClick={() => router.push('/audit-logs')}>
                进入
              </Button>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
