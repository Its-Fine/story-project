import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'ADMIN' | 'PARTICIPANT';
}

export const ProtectedRoute = ({ children, requiredRole }: ProtectedRouteProps) => {
  const { role, initialized } = useAuth();
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

  return <>{children}</>;
};
