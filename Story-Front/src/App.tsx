import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { MainPage } from './pages/MainPage';
import { ApplicationPage } from './pages/ApplicationPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage } from './pages/AdminPage';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Header } from './components/Layout/Header';
import { LanguageSwitcher } from './components/Layout/LanguageSwitcher';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { LanguageProvider } from './contexts/LanguageContext';
import './styles/global.scss';

const AppContent = () => {
  const { role, initialized } = useAuth();
  const location = useLocation();

  if (!initialized) {
    return <div className="app-loading">Загрузка...</div>;
  }

  return (
    <>
      <LanguageSwitcher />
      {role && location.pathname !== '/' && location.pathname !== '/profile' && <Header role={role} />}
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
    </>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LanguageProvider>
          <AppContent />
        </LanguageProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
