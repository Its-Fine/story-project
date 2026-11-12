import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { SignInForm } from '../../components/Forms/SignInForm';
import { SignUpForm } from '../../components/Forms/SignUpForm';
import { Notification } from '../../components/UI/Notification';
import { Logo } from '../../components/Logo/Logo';
import css from './index.module.scss';

export const MainPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
  const [notification, setNotification] = useState<{
    show: boolean;
    message: string;
    type: 'success' | 'error'
  }>({
    show: false,
    message: '',
    type: 'success'
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      navigate('/profile');
    }
  }, [navigate]);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ show: true, message, type });
  };

  const handleCloseNotification = () => {
    setNotification(prev => ({ ...prev, show: false }));
  };

  return (
    <>
      <DecorLines />
      <div className={css.mainPage}>
        <div className={css.container}>
          <div className={css.content}>
            <div className={css.hero}>
              <Logo size="large" />
              <h1 className={css.title}>Сказка#Дизайн</h1>
              <p className={css.subtitle}>Международный творческий конкурс</p>
            </div>

            <div className={css.formContainer}>
              <div className={css.tabs}>
                <button
                  className={`${css.tab} ${activeTab === 'login' ? css.active : ''}`}
                  onClick={() => setActiveTab('login')}
                >
                  Вход
                </button>
                <button
                  className={`${css.tab} ${activeTab === 'register' ? css.active : ''}`}
                  onClick={() => setActiveTab('register')}
                >
                  Регистрация
                </button>
              </div>

              {activeTab === 'login' ? (
                <SignInForm showNotification={showNotification} />
              ) : (
                <SignUpForm showNotification={showNotification} />
              )}
            </div>
          </div>
        </div>
      </div>

      <Notification
        message={notification.message}
        type={notification.type}
        isVisible={notification.show}
        onClose={handleCloseNotification}
      />
    </>
  );
};