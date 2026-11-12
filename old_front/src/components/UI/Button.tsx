import React from 'react';
import cn from 'classnames';
import css from './Button.module.scss';

interface ButtonProps {
  type?: 'button' | 'submit' | 'reset';
  variant?: 'primary' | 'secondary' | 'danger';
  onClick?: () => void;
  disabled?: boolean;
  children: React.ReactNode;
  className?: string;
}

export const Button = ({
  type = 'button',
  variant = 'primary',
  onClick,
  disabled,
  children,
  className
}: ButtonProps) => {
  return (
    <button
      type={type}
      className={cn(css.btn, css[variant], className)}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
};