import React from 'react';
import css from './Logo.module.scss';

interface LogoProps {
  size?: 'small' | 'medium' | 'large';
}

export const Logo = ({ size = 'medium' }: LogoProps) => {
  const logoSizes = {
    small: 60,
    medium: 120,
    large: 180
  };

  const dimension = logoSizes[size];

  return (
    <div className={css.logoContainer}>
      <img
        src="/logo.png"
        alt="Сказка#Дизайн"
        width={dimension}
        height={dimension}
        className={css.logo}
      />
    </div>
  );
};