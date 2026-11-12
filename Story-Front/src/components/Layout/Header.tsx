import React from 'react';
import { Link } from 'react-router-dom';
import css from './Header.module.scss';
import { getRoleLabel } from '../../utils/roleLabels';

interface HeaderProps {
  role?: string | null;
}

export const Header = ({ role }: HeaderProps) => {
  const isAdmin = role === 'ADMIN';

  return (
    <header className={css.header}>
      <div className={css.container}>
        <Link to="/" className={css.logo}>
          Сказка#Дизайн
        </Link>
        <nav className={css.nav}>
          <Link to="/profile" className={css.link}>
            Личный кабинет
          </Link>
          {isAdmin && (
            <Link to="/admin" className={`${css.link} ${css.adminLink}`}>
              Админка
            </Link>
          )}
        </nav>
        <div className={css.actions}>
          {role && <span className={css.role}>Роль: {getRoleLabel(role)}</span>}
          <button
            className={css.logout}
            onClick={() => {
              localStorage.removeItem('token');
              window.location.href = '/';
            }}
          >
            Выйти
          </button>
        </div>
      </div>
    </header>
  );
};