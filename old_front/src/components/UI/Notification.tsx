import React, { useEffect } from 'react';
import cn from 'classnames';
import css from './Notification.module.scss';

interface NotificationProps {
  message: string;
  type?: 'success' | 'error';
  isVisible: boolean;
  onClose: () => void;
  duration?: number;
}

export const Notification = ({
  message,
  type = 'success',
  isVisible,
  onClose,
  duration = 3000
}: NotificationProps) => {
  useEffect(() => {
    if (isVisible) {
      const timer = setTimeout(onClose, duration);
      return () => clearTimeout(timer);
    }
  }, [isVisible, onClose, duration]);

  return (
    <div className={cn(css.notification, css[type], { [css.show]: isVisible })}>
      {message}
    </div>
  );
};