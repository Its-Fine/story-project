import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { SignInForm } from '../../components/Forms/SignInForm';
import { SignUpForm } from '../../components/Forms/SignUpForm';
import { Notification } from '../../components/UI/Notification';
import { useAuth } from '../../contexts/AuthContext';
import { landingPath } from '../../utils/access';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

export const MainPage = () => {
  const navigate = useNavigate();
  const { role, permissions } = useAuth();
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
  const [notification, setNotification] = useState<{
    show: boolean; message: string; type: 'success' | 'error'
  }>({ show: false, message: '', type: 'success' });

  useEffect(() => {
    if (role) navigate(landingPath(permissions), { replace: true });
  }, [role, permissions, navigate]);

  useEffect(() => {
    const handleTabChange = (event: Event) => {
      const tab = (event as CustomEvent<'login' | 'register'>).detail;
      if (tab === 'login' || tab === 'register') setActiveTab(tab);
    };
    window.addEventListener('story:auth-tab', handleTabChange);
    return () => window.removeEventListener('story:auth-tab', handleTabChange);
  }, []);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ show: true, message, type });
  };

  return (
    <>
      <DecorLines />
      <main className={css.mainPage}>
        <div className={css.container}>
          <div className={css.content}>
            <section className={css.hero}>
              <img
                className={css.heroLogo}
                src="/story-logo.png"
                alt="Международный творческий конкурс Сказка #Дизайн"
              />
            </section>

            <section className={css.formContainer} aria-label={t('Личный кабинет')}>
              <div className={css.tabs}>
                <button type="button" className={`${css.tab} ${activeTab === 'login' ? css.active : ''}`} aria-pressed={activeTab === 'login'} onClick={() => setActiveTab('login')}>
                  {t('Вход')}
                </button>
                <button type="button" className={`${css.tab} ${activeTab === 'register' ? css.active : ''}`} aria-pressed={activeTab === 'register'} onClick={() => setActiveTab('register')}>
                  {t('Регистрация')}
                </button>
              </div>
              {activeTab === 'login'
                ? <SignInForm showNotification={showNotification} />
                : <SignUpForm showNotification={showNotification} />}
            </section>
          </div>
        </div>
      </main>
      <Notification
        message={notification.message}
        type={notification.type}
        isVisible={notification.show}
        onClose={() => setNotification(previous => ({ ...previous, show: false }))}
      />
    </>
  );
};
