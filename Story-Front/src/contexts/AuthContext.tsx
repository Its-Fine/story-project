import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/auth';

interface AuthContextValue {
  role: string | null;
  initialized: boolean;
  signIn: (accessToken: string) => Promise<string>;
  signOut: () => void;
  refreshSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [role, setRole] = useState<string | null>(null);
  const [initialized, setInitialized] = useState(false);

  const refreshSession = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setRole(null);
      setInitialized(true);
      return;
    }

    try {
      const response = await authApi.me(token);
      if (response.data?.role) {
        setRole(response.data.role);
      } else {
        localStorage.removeItem('token');
        setRole(null);
      }
    } catch {
      localStorage.removeItem('token');
      setRole(null);
    } finally {
      setInitialized(true);
    }
  }, []);

  useEffect(() => {
    void refreshSession();
  }, [refreshSession]);

  const signIn = useCallback(async (accessToken: string) => {
    localStorage.setItem('token', accessToken);
    try {
      const response = await authApi.me(accessToken);
      const nextRole = response.data?.role;
      if (!nextRole) {
        throw new Error(response.error?.message || 'Не удалось определить роль пользователя');
      }

      setRole(nextRole);
      return nextRole;
    } catch (error) {
      localStorage.removeItem('token');
      setRole(null);
      throw error;
    } finally {
      setInitialized(true);
    }
  }, []);

  const signOut = useCallback(() => {
    localStorage.removeItem('token');
    setRole(null);
  }, []);

  const value = useMemo(
    () => ({ role, initialized, signIn, signOut, refreshSession }),
    [role, initialized, signIn, signOut, refreshSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
};
