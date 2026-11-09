import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { MainPage } from './pages/MainPage';
import { ApplicationPage } from './pages/ApplicationPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage } from './pages/AdminPage';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Header } from './components/Layout/Header';
import { authApi } from './api/auth';
import './styles/global.scss';

function App() {
  const [role, setRole] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    const t = localStorage.getItem('token');
    setToken(t);
    if (t) {
      authApi.me(t)
        .then((res) => {
          if (res.data?.role) {
            setRole(res.data.role);
          }
          setInitialized(true);
        })
        .catch(() => {
          localStorage.removeItem('token');
          setRole(null);
          setInitialized(true);
        });
    } else {
      setInitialized(true);
    }
  }, []);

  if (!initialized) {
    return null;
  }

  return (
    <BrowserRouter>
      <Header role={role} />
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/application/:contestId" element={
          <ProtectedRoute requiredRole="PARTICIPANT">
            <ApplicationPage />
          </ProtectedRoute>
        } />
        <Route path="/profile" element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        } />
        <Route path="/admin" element={
          <ProtectedRoute requiredRole="ADMIN">
            <AdminPage />
          </ProtectedRoute>
        } />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;