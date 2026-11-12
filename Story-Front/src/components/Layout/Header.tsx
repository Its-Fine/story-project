import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import css from './Header.module.scss';
import { getRoleLabel } from '../../utils/roleLabels';
import { authApi } from '../../api/auth';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';

interface HeaderProps {
  role?: string | null;
}

export const Header = ({ role }: HeaderProps) => {
  const isAdmin = role === 'ADMIN';
  const navigate = useNavigate();
  const { signOut } = useAuth();
  const { t } = useLanguage();
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    const token = localStorage.getItem('token');
    try {
      if (token) await authApi.logout(token);
    } finally {
      signOut();
      navigate('/', { replace: true });
      setLoggingOut(false);
    }
  };

  return (
    <header className={css.header}>
      <div className={css.container}>
        <Link to="/" className={css.logo}>
          Сказка#Дизайн
        </Link>
        <nav className={css.nav}>
          <Link to="/profile" className={css.link}>
            {t('Личный кабинет')}
          </Link>
          {isAdmin && (
            <Link to="/admin" className={`${css.link} ${css.adminLink}`}>
              {t('Админка')}
            </Link>
          )}
        </nav>
        <div className={css.actions}>
          {role && <span className={css.role}>{t('Роль:')} {t(getRoleLabel(role))}</span>}
          <button className={css.logout} onClick={handleLogout} disabled={loggingOut}>
            {loggingOut ? t('Выход...') : t('Выйти')}
          </button>
        </div>
      </div>
    </header>
  );
};
