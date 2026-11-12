import React from 'react';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './LanguageSwitcher.module.scss';

export const LanguageSwitcher = () => {
  const { language, setLanguage } = useLanguage();

  return (
    <aside className={css.rail} aria-label="Language selector">
      <div className={css.switcher}>
        <button
          type="button"
          className={language === 'ru' ? css.active : ''}
          aria-pressed={language === 'ru'}
          onClick={() => setLanguage('ru')}
        >
          РУС
        </button>
        <span aria-hidden="true" />
        <button
          type="button"
          className={language === 'en' ? css.active : ''}
          aria-pressed={language === 'en'}
          onClick={() => setLanguage('en')}
        >
          ENG
        </button>
      </div>
    </aside>
  );
};
