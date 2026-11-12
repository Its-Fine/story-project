import React from 'react';
import css from './Logo.module.scss';

interface LogoProps {
  size?: 'small' | 'medium' | 'large';
}

export const Logo = ({ size = 'medium' }: LogoProps) => {
  const dimensions = {
    small: { width: 60, height: 60 },
    medium: { width: 120, height: 120 },
    large: { width: 410, height: 190 },
  };
  const dimension = dimensions[size];

  return (
    <div className={css.logoContainer}>
      <img
        src="/logo.png"
        alt="Сказка#Дизайн"
        width={dimension.width}
        height={dimension.height}
        className={css.logo}
      />
    </div>
  );
};
