import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../UI/Input';
import { Button } from '../UI/Button';
import { authApi } from '../../api/auth';
import css from './Forms.module.scss';

interface SignInFormProps {
  showNotification: (message: string, type?: 'success' | 'error') => void;
}

export const SignInForm = ({ showNotification }: SignInFormProps) => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [errors, setErrors] = useState({
    email: '',
    password: ''
  });
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (email: string): boolean => {
    const allowedDomains = ['yandex.ru', 'gmail.com', 'mail.ru'];
    if (!email.includes('@')) return false;
    const domain = email.split('@')[1];
    return allowedDomains.includes(domain);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id.replace('login-', '')]: value }));
    setErrors(prev => ({ ...prev, [id.replace('login-', '')]: '' }));
  };

  const validateForm = (): boolean => {
    const newErrors = {
      email: '',
      password: ''
    };
    let isValid = true;

    if (!formData.email) {
      newErrors.email = 'Введите email';
      isValid = false;
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Допустимы только домены: @yandex.ru, @gmail.com, @mail.ru';
      isValid = false;
    }

    if (!formData.password) {
      newErrors.password = 'Введите пароль';
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsLoading(true);

    const response = await authApi.login({
      email: formData.email,
      password: formData.password
    });

    if (response.data?.access_token) {
      localStorage.setItem('token', response.data.access_token);
      showNotification('Вход выполнен успешно!', 'success');
      setTimeout(() => {
        navigate('/profile');
      }, 1000);
    } else {
      showNotification('Ошибка входа. Проверьте email и пароль', 'error');
      setIsLoading(false);
    }
  };

  return (
    <form className={css.form} onSubmit={handleSubmit}>
      <Input
        label="Email"
        type="email"
        id="login-email"
        value={formData.email}
        onChange={handleChange}
        error={errors.email}
        required
      />

      <Input
        label="Пароль"
        type="password"
        id="login-password"
        value={formData.password}
        onChange={handleChange}
        error={errors.password}
        required
      />

      <div className={css.forgotPassword}>
        <a href="#" onClick={(e) => { e.preventDefault(); showNotification('Функция восстановления пароля будет доступна позже', 'error'); }}>
          Забыли пароль?
        </a>
      </div>

      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? 'Вход...' : 'Войти'}
      </Button>

      <p className={css.link}>
        Нет аккаунта?
        <a href="#" onClick={(e) => { e.preventDefault(); window.location.href = '/'; }}> Зарегистрироваться</a>
      </p>
    </form>
  );
};