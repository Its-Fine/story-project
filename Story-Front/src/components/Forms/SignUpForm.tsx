import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../UI/Input';
import { Button } from '../UI/Button';
import { authApi } from '../../api/auth';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './Forms.module.scss';

interface SignUpFormProps {
  showNotification: (message: string, type?: 'success' | 'error') => void;
}

export const SignUpForm = ({ showNotification }: SignUpFormProps) => {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const { t } = useLanguage();
  const [formData, setFormData] = useState({
    firstName: '', lastName: '', email: '', birthDate: '',
    password: '', confirmPassword: '', parentConsent: false
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  const calculateAge = (birthDate: string) => {
    const birth = new Date(`${birthDate}T00:00:00`);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    if (today.getMonth() < birth.getMonth() ||
      (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) age--;
    return age;
  };

  const isMinor = Boolean(formData.birthDate) && calculateAge(formData.birthDate) < 18;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const field = event.target.id.replace('register-', '');
    setFormData(previous => ({ ...previous, [field]: event.target.value }));
    setErrors(previous => ({ ...previous, [field]: '' }));
  };

  const validateForm = () => {
    const nextErrors: Record<string, string> = {};
    if (!formData.firstName.trim()) nextErrors.firstName = t('Введите имя');
    if (!formData.lastName.trim()) nextErrors.lastName = t('Введите фамилию');
    if (!formData.email.trim()) nextErrors.email = t('Введите email');
    else if (!validateEmail(formData.email)) nextErrors.email = t('Введите корректный email');

    if (!formData.birthDate) nextErrors.birthDate = t('Введите дату рождения');
    else {
      const age = calculateAge(formData.birthDate);
      if (age < 1 || age > 100) nextErrors.birthDate = t('Возраст должен быть от 1 до 100 лет');
    }

    if (!formData.password) nextErrors.password = t('Введите пароль');
    else if (formData.password.length < 8) nextErrors.password = t('Пароль должен быть не менее 8 символов');
    if (!formData.confirmPassword) nextErrors.confirmPassword = t('Подтвердите пароль');
    else if (formData.password !== formData.confirmPassword) nextErrors.confirmPassword = t('Пароли не совпадают');
    if (isMinor && !formData.parentConsent) {
      nextErrors.parentConsent = t('Для регистрации до 18 лет необходимо согласие родителя');
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validateForm() || isLoading) return;
    setIsLoading(true);

    try {
      const response = await authApi.register({
        first_name: formData.firstName.trim(),
        last_name: formData.lastName.trim(),
        email: formData.email.trim(),
        birth_date: formData.birthDate,
        password: formData.password,
        password_again: formData.confirmPassword,
        consent: true,
        parent_consent: isMinor ? formData.parentConsent : false,
      });

      if ((response.status === 200 || response.status === 201) && response.data?.access_token) {
        const role = await signIn(response.data.access_token);
        showNotification(t('Регистрация прошла успешно!'), 'success');
        navigate(role === 'ADMIN' ? '/admin' : '/profile', { replace: true });
        return;
      }

      const message = response.error?.message;
      showNotification(
        message === 'User already exists'
          ? t('Этот email уже зарегистрирован. Перейдите во вкладку «Вход».')
          : message === 'For users under 18, parental consent is required'
            ? t('Для участника младше 18 лет требуется согласие родителя.')
            : message || t('Не удалось зарегистрироваться. Проверьте данные и попробуйте ещё раз.'),
        'error'
      );
    } catch (error) {
      showNotification(error instanceof Error ? error.message : t('Не удалось зарегистрироваться. Проверьте данные и попробуйте ещё раз.'), 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form className={css.form} onSubmit={handleSubmit}>
      <div className={css.formRow}>
        <Input label={t('Имя')} id="register-firstName" value={formData.firstName} onChange={handleChange} error={errors.firstName} required />
        <Input label={t('Фамилия')} id="register-lastName" value={formData.lastName} onChange={handleChange} error={errors.lastName} required />
      </div>
      <div className={css.formRow}>
        <Input label={t('Email')} type="email" id="register-email" value={formData.email} onChange={handleChange} error={errors.email} required />
        <Input label={t('Дата рождения')} type="date" id="register-birthDate" value={formData.birthDate} onChange={handleChange} error={errors.birthDate} required />
      </div>
      <div className={css.formRow}>
        <Input label={t('Пароль')} type="password" id="register-password" value={formData.password} onChange={handleChange} error={errors.password} required />
        <Input label={t('Подтверждение')} type="password" id="register-confirmPassword" value={formData.confirmPassword} onChange={handleChange} error={errors.confirmPassword} required />
      </div>
      {isMinor && (
        <div className={css.consentField}>
          <label className={css.consentLabel}>
            <input type="checkbox" checked={formData.parentConsent} onChange={event => {
              setFormData(previous => ({ ...previous, parentConsent: event.target.checked }));
              setErrors(previous => ({ ...previous, parentConsent: '' }));
            }} />
            <span>{t('Родитель или законный представитель дал согласие на регистрацию участника')}</span>
          </label>
          {errors.parentConsent && <div className={css.consentError}>{errors.parentConsent}</div>}
        </div>
      )}
      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? t('Регистрация...') : t('Зарегистрироваться')}
      </Button>
      <p className={css.link}>
        {t('Уже есть аккаунт?')}
        <a href="#login" onClick={event => {
          event.preventDefault();
          window.dispatchEvent(new CustomEvent('story:auth-tab', { detail: 'login' }));
        }}> {t('Войти')}</a>
      </p>
      <div className={css.requiredNote}><span>*</span> {t('Обязательные поля')}</div>
    </form>
  );
};
