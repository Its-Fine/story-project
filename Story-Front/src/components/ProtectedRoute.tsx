import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'ADMIN' | 'PARTICIPANT';
  requiredPermission?: string;
}

export const ProtectedRoute = ({ children, requiredRole, requiredPermission }: ProtectedRouteProps) => {
  const { role, permissions, initialized } = useAuth();
  const location = useLocation();

  if (!initialized) {
    return <div className="app-loading">Загрузка...</div>;
  }

  if (!role) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to={role === 'ADMIN' ? '/admin' : '/profile'} replace />;
  }

  if (requiredPermission && !permissions.includes(requiredPermission)) {
    return <Navigate to={role === 'ADMIN' ? '/admin' : '/profile'} replace />;
  }

  return <>{children}</>;
};
