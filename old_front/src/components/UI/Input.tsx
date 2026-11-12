import React from 'react';
import cn from 'classnames';
import css from './Input.module.scss';

interface InputProps {
  label?: string;
  type?: string;
  id?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
  error?: string;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  maxLength?: number;
  className?: string;
}

export const Input = ({
  label,
  type = 'text',
  id,
  value,
  onChange,
  onBlur,
  error,
  required,
  disabled,
  placeholder,
  maxLength,
  className
}: InputProps) => {
  return (
    <div className={cn(css.formGroup, className)}>
      {label && (
        <label htmlFor={id} className={css.label}>
          {label} {required && <span className={css.required}>*</span>}
        </label>
      )}
      <input
        type={type}
        id={id}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        placeholder={placeholder}
        maxLength={maxLength}
        className={cn(css.input, { [css.error]: error })}
      />
      {error && <div className={css.errorMessage}>{error}</div>}
    </div>
  );
};