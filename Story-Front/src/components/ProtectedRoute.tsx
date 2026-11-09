import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { authApi } from '../api/auth';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'ADMIN' | 'PARTICIPANT';
}

export const ProtectedRoute = ({ children, requiredRole }: ProtectedRouteProps) => {
  const token = localStorage.getItem('token');
  const location = useLocation();
  const [isAuthorized, setIsAuthorized] = useState<boolean | null>(null);
  const [hasRole, setHasRole] = useState<boolean>(true);

  useEffect(() => {
    if (!token) {
      setIsAuthorized(false);
      return;
    }

    // Проверяем роль, если она требуется
    if (requiredRole) {
      authApi.me(token)
        .then((res) => {
          if (res.data?.role === requiredRole) {
            setHasRole(true);
            setIsAuthorized(true);
          } else {
            setHasRole(false);
            setIsAuthorized(true);
          }
        })
        .catch(() => {
          setIsAuthorized(false);
        });
    } else {
      setIsAuthorized(true);
    }
  }, [token, requiredRole]);

  if (isAuthorized === null) {
    return <div>Загрузка...</div>; // Или ваш компонент спиннера
  }

  if (!isAuthorized) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  if (!hasRole) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};