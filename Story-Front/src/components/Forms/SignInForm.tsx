import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../UI/Input';
import { Button } from '../UI/Button';
import { authApi } from '../../api/auth';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './Forms.module.scss';
import { isKeycloakEnabled, updateKeycloakPassword } from '../../auth/keycloak';

interface SignInFormProps {
  showNotification: (message: string, type?: 'success' | 'error') => void;
}

export const SignInForm = ({ showNotification }: SignInFormProps) => {
  const navigate = useNavigate();
  const { signIn, signInWithKeycloak } = useAuth();
  const { t } = useLanguage();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({ email: '', password: '' });
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    const field = id.replace('login-', '');
    setFormData(previous => ({ ...previous, [field]: value }));
    setErrors(previous => ({ ...previous, [field]: '' }));
  };

  const validateForm = () => {
    const nextErrors = { email: '', password: '' };
    if (!formData.email.trim()) nextErrors.email = t('Введите email');
    else if (!validateEmail(formData.email)) nextErrors.email = t('Введите корректный email');
    if (!formData.password) nextErrors.password = t('Введите пароль');
    setErrors(nextErrors);
    return !nextErrors.email && !nextErrors.password;
  };

  const handleKeycloakSignIn = async () => {
    try {
      await signInWithKeycloak();
    } catch {
      showNotification(t('Не удалось открыть вход через Keycloak'), 'error');
    }
  };

  const handlePasswordRecovery = async () => {
    try {
      await updateKeycloakPassword();
    } catch {
      showNotification(t('Не удалось открыть восстановление пароля'), 'error');
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validateForm() || isLoading) return;

    setIsLoading(true);
    try {
      const response = await authApi.login({
        email: formData.email.trim(),
        password: formData.password,
      });

      if (!response.data?.access_token) {
        showNotification(t('Ошибка входа. Проверьте email и пароль'), 'error');
        return;
      }

      const role = await signIn(response.data.access_token);
      showNotification(t('Вход выполнен успешно!'), 'success');
      navigate(role === 'ADMIN' ? '/admin' : '/profile', { replace: true });
    } catch {
      showNotification(t('Ошибка входа. Проверьте email и пароль'), 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form className={css.form} onSubmit={handleSubmit}>
      {isKeycloakEnabled() && (
        <>
          <Button type="button" variant="primary" onClick={() => void handleKeycloakSignIn()}>
            {t('Войти через Keycloak')}
          </Button>
          <div className={css.authDivider}><span>{t('или')}</span></div>
        </>
      )}
      <Input
        label={t('Email')}
        type="email"
        id="login-email"
        value={formData.email}
        onChange={handleChange}
        error={errors.email}
        required
      />
      <Input
        label={t('Пароль')}
        type="password"
        id="login-password"
        value={formData.password}
        onChange={handleChange}
        error={errors.password}
        required
      />
      <div className={css.forgotPassword}>
        <a href="#forgot-password" onClick={(event) => {
          event.preventDefault();
          if (isKeycloakEnabled()) {
            void handlePasswordRecovery();
          } else {
            showNotification(t('Функция восстановления пароля будет доступна позже'), 'error');
          }
        }}>
          {t('Забыли пароль?')}
        </a>
      </div>
      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? t('Вход...') : t('Войти')}
      </Button>
      <p className={css.link}>
        {t('Нет аккаунта?')}
        <a href="#register" onClick={(event) => {
          event.preventDefault();
          window.dispatchEvent(new CustomEvent('story:auth-tab', { detail: 'register' }));
        }}> {t('Зарегистрироваться')}</a>
      </p>
    </form>
  );
};
