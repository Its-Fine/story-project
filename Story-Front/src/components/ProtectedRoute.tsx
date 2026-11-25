import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { landingPath } from '../utils/access';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'ADMIN' | 'PARTICIPANT';
  requiredPermission?: string;
  requiredAnyPermissions?: string[];
}

export const ProtectedRoute = ({ children, requiredRole, requiredPermission, requiredAnyPermissions }: ProtectedRouteProps) => {
  const { role, permissions, initialized } = useAuth();
  const location = useLocation();

  if (!initialized) {
    return <div className="app-loading">Загрузка...</div>;
  }

  if (!role) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to={landingPath(permissions)} replace />;
  }

  if (requiredPermission && !permissions.includes(requiredPermission)) {
    return <Navigate to={landingPath(permissions)} replace />;
  }
  if (requiredAnyPermissions && !requiredAnyPermissions.some(permission => permissions.includes(permission))) {
    return <Navigate to="/profile" replace />;
  }

  return <>{children}</>;
};
