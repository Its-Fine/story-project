import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Link } from 'react-router-dom';

interface HeaderProps {
  role?: string | null;
}

export const Header = ({ role }: HeaderProps) => {
  const navigate = () => {
    // placeholder for future navigation logic
  };

  const isAdmin = role === 'ADMIN';

  return (
    <header className="header">
      <div className="header__container">
        <Link to="/" className="header__logo">
          Сказка#Дизайн
        </Link>
        <nav className="header__nav">
          <Link to="/profile" className="header__link">
            Личный кабинет
          </Link>
          {isAdmin && (
            <Link to="/admin" className="header__link header__link--admin">
              Админка
            </Link>
          )}
        </nav>
        <div className="header__actions">
          <span className="header__role">{role ? `Роль: ${role}` : ''}</span>
          <button
            className="header__btn"
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