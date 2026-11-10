import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../UI/Input';
import { Button } from '../UI/Button';
import { authApi } from '../../api/auth';
import css from './Forms.module.scss';

interface SignUpFormProps {
  showNotification: (message: string, type?: 'success' | 'error') => void;
}

export const SignUpForm = ({ showNotification }: SignUpFormProps) => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    birthDate: '',
    password: '',
    confirmPassword: ''
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (email: string): boolean => {
    const allowedDomains = ['yandex.ru', 'gmail.com', 'mail.ru'];
    if (!email.includes('@')) return false;
    const domain = email.split('@')[1];
    return allowedDomains.includes(domain);
  };

  const calculateAge = (birthDate: string): number => {
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id.replace('register-', '')]: value }));
    setErrors(prev => ({ ...prev, [id.replace('register-', '')]: '' }));
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    if (!formData.firstName) {
      newErrors.firstName = 'Введите имя';
      isValid = false;
    }

    if (!formData.lastName) {
      newErrors.lastName = 'Введите фамилию';
      isValid = false;
    }

    if (!formData.email) {
      newErrors.email = 'Введите email';
      isValid = false;
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Допустимы только домены: @yandex.ru, @gmail.com, @mail.ru';
      isValid = false;
    }

    if (!formData.birthDate) {
      newErrors.birthDate = 'Введите дату рождения';
      isValid = false;
    } else {
      const age = calculateAge(formData.birthDate);
      if (age < 1 || age > 100) {
        newErrors.birthDate = 'Возраст должен быть от 1 до 100 лет';
        isValid = false;
      }
    }

    if (!formData.password) {
      newErrors.password = 'Введите пароль';
      isValid = false;
    } else if (formData.password.length < 6) {
      newErrors.password = 'Пароль должен быть не менее 6 символов';
      isValid = false;
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Подтвердите пароль';
      isValid = false;
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Пароли не совпадают';
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsLoading(true);

    const response = await authApi.register({
      first_name: formData.firstName,
      last_name: formData.lastName,
      email: formData.email,
      birth_date: formData.birthDate,
      password: formData.password,
      password_again: formData.confirmPassword,
      consent: true,
      parent_consent: false
    });

    if (response.status === 200 || response.status === 201) {
      showNotification('Регистрация прошла успешно!', 'success');
      setTimeout(() => {
        navigate('/');
      }, 1500);
    } else {
      showNotification('Ошибка при регистрации', 'error');
      setIsLoading(false);
    }
  };

  return (
    <form className={css.form} onSubmit={handleSubmit}>
      <div className={css.formRow}>
        <Input
          label="Имя"
          id="register-firstName"
          value={formData.firstName}
          onChange={handleChange}
          error={errors.firstName}
          required
        />

        <Input
          label="Фамилия"
          id="register-lastName"
          value={formData.lastName}
          onChange={handleChange}
          error={errors.lastName}
          required
        />
      </div>

      <div className={css.formRow}>
        <Input
          label="Email"
          type="email"
          id="register-email"
          value={formData.email}
          onChange={handleChange}
          error={errors.email}
          required
        />

        <Input
          label="Дата рождения"
          type="date"
          id="register-birthDate"
          value={formData.birthDate}
          onChange={handleChange}
          error={errors.birthDate}
          required
        />
      </div>

      <div className={css.formRow}>
        <Input
          label="Пароль"
          type="password"
          id="register-password"
          value={formData.password}
          onChange={handleChange}
          error={errors.password}
          required
        />

        <Input
          label="Подтверждение"
          type="password"
          id="register-confirmPassword"
          value={formData.confirmPassword}
          onChange={handleChange}
          error={errors.confirmPassword}
          required
        />
      </div>

      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? 'Регистрация...' : 'Зарегистрироваться'}
      </Button>

      <p className={css.link}>
        Уже есть аккаунт?
        <a href="#" onClick={(e) => { e.preventDefault(); window.location.href = '/'; }}> Войти</a>
      </p>

      <div className={css.requiredNote}>
        <span>*</span> Обязательные поля
      </div>
    </form>
  );
};